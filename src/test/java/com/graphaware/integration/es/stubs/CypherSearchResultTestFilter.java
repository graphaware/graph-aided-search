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
package com.graphaware.integration.es.stubs;

import com.graphaware.integration.es.annotation.SearchFilter;
import com.graphaware.integration.es.domain.CypherResult;
import com.graphaware.integration.es.domain.ResultRow;
import com.graphaware.integration.es.filter.SearchResultCypherFilter;
import com.graphaware.integration.es.IndexInfo;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.common.settings.Settings;

@SearchFilter(name = "CypherSearchResultTestFilter")
public class CypherSearchResultTestFilter extends SearchResultCypherFilter {

    public CypherSearchResultTestFilter(Settings settings, IndexInfo indexSettings) {
        super(settings, indexSettings);
    }

    @Override
    protected Set<String> getFilteredItems() {
        Set<String> result = new HashSet<>();
        for (int i = 1; i <= 1000; i++) {
            if (i%3 == 0) {
                result.add(String.valueOf(i));
            }
        }
        return result;
    }
}
