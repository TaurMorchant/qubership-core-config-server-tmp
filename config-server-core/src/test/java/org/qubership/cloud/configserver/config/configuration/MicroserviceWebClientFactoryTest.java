package org.qubership.cloud.configserver.config.configuration;

import io.fabric8.mockwebserver.DefaultMockServer;
import io.fabric8.mockwebserver.MockServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.entity.RestClientResponseEntity;
import org.qubership.cloud.restclient.exception.MicroserviceRestClientResponseException;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.restclient.HttpMethod.GET;

class MicroserviceWebClientFactoryTest {

    private static final MockServer mockServer = new DefaultMockServer();

    @Test
    void createMicroserviceWebClientFactory_WithRetryPolicy() {
        MicroserviceRestClient client = new WebConfiguration().getMicroserviceRestClientFactory(1, 2, 3).create();

        mockServer.expect().get().withPath("/test").andReturn(202, "").once();
        RestClientResponseEntity<Void> response = client.doRequest(mockServer.url("/test"), GET, Map.of(), null, Void.class);
        Assertions.assertEquals(202, response.getHttpStatus());

        mockServer.expect().get().withPath("/test").andReturn(503, "").once();
        mockServer.expect().get().withPath("/test").andReturn(503, "").once();
        mockServer.expect().get().withPath("/test").andReturn(503, "").once();
        try {
            client.doRequest(mockServer.url("/test"), GET, new HashMap<>(), null, Void.class);
        } catch (MicroserviceRestClientResponseException e) {
            Assertions.assertEquals(503, e.getHttpStatus());
        }
        mockServer.expect().get().withPath("/test").andReturn(503, "").once();
        mockServer.expect().get().withPath("/test").andReturn(503, "").once();
        mockServer.expect().get().withPath("/test").andReturn(202, "").once();

        response = client.doRequest(mockServer.url("/test"), GET, new HashMap<>(), null, Void.class);
        Assertions.assertEquals(202, response.getHttpStatus());
    }
}