package ru.practicum.ewm.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestPrivateControllerTest {

    private final RequestService requestService = mock(RequestService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequestPrivateController(requestService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createsRequest() throws Exception {
        when(requestService.create(1L, 10L)).thenReturn(request());

        mockMvc.perform(post("/users/1/requests").param("eventId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void returnsUserRequests() throws Exception {
        when(requestService.getAllByUser(1L)).thenReturn(List.of(request()));

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event").value(10));
    }

    @Test
    void cancelsRequest() throws Exception {
        ParticipationRequestDto canceled = request();
        canceled.setStatus("CANCELED");
        when(requestService.cancel(1L, 100L)).thenReturn(canceled);

        mockMvc.perform(patch("/users/1/requests/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    private ParticipationRequestDto request() {
        return ParticipationRequestDto.builder()
                .id(100L)
                .created("2025-01-01T12:00:00.000")
                .event(10L)
                .requester(1L)
                .status("PENDING")
                .build();
    }
}
