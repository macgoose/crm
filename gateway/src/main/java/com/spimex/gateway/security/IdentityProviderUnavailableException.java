package com.spimex.gateway.security;

public final class IdentityProviderUnavailableException extends IdentityResolutionException {

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public IdentityProviderUnavailableException(String message) {
        super(message);
    }
}
