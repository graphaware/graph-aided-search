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

package com.graphaware.es.gas.booster;

import com.graphaware.es.gas.domain.IndexInfo;
import com.graphaware.es.gas.domain.TestIndexInfo;
import com.graphaware.es.gas.stubs.CypherSearchResultTestBooster;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class GraphBoosterTest {

    private SearchResultExternalBooster booster;

    @Before
    public void setUp() {
        Settings.Builder builder = Settings.builder();
        Settings settings = builder.build();
        IndexInfo indexInfo = TestIndexInfo.newInstance();
        booster = new CypherSearchResultTestBooster(settings, indexInfo);
    }

    @Test
    public void testComposeScoreIsSetByDefault() {
        assertEquals("*", booster.getComposeScoreOperator());
    }

    @Test
    public void testComposerScoreOperatorCanBeCustomized() throws Exception{

        booster.parseRequest(getBoosterSourceMap("+"));
        assertEquals("+", booster.getComposeScoreOperator());

        booster.parseRequest(getBoosterSourceMap("-"));
        assertEquals("-", booster.getComposeScoreOperator());

        booster.parseRequest(getBoosterSourceMap("/"));
        assertEquals("/", booster.getComposeScoreOperator());

        booster.parseRequest(getBoosterSourceMap("*"));
        assertEquals("*", booster.getComposeScoreOperator());

        booster.parseRequest(getBoosterSourceMap("replace"));
        assertEquals("replace", booster.getComposeScoreOperator());

        try {
            booster.parseRequest(getBoosterSourceMap("_"));
            assertTrue(false); // exception should be thrown
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Operator"));
        }
    }

    @Test
    public void testExceptionIsThrownWhenQueryParameterIsMissing() {
        HashMap<String, Object> sourceMap = new HashMap<>();
        HashMap<String, Object> externalParameters = new HashMap<>();
        sourceMap.put("gas-booster", externalParameters);
        try {
            booster.parseRequest(sourceMap);
            assertTrue(false); // If we come here then we have a bug
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("query parameter must not be null"));
        }
    }

    private HashMap<String, Object> getBoosterSourceMap(String operator) {
        HashMap<String, Object> sourceMap = new HashMap<>();
        HashMap<String, Object> externalParameters = new HashMap<>();
        externalParameters.put("query", "MATCH (n) RETURN n");
        externalParameters.put("operator", operator);
        sourceMap.put("gas-booster", externalParameters);

        return sourceMap;
    }
}
