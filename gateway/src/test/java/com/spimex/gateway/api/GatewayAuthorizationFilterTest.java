package com.spimex.gateway.api;

import com.spimex.gateway.application.RouteRuleMatcher;
import com.spimex.gateway.domain.AccessMode;
import com.spimex.gateway.domain.RouteRule;
import com.spimex.gateway.security.AvanpostIdentityResolver;
import com.spimex.gateway.security.ExternalIdentity;
import com.spimex.identity.IdentityHeaders;
import com.spimex.user.client.AuthorizationResponse;
import com.spimex.user.client.CrmUserServiceClient;
import com.spimex.user.client.UserResolutionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GatewayAuthorizationFilterTest {

    private final RouteRuleMatcher matcher = mock(RouteRuleMatcher.class);
    private final AvanpostIdentityResolver identityResolver = mock(AvanpostIdentityResolver.class);
    private final CrmUserServiceClient userServiceClient = mock(CrmUserServiceClient.class);
    private final GatewayAuthorizationFilter filter =
        new GatewayAuthorizationFilter(matcher, identityResolver, userServiceClient);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedRouteResolvesUserWithoutCheckingPermission() throws Exception {
        RouteRule rule = rule(AccessMode.AUTHENTICATED, null);
        UUID userId = UUID.randomUUID();
        authenticatedRequest(rule);
        when(userServiceClient.resolveUser(any()))
            .thenReturn(UserResolutionResponse.resolved(userId, 4));

        AtomicReference<String> propagatedUserId = new AtomicReference<>();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (request, ignored) ->
            propagatedUserId.set(((jakarta.servlet.http.HttpServletRequest) request)
                .getHeader(IdentityHeaders.USER_ID)));

        assertEquals(userId.toString(), propagatedUserId.get());
        verify(userServiceClient).resolveUser(any());
        verify(userServiceClient, never()).authorize(any());
    }

    @Test
    void permissionRouteUsesAuthorizationDecision() throws Exception {
        RouteRule rule = rule(AccessMode.PERMISSION, "ORGANIZATION_READ");
        UUID userId = UUID.randomUUID();
        authenticatedRequest(rule);
        when(userServiceClient.authorize(any()))
            .thenReturn(AuthorizationResponse.allowed(userId, 2));

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (request, ignored) -> { });

        assertEquals(200, response.getStatus());
        verify(userServiceClient).authorize(argThat(request ->
            "ORGANIZATION_READ".equals(request.getPermission())));
        verify(userServiceClient, never()).resolveUser(any());
    }

    @Test
    void unknownAuthenticatedRouteIsForbidden() throws Exception {
        authenticate();
        when(matcher.match(HttpMethod.POST, "/unknown")).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/unknown");
        request.setRequestURI("/unknown");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> fail("must deny"));

        assertEquals(403, response.getStatus());
    }

    @Test
    void onlyHealthEndpointSkipsFilter() {
        MockHttpServletRequest health = new MockHttpServletRequest("GET", "/actuator/health");
        health.setRequestURI("/actuator/health");
        MockHttpServletRequest metrics = new MockHttpServletRequest("GET", "/actuator/metrics");
        metrics.setRequestURI("/actuator/metrics");

        assertTrue(filter.shouldNotFilter(health));
        assertFalse(filter.shouldNotFilter(metrics));
    }

    private void authenticatedRequest(RouteRule rule) {
        authenticate();
        when(matcher.match(HttpMethod.POST, "/test")).thenReturn(Optional.of(rule));
        when(identityResolver.resolve(any(), nullable(String.class)))
            .thenReturn(new ExternalIdentity("demo", null));
    }

    private void authenticate() {
        Jwt jwt = mock(Jwt.class);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRequestURI("/test");
        return request;
    }

    private static RouteRule rule(AccessMode accessMode, String permission) {
        RouteRule rule = mock(RouteRule.class);
        when(rule.getAccessMode()).thenReturn(accessMode);
        when(rule.getPermissionCode()).thenReturn(permission);
        return rule;
    }
}
