package ru.practicum.ewm.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestManageControllerTest {

    private final RequestService requestService = mock(RequestService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequestManageController(requestService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsEventRequests() throws Exception {
        when(requestService.getByEvent(1L, 10L)).thenReturn(List.of(request("PENDING")));

        mockMvc.perform(get("/users/1/events/10/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void changesRequestStatus() throws Exception {
        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(request("CONFIRMED")))
                .rejectedRequests(List.of())
                .build();
        when(requestService.changeStatus(eq(1L), eq(10L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(result);

        mockMvc.perform(patch("/users/1/events/10/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new EventRequestStatusUpdateRequest(List.of(100L), "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.rejectedRequests").isEmpty());
    }

    private ParticipationRequestDto request(String status) {
        return ParticipationRequestDto.builder()
                .id(100L)
                .created("2025-01-01T12:00:00.000")
                .event(10L)
                .requester(2L)
                .status(status)
                .build();
    }
}
