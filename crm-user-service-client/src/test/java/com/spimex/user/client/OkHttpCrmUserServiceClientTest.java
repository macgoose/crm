package com.spimex.user.client;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OkHttpCrmUserServiceClientTest {

    private MockWebServer server;
    private CrmUserServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new OkHttpCrmUserServiceClient(server.url("/").toString(), new OkHttpClient());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void resolvesUserThroughDedicatedEndpoint() throws Exception {
        UUID userId = UUID.randomUUID();
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"resolved\":true,\"userId\":\"" + userId + "\",\"userVersion\":5}"));

        UserResolutionResponse response = client.resolveUser(new UserResolutionRequest("demo", null));
        RecordedRequest request = server.takeRequest();

        assertTrue(response.isResolved());
        assertEquals(userId, response.getUserId());
        assertEquals(5L, response.getUserVersion());
        assertEquals("/internal/v1/user-resolutions", request.getPath());
        assertEquals("POST", request.getMethod());
    }

    @Test
    void rejectsMalformedResolvedResponse() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"resolved\":true}"));

        assertThrows(CrmUserServiceProtocolException.class,
            () -> client.resolveUser(new UserResolutionRequest("demo", null)));
    }

    @Test
    void acceptsNormalNegativeResolution() {
        server.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"resolved\":false,\"denialReason\":\"USER_NOT_FOUND\"}"));

        UserResolutionResponse response = client.resolveUser(new UserResolutionRequest("missing", null));

        assertFalse(response.isResolved());
        assertEquals("USER_NOT_FOUND", response.getDenialReason());
    }
}
