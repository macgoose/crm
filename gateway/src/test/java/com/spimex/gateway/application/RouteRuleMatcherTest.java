package com.spimex.gateway.application;

import com.spimex.gateway.domain.AccessMode;
import com.spimex.gateway.domain.RouteRule;
import com.spimex.gateway.domain.RouteRuleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RouteRuleMatcherTest {

    private final RouteRuleRepository repository = mock(RouteRuleRepository.class);

    @Test
    void rejectsWildcardMethodAtStartup() {
        RouteRule wildcard = rule("wildcard", "*", "/api/**", AccessMode.AUTHENTICATED, null);
        when(repository.findAllByEnabledTrueOrderByRuleOrderAsc())
            .thenReturn(List.of(wildcard));

        assertThrows(IllegalStateException.class, () -> new RouteRuleMatcher(repository).refresh());
    }

    @Test
    void rejectsPermissionModeWithoutPermissionAtStartup() {
        RouteRule invalid = rule("invalid", "GET", "/api/test", AccessMode.PERMISSION, null);
        when(repository.findAllByEnabledTrueOrderByRuleOrderAsc())
            .thenReturn(List.of(invalid));

        assertThrows(IllegalStateException.class, () -> new RouteRuleMatcher(repository).refresh());
    }

    @Test
    void rejectsDuplicateMethodAndPathAtStartup() {
        RouteRule first = rule("first", "POST", "/api/test", AccessMode.AUTHENTICATED, null);
        RouteRule second = rule("second", "POST", "/api/test", AccessMode.AUTHENTICATED, null);
        when(repository.findAllByEnabledTrueOrderByRuleOrderAsc()).thenReturn(List.of(first, second));

        assertThrows(IllegalStateException.class, () -> new RouteRuleMatcher(repository).refresh());
    }

    @Test
    void rejectsDatabaseRouteThatCapturesPublicHealth() {
        RouteRule actuator = rule(
            "actuator", "GET", "/actuator/**", AccessMode.AUTHENTICATED, null);
        when(repository.findAllByEnabledTrueOrderByRuleOrderAsc())
            .thenReturn(List.of(actuator));

        assertThrows(IllegalStateException.class, () -> new RouteRuleMatcher(repository).refresh());
    }

    private static RouteRule rule(
        String key,
        String method,
        String path,
        AccessMode mode,
        String permission
    ) {
        RouteRule rule = mock(RouteRule.class);
        when(rule.getRouteKey()).thenReturn(key);
        when(rule.getHttpMethod()).thenReturn(method);
        when(rule.getPathPattern()).thenReturn(path);
        when(rule.getAccessMode()).thenReturn(mode);
        when(rule.getPermissionCode()).thenReturn(permission);
        when(rule.getTargetUri()).thenReturn("http://localhost:8082");
        return rule;
    }
}
