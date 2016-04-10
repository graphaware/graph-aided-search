package com.graphaware.integration.es;

import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphAidedSearchFilterTest {

    @Test
    public void testDefaultFilterOrder() {
        Settings settings = Settings.EMPTY;
        GraphAidedSearchFilter filter = new GraphAidedSearchFilter(settings);
        assertEquals(10, filter.order());
    }

    @Test
    public void testCustomFilterOrder() {
        Settings settings = Settings.builder()
                .put("indices.graphaware.filter.order", 25)
                .build();

        GraphAidedSearchFilter filter = new GraphAidedSearchFilter(settings);
        assertEquals(25, filter.order());
    }

}
