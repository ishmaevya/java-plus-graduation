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
import ru.practicum.ewm.common.dto.NewEventDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.handler.ErrorHandler;
import ru.practicum.ewm.event.service.EventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventPrivateControllerTest {

    private final EventService eventService = mock(EventService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EventPrivateController(eventService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createsEvent() throws Exception {
        when(eventService.create(eq(1L), any(NewEventDto.class))).thenReturn(event());

        NewEventDto request = NewEventDto.builder()
                .annotation("Concert with a long annotation")
                .category(10L)
                .description("Concert description text")
                .eventDate("2026-12-01 12:00:00")
                .location(new LocationDto(1.0F, 2.0F))
                .title("Concert")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    void returnsEventByUser() throws Exception {
        when(eventService.getByUserAndEvent(1L, 100L)).thenReturn(event());

        mockMvc.perform(get("/users/1/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiator.id").value(1));
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
                .views(0L)
                .likes(0L)
                .dislikes(0L)
                .rating(0L)
                .commentCount(0L)
                .build();
    }
}
