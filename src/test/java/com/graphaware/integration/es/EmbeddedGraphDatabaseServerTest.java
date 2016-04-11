/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.graphaware.integration.es;

import com.graphaware.integration.neo4j.test.EmbeddedGraphDatabaseServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ale
 */
public class EmbeddedGraphDatabaseServerTest {
    private EmbeddedGraphDatabaseServer server;
    
    public EmbeddedGraphDatabaseServerTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        server = new EmbeddedGraphDatabaseServer();
        server.start();
    }
    
    @After
    public void tearDown() {
        server.stop();
    }
    
    @Test
    public void someTest() {
        System.out.print("But a bp here");
    }
    
}
