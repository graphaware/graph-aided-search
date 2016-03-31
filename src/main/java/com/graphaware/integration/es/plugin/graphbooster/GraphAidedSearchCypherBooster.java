package com.graphaware.integration.es.plugin.graphbooster;

import com.graphaware.integration.es.plugin.GraphAidedSearchPlugin;
import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.common.settings.Settings;

import javax.ws.rs.core.MediaType;
import java.util.*;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

@GraphAidedSearchBooster(name = "GraphAidedSearchCypherBooster")
public class GraphAidedSearchCypherBooster extends GraphAidedSearchResultBooster {

    private final ESLogger logger;

    private static final String DEFAULT_SCORE_RESULT_NAME = "score";
    private static final String DEFAULT_ID_RESULT_NAME = "id";

    private String cypherQuery;
    private String scoreResultName;
    private String idResultName;

    public GraphAidedSearchCypherBooster(Settings settings, GASIndexInfo indexInfo) {
        super(settings, indexInfo);
        this.logger = Loggers.getLogger(GraphAidedSearchPlugin.INDEX_LOGGER_NAME, settings);
    }

    

    @Override
    protected void extendedParseRequest(HashMap extParams) {
        cypherQuery = (String) (extParams.get("query"));
        scoreResultName = extParams.get("scoreName") != null ? (String) extParams.get("scoreName") : DEFAULT_SCORE_RESULT_NAME;
        idResultName = extParams.get("identifier") != null ? (String) extParams.get("identifier") : DEFAULT_ID_RESULT_NAME;
        if (null == cypherQuery) {
            throw new RuntimeException("The Query Parameter cannot be null in gas-booster");
        }
    }

    @Override
    protected Map<String, Neo4JFilterResult> externalDoReorder(Set<String> keySet) {
        logger.warn("Query cypher for: " + keySet);
        Map<String, Float> res = executeCypher(getNeo4jHost(), cypherQuery);

        HashMap<String, Neo4JFilterResult> results = new HashMap<>();

        for (Map.Entry<String, Float> item : res.entrySet()) 
            results.put(item.getKey(), new Neo4JFilterResult(item.getKey(), item.getValue()));
        return results;
    }

    protected Map<String, Float> executeCypher(String serverUrl, String... cypherStatements) {
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

    protected Map<String, Float> post(String url, String json) {
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
        try {
            System.out.println(ObjectMapper.class.newInstance().writeValueAsString(results));
        } catch (Exception e) {
            //
        }
        @SuppressWarnings("unchecked")
        ArrayList<HashMap<String, Object>> errors = (ArrayList) results.get("errors");
        if (errors.size() > 0) {
            throw new RuntimeException("Cypher Execution Error, message is : " + errors.get(0).toString());
        }

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
        Map<String, Float> resultRows = new HashMap<>();
        for (Iterator<LinkedHashMap> it = rows.iterator(); it.hasNext();) {
            LinkedHashMap r = it.next();
            ArrayList row = (ArrayList)r.get("row");
            String key = String.valueOf(row.get(columnsMap.get(idResultName)));
            float value = Float.parseFloat(String.valueOf(row.get(columnsMap.get(scoreResultName))));
            resultRows.put(key, value);
        }
        return resultRows;
    }
}
