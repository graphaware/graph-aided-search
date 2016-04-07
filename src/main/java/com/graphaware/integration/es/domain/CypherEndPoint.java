/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public CypherEndPoint(Settings settings) {
        this.logger = Loggers.getLogger(INDEX_LOGGER_NAME, settings);
        cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
    }

    public Map<String, Object> post(String url, String json) {
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(json)
                .post(ClientResponse.class);
        GenericType<Map<String, Object>> type = new GenericType<Map<String, Object>>() {
        };
        Map<String, Object> results = response.getEntity(type);
        response.close();
        
        if (logger.isDebugEnabled()) {
            try {
                ObjectMapper oWrapper = ObjectMapper.class.newInstance();
                logger.debug(oWrapper.writeValueAsString(results)); //todo log instead of System.out?
            } catch (InstantiationException | IllegalAccessException | IOException e) {
                //
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> errors = (List) results.get(ERRORS);
        if (errors.size() > 0) {
            throw new RuntimeException("Cypher Execution Error, message is : " + errors.get(0).toString());
        }
        return results;
    }
}
