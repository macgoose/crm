package com.spimex.user.client.dto;

import com.spimex.user.client.exception.CrmUserServiceProtocolException;
import lombok.Getter;

import java.util.UUID;

@Getter
public final class AuthorizationResponse {

    private boolean allowed;
    private UUID userId;
    private Long userVersion;
    private String denialReason;

    public static AuthorizationResponse allowed(UUID userId, long userVersion) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.allowed = true;
        response.userId = userId;
        response.userVersion = userVersion;
        return response;
    }

    public static AuthorizationResponse denied(String denialReason) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.allowed = false;
        response.denialReason = denialReason;
        return response;
    }
}
