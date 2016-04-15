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

package com.graphaware.integration.es.filter;

import com.graphaware.integration.es.domain.IndexInfo;
import com.graphaware.integration.es.domain.TestIndexInfo;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class GraphFilterTest {

    private SearchResultCypherFilter filter;

    @Before
    public void setUp() {
        Settings.Builder builder = Settings.builder();
        IndexInfo indexInfo = TestIndexInfo.newInstance();
        filter = new SearchResultCypherFilter(builder.build(), indexInfo);
    }

    @Test
    public void testQueryParameterIsMandatory() {
        HashMap<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("gas-filter", new HashMap<String, Object>());

        try {
            filter.parseRequest(sourceMap);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Query Parameter is required"));
        }

    }

}
