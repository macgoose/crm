package com.spimex.user.client;

import lombok.Getter;

@Getter
public final class UserResolutionRequest {
    private final String login;
    private final String email;

    public UserResolutionRequest(String login, String email) {
        if (!hasText(login) && !hasText(email)) {
            throw new IllegalArgumentException("login or email must be provided");
        }
        this.login = trimToNull(login);
        this.email = trimToNull(email);
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
