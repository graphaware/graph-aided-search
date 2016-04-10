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
package com.graphaware.integration.es.domain;

import static com.graphaware.integration.es.domain.Constants.ERRORS;
import static com.graphaware.integration.es.domain.Constants.INDEX_LOGGER_NAME;
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
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

public class CypherEndPoint {

    private final ESLogger logger;
    private final ClientConfig cfg;
    private final StringBuilder stringBuilder;
    private final ObjectMapper mapper;

    public CypherEndPoint(Settings settings) {
        this.logger = Loggers.getLogger(INDEX_LOGGER_NAME, settings);
        cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        stringBuilder = new StringBuilder();
        mapper = new ObjectMapper();
    }

    public String buildCypherQuery(String cypherQuery) {
        return buildCypherQuery(cypherQuery, new HashMap<String, Object>());
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

    public Map<String, Object> post(String url, String json) {
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = null;
        Map<String, Object> results = null;
        try {
            response = resource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(json)
                    .post(ClientResponse.class);
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
            
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List) results.get(ERRORS);
        if (errors.size() > 0) {
            throw new RuntimeException("Cypher Execution Error, message is : " + errors.get(0).toString());
        }
        
        return results;
    }
}
