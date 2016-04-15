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
package com.graphaware.integration.es.filter;

import com.graphaware.integration.es.annotation.SearchFilter;
import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.cypher.CypherResult;
import com.graphaware.integration.es.cypher.ResultRow;
import com.graphaware.integration.es.util.NumberUtil;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;

import static com.graphaware.integration.es.domain.ClauseConstants.*;
import com.graphaware.integration.es.cypher.CypherEndPoint;
import static com.graphaware.integration.es.wrap.GraphAidedSearchActionListenerWrapper.GAS_FILTER_CLAUSE;

@SearchFilter(name = "SearchResultCypherFilter")
public class SearchResultCypherFilter implements SearchResultFilter {

    private static final Logger logger = Logger.getLogger(SearchResultCypherFilter.class.getName());

    private static final String DEFAULT_ID_RESULT_NAME = "id";
    private static final String ID_RESULT_NAME_KEY = "identifier";
    private static final int DEFAULT_RESULT_SIZE = 10;
    private static final int DEFAULT_FROM_VALUE = 0;

    private final String neo4jHost;
    private final int maxResultWindow;

    private final CypherEndPoint cypherEndPoint;

    private int maxResultSize = -1;
    private int size;
    private int from;
    private String cypherQuery;
    private boolean shouldExclude = true;
    private String idResultName;

    public SearchResultCypherFilter(Settings settings, IndexInfo indexSettings) {
        this.neo4jHost = indexSettings.getNeo4jHost();
        this.maxResultWindow = indexSettings.getMaxResultWindow();
        this.cypherEndPoint = new CypherEndPoint(settings,
                indexSettings.getNeo4jHost(),
                indexSettings.getNeo4jUsername(),
                indexSettings.getNeo4jPassword());
    }

    @Override
    public void parseRequest(Map<String, Object> sourceAsMap) {
        size = NumberUtil.getInt(sourceAsMap.get(SIZE), DEFAULT_RESULT_SIZE);
        from = NumberUtil.getInt(sourceAsMap.get(FROM), DEFAULT_FROM_VALUE);

        HashMap extParams = (HashMap) sourceAsMap.get(GAS_FILTER_CLAUSE);
        if (extParams != null) {
            cypherQuery = (String) extParams.get(QUERY);
            maxResultSize = NumberUtil.getInt(extParams.get(MAX_RESULT_SIZE), maxResultWindow);
            shouldExclude = extParams.containsKey(EXCLUDE) && String.valueOf(extParams.get(EXCLUDE)).equalsIgnoreCase(TRUE);
            idResultName = extParams.containsKey(ID_RESULT_NAME_KEY) ? String.valueOf(extParams.get(ID_RESULT_NAME_KEY)) : null;
        }
        if (maxResultSize > 0) {
            sourceAsMap.put(SIZE, maxResultSize);
        }
        if (null == cypherQuery) {
            throw new RuntimeException("The Query Parameter is required in gas-filter");
        }
    }

    @Override
    public InternalSearchHits modify(final InternalSearchHits hits) {
        Set<String> remoteFilter = getFilteredItems();
        final InternalSearchHit[] searchHits = hits.internalHits();
        Map<String, InternalSearchHit> hitMap = new HashMap<>();
        for (InternalSearchHit hit : searchHits) {
            hitMap.put(hit.getId(), hit);
        }

        InternalSearchHit[] tmpSearchHits = new InternalSearchHit[hitMap.size()];
        int k = 0;
        float maxScore = -1;
        for (Map.Entry<String, InternalSearchHit> item : hitMap.entrySet()) {
            if ((shouldExclude && !remoteFilter.contains(item.getKey()))
                    || (!shouldExclude && remoteFilter.contains(item.getKey()))) {
                tmpSearchHits[k] = item.getValue();
                k++;
                float score = item.getValue().getScore();
                if (maxScore < score) {
                    maxScore = score;
                }
            }
        }
        int totalSize = k;

        logger.log(Level.FINE, "k <= reorderSize: {0}", (k <= size));

        final int arraySize = (size + from) < k ? size
                : (k - from) > 0 ? (k - from) : 0;
        if (arraySize == 0) {
            return new InternalSearchHits(new InternalSearchHit[0], 0, 0);
        }

        InternalSearchHit[] newSearchHits = new InternalSearchHit[arraySize];
        k = 0;
        for (int i = from; i < arraySize + from; i++) {
            InternalSearchHit newId = tmpSearchHits[i];
            if (newId == null) {
                break;
            }
            newSearchHits[k++] = newId;
        }
        return new InternalSearchHits(newSearchHits, totalSize,
                hits.maxScore());
    }

    protected Set<String> getFilteredItems() {
        CypherResult result = getCypherResult();
        Set<String> filteredItems = new HashSet<>();

        for (ResultRow resultRow : result.getRows()) {
            filteredItems.add(getFilteredItem(resultRow));
        }

        return filteredItems;
    }

    protected CypherResult getCypherResult() {
        return cypherEndPoint.executeCypher(cypherQuery, new HashMap<String, Object>());
    }

    protected String getFilteredItem(ResultRow resultRow) {
        if (!resultRow.getValues().containsKey(getIdResultName())) {
            throw new RuntimeException("The cypher query result must contain the " + getIdResultName() + " column name");
        }

        return getIdentifier(resultRow.get(getIdResultName()));
    }

    public int getSize() {
        return size;
    }

    public int getFrom() {
        return from;
    }

    public String getIdResultName() {
        return null != idResultName ? idResultName : DEFAULT_ID_RESULT_NAME;
    }

    private static String getIdentifier(Object objectId) {
        if (objectId instanceof String) {
            return (String) objectId;
        }
        return String.valueOf(objectId);
    }
}
