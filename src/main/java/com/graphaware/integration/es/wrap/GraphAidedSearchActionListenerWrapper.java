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
package com.graphaware.integration.es.wrap;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.annotation.SearchBooster;
import com.graphaware.integration.es.annotation.SearchFilter;
import com.graphaware.integration.es.booster.SearchResultBooster;
import com.graphaware.integration.es.domain.PrivilegedSearchResultModifier;
import com.graphaware.integration.es.domain.SearchResultModifier;
import com.graphaware.integration.es.filter.SearchResultFilter;
import com.graphaware.integration.es.util.Instantiator;
import com.graphaware.integration.es.util.NumberUtil;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.graphaware.integration.es.domain.Constants.*;

public class GraphAidedSearchActionListenerWrapper implements ActionListenerWrapper<SearchResponse> {

    private final ESLogger logger;
    private final Settings settings;
    private final Instantiator instantiator;

    private final ClusterService clusterService;
    private final Cache<String, IndexInfo> scriptInfoCache;
    private final Client client;

    public GraphAidedSearchActionListenerWrapper(Settings settings, ClusterService clusterService, Client client) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.settings = settings;
        this.instantiator = new Instantiator(settings);

        this.clusterService = clusterService;
        this.client = client;
        this.scriptInfoCache = CacheBuilder.newBuilder().concurrencyLevel(16).expireAfterAccess(120, TimeUnit.SECONDS).build();
    }

    @Override
    public ActionListener<SearchResponse> wrap(SearchRequest request, ActionListener<SearchResponse> listener) throws CannotWrapException {
        checkCorrectType(request);
        checkScroll(request);
        checkNotAlreadyWrapped(request);
        checkSource(request);
        checkIndex(request);

        final long startTime = System.nanoTime();

        final Map<String, Object> source = SourceLookup.sourceAsMap(request.source());

        warnIfQueryBinary(source);
        final int size = NumberUtil.getInt(source.get(SIZE), 10);
        final int from = NumberUtil.getInt(source.get(FROM), 0);
        checkSizeAndFrom(size, from);

        final IndexInfo scriptInfo = getScriptInfo(request.indices()[0]);

        List<SearchResultModifier> modifiers = produceModifiers(scriptInfo, source);

        request.source(buildBytes(source));

        return createActionListener(request, listener, source, size, from, new WrappingActionListener(listener, startTime, modifiers, scriptInfo, settings));
    }

    private void checkCorrectType(SearchRequest request) throws CannotWrapException {
        switch (request.searchType()) {
            case DFS_QUERY_AND_FETCH:
            case QUERY_AND_FETCH:
            case QUERY_THEN_FETCH:
                return;
            default:
                throw new CannotWrapException("Incorrect type: " + request.searchType());
        }
    }

    private void checkScroll(SearchRequest request) throws CannotWrapException {
        if (request.scroll() != null) {
            throw new CannotWrapException("Has scroll");
        }
    }

    private void checkNotAlreadyWrapped(SearchRequest request) throws CannotWrapException {
        //Necessary to avoid infinite loop
        if (Boolean.FALSE.equals(request.getHeader(GAS_REQUEST))) {
            throw new CannotWrapException("Already wrapped");
        }
    }

    private void checkSource(SearchRequest request) throws CannotWrapException {
        BytesReference source = request.source();
        if (source == null) {
            source = request.extraSource();
            if (source == null) {
                throw new CannotWrapException("No source");
            }
        }
    }

    private void checkIndex(SearchRequest request) throws CannotWrapException {
        final String[] indices = request.indices();
        if (indices == null || indices.length != 1) {
            throw new CannotWrapException("Does not have a single index");
        }
    }

    private void warnIfQueryBinary(Map<String, Object> sourceAsMap) {
        if (sourceAsMap.get(QUERY_BINARY) != null) {
            String query = new String((byte[]) sourceAsMap.get(QUERY_BINARY));
            logger.warn("Binary query not supported: \n" + query);
        }
    }

    private void checkSizeAndFrom(int size, int from) throws CannotWrapException {
        if (size < 0 || from < 0) {
            throw new CannotWrapException("Negative size or from");
        }
    }

    private List<SearchResultModifier> produceModifiers(IndexInfo scriptInfo, Map<String, Object> source) throws CannotWrapException {
        List<SearchResultModifier> modifiers = new LinkedList<>();

        SearchResultBooster booster = instantiator.instantiate(GAS_BOOSTER_CLAUSE, source, scriptInfo, SearchResultBooster.class, SearchBooster.class);
        if (booster != null) {
            modifiers.add(new PrivilegedSearchResultModifier(booster));
        }

        SearchResultFilter filter = instantiator.instantiate(GAS_FILTER_CLAUSE, source, scriptInfo, SearchResultFilter.class, SearchFilter.class);
        if (filter != null) {
            modifiers.add(new PrivilegedSearchResultModifier(filter));
        }

        if (modifiers.isEmpty()) {
            throw new CannotWrapException("No modifiers");
        }

        return modifiers;
    }

    private BytesReference buildBytes(Map<String, Object> source) {
        final XContentBuilder builder;

        try {
            builder = XContentFactory.contentBuilder(Requests.CONTENT_TYPE);
            builder.map(source);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.bytes();
    }

    private ActionListener<SearchResponse> createActionListener(final SearchRequest request, final ActionListener<SearchResponse> listener, final Map<String, Object> source, final int size, final int from, final ActionListener<SearchResponse> searchResponseListener) {
        return new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse response) {
                searchResponseListener.onResponse(response);
            }

            @Override
            public void onFailure(Throwable t) {
                searchResponseListener.onFailure(t);
            }
        };
    }

    private IndexInfo getScriptInfo(final String index) {
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

                    return new IndexInfo(indexSettings.get(INDEX_GA_ES_NEO4J_HOST),
                            indexSettings.get(INDEX_GA_ES_NEO4J_USER),
                            indexSettings.get(INDEX_GA_ES_NEO4J_PWD),
                            indexSettings.getAsBoolean(INDEX_GA_ES_NEO4J_ENABLED, false),
                            indexSettings.getAsInt(INDEX_MAX_RESULT_WINDOW,
                                    DEFAULT_MAX_RESULT_WINDOW));
                }
            });
        } catch (final Exception e) {
            logger.warn("Failed to load ScriptInfo for {}.", e, index);
            return null;
        }
    }
}
