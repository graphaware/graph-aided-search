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
package com.graphaware.es.gas.cypher;

import com.graphaware.es.gas.domain.IndexInfo;
import com.graphaware.es.gas.util.UrlUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class CypherEndPoint {

    private static final String CYPHER_ENDPOINT = "/db/data/transaction/commit";
    private static final String CYPHER_RESPONSE_RESULTS_FIELD = "results";
    private static final String CYPHER_RESPONSE_DATA_FIELD = "data";
    private static final String CYPHER_RESPONSE_COLUMNS_FIELD = "columns";
    private static final String CYPHER_RESPONSE_ERRORS_FIELD = "errors";
    private static final String CYPHER_RESPONSE_ROW_FIELD = "row";

    private final ESLogger logger;
    private final ClientConfig cfg;
    private final StringBuilder stringBuilder;
    private final ObjectMapper mapper;
    private final String url;
    private final String neo4jPassword;
    private final String neo4jUsername;

    public CypherEndPoint(Settings settings, String neo4jUrl, String neo4jUsername, String neo4jPassword) {
        this.logger = Loggers.getLogger(IndexInfo.INDEX_LOGGER_NAME, settings);
        cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        stringBuilder = new StringBuilder();
        mapper = new ObjectMapper();
        this.url = neo4jUrl;
        this.neo4jUsername = neo4jUsername; 
        this.neo4jPassword = neo4jPassword;
    }

    public String buildCypherQuery(String cypherQuery) {
        return buildCypherQuery(cypherQuery, new HashMap<String, Object>());
    }

    public CypherResult executeCypher(String query, HashMap<String, Object> parameters) {
        HashMap<String, String> headers = new HashMap<>();

        return executeCypher(headers, query, parameters);
    }

    public CypherResult executeCypher(HashMap<String, String> headers, String query, HashMap<String, Object> parameters) {
        String jsonBody = buildCypherQuery(query, parameters);
        String cypherEndpoint = UrlUtil.buildUrlFromParts(url, CYPHER_ENDPOINT);
        Map<String, Object> response = post(cypherEndpoint, headers, jsonBody);
        checkErrors(response);

        return buildCypherResult(response);
    }

    public CypherResult buildCypherResult(Map<String, Object> response) {
        Map res = (Map) ((List) response.get(CYPHER_RESPONSE_RESULTS_FIELD)).get(0);
        List<Map> rows = (List) res.get(CYPHER_RESPONSE_DATA_FIELD);
        List<String> columns = (List) res.get(CYPHER_RESPONSE_COLUMNS_FIELD);
        int k = 0;
        Map<String, Integer> columnsMap = new HashMap<>();
        for (String c : columns) {
            columnsMap.put(c, k);
            ++k;
        }

        CypherResult result = new CypherResult();
        for (Map r : rows) {
            ResultRow resultRow = new ResultRow();
            List row = (List) r.get(CYPHER_RESPONSE_ROW_FIELD);
            for (String key : columns) {
                resultRow.add(key, row.get(columnsMap.get(key)));
            }
            result.addRow(resultRow);
        }

        return result;
    }

    public String buildCypherQuery(String cypherQuery, Map<String, Object> parameters) {
        try {
            stringBuilder.append("{\"statements\" : [");
            stringBuilder.append("{\"statement\" : \"").append(cypherQuery).append("\"");
            if (parameters.size() > 0) {
                stringBuilder.append(",").append("\"parameters\":");
                stringBuilder.append(mapper.writeValueAsString(parameters));
            }
            stringBuilder.append("}]}");

            return stringBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable to build the Cypher query : " + e.getMessage());
        }
    }

    public Map<String, Object> post(String url, HashMap<String, String> headers, String json) {
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && null != neo4jPassword) {
            headers.put(HttpHeaders.AUTHORIZATION, UrlUtil.getAuthorizationHeaderValue(neo4jUsername, neo4jPassword));
        }
        WebResource resource = Client.create(cfg).resource(url);
        WebResource.Builder builder = resource.accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(json);
        for (String k : headers.keySet()) {
            builder.header(k, headers.get(k));
        }
        ClientResponse response = null;
        Map<String, Object> results = null;
        try {
            response = builder.post(ClientResponse.class);
            GenericType<Map<String, Object>> type = new GenericType<Map<String, Object>>() {
            };
            results = response.getEntity(type);
        } finally {
            if (response != null)
                response.close();
        }

        if (logger.isDebugEnabled()) {
            try {
                ObjectMapper oWrapper = ObjectMapper.class.newInstance();
                logger.debug(oWrapper.writeValueAsString(results));
            } catch (InstantiationException | IllegalAccessException | IOException e) {
                //
            }
        }
        if (results == null) {
            logger.error("Null results from cypher endpoint for json:\n" + json);
            throw new RuntimeException("Cypher Execution Error. No results returned");
        }

        return results;
    }

    private void checkErrors(Map<String, Object> results) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List) results.get(CYPHER_RESPONSE_ERRORS_FIELD);
        if (errors.size() > 0) {
            throw new RuntimeException("Cypher Execution Error, message is : " + errors.get(0).toString());
        }
    }
}
