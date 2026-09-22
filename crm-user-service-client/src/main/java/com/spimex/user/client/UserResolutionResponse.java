package com.spimex.user.client;

import lombok.Getter;

import java.util.UUID;

@Getter
public final class UserResolutionResponse {
    private boolean resolved;
    private UUID userId;
    private Long userVersion;
    private String denialReason;

    public static UserResolutionResponse resolved(UUID userId, long userVersion) {
        UserResolutionResponse response = new UserResolutionResponse();
        response.resolved = true;
        response.userId = userId;
        response.userVersion = userVersion;
        response.validate();
        return response;
    }

    public static UserResolutionResponse denied(String denialReason) {
        UserResolutionResponse response = new UserResolutionResponse();
        response.denialReason = denialReason;
        response.validate();
        return response;
    }

    void validate() {
        if (resolved && (userId == null || userVersion == null || userVersion < 0)) {
            throw new CrmUserServiceProtocolException(
                "Resolved user response must contain userId and userVersion");
        }
        if (!resolved && (denialReason == null || denialReason.trim().isEmpty())) {
            throw new CrmUserServiceProtocolException(
                "Unresolved user response must contain denialReason");
        }
    }
}
