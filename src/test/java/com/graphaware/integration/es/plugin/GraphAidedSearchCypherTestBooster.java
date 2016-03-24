package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.graphbooster.GraphAidedSearchCypherBooster;
import com.graphaware.integration.es.plugin.neo4j.CypherResultRow;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import org.elasticsearch.common.settings.Settings;

import java.util.HashMap;
import java.util.Map;

@GraphAidedSearchBooster(name = "GraphAidedSearchCypherTestBooster")
public class GraphAidedSearchCypherTestBooster extends GraphAidedSearchCypherBooster {

    public GraphAidedSearchCypherTestBooster(Settings settings, GASIndexInfo indexInfo) {
        super(settings, indexInfo);
    }

    @Override
    public Map<String, CypherResultRow> executeCypher(String serverUlr, String... statements) {

        Map<String, CypherResultRow> results = new HashMap<>();
        for (int i = 0; i < 1000; ++i) {
            //results.put()
        }

        return results;
    }
}
