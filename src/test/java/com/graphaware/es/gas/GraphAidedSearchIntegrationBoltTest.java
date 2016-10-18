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
package com.graphaware.es.gas;

import com.graphaware.es.gas.cypher.CypherEndPoint;
import com.graphaware.es.gas.cypher.CypherEndPointBuilder;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.graphaware.es.gas.wrap.GraphAidedSearchActionListenerWrapper.*;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import org.elasticsearch.common.settings.Settings;
import static org.junit.Assert.assertEquals;

public class GraphAidedSearchIntegrationBoltTest extends GraphAidedSearchTest {

    private static final String INDEX_NAME = "test-index";
    private static final String DISABLED_INDEX_NAME = "disabled-test-index";
    private static final String TYPE_NAME = "test_data";
    protected static final String BOLT_URL = "bolt://localhost:7687";
    protected static final String HTTP_URL = "http://localhost:7474";

    private CypherEndPoint cypherEndPoint;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createIndices();
        createData();

    }

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

    @Override
    protected HashMap<String, Object> clusterSettings() {
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("script.search", "on");
        settings.put("http.cors.enabled", true);
        settings.put("http.cors.allow-origin", "*");
        settings.put("index.number_of_shards", 3);
        settings.put("index.number_of_replicas", 0);
        settings.put("discovery.zen.ping.unicast.hosts", "localhost:9301-9310");
        settings.put("plugin.types", "com.graphaware.es.gas.GraphAidedSearchPlugin");
        settings.put("index.unassigned.node_left.delayed_timeout", "0");

        return settings;
    }

    @Test
    public void testCypherFilterWithGraphAndBolt() throws IOException {
        executeCypher("UNWIND range(0, 100) as x CREATE (n) SET n.id = x");
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 1\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-filter\" :{"
                + "          \"name\": \"SearchResultCypherFilter\","
                + "          \"query\": \"MATCH (n) RETURN n.id as id\","
                + "          \"exclude\": false,"
                + "          \"protocol\": \"bolt\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);

        assertEquals(100, result.getTotal().intValue());
    }

    private void createData() throws IOException {
        for (int i = 0; i < 100; ++i) {
            index(INDEX_NAME, TYPE_NAME, String.valueOf(i), document(String.valueOf(i)));
        }
        refresh();
        assertHitCount(INDEX_NAME, TYPE_NAME, 100);
    }

    private void createIndices() {
        Map<String, Object> settings1 = new HashMap<>();
        settings1.put(INDEX_GA_ES_NEO4J_ENABLED, true);
        settings1.put(INDEX_GA_ES_NEO4J_HOST, HTTP_URL);
        settings1.put(INDEX_GA_ES_NEO4J_USER, NEO4J_USER);
        settings1.put(INDEX_GA_ES_NEO4J_PWD, NEO4J_PASSWORD);
        settings1.put(INDEX_GA_ES_NEO4J_BOLT_HOST, BOLT_URL);
        createIndex(INDEX_NAME, settings1);

        Map<String, Object> settings2 = new HashMap<>();
        settings2.put(INDEX_GA_ES_NEO4J_ENABLED, false);
        createIndex(DISABLED_INDEX_NAME, settings2);
    }

    private HashMap<String, Object> document(String id) {
        HashMap<String, Object> doc = new HashMap<>();
        doc.put("id", id);
        doc.put("message", "test " + id);
        doc.put("counter", Integer.valueOf(id));

        return doc;
    }

    protected String executeCypher(String query) {
        cypherEndPoint.executeCypher(query, new HashMap<String, Object>());
        return "";
    }
}
