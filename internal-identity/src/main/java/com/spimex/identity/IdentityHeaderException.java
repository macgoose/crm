package com.spimex.identity;

import lombok.Getter;

public final class IdentityHeaderException extends IllegalArgumentException {

    public enum Reason {
        MISSING_USER_ID,
        INVALID_USER_ID,
        MISSING_USER_VERSION,
        INVALID_USER_VERSION
    }

    @Getter
    private final Reason reason;

    public IdentityHeaderException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

}
