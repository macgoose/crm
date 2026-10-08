package com.spimex.user.client.exception;

import lombok.Getter;

public final class CrmUserServiceHttpException extends CrmUserServiceException {

    @Getter
    private final int statusCode;

    public CrmUserServiceHttpException(int statusCode) {
        super("CRM user service returned HTTP " + statusCode);
        this.statusCode = statusCode;
    }

}
