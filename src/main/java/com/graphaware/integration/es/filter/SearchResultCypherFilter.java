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
import com.graphaware.integration.es.GraphAidedSearch;
import com.graphaware.integration.es.util.NumberUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;

@SearchFilter(name = "SearchResultCypherFilter")
public class SearchResultCypherFilter implements SearchResultFilter {

    private static final Logger logger = Logger.getLogger(SearchResultCypherFilter.class.getName());
    private final String neo4jHost;
    private final int maxResultWindow;

    private int maxResultSize = -1;
    private int size;
    private int from;
    private String cypher;
    private boolean shouldExclude = true;

    public SearchResultCypherFilter(Settings settings, IndexInfo indexSettings) {
        this.neo4jHost = indexSettings.getNeo4jHost();
        this.maxResultWindow = indexSettings.getMaxResultWindow();
    }

    public void parseRequest(Map<String, Object> sourceAsMap) {
        size = NumberUtil.getInt(sourceAsMap.get("size"), 10);
        from = NumberUtil.getInt(sourceAsMap.get("from"), 0);

        HashMap extParams = (HashMap) sourceAsMap.get(GraphAidedSearch.GAS_FILTER_CLAUSE);
        if (extParams != null) {
            cypher = (String) extParams.get("query");
            maxResultSize = NumberUtil.getInt(extParams.get("maxResultSize"), maxResultWindow);
            shouldExclude = extParams.containsKey("exclude") && String.valueOf(extParams.get("exclude")).equalsIgnoreCase("true");
        }
        if (maxResultSize > 0) {
            sourceAsMap.put("size", maxResultSize);
        }
        if (null == cypher) {
            throw new RuntimeException("The Query Parameter is required in gas-filter");
        }
        sourceAsMap.put("from", 0);
    }

    public InternalSearchHits doFilter(final InternalSearchHits hits) {
        Set<String> remoteFilter = executeCypher(neo4jHost, cypher);
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

        logger.log(Level.WARNING, "k <= reorderSize: {0}", (k <= size));

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

    public int getSize() {
        return size;
    }

    public int getFrom() {
        return from;
    }

    public Set<String> executeCypher(String serverUrl, String... cypherStatements) {
        StringBuilder stringBuilder = new StringBuilder("{\"statements\" : [");
        for (String statement : cypherStatements) {
            stringBuilder.append("{\"statement\" : \"").append(statement).append("\"}").append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        stringBuilder.append("]}");

        while (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        return post(serverUrl + "/db/data/transaction/commit", stringBuilder.toString());
    }

    public Set<String> post(String url, String json) {
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(json)
                .post(ClientResponse.class);
        GenericType<Map<String, Object>> type = new GenericType<Map<String, Object>>() {
        };
        Map<String, Object> results = response.getEntity(type);

        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> errors = (ArrayList) results.get("errors");
        if (errors.size() > 0) {
            throw new RuntimeException("Cypher Execution Error, message is : " + errors.get(0).toString());
        }

        Map res = (Map) ((ArrayList) results.get("results")).get(0);

        ArrayList<LinkedHashMap> rows = (ArrayList) res.get("data");
        response.close();
        Set<String> newSet = new HashSet<>();
        for (Iterator<LinkedHashMap> it = rows.iterator(); it.hasNext();) {
            String nodeId = getIdentifier(((ArrayList) (it.next().get("row"))).get(0));
            newSet.add(String.valueOf(nodeId));
        }
        return newSet;
    }

    private static String getIdentifier(Object objectId) {
        if (objectId instanceof String) {
            return (String) objectId;
        }
        return String.valueOf(objectId);
    }

}
