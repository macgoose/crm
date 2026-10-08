package com.spimex.user.application;

import com.spimex.user.client.UserResponse;
import com.spimex.user.domain.model.user.User;
import com.spimex.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserQueryServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserQueryService service = new UserQueryService(repository);

    @Test
    void mapsBasicUserInformationWithoutFilteringInactiveUsers() {
        UUID id = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getLogin()).thenReturn("demo");
        when(user.getEmail()).thenReturn("demo@example.test");
        when(user.getFio()).thenReturn("Full Name");
        when(user.getShortName()).thenReturn("Name");
        when(user.isActive()).thenReturn(false);
        when(user.getVersion()).thenReturn(7L);
        when(repository.findById(id)).thenReturn(Optional.of(user));
        UserResponse result = service.findById(id).orElseThrow();
        assertEquals(id, result.getId());
        assertEquals("demo", result.getLogin());
        assertEquals("demo@example.test", result.getEmail());
        assertEquals("Full Name", result.getFio());
        assertEquals("Name", result.getShortName());
        assertFalse(result.getActive());
        assertEquals(7L, result.getVersion());
    }

    @Test
    void returnsEmptyForUnknownUser() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertTrue(service.findById(id).isEmpty());
    }
}
