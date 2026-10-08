package com.spimex.gateway.application;

import org.springframework.http.server.PathContainer;

final class PathPatternParserSupport {

    private PathPatternParserSupport() {
    }

    static PathContainer path(String value) {
        return PathContainer.parsePath(value);
    }
}
