package com.spimex.user.api;

import com.spimex.user.application.UserQueryService;
import com.spimex.user.client.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private final UserQueryService service = mock(UserQueryService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserController(service)).build();

    @Test
    void returnsBasicInformationForInactiveUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Optional.of(
            new UserResponse(id, null, "user@example.test", "Full Name", "Name", false, 3)));
        mvc.perform(get("/users/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.email").value("user@example.test"))
            .andExpect(jsonPath("$.fio").value("Full Name"))
            .andExpect(jsonPath("$.shortName").value("Name"))
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(Optional.empty());
        mvc.perform(get("/users/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidUuidBeforeCallingService() throws Exception {
        mvc.perform(get("/users/not-a-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
