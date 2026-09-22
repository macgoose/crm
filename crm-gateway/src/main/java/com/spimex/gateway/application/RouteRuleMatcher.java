package com.spimex.gateway.application;

import com.spimex.gateway.domain.AccessMode;
import com.spimex.gateway.domain.RouteRule;
import com.spimex.gateway.domain.RouteRuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class RouteRuleMatcher {

    private final RouteRuleRepository repository;
    private final PathPatternParser parser = new PathPatternParser();
    private volatile List<MatchedRouteRule> compiledRules = List.of();

    @PostConstruct
    public void refresh() {
        List<RouteRule> rules = repository.findAllByEnabledTrueOrderByRuleOrderAsc();
        validate(rules);
        compiledRules = rules.stream()
            .map(this::compile)
            .toList();
        log.info("Loaded and compiled {} enabled gateway route rules", compiledRules.size());
    }

    public List<RouteRule> rules() {
        return compiledRules.stream().map(MatchedRouteRule::rule).toList();
    }

    public Optional<RouteRule> match(HttpMethod method, String path) {
        Optional<RouteRule> result = compiledRules.stream()
            .filter(rule -> rule.matches(method, path))
            .map(MatchedRouteRule::rule)
            .findFirst();
        log.debug("Route match for method={} path={}: {}", method, path,
            result.map(RouteRule::getRouteKey).orElse("none"));
        return result;
    }

    private MatchedRouteRule compile(RouteRule rule) {
        return new MatchedRouteRule(
            rule,
            HttpMethod.valueOf(rule.getHttpMethod()),
            parser.parse(rule.getPathPattern())
        );
    }

    private void validate(List<RouteRule> rules) {
        Set<String> endpoints = new HashSet<>();
        for (RouteRule rule : rules) {
            requireText(rule.getRouteKey(), "routeKey");
            requireText(rule.getHttpMethod(), "httpMethod");
            requireText(rule.getPathPattern(), "pathPattern");
            requireText(rule.getTargetUri(), "targetUri");
            if ("*".equals(rule.getHttpMethod())) {
                throw new IllegalStateException(
                    "Route " + rule.getRouteKey() + " must declare an explicit HTTP method");
            }
            HttpMethod.valueOf(rule.getHttpMethod());
            parser.parse(rule.getPathPattern());
            URI target = URI.create(rule.getTargetUri());
            if (!("http".equalsIgnoreCase(target.getScheme())
                || "https".equalsIgnoreCase(target.getScheme()))
                || target.getHost() == null) {
                throw new IllegalStateException(
                    "Route " + rule.getRouteKey() + " has invalid HTTP targetUri");
            }

            if (rule.getAccessMode() == null) {
                throw new IllegalStateException("Route " + rule.getRouteKey() + " has no accessMode");
            }
            if (rule.getAccessMode() == AccessMode.PERMISSION
                && !hasText(rule.getPermissionCode())) {
                throw new IllegalStateException(
                    "PERMISSION route " + rule.getRouteKey() + " has no permissionCode");
            }
            if (rule.getAccessMode() == AccessMode.AUTHENTICATED
                && rule.getPermissionCode() != null) {
                throw new IllegalStateException(
                    "AUTHENTICATED route " + rule.getRouteKey() + " must not have permissionCode");
            }

            String endpoint = rule.getHttpMethod() + " " + rule.getPathPattern();
            if (!endpoints.add(endpoint)) {
                throw new IllegalStateException("Duplicate gateway endpoint: " + endpoint);
            }

            if (parser.parse(rule.getPathPattern()).matches(PathPatternParserSupport.path("/actuator/health"))) {
                throw new IllegalStateException(
                    "Database route " + rule.getRouteKey() + " must not match /actuator/health");
            }
        }
    }

    private static void requireText(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalStateException("Route " + field + " must not be blank");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
