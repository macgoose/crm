package com.spimex.user.application;

import com.spimex.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {

    @Test
    void authorizesOnlyByNormalizedPermission() {
        UserRepository userRepository = mock(UserRepository.class);
        UserResolutionService resolutionService = mock(UserResolutionService.class);
        UUID userId = UUID.randomUUID();
        when(resolutionService.resolve(" Demo.Admin ", null))
            .thenReturn(UserResolution.resolved(userId, 7));
        when(userRepository.hasPermission(userId, "ORGANIZATION_READ")).thenReturn(true);

        AuthorizationDecision decision = new AuthorizationService(userRepository, resolutionService)
            .authorize(" Demo.Admin ", null, " organization_read ");

        assertTrue(decision.allowed());
        verify(userRepository).hasPermission(userId, "ORGANIZATION_READ");
    }
}
