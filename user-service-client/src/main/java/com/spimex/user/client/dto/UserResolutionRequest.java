package com.spimex.user.client.dto;

import lombok.Getter;

import static com.spimex.user.client.util.StringUtils.hasText;
import static com.spimex.user.client.util.StringUtils.trimToNull;

@Getter
public final class UserResolutionRequest {
    private final String login;
    private final String email;

    public UserResolutionRequest(String login, String email) {
        if (!hasText(login) && !hasText(email))
            throw new IllegalArgumentException("login or email must be provided");

        this.login = trimToNull(login);
        this.email = trimToNull(email);
    }
}
