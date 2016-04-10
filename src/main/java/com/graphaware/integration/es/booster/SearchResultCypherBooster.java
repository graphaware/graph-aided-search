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
package com.graphaware.integration.es.booster;

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.annotation.SearchBooster;
import com.graphaware.integration.es.domain.ExternalResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.util.*;

import static com.graphaware.integration.es.domain.Constants.*;
import static com.graphaware.integration.es.util.ParamUtil.*;

import com.graphaware.integration.es.domain.CypherEndPoint;
import java.io.IOException;

@SearchBooster(name = "SearchResultCypherBooster")
public class SearchResultCypherBooster extends SearchResultExternalBooster {

    private final ESLogger logger;

    private final CypherEndPoint cypherEndPoint;

    private String cypherQuery;
    private String scoreResultName;
    private String idResultName;

    public SearchResultCypherBooster(Settings settings, IndexInfo indexInfo) {
        super(settings, indexInfo);
        this.logger = Loggers.getLogger(INDEX_LOGGER_NAME, settings);
        this.cypherEndPoint = new CypherEndPoint(settings);
    }

    @Override
    protected void extendedParseRequest(Map<String, String> extParams) {
        cypherQuery = extractParameter(QUERY, extParams);
        scoreResultName = extractParameter(SCORE_NAME, extParams, DEFAULT_SCORE_RESULT_NAME);
        idResultName = extractParameter(IDENTIFIER, extParams, DEFAULT_ID_RESULT_NAME);
    }

    @Override
    protected Map<String, ExternalResult> externalDoReorder(Set<String> keySet) {
        logger.debug("Query cypher for: " + keySet);
        Map<String, Float> res = executeCypher(getNeo4jHost(), keySet, cypherQuery);

        HashMap<String, ExternalResult> results = new HashMap<>();

        for (Map.Entry<String, Float> item : res.entrySet()) {
            results.put(item.getKey(), new ExternalResult(item.getKey(), item.getValue()));
        }
        return results;
    }

    protected Map<String, Float> executeCypher(String serverUrl, Set<String> resultKeySet, String... cypherStatements) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            stringBuilder.append("{\"statements\" : [");
            for (String statement : cypherStatements) {
                stringBuilder.append("{\"statement\" : \"").append(statement).append("\"").append(",");
                stringBuilder.append("\"parameters\":").append("{\"ids\":").append(ObjectMapper.class.newInstance().writeValueAsString(resultKeySet)).append("}").append("}");
            }
            stringBuilder.append("]}");
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            throw new RuntimeException("Unable to build the Cypher query : " + e.getMessage());
        }

        while (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        return post(serverUrl + CYPHER_ENDPOINT, stringBuilder.toString());
    }

    protected Map<String, Float> post(String url, String json) {
        Map<String, Object> results = cypherEndPoint.post(url, json);
        Map res = (Map) ((List) results.get(RESULTS)).get(0);
        List<Map> rows = (List) res.get(DATA);
        List<String> columns = (List) res.get(COLUMNS);
        int k = 0;
        Map<String, Integer> columnsMap = new HashMap<>();
        for (String c : columns) {
            columnsMap.put(c, k);
            ++k;
        }
        Map<String, Float> resultRows = new HashMap<>();
        for (Map r : rows) {
            List row = (List) r.get(ROW);
            String key = String.valueOf(row.get(columnsMap.get(idResultName)));
            float value = Float.parseFloat(String.valueOf(row.get(columnsMap.get(scoreResultName))));
            resultRows.put(key, value);
        }
        return resultRows;
    }
}
