package ru.practicum.ewm.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.LocationDto;
import ru.practicum.ewm.common.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventAdminControllerTest {

    private final EventService eventService = mock(EventService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventAdminController(eventService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsEventsForAdmin() throws Exception {
        when(eventService.getByAdmin(any(), any(), any(), any(), any(), eq(0), eq(10)))
                .thenReturn(List.of(event()));

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Concert"));
    }

    @Test
    void updatesEvent() throws Exception {
        when(eventService.updateByAdmin(eq(100L), any(UpdateEventAdminRequest.class))).thenReturn(event());

        mockMvc.perform(patch("/admin/events/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                UpdateEventAdminRequest.builder().stateAction("PUBLISH_EVENT").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    private EventFullDto event() {
        return EventFullDto.builder()
                .id(100L)
                .annotation("Concert with a long annotation")
                .category(new CategoryDto(10L, "Concerts"))
                .confirmedRequests(0L)
                .description("Concert description text")
                .eventDate("2026-12-01 12:00:00")
                .initiator(new UserShortDto(1L, "User"))
                .location(new LocationDto(1.0F, 2.0F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state("PUBLISHED")
                .title("Concert")
                .likes(0L)
                .dislikes(0L)
                .rating(0.0)
                .commentCount(0L)
                .build();
    }
}
