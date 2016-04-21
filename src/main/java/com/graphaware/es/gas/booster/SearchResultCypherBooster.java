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
package com.graphaware.es.gas.booster;

import com.graphaware.es.gas.annotation.SearchBooster;
import com.graphaware.es.gas.cypher.CypherEndPoint;
import com.graphaware.es.gas.cypher.CypherEndPointBuilder;
import com.graphaware.es.gas.cypher.CypherResult;
import com.graphaware.es.gas.cypher.ResultRow;
import com.graphaware.es.gas.domain.ExternalResult;
import com.graphaware.es.gas.domain.IndexInfo;
import com.graphaware.es.gas.util.NumberUtil;
import com.graphaware.es.gas.util.UrlUtil;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.graphaware.es.gas.domain.ClauseConstants.*;
import static com.graphaware.es.gas.util.ParamUtil.extractParameter;

@SearchBooster(name = "SearchResultCypherBooster")
public class SearchResultCypherBooster extends SearchResultExternalBooster {
    private static final String DEFAULT_PROTOCOL = "http";

    private final ESLogger logger;
    private CypherEndPoint cypherEndPoint;
    

    private String cypherQuery;
    private String scoreResultName;
    private String idResultName;

    public SearchResultCypherBooster(Settings settings, IndexInfo indexInfo) {
        super(settings, indexInfo);
        this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, settings);
    }

    @Override
    protected void extendedParseRequest(Map<String, String> extParams) {
        cypherQuery = extractParameter(QUERY, extParams);
        scoreResultName = extractParameter(SCORE_NAME, extParams, DEFAULT_SCORE_RESULT_NAME);
        idResultName = extractParameter(IDENTIFIER, extParams, DEFAULT_ID_RESULT_NAME);
        String protocol = extParams.containsKey(PROTOCOL) ? String.valueOf(extParams.get(PROTOCOL)) : DEFAULT_PROTOCOL;
        createCypherEndPoint(protocol, settings, indexSettings);
    }

    private void createCypherEndPoint(String type, Settings settings, IndexInfo indexSettings) {
        this.cypherEndPoint = new CypherEndPointBuilder(type).settings(settings).indexInfo(indexSettings).build();
    }

    @Override
    protected Map<String, ExternalResult> externalDoReorder(Set<String> keySet) {
        logger.debug("Query cypher for: " + keySet);
        return getExternalResults(keySet);
    }

    protected Map<String, ExternalResult> getExternalResults(Set<String> keySet) {
        CypherResult externalResult = cypherEndPoint.executeCypher(cypherQuery, getParameters(keySet));
        Map<String, ExternalResult> results = new HashMap<>();
        for (ResultRow resultRow : externalResult.getRows()) {
            checkResultRow(resultRow);
            results.put(String.valueOf(resultRow.get(getIdResultName())), new ExternalResult(String.valueOf(resultRow.get(getIdResultName())), NumberUtil.getFloat(resultRow.get(getScoreResultName()))));
        }

        return results;
    }

    public HashMap<String, Object> getParameters(Set<String> resultKeySet) {
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("ids", resultKeySet);

        return parameters;
    }

    public String getEndpoint(String serverUrl) {
        return UrlUtil.buildUrlFromParts(serverUrl);
    }

    public String getScoreResultName() {
        return null != scoreResultName ? scoreResultName : DEFAULT_SCORE_RESULT_NAME;
    }

    public String getIdResultName() {
        return null != idResultName ? idResultName : DEFAULT_ID_RESULT_NAME;
    }

    protected void checkResultRow(ResultRow resultRow) {
        if (!resultRow.getValues().containsKey(getIdResultName())) {
            dispatchInvalidResultException(getIdResultName());
        }
        if (!resultRow.getValues().containsKey(getScoreResultName())) {
            dispatchInvalidResultException(getScoreResultName());
        }
    }

    private void dispatchInvalidResultException(String missingKey) {
        throw new RuntimeException(String.format("The cypher query result must contain the %s column name", missingKey));
    }
}
