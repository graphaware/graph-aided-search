package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.query.GraphAidedSearch;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import java.util.List;
import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GraphAidedSearchPluginTest {

    private String clusterName;
    ElasticsearchClusterRunner runner;
    private static final int NUMBER_OF_NODES = 1;
    private static final String INDEX_NAME = "test_index";
    private static final String INDEX_2_NAME = "test_index_2";
    private static final String INDEX_ALIAS_NAME = "test_alias";
    private static final String DISABLED_INDEX_NAME = "test_disabled_index";
    private static final String TYPE_NAME = "test_data";

    private static final String NEO4J_HOSTNAME = "http://localhost:7474";
    private JestClient jstClient;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-testgas-" + System.currentTimeMillis();
        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("script.search", "on");
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.put("index.number_of_shards", 3);
                settingsBuilder.put("index.number_of_replicas", 0);
                settingsBuilder.putArray("discovery.zen.ping.unicast.hosts",
                        "localhost:9301-9310");
                settingsBuilder.put("plugin.types",
                        "com.graphaware.integration.es.plugin.GraphAidedSearchPlugin");
                settingsBuilder
                        .put("index.unassigned.node_left.delayed_timeout", "0");
            }
        }).build(newConfigs().numOfNode(NUMBER_OF_NODES).clusterName(clusterName));
        runner.ensureGreen();
        String conn = String.format("http://%s:%s",
                "localhost",
                "9201");
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(conn)
                .multiThreaded(true)
                .readTimeout(10000000) //for debug
                .build());
        this.jstClient = factory.getObject();
        createIndices();
        createData();
    }

    private void createIndices() {
        CreateIndexResponse createIndexResponse = runner
                .createIndex(INDEX_NAME,
                        Settings
                        .builder()
                        .put(GraphAidedSearch.INDEX_GA_ES_NEO4J_ENABLED, true)
                        .put(GraphAidedSearch.INDEX_GA_ES_NEO4J_HOST, NEO4J_HOSTNAME)
                        .build());
        assertTrue(createIndexResponse.isAcknowledged());
        IndicesAliasesResponse aliasesResponse = runner.updateAlias(INDEX_ALIAS_NAME,
                new String[]{INDEX_NAME}, null);
        assertTrue(aliasesResponse.isAcknowledged());

        CreateIndexResponse createDisabledIndexResponse = runner
                .createIndex(DISABLED_INDEX_NAME,
                        Settings
                        .builder()
                        .put(GraphAidedSearch.INDEX_GA_ES_NEO4J_ENABLED, false)
                        .build());
        assertTrue(createDisabledIndexResponse.isAcknowledged());
    }

    private void createData() {
        for (int i = 1; i <= 1000; i++) {
            String doc = "{\"id\":\"" + i + "\",\"msg\":\"test "
                    + i + "\",\"counter\":" + i + "}";
            final IndexResponse indexResponse1 = runner.insert(INDEX_NAME, TYPE_NAME,
                    String.valueOf(i), doc);
            assertTrue(indexResponse1.isCreated());
            final IndexResponse indexResponse2 = runner.insert(DISABLED_INDEX_NAME, TYPE_NAME,
                    String.valueOf(i), doc);
            assertTrue(indexResponse2.isCreated());
        }
        
         for (int i = 1001; i <= 2000; i++) {
            String doc = "{\"id\":\"" + i + "\",\"msg\":\"test "
                    + i + "\",\"counter\":" + i + "}";
            final IndexResponse indexResponse1 = runner.insert(INDEX_2_NAME, TYPE_NAME,
                    String.valueOf(i), doc);
            assertTrue(indexResponse1.isCreated());
        }
    }

    @After
    public void tearDown() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void testEverything() throws Exception {

        assertThat(NUMBER_OF_NODES, is(runner.getNodeSize()));
        final Client client = runner.client();
        final GraphAidedSearch ranker = runner.getInstance(GraphAidedSearch.class);
        final GASIndexInfo indexInfo = ranker.getScriptInfo(INDEX_NAME);
        assertEquals(indexInfo.getNeo4jHost(), NEO4J_HOSTNAME);
        assertTrue(indexInfo.isEnabled());

        final GASIndexInfo disabledIndexInfo = ranker.getScriptInfo(DISABLED_INDEX_NAME);
        assertFalse(disabledIndexInfo.isEnabled());

        final GASIndexInfo aliasInfo = ranker.getScriptInfo(INDEX_ALIAS_NAME);
        assertEquals(aliasInfo.getNeo4jHost(), NEO4J_HOSTNAME);
        assertTrue(aliasInfo.isEnabled());

        {
            //Test query without extension
            MatchQueryBuilder query = QueryBuilders.matchQuery("msg", "test 1");
            final SearchResponse searchResponse = client.prepareSearch(INDEX_NAME)
                    .setQuery(query)
                    .execute().actionGet();
            final SearchHits hits = searchResponse.getHits();
            assertEquals(1000, hits.getTotalHits());
            assertEquals(10, hits.hits().length);
            assertEquals("1", hits.hits()[0].id());
            assertEquals("10", hits.hits()[1].id());
            assertEquals("9", hits.hits()[9].id());
        }

        String msgTestFrom;
        {
            //Test extended based bosting query
            String query = "{"
                    + "   \"query\": {"
                    + "      \"bool\": {"
                    + "         \"should\": ["
                    + "            {"
                    + "                  \"match\": {"
                    + "                       \"msg\": \"test 1\""
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

            SearchResult result = jstClient.execute(search);

            assertEquals(1000, result.getTotal().intValue());
            List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
            assertEquals(10, hits.size());
            assertEquals("test 996", hits.get(0).source.getMsg());
            assertEquals(result.getMaxScore(), 50074, 1);
            msgTestFrom = hits.get(5).source.getMsg();
        }
        
        {
            //Test index disabled
            String query = "{"
                    + "   \"query\": {"
                    + "     \"match\": {"
                    + "         \"msg\": \"test 1\""
                    + "     }"
                    + "   }"
                    + "   ,\"gas-booster\" :{"
                    + "          \"name\": \"GraphAidedSearchTestBooster\","
                    + "          \"recoTarget\": \"Durgan%20LLC\""
                    + "      }"
                    + "}";

            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(DISABLED_INDEX_NAME)
                    .addType(TYPE_NAME)
                    .build();

            SearchResult result = jstClient.execute(search);

            assertEquals(1000, result.getTotal().intValue());
            List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
            assertEquals(10, hits.size());
            assertEquals("1", hits.get(0).source.getDocumentId());
            assertEquals(result.getMaxScore(), 3.8611162, 1);
        }

        {
            //Test maxResultSize
            String query = "{"
                    + "   \"query\": {"
                    + "      \"bool\": {"
                    + "         \"should\": ["
                    + "            {"
                    + "                  \"match\": {"
                    + "                       \"msg\": \"test 1\""
                    + "                   }"
                    + "            }"
                    + "         ]"
                    + "      }"
                    + "   }"
                    + "   ,\"gas-booster\" :{"
                    + "          \"name\": \"GraphAidedSearchTestBooster\","
                    + "          \"recoTarget\": \"Durgan%20LLC\","
                    + "          \"maxResultSize\": 100"
                    + "      }"
                    + "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(INDEX_NAME)
                    .addType(TYPE_NAME)
                    .build();

            SearchResult result = jstClient.execute(search);

            assertEquals(100, result.getTotal().intValue());
            List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
            assertEquals(10, hits.size());
            assertEquals(result.getMaxScore(), 14479, 1);
        }

        {
            //Test from and size
            String query = "{"
                    + "\"from\" : 5, \"size\" : 25,"
                    + "   \"query\": {"
                    + "      \"bool\": {"
                    + "         \"should\": ["
                    + "            {"
                    + "                  \"match\": {"
                    + "                       \"msg\": \"test 1\""
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

            SearchResult result = jstClient.execute(search);

            assertEquals(1000, result.getTotal().intValue());
            List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
            assertEquals(25, hits.size());
            assertEquals(msgTestFrom, hits.get(0).source.getMsg());
            assertEquals(result.getMaxScore(), 50074, 1);
        }

        {
            //Test from and size with maxResultSize
            String query = "{"
                    + "\"from\" : 5, \"size\" : 25,"
                    + "   \"query\": {"
                    + "      \"bool\": {"
                    + "         \"should\": ["
                    + "            {"
                    + "                  \"match\": {"
                    + "                       \"msg\": \"test 1\""
                    + "                   }"
                    + "            }"
                    + "         ]"
                    + "      }"
                    + "   }"
                    + "   ,\"gas-booster\" :{"
                    + "          \"name\": \"GraphAidedSearchTestBooster\","
                    + "          \"recoTarget\": \"Durgan%20LLC\","
                    + "          \"maxResultSize\": 20"
                    + "      }"
                    + "}";

            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(INDEX_NAME)
                    .addType(TYPE_NAME)
                    .build();

            SearchResult result = jstClient.execute(search);

            assertEquals(20, result.getTotal().intValue());
            List<SearchResult.Hit<JestMsgResult, Void>> hits = result.getHits(JestMsgResult.class);
            assertEquals(15, hits.size());
            assertEquals("test 46", hits.get(0).source.getMsg());
            assertEquals(result.getMaxScore(), 3861, 1);
        }

    }
}
