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
import com.graphaware.integration.es.domain.CypherResult;
import com.graphaware.integration.es.domain.ExternalResult;
import com.graphaware.integration.es.domain.ResultRow;
import com.graphaware.integration.es.util.NumberUtil;
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
        this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, settings);
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
        return getExternalResults(keySet);
    }

    protected Map<String, ExternalResult> getExternalResults(Set<String> keySet) {
        CypherResult externalResult = cypherEndPoint.executeCypher(getNeo4jHost(), cypherQuery, getParameters(keySet));
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
