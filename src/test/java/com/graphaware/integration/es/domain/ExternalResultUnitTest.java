package com.graphaware.integration.es.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalResultUnitTest {

    @Test
    public void testNewInstance() {
        ExternalResult externalResult = new ExternalResult("123", 23.8f);
        assertEquals("123", externalResult.getObjectId());
        assertEquals(23.8f, externalResult.getScore(), 0);
        assertNull(externalResult.getItem());
    }

    @Test
    public void testMethods() {
        ExternalResult externalResult = new ExternalResult("123", 12.0f);
        externalResult.setItem("123fff");
        externalResult.setNodeId(1L);
        externalResult.setScore(15.23f);
        externalResult.setObjectId("dd-23-ee");
        assertEquals("123fff", externalResult.getItem());
        assertEquals(1L, externalResult.getNodeId());
        assertEquals(15.23f, externalResult.getScore(), 0);
        assertEquals("dd-23-ee", externalResult.getObjectId());
    }

}
