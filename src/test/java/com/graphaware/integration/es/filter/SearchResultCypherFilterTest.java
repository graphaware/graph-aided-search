package com.graphaware.integration.es.filter;

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.TestIndexInfo;
import io.searchbox.core.SearchResult;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class SearchResultCypherFilterTest {

    @Test
    public void testSizeIsParsedFromRequest() {
        SearchResultCypherFilter filter = getFilter();
        HashMap<String, Object> map = getDefaultMap();
        map.put("size", 25);
        filter.parseRequest(map);
        assertEquals(25, filter.getSize());
    }

    @Test
    public void testSizeHasDefault() {
        SearchResultCypherFilter filter = getFilter();
        HashMap<String, Object> map = getDefaultMap();
        filter.parseRequest(map);
        assertEquals(10, filter.getSize());
    }

    @Test
    public void testFromIsParsedFromRequest() {
        SearchResultCypherFilter filter = getFilter();
        HashMap<String, Object> map = getDefaultMap();
        map.put("from", 100);
        filter.parseRequest(map);
        assertEquals(100, filter.getFrom());
    }

    @Test
    public void testFromHasDefault() {
        SearchResultCypherFilter filter = getFilter();
        HashMap<String, Object> map = getDefaultMap();
        filter.parseRequest(map);
        assertEquals(0, filter.getFrom());
    }

    private HashMap<String, Object> getDefaultMap() {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> gasFilter = new HashMap<>();
        gasFilter.put("query", "MATCH (n) RETURN n");
        map.put("gas-filter", gasFilter);

        return map;
    }

    private SearchResultCypherFilter getFilter() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = TestIndexInfo.newInstance();

        return new SearchResultCypherFilter(builder.build(), indexInfo);
    }

}
