package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.graphaware.integration.es.plugin.query.GraphAidedSearch;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

    @Override
    protected HashMap<String, Object> clusterSettings() {
        HashMap<String, Object> settings = new HashMap<>();
        settings.put("script.search", "on");
        settings.put("http.cors.enabled", true);
        settings.put("http.cors.allow-origin", "*");
        settings.put("index.number_of_shards", 3);
        settings.put("index.number_of_replicas", 0);
        settings.put("discovery.zen.ping.unicast.hosts","localhost:9301-9310");
        settings.put("plugin.types", "com.graphaware.integration.es.plugin.GraphAidedSearchPlugin");
        settings.put("index.unassigned.node_left.delayed_timeout", "0");

        return settings;
    }

    @Test
    public void testPluginSetup() {
        final GraphAidedSearch plugin = runner.getInstance(GraphAidedSearch.class);
        final GASIndexInfo indexInfo = plugin.getScriptInfo(INDEX_NAME);
        assertEquals(NEO4J_HOSTNAME, indexInfo.getNeo4jHost());
        assertTrue(indexInfo.isEnabled());
    }

    @Test
    public void testQueryWithoutPlugin() {
        MatchQueryBuilder query = QueryBuilders.matchQuery("message", "test 1");
        final SearchResponse searchResponse = client().prepareSearch(INDEX_NAME)
                .setQuery(query)
                .execute().actionGet();
        final SearchHits hits = searchResponse.getHits();
        assertEquals(100, hits.getTotalHits());
        assertEquals(10, hits.hits().length);
        assertEquals("1", hits.hits()[0].id());
    }

    @Test
    public void testExtendeBaseBoostingQuery() throws IOException{
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 99\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"GraphAidedSearchTestBooster\","
                + "          \"recoTarget\": \"Durgan%20LLC\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();
        SearchResult result = jestClient.execute(search);
        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());

        float withoutBoosterMaxScore = getResultForDocWithMessage("test 99").getMaxScore();
        assertEquals("test 99", hits.get(0).source.getMsg());
        float expectedScore = withoutBoosterMaxScore * (Integer.parseInt(hits.get(0).source.getDocumentId()) * 1000);
        assertEquals(expectedScore, result.getMaxScore(), 1);
    }

    @Test
    public void testReplaceBooster() throws IOException {
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 1\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-booster\" :{"
                + "          \"name\": \"GraphAidedSearchReplaceBooster\","
                + "          \"recoTarget\": \"Durgan%20LLC\""
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        SearchResult result = jestClient.execute(search);

        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());
        assertEquals("test 99", hits.get(0).source.getMsg());
        assertEquals(result.getMaxScore(), 99 * 1000, 1);
    }

    @Test
    public void testCypherFilterQueryWithExcludeFalse() throws IOException {
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 1\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-filter\" :{"
                + "          \"name\": \"GraphAidedSearchCypherTestFilter\","
                + "          \"exclude\": false"
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        SearchResult result = jestClient.execute(search);

        assertEquals(33, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());
        assertEquals("test 36", hits.get(0).source.getMsg());
        assertEquals(2.6, result.getMaxScore(), 0.1);
    }

    @Test
    public void testCypherFilterQueryWithExcludeTrue() throws IOException {
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 1\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-filter\" :{"
                + "          \"name\": \"GraphAidedSearchCypherTestFilter\","
                + "          \"exclude\": true"
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        SearchResult result = jestClient.execute(search);

        assertEquals(67, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        assertEquals(10, hits.size());
        assertEquals("test 35", hits.get(0).source.getMsg());
        assertEquals(2.5, result.getMaxScore(), 0.1);
    }

    @Test
    public void testCypherFilterWithFromAndSizeDifferentThanZero() throws IOException {
        String query = "{"
                + "\"from\" : 5, \"size\" : 25,"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 1\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "   ,\"gas-filter\" :{"
                + "          \"name\": \"GraphAidedSearchCypherTestFilter\","
                + "          \"maxResultSize\": 20,"
                + "          \"exclude\": false"
                + "      }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        SearchResult result = jestClient.execute(search);

        assertEquals(6, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
        assertEquals(1, hits.size());
        assertEquals("test 54", hits.get(0).source.getMsg());
        assertEquals(2.5, result.getMaxScore(), 0.1);
    }

    @Test
    public void testCypherBooster() throws IOException {
        String query = "{"
                    + "   \"query\": {"
                    + "      \"bool\": {"
                    + "         \"should\": ["
                    + "            {"
                    + "                  \"match\": {"
                    + "                       \"message\": \"test 1\""
                    + "                   }"
                    + "            }"
                    + "         ]"
                    + "      }"
                    + "   }"
                    + "   ,\"gas-booster\" :{"
                    + "          \"name\": \"GraphAidedSearchCypherTestBooster\","
                    + "          \"query\": \"MATCH (n:User)-[:RATED]->(m) where n.id = 2 RETURN m.id as id, size((m)<-[:RATED]-()) as score\","
                    + "          \"scoreName\": \"score\","
                    + "          \"identifier\": \"id\""
                    + "      }"
                    + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        SearchResult result = jestClient.execute(search);

        assertEquals(100, result.getTotal().intValue());
        List<SearchResult.Hit<JestMsgResult, Void>> hits = getHitsForResult(result);
        float withoutBoosterMaxScore = getResultForDocWithMessage("test1").getMaxScore();

        assertEquals(10, hits.size());
        assertEquals("test 99", hits.get(0).source.getMsg());
        assertEquals(7121, result.getMaxScore(), 1.0);
        assertTrue(withoutBoosterMaxScore < result.getMaxScore());
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
        doc.put("message", "test " + id);
        doc.put("counter", Integer.valueOf(id));

        return doc;
    }

    private SearchResult getResultForDocWithMessage(String message) throws IOException {
        String query = "{"
                + "   \"query\": {"
                + "      \"bool\": {"
                + "         \"should\": ["
                + "            {"
                + "                  \"match\": {"
                + "                       \"message\": \"test 99\""
                + "                   }"
                + "            }"
                + "         ]"
                + "      }"
                + "   }"
                + "}";

        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(INDEX_NAME)
                .addType(TYPE_NAME)
                .build();

        return jestClient.execute(search);
    }

    private List<SearchResult.Hit<JestMsgResult, Void>> getHitsForResult(SearchResult result) {
        return result.getHits(JestMsgResult.class);
    }

}
