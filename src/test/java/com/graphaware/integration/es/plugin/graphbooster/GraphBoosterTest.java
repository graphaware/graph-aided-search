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
        HashMap<String, Object> sourceMap = new HashMap<>();
        HashMap<String, Object> externalParameters = new HashMap<>();
        externalParameters.put("operator", "+");
        sourceMap.put("gas-booster", externalParameters);
        booster.parseRequest(sourceMap);
        assertEquals("+", booster.getComposeScoreOperator());

        externalParameters.put("operator", "-");
        sourceMap.put("gas-booster", externalParameters);
        booster.parseRequest(sourceMap);
        assertEquals("-", booster.getComposeScoreOperator());

        externalParameters.put("operator", "/");

    }

}
