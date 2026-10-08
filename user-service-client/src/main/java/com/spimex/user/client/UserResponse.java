package com.spimex.user.client;

import lombok.Getter;
import java.util.UUID;

/** Basic user information from the internal user lookup endpoint. */
@Getter
public final class UserResponse {
    private UUID id;
    private String login;
    private String email;
    private String fio;
    private String shortName;
    private Boolean active;
    private Long version;

    public UserResponse(UUID id, String login, String email, String fio,
                        String shortName, boolean active, long version) {
        this.id = id;
        this.login = login;
        this.email = email;
        this.fio = fio;
        this.shortName = shortName;
        this.active = active;
        this.version = version;
        validate();
    }

    void validate() {
        if (id == null || fio == null || shortName == null || active == null
            || version == null || version < 0) {
            throw new CrmUserServiceProtocolException(
                "User response is missing required fields or has an invalid version");
        }
    }
}
