package com.graphaware.integration.es.booster;

import com.graphaware.integration.es.IndexInfo;
import com.graphaware.integration.es.annotation.SearchBooster;
import com.graphaware.integration.es.domain.ClauseConstants;
import com.graphaware.integration.es.domain.ExternalResult;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.junit.After;

import static org.junit.Assert.*;
import org.junit.Before;
import org.mockserver.model.Header;
import org.mockserver.integration.ClientAndServer;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import org.mockserver.model.Parameter;

public class SearchResultNeo4jBoosterTest {

    private ClientAndServer mockServer;

    @Before
    public void startMockServer() {
        mockServer = startClientAndServer(1080);
    }
    
    @After
    public void stopProxy() {
        mockServer.stop();
    }

    @Test
    public void testNewInstance() {
        SearchResultNeo4jBooster booster = getBooster();
        assertTrue(booster instanceof SearchResultExternalBooster);
    }

    @Test
    public void testExtendedParseRequest() {
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(ClauseConstants.KEY_PROPERTY, "objectId");
        externalParams.put(ClauseConstants.RECO_TARGET, "12");
        externalParams.put(ClauseConstants.NEO4J_ENDPOINT, "reco/");
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
        ExternalResult result2 = new ExternalResult("456", 20.0f);
        externalResults.add(result1);
        externalResults.add(result2);
        Map<String, ExternalResult> results = getBooster().getReorderedResults(externalResults);
        assertTrue(results.containsKey("123"));
        assertTrue(results.containsKey("456"));
        assertEquals(10.0f, results.get("123").getScore(), 0);
    }

    @Test
    public void testExternalDoReorder() {
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(ClauseConstants.KEY_PROPERTY, "objectId");
        externalParams.put(ClauseConstants.RECO_TARGET, "12");
        externalParams.put(ClauseConstants.NEO4J_ENDPOINT, "reco/");
        SearchResultNeo4jBooster testBooster = getTestBooster();
        testBooster.extendedParseRequest(externalParams);
        Set<String> keySet = new HashSet<>();
        keySet.add("123");
        keySet.add("456");
        Map<String, ExternalResult> results = testBooster.externalDoReorder(keySet);
        assertTrue(results.containsKey("123"));
        assertTrue(results.containsKey("456"));
        assertEquals(123.0f, results.get("123").getScore(), 0);
    }
    
    private static final String LS = System.getProperty("line.separator");

    @Test
    public void testGetExternalResults() {
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
                                        "        \"objectId\": \"270\"," + LS +
                                        "        \"score\": 3.4" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 2," + LS +
                                        "        \"objectId\": \"123\"," + LS +
                                        "        \"score\": 3.5" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 3," + LS +
                                        "        \"objectId\": \"456\"," + LS +
                                        "        \"score\": 3.6" + LS +
                                        "    }" + LS +
                                        "]")
                );
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(ClauseConstants.KEY_PROPERTY, "objectId");
        externalParams.put(ClauseConstants.RECO_TARGET, "12");
        externalParams.put(ClauseConstants.NEO4J_ENDPOINT, "reco/");
        SearchResultNeo4jBooster testBooster = getMockBooster();
        testBooster.extendedParseRequest(externalParams);
        Set<String> keySet = new HashSet<>();
        keySet.add("123");
        keySet.add("456");
        Map<String, ExternalResult> results = testBooster.externalDoReorder(keySet);
        assertTrue(results.containsKey("123"));
        assertTrue(results.containsKey("456"));
        assertEquals(3.5f, results.get("123").getScore(), 0);
    }

    @Test
    public void testRestUrlBuilder() {
        SearchResultNeo4jBooster booster = getBooster();
        HashMap<String, String> externalParams = new HashMap<>();
        externalParams.put(ClauseConstants.KEY_PROPERTY, "objectId");
        externalParams.put(ClauseConstants.RECO_TARGET, "12");
        externalParams.put(ClauseConstants.NEO4J_ENDPOINT, "reco/");
        booster.extendedParseRequest(externalParams);
        assertEquals("http://localhost:7474/reco/12", booster.getEndpoint());
        externalParams.remove(ClauseConstants.NEO4J_ENDPOINT);
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
        assertTrue(parameters.containsKey(ClauseConstants.LIMIT));
        assertTrue(parameters.containsKey(ClauseConstants.FROM));
        assertTrue(parameters.containsKey(ClauseConstants.KEY_PROPERTY));
        assertTrue(parameters.containsKey(ClauseConstants.IDS));
        //assertEquals(getBooster().implodeKeySet(keySet), parameters.get(Constants.IDS));
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

    private SearchResultNeo4jBooster getTestBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = new IndexInfo("http://localhost:7474/", true, 10);

        return new SearchResultNeo4jBoostertest(builder.build(), indexInfo);
    }
    
    private SearchResultNeo4jBooster getMockBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = new IndexInfo("http://localhost:1080/", true, 10);

        return new SearchResultNeo4jBooster(builder.build(), indexInfo);
    }

    @SearchBooster(name = "SearchResultNeo4jBoostertest")
    class SearchResultNeo4jBoostertest extends SearchResultNeo4jBooster {

        public SearchResultNeo4jBoostertest(Settings settings, IndexInfo indexSettings) {
            super(settings, indexSettings);
        }

        public List<ExternalResult> getExternalResults(Set<String> keySet) {
            List<ExternalResult> externalResults = new ArrayList<>();
            for (String key : keySet) {
                ExternalResult res = new ExternalResult(key, Float.parseFloat(key));
                externalResults.add(res);
            }
            return externalResults;
        }

    }
}
