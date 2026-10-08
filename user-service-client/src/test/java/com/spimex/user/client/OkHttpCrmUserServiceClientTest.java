package com.spimex.user.client;

import com.spimex.user.client.dto.UserResolutionRequest;
import com.spimex.user.client.dto.UserResolutionResponse;
import com.spimex.user.client.dto.UserResponse;
import com.spimex.user.client.exception.CrmUserServiceHttpException;
import com.spimex.user.client.exception.CrmUserServiceProtocolException;
import com.spimex.user.client.exception.CrmUserServiceUnavailableException;
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
    void getsBasicUserInformationById() throws Exception {
        UUID id = UUID.randomUUID();
        server.enqueue(new MockResponse().setBody(userJson(id)));
        UserResponse response = client.getUser(id);
        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/users/" + id, request.getPath());
        assertEquals("application/json", request.getHeader("Accept"));
        assertEquals(id, response.getId());
        assertNull(response.getLogin());
        assertEquals("demo@example.test", response.getEmail());
        assertEquals("Full Name", response.getFio());
        assertEquals("Name", response.getShortName());
        assertFalse(response.getActive());
        assertEquals(2L, response.getVersion());
    }

    @Test
    void reportsMissingUserAsHttp404() {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertEquals(404, assertThrows(CrmUserServiceHttpException.class,
            () -> client.getUser(UUID.randomUUID())).getStatusCode());
    }

    @Test
    void rejectsNullUserIdWithoutSendingRequest() {
        assertThrows(NullPointerException.class, () -> client.getUser(null));
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void reportsUnavailableUserEndpoint() throws Exception {
        server.shutdown();
        assertThrows(CrmUserServiceUnavailableException.class,
            () -> client.getUser(UUID.randomUUID()));
    }

    private static String userJson(UUID id) {
        return "{\"id\":\"" + id + "\",\"login\":null,\"email\":\"demo@example.test\","
            + "\"fio\":\"Full Name\",\"shortName\":\"Name\",\"active\":false,\"version\":2}";
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
        assertEquals("/user-resolutions", request.getPath());
        assertEquals("POST", request.getMethod());
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
