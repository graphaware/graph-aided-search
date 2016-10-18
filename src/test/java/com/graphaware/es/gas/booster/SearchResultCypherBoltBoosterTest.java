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
    protected static final String BOLT_URL = "bolt://localhost:7687";
    protected static final String HTTP_URL = "http://localhost:7474";

    protected void createNeo4jServer() throws IOException {
        neo4jServer = new EmbeddedGraphDatabaseServer();
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put("dbms.connector.0.enabled", true);
        neo4jServer.start(serverParams);
        cypherEndPoint = new CypherEndPointBuilder(CypherEndPointBuilder.CypherEndPointType.BOLT)
                .neo4jHostname(HTTP_URL)
                .neo4jBoltHostname(BOLT_URL)
                .settings(Settings.EMPTY)
                .encryption(false)
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
        IndexInfo indexInfo = new IndexInfo(HTTP_URL, BOLT_URL, NEO4J_USER, NEO4J_PASSWORD, true, 0, false);

        return new SearchResultCypherBooster(builder.build(), indexInfo);
    }

    protected String executeCypher(String query) {
        CypherResult res = cypherEndPoint.executeCypher(query, new HashMap<String, Object>());
        return "OK";
    }
}
