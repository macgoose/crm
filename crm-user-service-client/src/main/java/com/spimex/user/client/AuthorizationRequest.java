package com.spimex.user.client;

import lombok.Getter;

@Getter
public final class AuthorizationRequest {

    private final String login;
    private final String email;
    private final String permission;

    public AuthorizationRequest(String login, String email, String permission) {
        if (!hasText(login) && !hasText(email)) {
            throw new IllegalArgumentException("login or email must be provided");
        }
        requireText(permission, "permission");
        this.login = trimToNull(login);
        this.email = trimToNull(email);
        this.permission = trimToNull(permission);
    }

    private static String requireText(String value, String name) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
