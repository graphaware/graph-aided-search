
/*
 * Copyright (c) 2015 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.graphaware.integration.es.reco.demo;

import com.graphaware.integration.es.util.TestUtil;
import com.graphaware.integration.es.test.ElasticSearchClient;
import com.graphaware.integration.es.test.ElasticSearchServer;
import com.graphaware.integration.es.test.EmbeddedElasticSearchServer;
import com.graphaware.integration.es.test.JestElasticSearchClient;
import com.graphaware.test.integration.NeoServerIntegrationTest;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.parboiled.common.FileUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

public class CompleteElasticSearchModuleIntegrationEndToEndTest extends NeoServerIntegrationTest {

    private static final String ES_HOST = "localhost";
    private static final String ES_PORT = "9201";
    private static final String ES_CONN = String.format("http://%s:%s", ES_HOST, ES_PORT);
    private static final String ES_INDEX = "neo4jes";

    private ElasticSearchClient esClient;
    private ElasticSearchServer esServer;

    @Override
    protected String neo4jConfigFile() {
        return "neo4j-elasticsearch-reco.properties";
    }

    @Override
    protected String neo4jServerConfigFile() {
        return "neo4j-server-es.properties";
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        esServer = new EmbeddedElasticSearchServer();
        esServer.start();
        esClient = new JestElasticSearchClient(ES_HOST, ES_PORT);
        super.setUp();

//        for (String query : FileUtils.readAllText(new ClassPathResource("demo-data.cyp").getFile()).split(";" + System.getProperty("line.separator"))) {
        for (String query : FileUtils.readAllText(new ClassPathResource("demo-data.cyp").getFile()).split(";" + System.getProperty("line.separator"))) {
            httpClient.executeCypher(baseUrl(), query);
        }
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        esClient.shutdown();
        esServer.stop();
        super.tearDown();
    }

    @Test
    public void test() throws IOException {
        TestUtil.waitFor(5000);

        String executeCypher = httpClient.executeCypher(baseUrl(), "MATCH (p:Person {firstname:'Kelly', lastname:'Krajcik'}) return p");
        String response = httpClient.get(ES_CONN + "/" + ES_INDEX + "/Person/_search?q=firstname:Kelly", HttpStatus.OK_200);

//    String query = "{\n" +
//"  \"bool\" : {\n" +
//"    \"must\" : {\n" +
//"      \"match_all\" : { }\n" +
//"    },\n" +
//"    \"should\" : {\n" +
//"      \"match\" : {\n" +
//"        \"__forUser\" : {\n" +
//"          \"query\" : \"1000\",\n" +
//"          \"type\" : \"boolean\"\n" +
//"        }\n" +
//"      }\n" +
//"    }\n" +
//"  }\n" +
//"}";
//    
//    String query = "{" +
//          "\"match_all\" : {}" +
//        "}";
        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
            Assert.assertEquals("Estefania Bashirian", hits.get(0).source.getName());
            Assert.assertEquals("Wilton Emmerich", hits.get(1).source.getName());
            Assert.assertEquals("Emilie Bins", hits.get(2).source.getName());
        }

        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"," +
                    "          \"maxResultSize\": 50" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
        }

        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderMixedBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
//      assertEquals("Wilton Emmerich", hits.get(0).source.getName());
//      assertEquals("Emilie Bins", hits.get(1).source.getName());
//      assertEquals("Keegan Wolf", hits.get(2).source.getName());
        }


        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-filter\" :{" +
                    "          \"name\": \"GACypherQueryFilter\"," +
                    "          \"query\": \"MATCH (n:Person) RETURN n.uuid\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();
            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
//      assertEquals("148", hits.get(0).source.getDocumentId());
//      assertEquals("197", hits.get(1).source.getDocumentId());
//      assertEquals("102", hits.get(2).source.getDocumentId());
        }


        //String response = httpClient.get(ES_CONN + "/" + ES_INDEX + "/Company/_search?q=firstname:Kelly", HttpStatus.OK_200);
        String result1 = httpClient.get(baseUrl() + "/graphaware/recommendation/filter/Durgan%20LLC?limit=10&ids=148,197,27,4,5,6,7,8,9&keyProperty=uuid", HttpStatus.OK_200);

        //boolean res = response.contains("total\": 1");
        //assertEquals(res, true);

//    Get get = new Get.Builder(ES_INDEX, nodeId).type(car.name()).build();
//    JestResult result = null;
//
//    try
//    {
//      result = client.execute(get);
//    }
//    catch (IOException e)
//    {
//      e.printStackTrace();
//    }
//
//    notNull(result);
//    isTrue(result.isSucceeded());
    }
}
