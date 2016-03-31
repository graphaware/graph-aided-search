package com.graphaware.integration.es.plugin.graphfilter;

import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.graphaware.integration.es.plugin.query.GASIndexInfoTest;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class GraphFilterTest {

    private GraphAidedSearchCypherFilter filter;

    @Before
    public void setUp() {
        Settings.Builder builder = Settings.builder();
        GASIndexInfo indexInfo = GASIndexInfoTest.newInstance();
        filter = new GraphAidedSearchCypherFilter(builder.build(), indexInfo);
    }

    @Test
    public void testQueryParameterIsMandatory() {
        HashMap<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("gas-filter", new HashMap<String, Object>());

        try {
            filter.parseRequest(sourceMap);
            assertTrue(false); // if we reach this line it is a bug
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Query Parameter is required"));
        }

    }

}
