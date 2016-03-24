package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.graphbooster.GraphAidedSearchCypherBooster;
import com.graphaware.integration.es.plugin.neo4j.CypherResultRow;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;

import java.util.HashMap;
import java.util.Map;

@GraphAidedSearchBooster(name = "GraphAidedSearchCypherTestBooster")
public class GraphAidedSearchCypherTestBooster extends GraphAidedSearchCypherBooster {

    public GraphAidedSearchCypherTestBooster(GASIndexInfo indexInfo) {
        super(indexInfo);
    }

    public Map<String, CypherResultRow> executeCypher(String serverUlr, String... statements) {

        Map<String, CypherResultRow> results = new HashMap<>();
        for (int i = 0; i < 1000; ++i) {
            //results.put()
        }

        return results;
    }
}
