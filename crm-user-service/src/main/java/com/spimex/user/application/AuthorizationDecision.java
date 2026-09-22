package com.spimex.user.application;

import java.util.UUID;

public record AuthorizationDecision(
        boolean allowed,
        UUID userId,
        Long userVersion,
        DenialReason denialReason
) {
    public static AuthorizationDecision allowed(UUID userId, long userVersion) {
        return new AuthorizationDecision(true, userId, userVersion, null);
    }

    public static AuthorizationDecision denied(DenialReason reason) {
        return new AuthorizationDecision(false, null, null, reason);
    }

    public static AuthorizationDecision denied(DenialReason reason, UUID userId, long userVersion) {
        return new AuthorizationDecision(false, userId, userVersion, reason);
    }
}
