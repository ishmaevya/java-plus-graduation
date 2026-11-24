package ru.practicum.ewm.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestInternalControllerTest {

    private final RequestService requestService = mock(RequestService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequestInternalController(requestService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsRequestById() throws Exception {
        when(requestService.getById(100L)).thenReturn(request());

        mockMvc.perform(get("/internal/requests/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requester").value(1));
    }

    @Test
    void returnsRequestsByIds() throws Exception {
        when(requestService.getByIds(List.of(100L))).thenReturn(List.of(request()));

        mockMvc.perform(post("/internal/requests/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event").value(10));
    }

    @Test
    void returnsConfirmedCount() throws Exception {
        when(requestService.getConfirmedCount(10L)).thenReturn(2L);

        mockMvc.perform(get("/internal/requests/event/10/confirmed-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    void returnsConfirmedCounts() throws Exception {
        when(requestService.getConfirmedCounts(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 3L, 11L, 1L));

        mockMvc.perform(post("/internal/requests/confirmed-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(10L, 11L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['10']").value(3))
                .andExpect(jsonPath("$['11']").value(1));
    }

    @Test
    void returnsConfirmedRequests() throws Exception {
        when(requestService.getConfirmed(10L)).thenReturn(List.of(request()));

        mockMvc.perform(get("/internal/requests/event/10/confirmed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    private ParticipationRequestDto request() {
        return ParticipationRequestDto.builder()
                .id(100L)
                .created("2025-01-01T12:00:00.000")
                .event(10L)
                .requester(1L)
                .status("CONFIRMED")
                .build();
    }
}
