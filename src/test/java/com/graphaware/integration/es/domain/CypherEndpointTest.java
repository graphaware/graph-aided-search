package com.graphaware.integration.es.domain;

import com.graphaware.integration.es.util.TestHttpClient;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class CypherEndpointTest {

    private CypherEndPoint cypherEndPoint;

    private EmbeddedGraphDatabaseServer server;

    private TestHttpClient httpClient;

    private static final String NEO4J_SERVER_URL = "http://localhost:7474";

    private static final String NEO4J_CUSTOM_PASSWORD = "password";

    @Before
    public void setUp() {
        cypherEndPoint = new CypherEndPoint(Settings.EMPTY);
        server = new EmbeddedGraphDatabaseServer();
        server.start();
        httpClient = new TestHttpClient();
        changePassword();
        emptyDB();
    }

    @Test
    public void testExecuteCypher() throws Exception {
        httpClient.executeCypher(NEO4J_SERVER_URL, getHeaders(NEO4J_CUSTOM_PASSWORD), "UNWIND range(0, 10) as x CREATE (n:Test) SET n.id = x");
        String query = "MATCH (n) RETURN n.id as id";
        HashMap<String, Object> params = new HashMap<>();
        CypherEndPoint cypherEndPoint = new CypherEndPoint(Settings.EMPTY);
        CypherResult result = cypherEndPoint.executeCypher(getCypherEndpoint(), getHeaders(NEO4J_CUSTOM_PASSWORD), query, params);
        assertEquals(10, result.getRows().size());
        int i = -1;
        for (ResultRow resultRow : result.getRows()) {
            assertTrue(resultRow.getValues().containsKey("id"));
            assertEquals(++i, resultRow.getValues().get("id"));
        }
    }

    @Test
    public void testSendSingleStatement() throws Exception{
        String response = httpClient.executeCypher(NEO4J_SERVER_URL, getHeaders(NEO4J_CUSTOM_PASSWORD), "CREATE (n) RETURN id(n)");
    }

    @Test
    public void testBuildCypherQuery() {
        String query = "MATCH (n) RETURN n";
        String json = cypherEndPoint.buildCypherQuery(query);
        assertEquals("{\"statements\" : [{\"statement\" : \"MATCH (n) RETURN n\"}]}", json);
    }

    @Test
    public void testBuildQueryWithParameters() {
        String query = "MATCH (n) WHERE id(n) IN {ids}";
        HashMap<String, Object> parameters = new HashMap<>();
        List<Integer> ids = new ArrayList<>();
        ids.add(1);
        ids.add(2);
        ids.add(3);
        parameters.put("ids", ids);
        String json = cypherEndPoint.buildCypherQuery(query, parameters);
        assertEquals("{\"statements\" : [{\"statement\" : \"MATCH (n) WHERE id(n) IN {ids}\",\"parameters\":{\"ids\":[1,2,3]}}]}", json);
    }

    private String getCypherEndpoint() {
        return NEO4J_SERVER_URL + "/db/data/transaction/commit";
    }

    private void emptyDB() {
        httpClient.executeCypher(NEO4J_SERVER_URL, getHeaders(NEO4J_CUSTOM_PASSWORD), "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r,n");
    }

    private void changePassword() {
        String json = "{\"password\":\"" + NEO4J_CUSTOM_PASSWORD + "\"}";
        try {
            httpClient.post(NEO4J_SERVER_URL + "/user/neo4j/password", json, getHeaders("neo4j"), 200);
        } catch (AssertionError e) {
            // password was already changed in a previous test and the dbms auth directory is already existing
        }
    }

    private HashMap<String, String> getHeaders(String password) {
        HashMap<String, String> headers = new HashMap<>();
        try {
            String credentials = "neo4j:" + password;
            headers.put("Authorization", "Basic " + Base64.encodeBase64String(credentials.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return headers;
    }

    private String getJsonBody(String query) throws Exception{
        HashMap<String, Object> body = new HashMap<>();
        HashMap<String, String> statement = new HashMap<>();
        List<HashMap<String, String>> statements = new ArrayList<>();
        statement.put("statement", query);
        statements.add(statement);
        body.put("statements", statements);

        return ObjectMapper.class.newInstance().writeValueAsString(body);
    }

    @After
    public void tearDown() {
        server.stop();
    }
}
