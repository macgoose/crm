package com.spimex.user.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.spimex.user.application.UserResolution;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResolutionResponse(
    boolean resolved,
    UUID userId,
    Long userVersion,
    String denialReason
) {
    public static UserResolutionResponse from(UserResolution resolution) {
        return new UserResolutionResponse(
            resolution.resolved(),
            resolution.userId(),
            resolution.userVersion(),
            resolution.denialReason() == null ? null : resolution.denialReason().name()
        );
    }
}
