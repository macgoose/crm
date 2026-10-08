package com.spimex.gateway.security;

import com.spimex.gateway.api.GatewayAuthorizationFilter;
import com.spimex.gateway.application.RouteRuleMatcher;
import com.spimex.user.client.CrmUserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Configuration
@Slf4j
public class SecurityConfiguration {

    @Bean
    GatewayAuthorizationFilter gatewayAuthorizationFilter(
        RouteRuleMatcher routeRuleMatcher,
        AvanpostIdentityResolver identityResolver,
        CrmUserServiceClient userServiceClient) {
        return new GatewayAuthorizationFilter(routeRuleMatcher, identityResolver, userServiceClient);
    }

    @Bean
    FilterRegistrationBean<GatewayAuthorizationFilter> disableContainerRegistration(
        GatewayAuthorizationFilter filter) {
        FilterRegistrationBean<GatewayAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        GatewayAuthorizationFilter gatewayAuthorizationFilter
    ) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(this::authenticationEntryPoint)
            )
            .addFilterAfter(gatewayAuthorizationFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private void authenticationEntryPoint(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        int status = exception instanceof AuthenticationServiceException
            ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
            : HttpServletResponse.SC_UNAUTHORIZED;

        log.warn("JWT authentication failed for method={} path={} status={}: {}",
            request.getMethod(), request.getRequestURI(), status, exception.getMessage());
        response.sendError(status);
    }
}
