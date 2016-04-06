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

package com.graphaware.integration.es.plugin.graphbooster;

import com.graphaware.integration.es.plugin.query.GASIndexInfo;
import com.graphaware.integration.es.plugin.query.GASIndexInfoTest;
import com.graphaware.integration.es.plugin.stubs.GraphAidedSearchCypherTestBooster;
import org.elasticsearch.common.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class GraphBoosterTest {

    private GraphAidedSearchResultBooster booster;

    @Before
    public void setUp() {
        Settings.Builder builder = Settings.builder();
        Settings settings = builder.build();
        GASIndexInfo indexInfo = GASIndexInfoTest.newInstance();
        booster = new GraphAidedSearchCypherTestBooster(settings, indexInfo);
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
            assertTrue(e.getMessage().contains("Query Parameter cannot be null"));
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
