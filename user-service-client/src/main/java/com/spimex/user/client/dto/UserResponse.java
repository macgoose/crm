package com.spimex.user.client.dto;

import lombok.Getter;
import java.util.UUID;

@Getter
public final class UserResponse {
    private final UUID id;
    private final String login;
    private final String email;
    private final String fio;
    private final String shortName;
    private final Boolean active;
    private final Long version;

    public UserResponse(UUID id, String login, String email, String fio,
                        String shortName, boolean active, long version) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.fio = fio;
        this.shortName = shortName;
        this.active = active;
        this.version = version;
    }
}
