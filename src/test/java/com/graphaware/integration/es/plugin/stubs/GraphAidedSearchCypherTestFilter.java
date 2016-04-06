/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.graphaware.integration.es.plugin.stubs;

import com.graphaware.integration.es.plugin.annotation.GraphAidedSearchFilter;
import com.graphaware.integration.es.plugin.graphfilter.GraphAidedSearchCypherFilter;
import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import java.util.HashSet;
import java.util.Set;
import org.elasticsearch.common.settings.Settings;

/**
 *
 * @author alessandro@graphaware.com
 */
@GraphAidedSearchFilter(name = "GraphAidedSearchCypherTestFilter")
public class GraphAidedSearchCypherTestFilter extends GraphAidedSearchCypherFilter {

    public GraphAidedSearchCypherTestFilter(Settings settings, GASIndexInfo indexSettings) {
        super(settings, indexSettings);
    }
    
    public Set<String> executeCypher(String serverUrl, String... cypherStatements) {
        Set<String> result = new HashSet<>();
        for (int i = 1; i <= 1000; i++) {
            if (i%3 == 0) {
                result.add(String.valueOf(i));
            }
        }
        return result;
    }
}
