package com.spimex.user.application;

import com.spimex.user.domain.model.user.User;
import com.spimex.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserResolutionServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final UserResolutionService service = new UserResolutionService(repository);

    @Test
    void resolvesActiveUserByNormalizedLogin() {
        User user = user(true);
        when(repository.findByLogin("demo.admin")).thenReturn(Optional.of(user));

        UserResolution result = service.resolve(" Demo.Admin ", null);

        assertTrue(result.resolved());
        assertEquals(user.getId(), result.userId());
        assertEquals(user.getVersion(), result.userVersion());
    }

    @Test
    void resolvesWhenLoginAndEmailIdentifySameUser() {
        User user = user(true);
        when(repository.findByLogin("demo.admin")).thenReturn(Optional.of(user));
        when(repository.findByEmail("admin@example.test")).thenReturn(Optional.of(user));

        UserResolution result = service.resolve("Demo.Admin", "ADMIN@example.test");

        assertTrue(result.resolved());
    }

    @Test
    void deniesConflictingLoginAndEmail() {
        User loginUser = user(true);
        User emailUser = user(true);
        when(repository.findByLogin("demo.admin")).thenReturn(Optional.of(loginUser));
        when(repository.findByEmail("other@example.test")).thenReturn(Optional.of(emailUser));

        UserResolution result = service.resolve("demo.admin", "other@example.test");

        assertFalse(result.resolved());
        assertEquals(DenialReason.IDENTITY_CONFLICT, result.denialReason());
    }

    @Test
    void deniesUnknownUser() {
        when(repository.findByLogin("missing")).thenReturn(Optional.empty());

        UserResolution result = service.resolve("missing", null);

        assertFalse(result.resolved());
        assertEquals(DenialReason.USER_NOT_FOUND, result.denialReason());
    }

    @Test
    void deniesInactiveUser() {
        User inactiveUser = user(false);
        when(repository.findByLogin("inactive")).thenReturn(Optional.of(inactiveUser));

        UserResolution result = service.resolve("inactive", null);

        assertFalse(result.resolved());
        assertEquals(DenialReason.USER_INACTIVE, result.denialReason());
        assertEquals(inactiveUser.getId(), result.userId());
        assertEquals(inactiveUser.getVersion(), result.userVersion());
    }

    private static User user(boolean active) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getVersion()).thenReturn(3L);
        when(user.isActive()).thenReturn(active);
        return user;
    }
}
