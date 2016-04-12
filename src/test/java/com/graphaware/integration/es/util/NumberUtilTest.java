package com.graphaware.integration.es.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumberUtilTest {

    private static final int DEFAULT_VALUE = 10;

    @Test
    public void testDefaultValueIsReturned() {
        assertEquals(DEFAULT_VALUE, NumberUtil.getInt(null, DEFAULT_VALUE));
    }

    @Test
    public void testIntegersAreReturnedWhenIntegersGiven() {
        assertEquals(10, NumberUtil.getInt(10, DEFAULT_VALUE));
    }

    @Test
    public void testIntegerIsReturnedWhenFloatIsGiven() {
        assertEquals(12, NumberUtil.getInt(12.04f, DEFAULT_VALUE));
    }

    @Test
    public void testIntegerIsReturnedWhenStringIsGiven() {
        assertEquals(110, NumberUtil.getInt("110", DEFAULT_VALUE));
    }

    @Test
    public void testIntegerIsReturnedAsFloat() {
        assertEquals(10.0f, NumberUtil.getFloat(10), 0);
    }

    @Test
    public void testFloatIsReturnedAsFloat() {
        assertEquals(12.375f, NumberUtil.getFloat(12.375f), 0);
    }

    @Test
    public void testStringIsReturnedAsFloat() {
        assertEquals(12.678f, NumberUtil.getFloat("12.678"), 0);
    }

}
