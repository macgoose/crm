package com.spimex.user.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorizationRequest(
    @Size(max = 255) String login,
    @Size(max = 320) String email,
    @NotBlank @Size(max = 100) String permission
) {

    @AssertTrue(message = "login or email must be provided")
    public boolean isIdentityPresent() {
        return hasText(login) || hasText(email);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
