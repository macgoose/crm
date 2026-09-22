package com.spimex.gateway.routing;

import com.spimex.gateway.domain.RouteRule;
import com.spimex.gateway.application.RouteRuleMatcher;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.function.*;

import java.net.URI;

@Configuration
@Slf4j
public class GatewayRoutesConfiguration {

    @Bean
    RouterFunction<ServerResponse> databaseGatewayRoutes(RouteRuleMatcher routeRuleMatcher) {
        RouterFunction<ServerResponse> routes = null;

        for (RouteRule rule : routeRuleMatcher.rules()) {
            log.debug("Registering gateway route routeKey={} method={} path={} target={}",
                rule.getRouteKey(), rule.getHttpMethod(), rule.getPathPattern(), rule.getTargetUri());
            RequestPredicate predicate = RequestPredicates
                .method(HttpMethod.valueOf(rule.getHttpMethod()))
                .and(RequestPredicates.path(rule.getPathPattern()));

            URI target = URI.create(rule.getTargetUri());
            RouterFunctions.Builder builder = GatewayRouterFunctions
                .route(rule.getRouteKey())
                .route(predicate, HandlerFunctions.http())
                .before(BeforeFilterFunctions.uri(target.resolve("/")));

            if (
                target.getRawPath() != null &&
                !target.getRawPath().isBlank()
                && !"/".equals(target.getRawPath())
            ) {
                builder.before(BeforeFilterFunctions.setPath(target.getRawPath()));
            }

            RouterFunction<ServerResponse> route = builder.build();
            routes = routes == null ? route : routes.and(route);
        }

        if (routes == null) {
            log.warn("No enabled gateway routes were configured");
            return RouterFunctions.route(
                RequestPredicates.path("/__no_gateway_routes__"),
                request -> ServerResponse.notFound().build()
            );
        }

        log.info("Registered {} database-backed gateway routes", routeRuleMatcher.rules().size());
        return routes;
    }
}
