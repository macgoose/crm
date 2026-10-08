package com.spimex.gateway.application;

import com.spimex.gateway.domain.RouteRule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.pattern.PathPattern;

@RequiredArgsConstructor
final class MatchedRouteRule {

    private final RouteRule rule;
    private final HttpMethod method;
    private final PathPattern pathPattern;

    RouteRule rule() {
        return rule;
    }

    boolean matches(HttpMethod candidateMethod, String path) {
        return method.equals(candidateMethod)
            && pathPattern.matches(PathPatternParserSupport.path(path));
    }
}
