package com.spimex.user.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserResolutionRequestTest {

    @Test
    void trimsIdentityAttributes() {
        UserResolutionRequest request = new UserResolutionRequest(" demo ", " user@example.test ");

        assertEquals("demo", request.getLogin());
        assertEquals("user@example.test", request.getEmail());
    }

    @Test
    void requiresAtLeastOneIdentityAttribute() {
        assertThrows(IllegalArgumentException.class, () -> new UserResolutionRequest(" ", null));
    }
}
