package com.graphaware.integration.es;

import com.graphaware.integration.es.domain.ExternalResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;

import java.util.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.junit.After;

import static org.junit.Assert.*;
import org.junit.Before;
import org.mockserver.model.Header;
import org.mockserver.integration.ClientAndServer;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServerTest {

    private ClientAndServer mockServer;

    @Before
    public void startMockServer() {
        mockServer = startClientAndServer(1080);
    }
    
    @After
    public void stopProxy() {
        mockServer.stop();
    }
    
    private static final String LS = System.getProperty("line.separator");

    @Test
    public void testGetExternalResults() {
        mockServer
                .when(
                        request()
                                .withPath("/reco/12")
                )
                .respond(response()
                                .withHeaders(
                                        new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                                )
                                .withBody("" +
                                        "[" + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 1," + LS +
                                        "        \"objectId\": \"270\"," + LS +
                                        "        \"score\": 3" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 2," + LS +
                                        "        \"objectId\": \"123\"," + LS +
                                        "        \"score\": 3" + LS +
                                        "    }," + LS +
                                        "    {" + LS +
                                        "        \"nodeId\": 3," + LS +
                                        "        \"objectId\": \"456\"," + LS +
                                        "        \"score\": 3" + LS +
                                        "    }" + LS +
                                        "]")
                );
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        MultivaluedMap param = new MultivaluedMapImpl();
        param.add("limit", String.valueOf(Integer.MAX_VALUE));
        param.add("from", String.valueOf(0));
        param.add("keyProperty", "objectId");
        param.add("ids", "123,456");
        WebResource resource = Client.create(cfg).resource("http://localhost:1080/reco/12");
        ClientResponse response = resource
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, param);
        GenericType<List<ExternalResult>> type = new GenericType<List<ExternalResult>>() {
        };
        List<ExternalResult> externalResults = response.getEntity(type);
//        String entity = response.getEntity(String.class);
        assertNotNull(externalResults);
        assertEquals(externalResults.size(), 3);
    }
}
