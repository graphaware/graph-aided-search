
/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.integration.es;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.graphaware.integration.es.annotation.SearchBooster;
import com.graphaware.integration.es.annotation.SearchFilter;
import com.graphaware.integration.es.booster.SearchResultBooster;
import com.graphaware.integration.es.domain.RetrySearchException;
import com.graphaware.integration.es.domain.SearchResultModifier;
import com.graphaware.integration.es.filter.SearchResultFilter;
import com.graphaware.integration.es.util.NumberUtil;
import com.graphaware.integration.es.util.PluginClassLoader;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.lookup.SourceLookup;
import org.elasticsearch.search.profile.InternalProfileShardResults;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.netty.ChannelBufferStreamInput;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.graphaware.integration.es.domain.Constants.*;
import static org.elasticsearch.action.search.ShardSearchFailure.readShardSearchFailure;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.script.ScriptService;
import static org.elasticsearch.search.internal.InternalSearchHits.readSearchHits;

public class GraphAidedSearch extends AbstractLifecycleComponent<GraphAidedSearch> {

    private final ClusterService clusterService;
    private final Cache<String, IndexInfo> scriptInfoCache;
    private final Map<Class<? extends SearchResultModifier>, Map<String, ?>> classCache = new HashMap<>();

    private final Client client;

    @Inject
    public GraphAidedSearch(final Settings settings,
            final Client client,
            final ClusterService clusterService,
            final ScriptService scriptService, 
            final ThreadPool threadPool,
            final ActionFilters filters) {
        super(settings);
        this.clusterService = clusterService;
        this.client = client;

        scriptInfoCache = CacheBuilder
                .newBuilder()
                .concurrencyLevel(16)
                .expireAfterAccess(120, TimeUnit.SECONDS)
                .build();
        
        for (final ActionFilter filter : filters.filters()) {
            if (filter instanceof GraphAidedSearchFilter) {
                ((GraphAidedSearchFilter) filter).setGraphAidedSearch(this);
                if (logger.isDebugEnabled()) {
                    logger.debug("Set GraphAidedSearch to " + filter);
                }
            }
        }
    }

    public ActionListener<SearchResponse> wrapActionListener(final String action, final SearchRequest request, final ActionListener<SearchResponse> listener) {
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

        //Necessary to avoid infinite loop
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
        final IndexInfo scriptInfo = getScriptInfo(index);

        try {
            final long startTime = System.nanoTime();
            final Map<String, Object> sourceAsMap = SourceLookup.sourceAsMap(source);
            if (sourceAsMap.get(QUERY_BINARY) != null) {
                String query = new String((byte[]) sourceAsMap.get(QUERY_BINARY));
                logger.warn("Binary query not supported: \n" + query);
            }
            final int size = NumberUtil.getInt(sourceAsMap.get(SIZE), 10);
            final int from = NumberUtil.getInt(sourceAsMap.get(FROM), 0);

            if (size < 0 || from < 0) {
                return null;
            }

            SearchResultBooster booster = get(sourceAsMap, scriptInfo, SearchResultBooster.class, SearchBooster.class, GAS_BOOSTER_CLAUSE);
            SearchResultFilter filter = get(sourceAsMap, scriptInfo, SearchResultFilter.class, SearchFilter.class, GAS_FILTER_CLAUSE);

            if (booster == null && filter == null) {
                return null;
            }

            final XContentBuilder builder = XContentFactory.contentBuilder(Requests.CONTENT_TYPE);

            builder.map(sourceAsMap);
            request.source(builder.bytes());

            final ActionListener<SearchResponse> searchResponseListener = createSearchResponseListener(listener, startTime, booster, filter, scriptInfo);

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
                        newSourceAsMap.put(SIZE, size);
                        newSourceAsMap.put(FROM, from);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Original Query: \n{}\nNew Query: \n{}", sourceAsMap, newSourceAsMap);
                        }
                        try {
                            final XContentBuilder builder = XContentFactory.contentBuilder(Requests.CONTENT_TYPE);
                            builder.map(newSourceAsMap);
                            request.source(builder.bytes());
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
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private ActionListener<SearchResponse> createSearchResponseListener(final ActionListener<SearchResponse> listener, final long startTime, final SearchResultBooster booster, final SearchResultFilter filter, final IndexInfo indexInfo) {
        return new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(final SearchResponse response) {
                if (response.getHits().getTotalHits() == 0) {
                    listener.onResponse(response);
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Reboosting results: {}", response);
                }

                try {
                    if (indexInfo.isEnabled()) {
                        final SearchResponse newResponse = handleResponse(response, startTime, booster, filter);
                        listener.onResponse(newResponse);
                    } else {
                        listener.onResponse(response);
                    }
                } catch (final RetrySearchException e) {
                    throw e;
                } catch (final Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to parse a search response.", e);
                    }
                    throw new RuntimeException("Failed to parse a search response.", e);
                }
            }

            @Override
            public void onFailure(final Throwable e) {
                listener.onFailure(e);
            }
        };
    }

    private SearchResponse handleResponse(final SearchResponse response, final long startTime, final SearchResultBooster booster, final SearchResultFilter filter) throws IOException {
        final BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);

        if (logger.isDebugEnabled()) {
            logger.debug("Reading headers...");
        }

