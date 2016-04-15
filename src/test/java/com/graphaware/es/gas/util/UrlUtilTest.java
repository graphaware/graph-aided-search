package com.graphaware.es.gas.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UrlUtilTest {

    @Test
    public void buildEndpointTest() {
        String neo4jHost = "http://localhost:7474/";
        String boosterEndpoint = "/reco/engine//";
        String recoId = "15";
        String endpoint = UrlUtil.buildUrlFromParts(neo4jHost, boosterEndpoint, recoId);
        assertEquals("http://localhost:7474/reco/engine/15", endpoint);
    }
}
