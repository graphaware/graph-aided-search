package com.graphaware.integration.es.plugin.graphbooster;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.neo4j.CypherResultRow;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.graphaware.integration.es.plugin.query.GraphAidedSearch;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;

import javax.ws.rs.core.MediaType;
import java.util.*;

@GraphAidedSearchBooster(name = "GraphAidedSearchCypherBooster")
public class GraphAidedSearchCypherBooster implements IGraphAidedSearchResultBooster {

    private final String neo4jHost;

    private String cypherQuery;

    private String scoreResultName;

    private String idResultName;

    public GraphAidedSearchCypherBooster(GASIndexInfo indexInfo) {
        this.scoreResultName = "score";
        this.idResultName = "id";
        this.neo4jHost = indexInfo.getNeo4jHost();
    }

    @Override
    public InternalSearchHits doReorder(InternalSearchHits hits) {
        Map<String, CypherResultRow> remoteBooster = executeCypher(neo4jHost, cypherQuery);
        final InternalSearchHit[] searchHits = hits.internalHits();
        Map<String, InternalSearchHit> hitsMap = new HashMap<>();
        for (InternalSearchHit hit : searchHits) {
            hitsMap.put(hit.getId(), hit);
        }

        InternalSearchHit[] temporarySearchHits = new InternalSearchHit[hitsMap.size()];
        int k = 0;
        for (Map.Entry<String, InternalSearchHit> item : hitsMap.entrySet()) {
            if (remoteBooster.containsKey(item.getKey())) {
                temporarySearchHits[k] = item.getValue();
                ++k;
                float score = item.getValue().getScore();
                score += (Float) remoteBooster.get(item.getKey()).values().get(scoreResultName);
            }
        }

        return new InternalSearchHits(temporarySearchHits, hitsMap.size(), hits.maxScore());
    }

    @Override
    public void parseRequest(Map<String, Object> sourceAsMap) throws Exception{
        HashMap extParams = (HashMap) sourceAsMap.get(GraphAidedSearch.GAS_BOOSTER_CLAUSE);
        scoreResultName = extParams.containsKey("score_result_name") ? extParams.get("score_result_name").toString() : scoreResultName;
        idResultName = extParams.containsKey("id_result_name") ? extParams.get("id_result_name").toString() : idResultName;
        if (!extParams.containsKey("query")) {
            throw new Exception("Missing the \"query\" key for GraphAidedSearchCypherBooster");
        }
        cypherQuery = extParams.get("query").toString();
    }

    public Map<String, CypherResultRow> executeCypher(String serverUrl, String... cypherStatements) {
        StringBuilder stringBuilder = new StringBuilder("{\"statements\" : [");
        for (String statement : cypherStatements) {
            stringBuilder.append("{\"statement\" : \"").append(statement).append("\"}").append(",");
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        stringBuilder.append("]}");

        while (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        return post(serverUrl + "/db/data/transaction/commit", stringBuilder.toString());
    }

    public Map<String, CypherResultRow> post(String url, String json) {
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        WebResource resource = Client.create(cfg).resource(url);
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .entity(json)
                .post(ClientResponse.class);
        GenericType<Map<String, Object>> type = new GenericType<Map<String, Object>>() {
        };
        Map<String, Object> results = response.getEntity(type);

        Map res = (Map) ((ArrayList) results.get("results")).get(0);
        ArrayList<LinkedHashMap> rows = (ArrayList) res.get("data");
        List<String> columns = (List) res.get("columns");
        response.close();
        int k = 0;
        Map<String, Integer> columnsMap = new HashMap<>();
        for (String c : columns) {
            columnsMap.put(c, k);
            ++k;
        }
        Map<String, CypherResultRow> resultRows = new HashMap<>();
        for (Iterator<LinkedHashMap> it = rows.iterator(); it.hasNext();) {
            CypherResultRow resultRow = new CypherResultRow();
            LinkedHashMap row = it.next();
            resultRow.values().put(idResultName, row.get(columnsMap.get(idResultName)));
            resultRow.values().put(scoreResultName, row.get(columnsMap.get(scoreResultName)));
            resultRows.put(idResultName, resultRow);
        }
        return resultRows;
    }
}
