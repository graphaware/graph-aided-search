package com.graphaware.es.gas.domain;

import com.graphaware.es.gas.cypher.CypherBoltEndPoint;
import com.graphaware.es.gas.cypher.CypherResult;
import com.graphaware.es.gas.cypher.ResultRow;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServerConfig;
import java.io.File;
import static java.lang.System.getProperty;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CypherEndpointBoltTest {

    
    private CypherBoltEndPoint cypherEndPoint;

    private EmbeddedGraphDatabaseServer server;

    private static final File DEFAULT_KNOWN_HOSTS = new File(getProperty("user.home"),
            ".neo4j" + File.separator + "known_hosts");

    private static final String NEO4J_CUSTOM_PASSWORD = "password";
    private static final String NEO4J_CUSTOM_USER = "neo4j";

    @Before
    public void setUp() {
        server = new EmbeddedGraphDatabaseServer();
        Map<String, Object> serverParams = new HashMap<>();
        serverParams.put(EmbeddedGraphDatabaseServerConfig.CONFIG_REST_ENABLE_BOLT, "true");
        server.start(serverParams);
        cypherEndPoint = new CypherBoltEndPoint(Settings.EMPTY,
                server.getURL(),
                NEO4J_CUSTOM_USER,
                NEO4J_CUSTOM_PASSWORD);
    }
    
    @After
    public void clear() {
        deleteDefaultKnownCertFileIfExists();
    }
    
    public static void deleteDefaultKnownCertFileIfExists() {
        if (DEFAULT_KNOWN_HOSTS.exists()) {
            DEFAULT_KNOWN_HOSTS.delete();
        }
    }

    @Test
    public void testExecuteCypher() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        cypherEndPoint.executeCypher("UNWIND range(1, 10) as x CREATE (n:Test) SET n.id = x", params);
        String query = "MATCH (n) RETURN n.id as id";
        CypherResult result = cypherEndPoint.executeCypher(query, params);
        assertEquals(10, result.getRows().size());
        int i = 0;
        for (ResultRow resultRow : result.getRows()) {
            assertTrue(resultRow.getValues().containsKey("id"));
            assertEquals(String.valueOf(++i), String.valueOf(resultRow.getValues().get("id")));
        }
    }

    @After
    public void tearDown() {
        server.stop();
    }
}
