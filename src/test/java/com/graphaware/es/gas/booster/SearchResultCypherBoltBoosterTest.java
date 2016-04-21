/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.es.gas.booster;

import com.graphaware.es.gas.cypher.CypherEndPoint;
import com.graphaware.es.gas.cypher.CypherEndPointBuilder;
import com.graphaware.es.gas.cypher.CypherResult;
import com.graphaware.es.gas.domain.IndexInfo;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServerConfig;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author ale
 */
public class SearchResultCypherBoltBoosterTest extends SearchResultCypherBoosterTest {

    private CypherEndPoint cypherEndPoint;

    protected void createNeo4jServer() throws IOException {
        neo4jServer = new EmbeddedGraphDatabaseServer();
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put(EmbeddedGraphDatabaseServerConfig.CONFIG_REST_ENABLE_BOLT, "true");
        neo4jServer.start(serverParams);
        cypherEndPoint = new CypherEndPointBuilder(CypherEndPointBuilder.CypherEndPointType.BOLT)
                .neo4jHostname(neo4jServer.getURL())
                .settings(Settings.EMPTY)
                .build();
    }

    protected HashMap<String, Object> getDefaultMap(String query) {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> gasFilter = new HashMap<>();
        gasFilter.put("query", query);
        gasFilter.put("protocol", "bolt");
        map.put("gas-booster", gasFilter);

        return map;
    }

    protected SearchResultCypherBooster getBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = new IndexInfo(getNeo4jURL(), getNeo4jURL(), NEO4J_USER, NEO4J_PASSWORD, true, 0);

        return new SearchResultCypherBooster(builder.build(), indexInfo);
    }
    
    protected String executeCypher(String query) {
        CypherResult res = cypherEndPoint.executeCypher(query);
        return "OK";
    }
}
