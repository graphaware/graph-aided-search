package com.graphaware.integration.es.domain;

import com.graphaware.integration.es.cypher.CypherEndPoint;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CypherEndpointUnitTest {

    private CypherEndPoint cypherEndPoint;
    
    private static final String NEO4J_SERVER_URL = "http://localhost:7474";
    private static final String NEO4J_CUSTOM_PASSWORD = "password";
    private static final String NEO4J_CUSTOM_USER = "neo4j";

    @Before
    public void setUp() {
        cypherEndPoint = new CypherEndPoint(Settings.EMPTY,
                NEO4J_SERVER_URL,
                NEO4J_CUSTOM_USER,
                NEO4J_CUSTOM_PASSWORD);
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
}
