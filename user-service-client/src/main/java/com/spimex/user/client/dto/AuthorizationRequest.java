package com.spimex.user.client.dto;

import lombok.Getter;

import static com.spimex.user.client.util.StringUtils.*;

@Getter
public final class AuthorizationRequest {

    private final String login;
    private final String email;
    private final String permission;

    public AuthorizationRequest(String login, String email, String permission) {
        if (!hasText(login) && !hasText(email))
            throw new IllegalArgumentException("login or email must be provided");

        requireText(permission, "permission");
        this.login = trimToNull(login);
        this.email = trimToNull(email);
        this.permission = trimToNull(permission);
    }
}
