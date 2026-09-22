package com.spimex.user.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresPermission() {
        assertFalse(validator.validate(new AuthorizationRequest("demo.admin", null, null)).isEmpty());
        assertFalse(validator.validate(new AuthorizationRequest("demo.admin", null, " ")).isEmpty());
    }

    @Test
    void acceptsPermissionWithEitherIdentityAttribute() {
        assertTrue(validator.validate(
            new AuthorizationRequest("demo.admin", null, "ORGANIZATION_READ")
        ).isEmpty());
        assertTrue(validator.validate(
            new AuthorizationRequest(null, "demo.admin@example.local", "ORGANIZATION_READ")
        ).isEmpty());
    }
}
