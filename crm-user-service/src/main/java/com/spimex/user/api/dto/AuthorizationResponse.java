package com.spimex.user.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.spimex.user.application.AuthorizationDecision;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthorizationResponse(
        boolean allowed,
        UUID userId,
        Long userVersion,
        String denialReason
) {
    public static AuthorizationResponse from(AuthorizationDecision decision) {
        return new AuthorizationResponse(
                decision.allowed(),
                decision.userId(),
                decision.userVersion(),
                decision.denialReason() == null ? null : decision.denialReason().name());
    }
}
