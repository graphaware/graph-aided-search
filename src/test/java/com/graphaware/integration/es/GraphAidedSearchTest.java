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

package com.graphaware.integration.es;

import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class GraphAidedSearchTest {

    private final String DEFAULT_CLUSTER_NAME = "graph-aided-search-cluster";

    protected ElasticsearchClusterRunner runner;

    protected EmbeddedGraphDatabaseServer neo4jServer;

    protected JestClient jestClient;

    protected ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception{
        createCluster();
        createJestClient();
        createNeo4jServer();
        objectMapper = new ObjectMapper();
    }

    protected void createCluster() {
        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Settings.Builder settingsBuilder) {
                for (String key : clusterSettings().keySet()) {
                    settingsBuilder.put(key, clusterSettings().get(key));
                }
            }
        }).build(newConfigs().numOfNode(numberOfNodes()).clusterName(clusterName()));
        runner.ensureGreen();
    }

    protected void createJestClient() {
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(getConnection())
                .multiThreaded(true)
                .readTimeout(10000000)
                .build());
        jestClient = factory.getObject();
    }

    protected void createNeo4jServer() {
        neo4jServer = new EmbeddedGraphDatabaseServer();
        neo4jServer.start();
    }

    protected CreateIndexResponse createIndex(String indexName) {
        return createIndex(indexName, new HashMap<String, Object>());
    }

    protected CreateIndexResponse createIndex(String indexName, Map<String, Object> settings) {
        Settings.Builder builder = Settings.builder();
        for (String k : settings.keySet()) {
            builder.put(k, settings.get(k));
        }
        CreateIndexResponse createIndexResponse = runner
                .createIndex(indexName, builder.build());
        assertTrue(createIndexResponse.isAcknowledged());
        return createIndexResponse;
    }

    protected int numberOfNodes() {
        return 1;
    }

    protected String clusterName() {
        return DEFAULT_CLUSTER_NAME;
    }

    protected HashMap<String, Object> clusterSettings() {
        return new HashMap<>();
    }

    protected String clientHost() {
        return "localhost";
    }

    protected int clientPort() {
        return 9201;
    }

    private String getConnection() {
        return String.format("http://%s:%d", clientHost(), clientPort());
    }

    protected IndexResponse index(String indexName, String type, String id, HashMap<String, Object> fields) throws IOException {
        return index(indexName, type, id, objectMapper.writeValueAsString(fields));
    }

    protected IndexResponse index(String indexName, String type, String id, String source) {
        IndexResponse indexResponse = runner.insert(indexName, type, id, source);
        assertTrue(indexResponse.isCreated());

        return indexResponse;
    }

    protected void assertHitCount(String indexName, String typeName, int expected) {
        assertEquals(expected, runner.client().prepareSearch(indexName).setTypes(typeName).setSize(0).get().getHits().getTotalHits());
    }

    protected final void refresh() {
        runner.refresh();
    }

    protected final Client client() {
        return runner.client();
    }

    @After
    public void tearDown() {
        runner.close();
        runner.clean();
        neo4jServer.stop();
    }
}
