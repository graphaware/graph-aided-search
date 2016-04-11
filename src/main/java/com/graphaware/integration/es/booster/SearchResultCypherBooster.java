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
import com.graphaware.integration.es.util.UrlUtil;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.util.*;

import static com.graphaware.integration.es.domain.Constants.*;
import static com.graphaware.integration.es.util.ParamUtil.*;

import com.graphaware.integration.es.domain.CypherEndPoint;

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
        return getResults(executeCypher(getNeo4jHost(), keySet, cypherQuery));
    }

    public HashMap<String, ExternalResult> getResults(Map<String, Float> externalResults) {
        HashMap<String, ExternalResult> results = new HashMap<>();
        for (Map.Entry<String, Float> item : externalResults.entrySet()) {
            results.put(item.getKey(), new ExternalResult(item.getKey(), item.getValue()));
        }

        return results;
    }

    protected Map<String, Float> executeCypher(String serverUrl, Set<String> resultKeySet, String cypherStatement) {

        return post(getEndpoint(serverUrl), getCypherQuery(cypherStatement, resultKeySet));
    }

    public String getCypherQuery(String cypherStatement, Set<String> resultKeySet) {
        return cypherEndPoint.buildCypherQuery(cypherStatement, getParameters(resultKeySet));
    }

    public HashMap<String, Object> getParameters(Set<String> resultKeySet) {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("ids", resultKeySet);

        return parameters;
    }

    public String getEndpoint(String serverUrl) {

        return UrlUtil.buildUrlFromParts(serverUrl, CYPHER_ENDPOINT);
    }

    public String getScoreResultName() {
        return null != scoreResultName ? scoreResultName : DEFAULT_SCORE_RESULT_NAME;
    }

    public String getIdResultName() {
        return null != idResultName ? idResultName : DEFAULT_ID_RESULT_NAME;
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
