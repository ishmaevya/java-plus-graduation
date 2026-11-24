package ru.practicum.ewm.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.exception.UserNotFoundException;
import ru.practicum.ewm.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerTest {

    private final UserService userService = mock(UserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserAdminController(userService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createsUser() throws Exception {
        NewUserRequest request = NewUserRequest.builder()
                .email("user@example.com")
                .name("User")
                .build();
        when(userService.create(any(NewUserRequest.class))).thenReturn(user());

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void returnsUsers() throws Exception {
        when(userService.getAll(anyList(), anyInt(), anyInt())).thenReturn(List.of(user()));

        mockMvc.perform(get("/admin/users")
                        .param("ids", "1")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("User"));
    }

    @Test
    void deletesUser() throws Exception {
        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void returnsNotFoundForMissingUser() throws Exception {
        doThrow(new UserNotFoundException(42L)).when(userService).delete(eq(42L));

        mockMvc.perform(delete("/admin/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User with ID = 42 not found."));
    }

    private UserDto user() {
        return UserDto.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .build();
    }
}
