package com.spimex.gateway.api;

import com.spimex.gateway.application.RouteRuleMatcher;
import com.spimex.gateway.domain.AccessMode;
import com.spimex.gateway.domain.RouteRule;
import com.spimex.gateway.security.AvanpostIdentityResolver;
import com.spimex.gateway.security.ExternalIdentity;
import com.spimex.gateway.security.IdentityProviderUnavailableException;
import com.spimex.gateway.security.IdentityResolutionException;
import com.spimex.identity.InternalIdentity;
import com.spimex.user.client.AuthorizationRequest;
import com.spimex.user.client.AuthorizationResponse;
import com.spimex.user.client.CrmUserServiceClient;
import com.spimex.user.client.CrmUserServiceException;
import com.spimex.user.client.UserResolutionRequest;
import com.spimex.user.client.UserResolutionResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class GatewayAuthorizationFilter extends OncePerRequestFilter {

    private final RouteRuleMatcher routeRuleMatcher;
    private final AvanpostIdentityResolver identityResolver;
    private final CrmUserServiceClient userServiceClient;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/actuator/health")
            || request.getRequestURI().startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        RouteRule rule = routeRuleMatcher
            .match(HttpMethod.valueOf(request.getMethod()), request.getRequestURI())
            .orElse(null);

        if (rule == null) {
            log.warn("Access denied: no route rule for {} {}", request.getMethod(), request.getRequestURI());
            reject(response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED");
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            log.warn("Unauthenticated request to {} {}", request.getMethod(), request.getRequestURI());
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED");
            return;
        }

        try {
            ExternalIdentity externalIdentity = identityResolver.resolve(
                jwtAuthentication.getToken(), bearerToken(request)
            );

            InternalIdentity internalIdentity = resolveInternalIdentity(rule, externalIdentity);
            if (internalIdentity == null) {
                log.warn("Access denied for routeKey={} method={} path={}",
                    rule.getRouteKey(), request.getMethod(), request.getRequestURI());
                reject(response, HttpServletResponse.SC_FORBIDDEN, "ACCESS_DENIED");
                return;
            }

            log.debug("Access granted: routeKey={} method={} path={} userId={}",
                rule.getRouteKey(), request.getMethod(), request.getRequestURI(), internalIdentity.getUserId());
            filterChain.doFilter(new TrustedIdentityRequest(request, internalIdentity), response);
        } catch (IdentityResolutionException exception) {
            int status = exception instanceof IdentityProviderUnavailableException
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                : HttpServletResponse.SC_FORBIDDEN;

            log.warn("Identity resolution failed for routeKey={} method={} path={} status={}: {}",
                rule.getRouteKey(), request.getMethod(), request.getRequestURI(), status, exception.getMessage());
            reject(response, status, status == 503 ? "IDENTITY_PROVIDER_UNAVAILABLE" : "ACCESS_DENIED");
        } catch (CrmUserServiceException exception) {
            log.warn("User service call failed for routeKey={} method={} path={}: {}",
                rule.getRouteKey(), request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
            reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "USER_SERVICE_UNAVAILABLE");
        }
    }

    private InternalIdentity resolveInternalIdentity(RouteRule rule, ExternalIdentity externalIdentity) {
        log.debug("Resolving internal identity for routeKey={} accessMode={} loginPresent={} emailPresent={}",
            rule.getRouteKey(), rule.getAccessMode(), externalIdentity.login() != null, externalIdentity.email() != null);
        if (rule.getAccessMode() == AccessMode.AUTHENTICATED) {
            UserResolutionResponse resolution = userServiceClient.resolveUser(
                new UserResolutionRequest(externalIdentity.login(), externalIdentity.email())
            );
            return resolution.isResolved()
                ? new InternalIdentity(resolution.getUserId(), resolution.getUserVersion())
                : null;
        }

        AuthorizationResponse decision = userServiceClient.authorize(new AuthorizationRequest(
            externalIdentity.login(),
            externalIdentity.email(),
            rule.getPermissionCode()
        ));
        return decision.isAllowed()
            ? new InternalIdentity(decision.getUserId(), decision.getUserVersion())
            : null;
    }

    private static String bearerToken(HttpServletRequest request) {
        String value = request.getHeader("Authorization");
        if (value == null || !value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return value.substring(7).trim();
    }

    private static void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
