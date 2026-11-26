package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventPublicControllerTest {

    private final EventService eventService = mock(EventService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventPublicController(eventService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsPublishedEvents() throws Exception {
        when(eventService.getPublished(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(EventShortDto.builder().id(100L).title("Concert").build()));

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Concert"));
    }

    @Test
    void returnsEventById() throws Exception {
        when(eventService.getPublishedById(anyLong(), any())).thenReturn(EventFullDto.builder()
                .id(100L)
                .title("Concert")
                .build());

        mockMvc.perform(get("/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));
    }
}
