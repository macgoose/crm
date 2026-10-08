package com.spimex.user.client.dto;

import com.spimex.user.client.exception.CrmUserServiceProtocolException;
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
        return response;
    }

    public static UserResolutionResponse denied(String denialReason) {
        UserResolutionResponse response = new UserResolutionResponse();
        response.denialReason = denialReason;
        return response;
    }
}
