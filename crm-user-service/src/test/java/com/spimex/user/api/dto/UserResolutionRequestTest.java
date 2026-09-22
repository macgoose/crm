package com.spimex.user.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserResolutionRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresLoginOrEmail() {
        assertFalse(validator.validate(new UserResolutionRequest(null, null)).isEmpty());
        assertFalse(validator.validate(new UserResolutionRequest(" ", " ")).isEmpty());
    }

    @Test
    void acceptsEitherIdentityAttribute() {
        assertTrue(validator.validate(new UserResolutionRequest("demo.admin", null)).isEmpty());
        assertTrue(validator.validate(
            new UserResolutionRequest(null, "demo.admin@example.local")).isEmpty());
    }
}
