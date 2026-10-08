package com.spimex.identity;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@Getter
@EqualsAndHashCode
public final class InternalIdentity {

    private final UUID userId;
    private final long userVersion;

    public InternalIdentity(UUID userId, long userVersion) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        if (userVersion < 0) {
            throw new IllegalArgumentException("userVersion must not be negative");
        }
        this.userVersion = userVersion;
    }

    @Override
    public String toString() {
        return "InternalIdentity{userId=" + userId + ", userVersion=" + userVersion + '}';
    }
}
