package com.graphaware.integration.es.plugin;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.graphbooster.GraphAidedSearchCypherBooster;
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
    protected Map<String, Float> executeCypher(String serverUrl, String... cypherStatements) {

        Map<String, Float> results = new HashMap<>();
        for (int i = 0; i < 1000; ++i) {
            results.put(String.valueOf(i), 1000.0f*i);
        }
        return results;
    }
}
