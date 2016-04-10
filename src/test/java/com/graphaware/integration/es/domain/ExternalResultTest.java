package com.graphaware.integration.es.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalResultTest {

    @Test
    public void testNewInstance() {
        ExternalResult result = new ExternalResult("123", 1.25f);
        assertEquals("123", result.getObjectId());
        assertEquals(1.25f, result.getScore(), 0);
    }
}
