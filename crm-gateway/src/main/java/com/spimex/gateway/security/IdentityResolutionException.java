package com.spimex.gateway.security;

public class IdentityResolutionException extends RuntimeException {

    public IdentityResolutionException(String message) {
        super(message);
    }

    public IdentityResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
