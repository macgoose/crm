package com.spimex.user.client.exception;

public class CrmUserServiceException extends RuntimeException {

    public CrmUserServiceException(String message) {
        super(message);
    }

    public CrmUserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
