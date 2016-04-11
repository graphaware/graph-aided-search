package com.graphaware.integration.es.booster;

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.TestIndexInfo;
import com.graphaware.integration.es.domain.Constants;
import com.graphaware.integration.es.domain.ExternalResult;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SearchResultCypherBoosterTest {

    @Test
    public void testDefaultBoosterSettings() {
        SearchResultCypherBooster booster = getBooster();
        Map<String, String> request = new HashMap<>();
        request.put("query", "MATCH (n) RETURN n");
        booster.extendedParseRequest(request);
        assertEquals(Constants.DEFAULT_SCORE_RESULT_NAME, booster.getScoreResultName());
        assertEquals(Constants.DEFAULT_ID_RESULT_NAME, booster.getIdResultName());
    }

    @Test
    public void testGetParameters() {
        Set<String> keySet = new HashSet<>();
        keySet.add("1");
        keySet.add("3");
        keySet.add("5");
        HashMap<String, Object> parameters = getBooster().getParameters(keySet);
        assertTrue(parameters.containsKey("ids"));
        Set<String> ids = (Set<String>) parameters.get("ids");
        assertEquals(3, ids.size());
        assertTrue(ids.contains("1"));
        assertTrue(ids.contains("3"));
        assertTrue(ids.contains("5"));
    }

    private HashMap<String, Object> getDefaultMap() {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> gasFilter = new HashMap<>();
        gasFilter.put("query", "MATCH (n) RETURN n");
        map.put("gas-filter", gasFilter);

        return map;
    }

    private SearchResultCypherBooster getBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = TestIndexInfo.newInstance();

        return new SearchResultCypherBooster(builder.build(), indexInfo);
    }
}
