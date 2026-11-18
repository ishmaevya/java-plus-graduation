package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.event.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventInternalControllerTest {

    private final EventService eventService = mock(EventService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventInternalController(eventService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsSnapshot() throws Exception {
        when(eventService.getSnapshot(100L)).thenReturn(EventSnapshotDto.builder()
                .id(100L)
                .initiatorId(1L)
                .categoryId(10L)
                .state("PUBLISHED")
                .participantLimit(10)
                .requestModeration(true)
                .eventDate(LocalDateTime.of(2026, 12, 1, 12, 0))
                .build());

        mockMvc.perform(get("/internal/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiatorId").value(1))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    void returnsEventsByIds() throws Exception {
        when(eventService.getByIds(List.of(100L)))
                .thenReturn(List.of(EventShortDto.builder().id(100L).title("Concert").build()));

        mockMvc.perform(post("/internal/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[100]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Concert"));
    }
}
