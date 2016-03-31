/*
 * Copyright (c) 2015 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.graphaware.integration.es.plugin.graphbooster;

import com.graphaware.integration.es.plugin.GraphAidedSearchPlugin;
import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author alessandro@graphaware.com
 */
@GraphAidedSearchBooster(name = "GraphAidedSearchNeo4jBooster")
public class GraphAidedSearchNeo4jBooster extends GraphAidedSearchResultBooster {

    private String restEndpoint = null;
    private ESLogger logger;

    public GraphAidedSearchNeo4jBooster(Settings settings, GASIndexInfo indexSettings) {
        super(settings, indexSettings);
        this.logger = Loggers.getLogger(GraphAidedSearchPlugin.INDEX_LOGGER_NAME, settings);
    }

    @Override
    protected Map<String, Neo4JFilterResult> externalDoReorder(Set<String> keySet) {
        logger.warn("Query cypher for: " + keySet);

        String recommendationEndopint = getRestURL()
                + getTargetId();
        
        boolean isFirst = true;
        String ids = "";
        for (String id : keySet) {
            if (!isFirst) {
                ids = ids.concat(",");
            }
            isFirst = false;
            ids = ids.concat(id);
        }
        MultivaluedMap param = new MultivaluedMapImpl();
        param.add("limit", Integer.MAX_VALUE);
        param.add("from", getFrom());
        param.add("keyProperty", getKeyProperty());
        param.add("ids", ids);
        logger.warn("Call: " + recommendationEndopint);

        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        WebResource resource = Client.create(cfg).resource(recommendationEndopint);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, param);
        GenericType<List<Neo4JFilterResult>> type = new GenericType<List<Neo4JFilterResult>>() {
        };
        List<Neo4JFilterResult> res = response.getEntity(type);
        response.close();

        HashMap<String, Neo4JFilterResult> results = new HashMap<>();

        for (Neo4JFilterResult item : res) {
            results.put(item.getItem(), item);
        }

        return results;
    }
    
    @Override
    protected void extendedParseRequest(HashMap extParams) {
        restEndpoint = (String)(extParams.get("neo4j.endpoint"));
    }

    private String getRestURL() {
        String endpoint = getNeo4jHost();
        if (restEndpoint != null) {
            endpoint += restEndpoint;
        } else {
            endpoint += "/graphaware/recommendation/filter";
        }
        return endpoint;
    }

}
