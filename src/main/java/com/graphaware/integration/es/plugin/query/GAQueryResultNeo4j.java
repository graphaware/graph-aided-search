
/*
 * Copyright (c) 2015 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.graphaware.integration.es.plugin.query;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.graphaware.integration.es.plugin.GraphAidedSearchPlugin;
import com.graphaware.integration.es.plugin.annotation.GAGraphBooster;
import com.graphaware.integration.es.plugin.annotation.GAGraphFilter;
import com.graphaware.integration.es.plugin.graphbooster.IGAResultBooster;
import com.graphaware.integration.es.plugin.graphfilter.IGAResultFilter;
import com.graphaware.integration.es.plugin.util.GASUtil;
import com.graphaware.integration.es.plugin.util.GASServiceLoader;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.lookup.SourceLookup;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.threadpool.ThreadPool;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.action.search.ShardSearchFailure.readShardSearchFailure;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import static org.elasticsearch.search.internal.InternalSearchHits.readSearchHits;
import org.elasticsearch.search.profile.InternalProfileShardResults;
import org.elasticsearch.transport.netty.ChannelBufferStreamInput;

public class GAQueryResultNeo4j extends AbstractComponent {

    protected static ESLogger logger;

    public static final String INDEX_GA_ES_NEO4J_ENABLED = "index.ga-es-neo4j.enable";
    public static final String INDEX_MAX_RESULT_WINDOW = "index.max_result_window";
//    public static final String INDEX_GA_ES_NEO4J_REORDER_TYPE = "index.ga-es-neo4j.booster.defaultClass";
//    public static final String INDEX_GA_ES_NEO4J_KEY_PROPERTY = "index.ga-es-neo4j.booster.keyProperty";
    public static final String INDEX_GA_ES_NEO4J_HOST = "index.ga-es-neo4j.neo4j.hostname";
//    public static final String DEFAULT_KEY_PROPERTY = "uuid";

    private static final String GAS_REQUEST = "_gas";

    private static final String GAS_BOOSTER_CLAUSE = "ga-booster";
    private static final String GAS_FILTER_CLAUSE = "ga-filter";

    private final ClusterService clusterService;
    private final boolean enabled;
    private final Cache<String, GASIndexInfo> scriptInfoCache;

    private Client client;

    private Map<String, Class<IGAResultBooster>> boostersClasses;
    private Map<String, Class<IGAResultFilter>> filtersClasses;

    @Inject
    public GAQueryResultNeo4j(final Settings settings,
            final ClusterService clusterService,
            final ThreadPool threadPool) {
        super(settings);
        this.clusterService = clusterService;
        this.logger = Loggers.getLogger(GraphAidedSearchPlugin.INDEX_LOGGER_NAME, settings);
        this.enabled = settings.getAsBoolean(INDEX_GA_ES_NEO4J_ENABLED, false);
        final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .concurrencyLevel(16);
        builder.expireAfterAccess(120, TimeUnit.SECONDS);
        scriptInfoCache = builder.build();
    }

    public ActionListener<SearchResponse> wrapActionListener(
            final String action, final SearchRequest request,
            final ActionListener<SearchResponse> listener) {
        switch (request.searchType()) {
            case DFS_QUERY_AND_FETCH:
            case QUERY_AND_FETCH:
            case QUERY_THEN_FETCH:
                break;
            default:
                return null;
        }

        if (request.scroll() != null) {
            return null;
        }

        //Necessary to avoid ifinite loop
        final Object isGASRequest = request.getHeader(GAS_REQUEST);
        if (isGASRequest instanceof Boolean && !((Boolean) isGASRequest)) {
            return null;
        }

        BytesReference source = request.source();
        if (source == null) {
            source = request.extraSource();
            if (source == null) {
                return null;
            }
        }

        final String[] indices = request.indices();
        if (indices == null || indices.length != 1) {
            return null;
        }

        final String index = indices[0];
        final GASIndexInfo scriptInfo = getScriptInfo(index);

        try {
            final long startTime = System.nanoTime();
            final Map<String, Object> sourceAsMap = SourceLookup.sourceAsMap(source);
            if (sourceAsMap.get("query_binary") != null) {
                String query = new String((byte[]) sourceAsMap.get("query_binary"));
                logger.warn("Binary query not supported: \n" + query);
            }
            final int size = GASUtil.getInt(sourceAsMap.get("size"), 10);
            final int from = GASUtil.getInt(sourceAsMap.get("from"), 0);
//
            if (size < 0 || from < 0) {
                return null;
            }

            IGAResultBooster booster = getGABoosters(sourceAsMap, scriptInfo);
            IGAResultFilter filter = getGAFilters(sourceAsMap, scriptInfo);

            if (booster == null && filter == null) {
                return null;
            }

            final XContentBuilder builder = XContentFactory
                    .contentBuilder(Requests.CONTENT_TYPE);

            builder.map(sourceAsMap);
            request.source(builder.bytes());

            final ActionListener<SearchResponse> searchResponseListener
                    = createSearchResponseListener(listener, startTime, booster, filter, scriptInfo);
            return new ActionListener<SearchResponse>() {
                @Override
                public void onResponse(SearchResponse response) {
                    try {
                        searchResponseListener.onResponse(response);
                    } catch (RetrySearchException e) {
                        Map<String, Object> newSourceAsMap = e.rewrite(sourceAsMap);
                        if (newSourceAsMap == null) {
                            throw new RuntimeException("Failed to rewrite source: " + sourceAsMap);
                        }
                        newSourceAsMap.put("size", size);
                        newSourceAsMap.put("from", from);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Original Query: \n{}\nNew Query: \n{}", sourceAsMap, newSourceAsMap);
                        }
                        try {
                            final XContentBuilder builder = XContentFactory.contentBuilder(Requests.CONTENT_TYPE);
                            builder.map(newSourceAsMap);
                            request.source(builder.bytes());
//                            for (String name : request.getHeaders()) {
//                                if (name.startsWith("filter.codelibs.")) {
//                                    request.putHeader(name, Boolean.FALSE);
//                                }
//                            }
                            request.putHeader(GAS_REQUEST, Boolean.FALSE);
                            client.search(request, listener);
                        } catch (IOException ioe) {
                            throw new RuntimeException("Failed to parse a new source.", ioe);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    searchResponseListener.onFailure(t);
                }
            };
        } catch (final IOException e) {
            throw new RuntimeException("Failed to parse a source.", e);
        }
    }

    private ActionListener<SearchResponse> createSearchResponseListener(
            final ActionListener<SearchResponse> listener, final long startTime, final IGAResultBooster booster, final IGAResultFilter filter, final GASIndexInfo indexInfo) {
        return new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse response) {
                if (response.getHits().getTotalHits() == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No reranking results: {}", response);
                    }
                    listener.onResponse(response);
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Reranking results: {}", response);
                }

                try {
                    if (indexInfo.isEnabled()) {
                        final SearchResponse newResponse = handleResponse(response, startTime, booster, filter);
                        listener.onResponse(newResponse);
                    }
                    else {
                        listener.onResponse(response);
                    }
                } catch (final RetrySearchException e) {
                    throw e;
                } catch (final Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to parse a search response.", e);
                    }
                    throw new RuntimeException(
                            "Failed to parse a search response.", e);
                }
            }

            @Override
            public void onFailure(final Throwable e) {
                listener.onFailure(e);
            }
        };
    }

    private SearchResponse handleResponse(final SearchResponse response, final long startTime, IGAResultBooster booster, IGAResultFilter filter) throws IOException {
        final BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);

        if (logger.isDebugEnabled()) {
            logger.debug("Reading headers...");
        }
        final ChannelBufferStreamInput in = new ChannelBufferStreamInput(
                out.bytes().toChannelBuffer());
        Map<String, Object> headers = null;
        if (in.readBoolean()) {
            headers = in.readMap();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Reading hits...");
        }
        final InternalSearchHits hits = readSearchHits(in);
        InternalSearchHits newHits = hits;
        if (booster != null) {
            newHits = booster.doReorder(hits);
        }
        if (filter != null) {
            newHits = filter.doFilter(newHits);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Reading aggregations...");
        }
        InternalAggregations aggregations = null;
        if (in.readBoolean()) {
            aggregations = InternalAggregations
                    .readAggregations(in);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Reading suggest...");
        }
        Suggest suggest = null;
        if (in.readBoolean()) {
            suggest = Suggest.readSuggest(Suggest.Fields.SUGGEST,
                    in);
        }
        final boolean timedOut = in.readBoolean();

        Boolean terminatedEarly = in.readOptionalBoolean();
        InternalProfileShardResults profileResults;

        if (in.getVersion().onOrAfter(Version.V_2_2_0) && in.readBoolean()) {
            profileResults = new InternalProfileShardResults(in);
        } else {
            profileResults = null;
        }

        final InternalSearchResponse internalResponse = new InternalSearchResponse(
                newHits, aggregations, suggest, profileResults, timedOut,
                terminatedEarly);
        final int totalShards = in.readVInt();
        final int successfulShards = in.readVInt();
        final int size = in.readVInt();
        ShardSearchFailure[] shardFailures;
        if (size == 0) {
            shardFailures = ShardSearchFailure.EMPTY_ARRAY;
        } else {
            shardFailures = new ShardSearchFailure[size];
            for (int i = 0; i < shardFailures.length; i++) {
                shardFailures[i] = readShardSearchFailure(in);
            }
        }
        final String scrollId = in.readOptionalString();
        final long tookInMillis = (System.nanoTime() - startTime) / 1000000;

        if (logger.isDebugEnabled()) {
            logger.debug("Creating new SearchResponse...");
        }
        final SearchResponse newResponse = new SearchResponse(
                internalResponse, scrollId, totalShards,
                successfulShards, tookInMillis, shardFailures);
        if (headers != null) {
            for (final Map.Entry<String, Object> entry : headers
                    .entrySet()) {
                newResponse.putHeader(entry.getKey(),
                        entry.getValue());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Rewriting overhead time: {} - {} = {}ms",
                    tookInMillis, response.getTookInMillis(),
                    tookInMillis - response.getTookInMillis());
        }
        return newResponse;
    }

    public GASIndexInfo getScriptInfo(final String index) {
        try {
            return scriptInfoCache.get(index, new Callable<GASIndexInfo>() {
                @Override
                public GASIndexInfo call() throws Exception {
                    final MetaData metaData = clusterService.state().getMetaData();
                    AliasOrIndex aliasOrIndex = metaData.getAliasAndIndexLookup().get(index);
                    if (aliasOrIndex == null) {
                        return GASIndexInfo.NO_SCRIPT_INFO;
                    }
                    Settings indexSettings = null;
                    for (IndexMetaData indexMD : aliasOrIndex.getIndices()) {
                        final Settings scriptSettings = indexMD.getSettings();
                        final String script = scriptSettings
                                .get(INDEX_GA_ES_NEO4J_HOST);
                        if (script != null && script.length() > 0) {
                            indexSettings = scriptSettings;
                        }
                    }

                    if (indexSettings == null) {
                        return GASIndexInfo.NO_SCRIPT_INFO;
                    }

                    final GASIndexInfo scriptInfo = new GASIndexInfo(indexSettings.get(INDEX_GA_ES_NEO4J_HOST),
                            indexSettings.getAsBoolean(INDEX_GA_ES_NEO4J_ENABLED, false), indexSettings.getAsInt(INDEX_MAX_RESULT_WINDOW, 10000));

                    return scriptInfo;
                }
            });
        } catch (final Exception e) {
            logger.warn("Failed to load ScriptInfo for {}.", e, index);
            return null;
        }
    }

    private IGAResultBooster getGABoosters(Map<String, Object> sourceAsMap, GASIndexInfo indexSettings) {
        HashMap extParams = (HashMap) sourceAsMap.get(GAS_BOOSTER_CLAUSE);
        if (extParams == null) {
            return null;
        }
        String name = (String) extParams.get("name");
        IGAResultBooster booster = getBooster(name, indexSettings);
        if (booster == null) {
            logger.warn("No booster found with name " + name);
            sourceAsMap.remove(GAS_BOOSTER_CLAUSE);
            return null;
        }

        booster.parseRequest(sourceAsMap);
        sourceAsMap.remove(GAS_BOOSTER_CLAUSE);
        return booster;
    }

    private IGAResultFilter getGAFilters(Map<String, Object> sourceAsMap, GASIndexInfo indexSettings) {
        HashMap extParams = (HashMap) sourceAsMap.get(GAS_FILTER_CLAUSE);
        if (extParams == null) {
            return null;
        }
        String name = (String) extParams.get("name");
        IGAResultFilter filter = getFilter(name, indexSettings);
        if (filter == null) {
            logger.warn("No booster found with name " + name);
            sourceAsMap.remove(GAS_FILTER_CLAUSE);
            return null;
        }

        filter.parseRequest(sourceAsMap);
        sourceAsMap.remove(GAS_FILTER_CLAUSE);
        return filter;
    }

    private IGAResultBooster getBooster(String name, GASIndexInfo indexSettings) {
        if (boostersClasses == null) {
            boostersClasses = loadBoosters();
        }

        if (boostersClasses.isEmpty() || !boostersClasses.containsKey(name.toLowerCase())) {
            return null;
        }
        Class< IGAResultBooster> boosterClass = boostersClasses.get(name.toLowerCase());
        IGAResultBooster newBooster = null;
        try {
            try {
                Constructor<IGAResultBooster> constructor = boosterClass.getConstructor(Settings.class, GASIndexInfo.class);
                newBooster = constructor.newInstance(settings, indexSettings);
            } catch (NoSuchMethodException ex) {
                logger.warn("No constructor with settings for class {}. Using default", boosterClass.getName());
                newBooster = boosterClass.newInstance();
            } catch (IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                logger.error("Error while creating new instance for booster {}", boosterClass.getName(), ex);
            }
            return newBooster;
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error("Error while initializing new booster", ex);
            return null;
        }
    }

    private static Map<String, Class<IGAResultBooster>> loadBoosters() {
        logger.warn("Loading booster!!");
        Collection<Class<IGAResultBooster>> boosters = GASServiceLoader.loadClass(IGAResultBooster.class, GAGraphBooster.class).values();

        HashMap<String, Class<IGAResultBooster>> result = new HashMap<>();
        for (Class<IGAResultBooster> boosterClass : boosters) {
            String name = boosterClass.getAnnotation(GAGraphBooster.class).name().toLowerCase();
            logger.warn("Available booster: " + name);
            result.put(name, boosterClass);
        }
        return result;
    }

    private IGAResultFilter getFilter(String name, GASIndexInfo indexSettings) {
        if (filtersClasses == null) {
            filtersClasses = loadFilters();
        }

        if (filtersClasses.isEmpty() || !filtersClasses.containsKey(name.toLowerCase())) {
            return null;
        }
        Class< IGAResultFilter> filterClass = filtersClasses.get(name.toLowerCase());
        IGAResultFilter newFilter = null;
        try {
            try {
                Constructor<IGAResultFilter> constructor = filterClass.getConstructor(Settings.class, GASIndexInfo.class);
                newFilter = constructor.newInstance(settings, indexSettings);
            } catch (NoSuchMethodException ex) {
                logger.warn("No constructor with settings for class {}. Using default", filterClass.getName());
                newFilter = filterClass.newInstance();
            } catch (IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                logger.error("Error while creating new instance for booster {}", filterClass.getName(), ex);
            }
            return newFilter;
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error("Error while initializing new booster", ex);
            return null;
        }
    }

    private static Map<String, Class<IGAResultFilter>> loadFilters() {
        Collection<Class<IGAResultFilter>> filters = GASServiceLoader.loadClass(IGAResultFilter.class, GAGraphFilter.class).values();

        HashMap<String, Class<IGAResultFilter>> result = new HashMap<>();
        for (Class<IGAResultFilter> filterClass : filters) {
            String name = filterClass.getAnnotation(GAGraphFilter.class).name().toLowerCase();
            result.put(name, filterClass);
        }
        return result;
    }
}
