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
package com.graphaware.integration.es.plugin.stubs;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchBooster;
import com.graphaware.integration.es.plugin.graphbooster.GraphAidedSearchResultBooster;
import com.graphaware.integration.es.plugin.graphbooster.Neo4JFilterResult;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author alessandro@graphaware.com
 */
@GraphAidedSearchBooster(name = "GraphAidedSearchTestBooster")
public class GraphAidedSearchTestBooster extends GraphAidedSearchResultBooster {


    public GraphAidedSearchTestBooster(Settings settings, GASIndexInfo indexSettings) {
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
