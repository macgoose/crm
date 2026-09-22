package com.spimex.user.application;

import java.util.UUID;

public record UserResolution(
    boolean resolved,
    UUID userId,
    Long userVersion,
    DenialReason denialReason
) {
    public static UserResolution resolved(UUID userId, long userVersion) {
        return new UserResolution(true, userId, userVersion, null);
    }

    public static UserResolution denied(DenialReason reason) {
        return new UserResolution(false, null, null, reason);
    }

    public static UserResolution denied(DenialReason reason, UUID userId, long userVersion) {
        return new UserResolution(false, userId, userVersion, reason);
    }
}
