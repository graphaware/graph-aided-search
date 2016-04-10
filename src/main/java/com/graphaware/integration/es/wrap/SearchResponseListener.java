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

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.domain.RetrySearchException;
import com.graphaware.integration.es.domain.SearchResultModifier;
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

public class SearchResponseListener implements ActionListener<SearchResponse> {

    private final ESLogger logger;
    private final ActionListener<SearchResponse> listener;
    private final long startTime;
    private final List<SearchResultModifier> modifiers;
    private final IndexInfo indexInfo;

    public SearchResponseListener(ActionListener<SearchResponse> listener, long startTime, List<SearchResultModifier> modifiers, IndexInfo indexInfo, Settings settings) {
        this.logger = Loggers.getLogger(getClass(), settings);
        this.listener = listener;
        this.startTime = startTime;
        this.modifiers = modifiers;
        this.indexInfo = indexInfo;
    }

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
                final SearchResponse newResponse = handleResponse(response, startTime, modifiers);
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

    private SearchResponse handleResponse(final SearchResponse response, final long startTime, final List<SearchResultModifier> modifiers) throws IOException {
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

        InternalSearchHits hits = readSearchHits(in);

        for (final SearchResultModifier modifier : modifiers) {
            hits = modifier.modify(hits);
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

        final InternalSearchResponse internalResponse = new InternalSearchResponse(hits, aggregations, suggest, profileResults, timedOut, terminatedEarly);
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
}