        final ChannelBufferStreamInput in = new ChannelBufferStreamInput(out.bytes().toChannelBuffer());

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
            newHits = AccessController.doPrivileged(new PrivilegedAction<InternalSearchHits>() {
                @Override
                public InternalSearchHits run() {
                    return booster.modify(hits);
                }
            });
        }
        if (filter != null) {
            final InternalSearchHits newHitsFinal = newHits;
            newHits = AccessController.doPrivileged(new PrivilegedAction<InternalSearchHits>() {
                @Override
                public InternalSearchHits run() {
                    return filter.modify(newHitsFinal);
                }
            });
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

        final InternalSearchResponse internalResponse = new InternalSearchResponse(newHits, aggregations, suggest, profileResults, timedOut, terminatedEarly);
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
        final SearchResponse newResponse = new SearchResponse(internalResponse, scrollId, totalShards, successfulShards, tookInMillis, shardFailures);
        if (headers != null) {
            for (final Map.Entry<String, Object> entry : headers
                    .entrySet()) {
                newResponse.putHeader(entry.getKey(),
                        entry.getValue());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Rewriting overhead time: {} - {} = {}ms", tookInMillis, response.getTookInMillis(), tookInMillis - response.getTookInMillis());
        }

        return newResponse;
    }

    public IndexInfo getScriptInfo(final String index) {
        try {
            return scriptInfoCache.get(index, new Callable<IndexInfo>() {
                @Override
                public IndexInfo call() throws Exception {
                    final MetaData metaData = clusterService.state().getMetaData();
                    AliasOrIndex aliasOrIndex = metaData.getAliasAndIndexLookup().get(index);
                    if (aliasOrIndex == null) {
                        return IndexInfo.NO_SCRIPT_INFO;
                    }
                    Settings indexSettings = null;
                    for (IndexMetaData indexMD : aliasOrIndex.getIndices()) {
                        final Settings scriptSettings = indexMD.getSettings();
                        final String script = scriptSettings.get(INDEX_GA_ES_NEO4J_HOST);
                        if (script != null && script.length() > 0) {
                            indexSettings = scriptSettings;
                        }
                    }

                    if (indexSettings == null) {
                        return IndexInfo.NO_SCRIPT_INFO;
                    }

                    return new IndexInfo(indexSettings.get(INDEX_GA_ES_NEO4J_HOST), indexSettings.getAsBoolean(INDEX_GA_ES_NEO4J_ENABLED, false), indexSettings.getAsInt(INDEX_MAX_RESULT_WINDOW, DEFAULT_MAX_RESULT_WINDOW));
                }
            });
        } catch (final Exception e) {
            logger.warn("Failed to load ScriptInfo for {}.", e, index);
            return null;
        }
    }

    private <T extends SearchResultModifier> T get(Map<String, Object> sourceAsMap, IndexInfo indexSettings, final Class<T> clazz, final Class<? extends Annotation> annotationClass, String clause) throws Exception {
        HashMap extParams = (HashMap) sourceAsMap.get(clause);
        if (extParams == null) {
            return null;
        }
        String name = (String) extParams.get(NAME);
        T booster = getPrivileged(name, indexSettings, clazz, annotationClass);
        if (booster == null) {
            logger.warn("No {} found with name {}", clazz.getName(), name);
            sourceAsMap.remove(clause);
            return null;
        }

        booster.parseRequest(sourceAsMap);
        sourceAsMap.remove(clause);
        return booster;
    }

    private <T extends SearchResultModifier> T getPrivileged(final String name, final IndexInfo indexSettings, final Class<T> clazz, final Class<? extends Annotation> annotationClass) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                return instantiate(name, indexSettings, clazz, annotationClass);
            }
        });
    }

    private <T extends SearchResultModifier> T instantiate(String name, IndexInfo indexSettings, Class<T> clazz, Class<? extends Annotation> annotationClass) {
        Map<String, Class<T>> classes = loadCachedClasses(clazz, annotationClass);

        if (classes.isEmpty() || !classes.containsKey(name.toLowerCase())) {
            return null;
        }

        Class<T> filterClass = classes.get(name.toLowerCase());
        T result = null;

        try {
            try {
                Constructor<T> constructor = filterClass.getConstructor(Settings.class, IndexInfo.class);
                result = constructor.newInstance(settings, indexSettings);
            } catch (NoSuchMethodException ex) {
                logger.warn("No constructor with settings for class {}. Using default", filterClass.getName());
                result = filterClass.newInstance();
            } catch (IllegalArgumentException | InvocationTargetException | SecurityException ex) {
                logger.error("Error while creating new instance for {}", filterClass.getName(), ex);
            }
            return result;
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error("Error while initializing new {}", filterClass.getName(), ex);
            return null;
        }
    }

    private <T extends SearchResultModifier> Map<String, Class<T>> loadCachedClasses(Class<T> clazz, Class<? extends Annotation> annotationClass) {
        @SuppressWarnings("unchecked")
        Map<String, Class<T>> cachedMap = (Map<String, Class<T>>) classCache.get(clazz);

        if (cachedMap == null) {
            cachedMap = loadClasses(clazz, annotationClass);
            classCache.put(clazz, cachedMap);
        }

        return cachedMap;
    }

    private <T> Map<String, Class<T>> loadClasses(Class<T> clazz, Class<? extends Annotation> annotationClass) {
        Collection<Class<T>> classes = PluginClassLoader.loadClass(clazz, annotationClass).values();

        Map<String, Class<T>> result = new HashMap<>();

        for (Class<T> cls : classes) {
            Annotation annotation = cls.getAnnotation(annotationClass);
            try {
                Method nameMethod = annotationClass.getDeclaredMethod("name");
                result.put(((String) nameMethod.invoke(annotation)).toLowerCase(), cls);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    @Override
    protected void doStart() {
        
    }

    @Override
    protected void doStop() {
        
    }

    @Override
    protected void doClose() {
        
    }
}
