package ru.practicum.ewm.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.user.exception.UserNotFoundException;
import ru.practicum.ewm.user.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserInternalControllerTest {

    private final UserService userService = mock(UserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserInternalController(userService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsUserById() throws Exception {
        when(userService.getById(1L)).thenReturn(user());

        mockMvc.perform(get("/internal/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void returnsShortUserById() throws Exception {
        when(userService.getShortById(1L)).thenReturn(new UserShortDto(1L, "User"));

        mockMvc.perform(get("/internal/users/1/short"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("User"));
    }

    @Test
    void returnsUsersByIds() throws Exception {
        when(userService.getByIds(anyList())).thenReturn(List.of(user()));

        mockMvc.perform(post("/internal/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void returnsNotFoundWhenUserMissing() throws Exception {
        when(userService.getById(eq(42L))).thenThrow(new UserNotFoundException(42L));

        mockMvc.perform(get("/internal/users/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    private UserDto user() {
        return UserDto.builder()
                .id(1L)
                .email("user@example.com")
                .name("User")
                .build();
    }
}
