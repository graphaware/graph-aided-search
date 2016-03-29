package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.graphaware.integration.es.plugin.query.GraphAidedSearch;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class GraphAidedSearchIntegrationTest extends GraphAidedSearchTest {

    private static final String INDEX_NAME = "test-index";

    private static final String DISABLED_INDEX_NAME = "disabled-test-index";

    private static final String NEO4J_HOSTNAME = "http://localhost:7474";

    private static final String TYPE_NAME = "test_data";

    @Override
    public void setUp() throws Exception{
        super.setUp();
        createIndices();
        createData();
    }

    @Test
    public void testPluginSetup() {
        final GraphAidedSearch plugin = runner.getInstance(GraphAidedSearch.class);
        final GASIndexInfo indexInfo = plugin.getScriptInfo(INDEX_NAME);
        assertEquals(NEO4J_HOSTNAME, indexInfo.getNeo4jHost());
        assertTrue(indexInfo.isEnabled());
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
        settings1.put(GraphAidedSearch.INDEX_GA_ES_NEO4J_ENABLED, true);
        settings1.put(GraphAidedSearch.INDEX_GA_ES_NEO4J_HOST, NEO4J_HOSTNAME);
        createIndex(INDEX_NAME, settings1);

        Map<String, Object> settings2 = new HashMap<>();
        settings2.put(GraphAidedSearch.INDEX_GA_ES_NEO4J_ENABLED, false);
        createIndex(DISABLED_INDEX_NAME, settings2);
    }

    private HashMap<String, Object> document(String id) {
        HashMap<String, Object> doc = new HashMap<>();
        doc.put("id", id);
        doc.put("message", "test document " + id);
        doc.put("counter", Integer.valueOf(id));

        return doc;
    }

}
