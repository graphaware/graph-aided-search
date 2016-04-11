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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;

import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.graphaware.integration.es.domain.Constants.*;

@SearchBooster(name = "SearchResultNeo4jBooster")
public class SearchResultNeo4jBooster extends SearchResultExternalBooster {

    private String boosterEndpoint = null;
    private final ESLogger logger;
    private String targetId;
    private String keyProperty;

    public SearchResultNeo4jBooster(Settings settings, IndexInfo indexSettings) {
        super(settings, indexSettings);
        this.logger = Loggers.getLogger(INDEX_LOGGER_NAME, settings);
    }

    @Override
    protected Map<String, ExternalResult> externalDoReorder(Set<String> keySet) {
        logger.debug("External Neo4j booster for : " + keySet);
        logger.debug("Call: " + getEndpoint());

        return getReorderedResults(getExternalResults(keySet));

    }

    public Map<String, ExternalResult> getReorderedResults(List<ExternalResult> externalResults) {
        HashMap<String, ExternalResult> results = new HashMap<>();
        for (ExternalResult item : externalResults) {
            results.put(item.getItem(), item);
        }

        return results;
    }

    public List<ExternalResult> getExternalResults(Set<String> keySet) {
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        WebResource resource = Client.create(cfg).resource(getEndpoint());
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, getParameters(keySet));
        GenericType<List<ExternalResult>> type = new GenericType<List<ExternalResult>>() {
        };
        List<ExternalResult> externalResults = response.getEntity(type);
        response.close();

        return externalResults;
    }

    @Override
    protected void extendedParseRequest(Map<String, String> extParams) {
        targetId = extParams.get(RECO_TARGET);
        keyProperty = extParams.get(KEY_PROPERTY) != null ? extParams.get(KEY_PROPERTY) : DEFAULT_KEY_PROPERTY;
        boosterEndpoint = extParams.get(NEO4J_ENDPOINT);
    }

    public Map<String, String> getParameters(Set<String> keySet) {
        Map<String, String> param = new HashMap<>();
        param.put(LIMIT, String.valueOf(Integer.MAX_VALUE));
        param.put(FROM, String.valueOf(getFrom()));
        param.put(KEY_PROPERTY, getKeyProperty());
        param.put(IDS, implodeKeySet(keySet));

        return param;
    }

    protected String getTargetId() {
        return targetId;
    }

    public String getKeyProperty() {
        return keyProperty;
    }

    public String getEndpoint() {
        String boosterUrl = null != boosterEndpoint ? boosterEndpoint : DEFAULT_REST_ENDPOINT;

        return UrlUtil.buildUrlFromParts(getNeo4jHost(), boosterUrl, targetId);
    }

    public String implodeKeySet(Set<String> keySet) {
        boolean isFirst = true;
        String ids = "";
        for (String id : keySet) {
            if (!isFirst) {
                ids = ids.concat(",");
            }
            isFirst = false;
            ids = ids.concat(id);
        }

        return ids;
    }

}
