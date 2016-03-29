/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.es.plugin.stubs;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.graphbooster.GraphAidedSearchResultScoreReplace;
import com.graphaware.integration.es.plugin.graphbooster.Neo4JFilterResult;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author ale
 */
@GraphAidedSearchBooster(name = "GraphAidedSearchReplaceBooster")
public class GraphAidedSearchReplaceBooster extends GraphAidedSearchResultScoreReplace {

    public GraphAidedSearchReplaceBooster(Settings settings, GASIndexInfo indexSettings) {
        super(settings, indexSettings);
    }

    @Override
    protected Map<String, Neo4JFilterResult> externalDoReorder(Set<String> keySet) {
        Map<String, Neo4JFilterResult> result = new HashMap<>();
        for (String id : keySet) {
            result.put(id, new Neo4JFilterResult(id, Integer.parseInt(id) * 1000));
        }
        return result;
    }
    
}
