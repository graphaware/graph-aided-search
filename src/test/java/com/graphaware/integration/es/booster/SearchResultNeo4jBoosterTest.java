package com.graphaware.integration.es.booster;

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.domain.Constants;
import com.graphaware.integration.es.domain.ExternalResult;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class SearchResultNeo4jBoosterTest {

    @Test
    public void testNewInstance() {
        SearchResultNeo4jBooster booster = getBooster();
        assertTrue(booster instanceof SearchResultExternalBooster);
    }

    @Test
    public void testExtendedParseRequest() {
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(Constants.KEY_PROPERTY, "objectId");
        externalParams.put(Constants.RECO_TARGET, "12");
        externalParams.put(Constants.NEO4J_ENDPOINT, "reco/");
        SearchResultNeo4jBooster booster = getBooster();
        booster.extendedParseRequest(externalParams);
        assertEquals("objectId", booster.getKeyProperty());
        assertEquals("12", booster.getTargetId());
        assertEquals("http://localhost:7474/", booster.getNeo4jHost());
        assertEquals("http://localhost:7474/reco/12", booster.getEndpoint());
    }

    @Test
    public void testGetReorderedResults() {
        List<ExternalResult> externalResults = new ArrayList<>();
        ExternalResult result1 = new ExternalResult("123", 10.0f);
        result1.setItem("123");
        ExternalResult result2 = new ExternalResult("456", 20.0f);
        result2.setItem("456");
        externalResults.add(result1);
        externalResults.add(result2);
        Map<String, ExternalResult> results = getBooster().getReorderedResults(externalResults);
        assertTrue(results.containsKey("123"));
        assertTrue(results.containsKey("456"));
        assertEquals(10.0f, results.get("123").getScore(), 0);
    }

    @Test
    public void testRestUrlBuilder() {
        SearchResultNeo4jBooster booster = getBooster();
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(Constants.KEY_PROPERTY, "objectId");
        externalParams.put(Constants.RECO_TARGET, "12");
        externalParams.put(Constants.NEO4J_ENDPOINT, "reco/");
        booster.extendedParseRequest(externalParams);
        assertEquals("http://localhost:7474/reco/12", booster.getEndpoint());
        externalParams.remove(Constants.NEO4J_ENDPOINT);
        booster.extendedParseRequest(externalParams);
        assertEquals("http://localhost:7474/graphaware/recommendation/filter/12", booster.getEndpoint());
    }

    @Test
    public void testBuildParameters() {
        Set<String> keySet = new HashSet<>();
        keySet.add("1");
        keySet.add("2");
        keySet.add("33");
        Map<String, String> parameters = getBooster().getParameters(keySet);
        assertTrue(parameters.containsKey(Constants.LIMIT));
        assertTrue(parameters.containsKey(Constants.FROM));
        assertTrue(parameters.containsKey(Constants.KEY_PROPERTY));
        assertTrue(parameters.containsKey(Constants.IDS));
        assertEquals(getBooster().implodeKeySet(keySet), parameters.get(Constants.IDS));
    }

    @Test
    public void testImplodeKeySet() {
        Set<String> keySet = new HashSet<>();
        keySet.add("1");
        keySet.add("2");
        keySet.add("33");
        String imploded = getBooster().implodeKeySet(keySet);
        assertEquals(6, imploded.length());
        // @todo result is not 1,2,33 but 2,1,33. Is it safe to rely on the order ?
    }

    private HashMap<String, Object> getDefaultMap() {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> gasFilter = new HashMap<>();
        gasFilter.put("query", "MATCH (n) RETURN n");
        map.put("gas-filter", gasFilter);

        return map;
    }

    private SearchResultNeo4jBooster getBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = new IndexInfo("http://localhost:7474/", true, 10);

        return new SearchResultNeo4jBooster(builder.build(), indexInfo);
    }
}
