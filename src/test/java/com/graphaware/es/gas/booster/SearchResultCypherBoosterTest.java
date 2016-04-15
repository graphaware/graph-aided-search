package com.graphaware.es.gas.booster;

import com.graphaware.es.gas.GraphAidedSearchTest;
import com.graphaware.es.gas.domain.IndexInfo;

import static com.graphaware.es.gas.booster.SearchResultExternalBooster.DEFAULT_ID_RESULT_NAME;
import static com.graphaware.es.gas.booster.SearchResultExternalBooster.DEFAULT_SCORE_RESULT_NAME;

import com.graphaware.es.gas.domain.ExternalResult;
import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import org.springframework.core.io.ClassPathResource;

public class SearchResultCypherBoosterTest extends GraphAidedSearchTest {

    @Override
    protected void eventuallyPopulateDatabase() {
        try {
            String graphgenPath = new ClassPathResource("graphgen-test-data.cyp").getFile().getAbsolutePath();
            neo4jServer.populate(graphgenPath);
        }
        catch (Exception ex) {
            throw new RuntimeException("Error while populating database", ex);
        }
    }

    @Test
    public void testDefaultBoosterSettings() {
        SearchResultCypherBooster booster = getBooster();
        Map<String, String> request = new HashMap<>();
        request.put("query", "MATCH (n) RETURN n");
        booster.extendedParseRequest(request);
        assertEquals(DEFAULT_SCORE_RESULT_NAME, booster.getScoreResultName());
        assertEquals(DEFAULT_ID_RESULT_NAME, booster.getIdResultName());
    }

    @Test
    public void testGetExternalResultsWhenKeySetIsEmpty() {
        Set<String> keySet = new HashSet<>();
        SearchResultCypherBooster booster = getBooster();
        booster.extendedParseRequest((Map<String, String>) getDefaultMap().get("gas-booster"));
        Map<String, ExternalResult> externalResults = booster.getExternalResults(keySet);
        assertEquals(0, externalResults.size());
    }

    @Test
    public void testExternalResultsAreReturned() {
        executeCypher("UNWIND range(1,10) as x CREATE (n:Test) SET n.id = x");
        Set<String> keySet = new HashSet<>();
        for (int i = 1; i <= 10; ++i) {
            keySet.add(String.valueOf(i));
        }
        SearchResultCypherBooster booster = getBooster();
        booster.extendedParseRequest((Map<String, String>) getDefaultMap("MATCH (n:Test) RETURN n.id as id, 10 as score").get("gas-booster"));
        Map<String, ExternalResult> externalResults = booster.getExternalResults(keySet);
        assertEquals(10, externalResults.size());
        assertEquals(10.0f, externalResults.get("3").getScore(), 0);
    }

    @Test
    public void testExternalResultsAreReturnedWithCustomIdentifierAndScoreName() {
        executeCypher("UNWIND range(1,10) as x CREATE (n:Test) SET n.id = x");
        Set<String> keySet = new HashSet<>();
        for (int i = 1; i <= 10; ++i) {
            keySet.add(String.valueOf(i));
        }
        SearchResultCypherBooster booster = getBooster();
        Map<String, String> map = (Map<String, String>) getDefaultMap("MATCH (n:Test) RETURN n.id as uuid, 15 as freq").get("gas-booster");
        map.put("identifier", "uuid");
        map.put("scoreName", "freq");
        booster.extendedParseRequest(map);
        Map<String, ExternalResult> externalResults = booster.getExternalResults(keySet);
        assertEquals(10, externalResults.size());
        assertEquals(15.0f, externalResults.get("1").getScore(), 0);
    }

    @Test
    public void testExceptionIsThrownWhenScoreResultNameIsNotReturned() {
        executeCypher("UNWIND range(1,10) as x CREATE (n:Test) SET n.id = x");
        Set<String> keySet = new HashSet<>();
        for (int i = 1; i < 10; ++i) {
            keySet.add(String.valueOf(i));
        }
        SearchResultCypherBooster booster = getBooster();
        booster.extendedParseRequest((Map<String, String>) getDefaultMap().get("gas-booster"));
        try {
            Map<String, ExternalResult> externalResults = booster.getExternalResults(keySet);
            assertEquals(1, 2); // If we're here it's a bug
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("The cypher query result must contain the score column name"));
        }
    }



    @Test
    public void testExceptionIsThrownWhenIdResultNameIsNotPresent() {
        executeCypher("UNWIND range(1,10) as x CREATE (n:Test) SET n.id = x");
        Set<String> keySet = new HashSet<>();
        for (int i = 1; i < 10; ++i) {
            keySet.add(String.valueOf(i));
        }
        SearchResultCypherBooster booster = getBooster();
        booster.extendedParseRequest((Map<String, String>) getDefaultMap("MATCH (n:Test) RETURN id(n)").get("gas-booster"));
        try {
            Map<String, ExternalResult> externalResults = booster.getExternalResults(keySet);
            assertEquals(1, 2); // If we're here it's a bug
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("The cypher query result must contain the id column name"));
        }
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
        return getDefaultMap("MATCH (n:Test) RETURN n.id as id");
    }

    private HashMap<String, Object> getDefaultMap(String query) {
        HashMap<String, Object> map = new HashMap<>();
        HashMap<String, Object> gasFilter = new HashMap<>();
        gasFilter.put("query", query);
        map.put("gas-booster", gasFilter);

        return map;
    }

    private SearchResultCypherBooster getBooster() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = new IndexInfo(NEO4J_SERVER_URL, NEO4J_USER, NEO4J_PASSWORD, true, 0);

        return new SearchResultCypherBooster(builder.build(), indexInfo);
    }
}
