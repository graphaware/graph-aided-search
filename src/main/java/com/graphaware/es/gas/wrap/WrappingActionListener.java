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

package com.graphaware.es.gas.wrap;

import com.graphaware.es.gas.domain.IndexInfo;
import com.graphaware.es.gas.modifier.SearchResultModifier;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.search.profile.InternalProfileShardResults;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.transport.netty.ChannelBufferStreamInput;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.action.search.ShardSearchFailure.readShardSearchFailure;
import static org.elasticsearch.search.internal.InternalSearchHits.readSearchHits;
import static org.elasticsearch.search.internal.InternalSearchHits.readSearchHits;

public class WrappingActionListener implements ActionListener<SearchResponse> {

    private final ESLogger logger;
    private final ActionListener<SearchResponse> wrapped;
    private final long startTime;
    private final List<SearchResultModifier> modifiers;
    private final IndexInfo indexInfo;

    public WrappingActionListener(ActionListener<SearchResponse> wrapped, long startTime, List<SearchResultModifier> modifiers, IndexInfo indexInfo, Settings settings) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.wrapped = wrapped;
        this.startTime = startTime;
        this.modifiers = modifiers;
        this.indexInfo = indexInfo;
    }

    @Override
    public void onResponse(final SearchResponse response) {
        if (response.getHits().getTotalHits() == 0 || !indexInfo.isEnabled()) {
            wrapped.onResponse(response);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Boosting results: {}", response);
        }

        try {
            wrapped.onResponse(handleResponse(response, startTime, modifiers));
        } catch (final Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to parse a search response.", e);
            }
            throw new RuntimeException("Failed to parse a search response.", e);
        }
    }

    @Override
    public void onFailure(final Throwable e) {
        wrapped.onFailure(e);
    }

    private SearchResponse handleResponse(final SearchResponse response, final long startTime, final List<SearchResultModifier> modifiers) throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        response.writeTo(out);
        ChannelBufferStreamInput in = new ChannelBufferStreamInput(out.bytes().toChannelBuffer());

        Map<String, Object> headers = readHeaders(in);
        InternalSearchHits hits = modifyHits(modifiers, readHits(in));
        InternalAggregations aggregations = readAggregations(in);
        Suggest suggest = readSuggestions(in);
        Boolean timedOut = in.readBoolean();
        Boolean terminatedEarly = in.readOptionalBoolean();
        InternalProfileShardResults profileResults = readInternalProfileShardResults(in);
        InternalSearchResponse internalResponse =  new InternalSearchResponse(hits, aggregations, suggest, profileResults, timedOut, terminatedEarly);
        SearchResponse newResponse = createNewResponse(startTime, in, internalResponse);

        copyHeaders(headers, newResponse);

        logTime(response, startTime);

        return newResponse;
    }

    private Map<String, Object> readHeaders(ChannelBufferStreamInput in) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Reading headers...");
        }

        Map<String, Object> headers = null;
        if (in.readBoolean()) {
            headers = in.readMap();
        }
        return headers;
    }

    private InternalSearchHits readHits(ChannelBufferStreamInput in) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Reading hits...");
        }

        return readSearchHits(in);
    }

    private InternalSearchHits modifyHits(List<SearchResultModifier> modifiers, InternalSearchHits hits) {
        for (final SearchResultModifier modifier : modifiers) {
            hits = modifier.modify(hits);
        }
        return hits;
    }

    private InternalAggregations readAggregations(ChannelBufferStreamInput in) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Reading aggregations...");
        }

        InternalAggregations aggregations = null;
        if (in.readBoolean()) {
            aggregations = InternalAggregations.readAggregations(in);
        }
        return aggregations;
    }

    private Suggest readSuggestions(ChannelBufferStreamInput in) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Reading suggest...");
        }

        Suggest suggest = null;
        if (in.readBoolean()) {
            suggest = Suggest.readSuggest(Suggest.Fields.SUGGEST, in);
        }
        return suggest;
    }

    private InternalProfileShardResults readInternalProfileShardResults(ChannelBufferStreamInput in) throws IOException {
        
        InternalProfileShardResults profileResults;

        if (in.getVersion().onOrAfter(Version.V_2_2_0) && in.readBoolean()) {
            profileResults = new InternalProfileShardResults(in);
        } else {
            profileResults = null;
        }
        return profileResults;

        //return new InternalSearchResponse(hits, aggregations, suggest, profileResults, timedOut, terminatedEarly);
    }

    private SearchResponse createNewResponse(long startTime, ChannelBufferStreamInput in, InternalSearchResponse internalResponse) throws IOException {
        int totalShards = in.readVInt();
        int successfulShards = in.readVInt();
        int size = in.readVInt();

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

        if (logger.isDebugEnabled()) {
            logger.debug("Creating new SearchResponse...");
        }

        return new SearchResponse(internalResponse, scrollId, totalShards, successfulShards, (System.nanoTime() - startTime) / 1000000, shardFailures);
    }

    private void copyHeaders(Map<String, Object> headers, SearchResponse newResponse) {
        if (headers != null) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                newResponse.putHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    private void logTime(SearchResponse response, long startTime) {
        if (logger.isDebugEnabled()) {
            long tookInMillis = (System.nanoTime() - startTime) / 1000000;
            logger.debug("Rewriting overhead time: {} - {} = {}ms", tookInMillis, response.getTookInMillis(), tookInMillis - response.getTookInMillis());
        }
    }
}
