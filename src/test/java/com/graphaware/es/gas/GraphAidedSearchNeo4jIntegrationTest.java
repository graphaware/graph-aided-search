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

import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.After;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.graphaware.es.gas.wrap.GraphAidedSearchActionListenerWrapper.*;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class GraphAidedSearchNeo4jIntegrationTest extends GraphAidedSearchTest {

    private static final String INDEX_NAME_MOCK = "test-index-mock";
    private static final String NEO4J_MOCK_HOSTNAME = "http://localhost:1081";
    private static final int MOCK_PORT = 1081;
    private static final String TYPE_NAME = "test_data";
    //
    private static final String LS = System.getProperty("line.separator");

    private ClientAndServer mockServer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockServer = startClientAndServer(MOCK_PORT);
        createMockIndex();
        createData();

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

    @After
    public void stopMock() {
        mockServer.stop();
    }

    private void createMockIndex() {
        Map<String, Object> settings = new HashMap<>();
        settings.put(INDEX_GA_ES_NEO4J_ENABLED, true);
        settings.put(INDEX_GA_ES_NEO4J_HOST, NEO4J_MOCK_HOSTNAME);
        settings.put(INDEX_GA_ES_NEO4J_USER, NEO4J_USER);
        settings.put(INDEX_GA_ES_NEO4J_PWD, NEO4J_PASSWORD);
        createIndex(INDEX_NAME_MOCK, settings);
    }

    @Test
    public void testSearchResultNeo4jBooster() throws IOException {
        setMockResponse();
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 1", "1");
        assertEquals("test 1", hits.get(0).source.getMsg());
        float expectedScore = withoutBoosterMaxScore * 10.5f;
        assertEquals(expectedScore, result.getMaxScore(), 1);
    }

    @Test
    public void testSearchResultNeo4jBoosterAuth() throws IOException {
        setMockAuthResponse("bmVvNGo6cGFzc3dvcmQ=");
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 1", "1");
        assertEquals("test 1", hits.get(0).source.getMsg());
        float expectedScore = withoutBoosterMaxScore * 10.5f;
        assertEquals(expectedScore, result.getMaxScore(), 1);
    }

    @Test
    public void testSearchResultNeo4jBoosterAuthFail() throws IOException {
        setMockAuthResponse("MUST FAIL");
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertNotNull(result);
        assertNull(result.getTotal());

    }

    @Test
    public void testSearchResultNeo4jBoosterSubtract() throws IOException {
        setMockResponse();
        String query = "{"
                + "\"size\": 100,"
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\","
                + "          \"operator\": \"-\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(100, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 1", "1");
        float expectedScore = withoutBoosterMaxScore - 10.5f;
        float newResult = getResult(result, "1");
        assertEquals(expectedScore, newResult, 1);
    }

    @Test
    public void testSearchResultNeo4jBoosterDivide() throws IOException {
        setMockResponse();
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\","
                + "          \"operator\": \"/\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 1", "1");
        float expectedScore = withoutBoosterMaxScore / 10.5f;
        float newResult = getResult(result, "1");
        assertEquals(expectedScore, newResult, 1);
    }

    @Test
    public void testSearchResultNeo4jBoosterReplace() throws IOException {
        setMockResponse();
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\","
                + "          \"operator\": \"replace\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());

        float newResult = getResult(result, "1");
        assertEquals(10.5f, newResult, 1);
    }

    @Test
    public void testSearchResultNeo4jBoosterFrom() throws IOException {
        setMockResponse();
        String query = "{"
                + "\"from\" : 5, \"size\" : 25,"
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
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"SearchResultNeo4jBooster\","
                + "          \"target\": \"12\","
                + "          \"neo4j.endpoint\": \"/reco/\","
                + "          \"maxResultSize\": 20"
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(20, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(15, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 52").getMaxScore();
        assertFalse("test 1".equalsIgnoreCase(hits.get(0).source.getMsg()));

        assertNotEquals(withoutBoosterMaxScore, result.getMaxScore(), 1);
    }

    private void setMockResponse() {
        mockServer
                .when(
                        request()
                                .withPath("/reco/12")
                )
                .respond(response()
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                )
                                .withBody("" +
                                        "[" + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 1," + LS +
                                        "        \"objectId\": \"1\"," + LS +
                                        "        \"score\": 10.5" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 2," + LS +
                                        "        \"objectId\": \"2\"," + LS +
                                        "        \"score\": 2.2" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 3," + LS +
                                        "        \"objectId\": \"3\"," + LS +
                                        "        \"score\": 1" + LS +
                                        "    }" + LS +
                                        "]")
                );
    }

    private void setMockAuthResponse(String auth) {
        mockServer
                .when(
                        request()
                                .withPath("/reco/12")
                                .withHeaders(
                                        new Header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                                )
                )
                .respond(response()
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                )
                                .withBody("" +
                                        "[" + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 1," + LS +
                                        "        \"objectId\": \"1\"," + LS +
                                        "        \"score\": 10.5" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 2," + LS +
                                        "        \"objectId\": \"2\"," + LS +
                                        "        \"score\": 2.2" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 3," + LS +
                                        "        \"objectId\": \"3\"," + LS +
                                        "        \"score\": 1" + LS +
                                        "    }" + LS +
                                        "]")
                );
    }

    private void createData() throws IOException {
        for (int i = 0; i < 100; ++i) {
            index(INDEX_NAME_MOCK, TYPE_NAME, String.valueOf(i), document(String.valueOf(i)));
        }
        refresh();
        assertHitCount(INDEX_NAME_MOCK, TYPE_NAME, 100);
    }

    private HashMap<String, Object> document(String id) {
        HashMap<String, Object> doc = new HashMap<>();
        doc.put("id", id);
        doc.put("message", "test " + id);
        doc.put("counter", Integer.valueOf(id));

        return doc;
    }

    private SearchResult getResultForDocWithMessage(String message) throws IOException {
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"" + message + "\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();

        return jestClient.execute(search);
    }

    private float getResultForDocWithMessage(String message, String value) throws IOException {
        String query = "{"
                + "\"size\" : 20,"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"" + message + "\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME_MOCK)
                .addType(TYPE_NAME)
                .build();

        SearchResult results = jestClient.execute(search);
        return getResult(results, value);
    }

    private float getResult(SearchResult results, String value) {
        List<SearchResult.Hit<JestMsgResult, Void>> hits = results.getHits(JestMsgResult.class);
        for (SearchResult.Hit<JestMsgResult, Void> hit : hits) {
            if (hit.source.getDocumentId().equalsIgnoreCase(value)) {
                return hit.score.floatValue();
            }
        }
        return -1f;
    }

    private List<SearchResult.Hit<JestMsgResult, Void>> getHitsForResult(SearchResult result) {
        return result.getHits(JestMsgResult.class);
    }

}
