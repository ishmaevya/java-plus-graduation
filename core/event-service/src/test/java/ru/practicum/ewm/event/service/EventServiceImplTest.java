package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.client.CategoryClient;
import ru.practicum.ewm.common.client.CommentClient;
import ru.practicum.ewm.common.client.RatingClient;
import ru.practicum.ewm.common.client.RequestClient;
import ru.practicum.ewm.common.client.SubscriptionClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.LocationDto;
import ru.practicum.ewm.common.dto.NewEventDto;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.common.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.event.exception.EventNotFoundException;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.EventSpecification;
import ru.practicum.ewm.stats.clients.StatsClient;
import ru.practicum.ewm.stats.dto.StatHitResponseElement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSpecification eventSpecification;

    @Spy
    private EventMapper mapper = new EventMapper();

    @Mock
    private UserClient userClient;

    @Mock
    private CategoryClient categoryClient;

    @Mock
    private RequestClient requestClient;

    @Mock
    private RatingClient ratingClient;

    @Mock
    private CommentClient commentClient;

    @Mock
    private SubscriptionClient subscriptionClient;

    @Mock
    private StatsClient statsClient;

    @InjectMocks
    private EventServiceImpl service;

    @Test
    void createSavesPendingEvent() {
        when(userClient.getById(1L)).thenReturn(user());
        when(categoryClient.getById(10L)).thenReturn(category());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(100L);
            return event;
        });
        stubCounters(100L);

        EventFullDto result = service.create(1L, newEvent());

        assertEquals(100L, result.getId());
        assertEquals(EventState.PENDING.name(), result.getState());
        assertEquals(10L, result.getCategory().getId());
        assertEquals(1L, result.getInitiator().getId());
    }

    @Test
    void createRejectsDateWithinTwoHours() {
        when(userClient.getById(1L)).thenReturn(user());
        when(categoryClient.getById(10L)).thenReturn(category());
        NewEventDto request = newEvent();
        request.setEventDate(LocalDateTime.now().plusHours(1).format(FORMATTER));

        assertThrows(ValidationException.class, () -> service.create(1L, request));
    }

    @Test
    void updateByAdminPublishesPendingEvent() {
        Event event = event(EventState.PENDING);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(categoryClient.getById(10L)).thenReturn(category());
        when(userClient.getById(1L)).thenReturn(user());
        when(eventRepository.save(event)).thenReturn(event);
        stubCounters(100L);

        EventFullDto result = service.updateByAdmin(100L, UpdateEventAdminRequest.builder()
                .stateAction("PUBLISH_EVENT")
                .build());

        assertEquals(EventState.PUBLISHED.name(), result.getState());
    }

    @Test
    void updateByAdminRejectsPublishingPublishedEvent() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event(EventState.PUBLISHED)));

        assertThrows(ConflictException.class, () -> service.updateByAdmin(100L,
                UpdateEventAdminRequest.builder().stateAction("PUBLISH_EVENT").build()));
    }

    @Test
    void getPublishedByIdReturnsOnlyPublished() {
        Event event = event(EventState.PUBLISHED);
        when(eventRepository.findByIdAndState(100L, EventState.PUBLISHED)).thenReturn(Optional.of(event));
        when(categoryClient.getById(10L)).thenReturn(category());
        when(userClient.getById(1L)).thenReturn(user());
        stubCounters(100L);

        EventFullDto result = service.getPublishedById(100L);

        assertEquals(100L, result.getId());
    }

    @Test
    void getPublishedByIdThrowsForMissingEvent() {
        when(eventRepository.findByIdAndState(404L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> service.getPublishedById(404L));
    }

    @Test
    void getSnapshotMapsEvent() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event(EventState.PUBLISHED)));

        EventSnapshotDto result = service.getSnapshot(100L);

        assertEquals(100L, result.getId());
        assertEquals(1L, result.getInitiatorId());
        assertEquals(EventState.PUBLISHED.name(), result.getState());
    }

    @Test
    void getByIdsKeepsRequestedOrder() {
        Event first = event(EventState.PUBLISHED);
        Event second = event(EventState.PUBLISHED);
        second.setId(200L);
        when(eventRepository.findByIdIn(List.of(200L, 100L))).thenReturn(List.of(first, second));
        when(categoryClient.getByIds(anyList())).thenReturn(List.of(category()));
        when(userClient.getByIds(anyList())).thenReturn(List.of(user()));
        when(ratingClient.getStatsBatch(anyList())).thenReturn(Map.of());
        when(commentClient.getCounts(anyList())).thenReturn(Map.of());
        when(requestClient.getConfirmedCounts(anyList())).thenReturn(Map.of());
        when(statsClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(List.of(
                new StatHitResponseElement("ewm-main-service", "/events/200", 5L)));

        List<EventShortDto> result = service.getByIds(List.of(200L, 100L));

        assertEquals(List.of(200L, 100L), result.stream().map(EventShortDto::getId).toList());
        assertEquals(5L, result.getFirst().getViews());
        verify(statsClient, times(1)).getStats(any(), any(), anyList(), anyBoolean());
        verify(requestClient, times(1)).getConfirmedCounts(anyList());
    }

    private void stubCounters(Long eventId) {
        when(requestClient.getConfirmedCounts(List.of(eventId))).thenReturn(Map.of(eventId, 0L));
        when(ratingClient.getStatsBatch(List.of(eventId))).thenReturn(Map.of(eventId, RatingStatsDto.EMPTY));
        when(commentClient.getCounts(List.of(eventId))).thenReturn(Map.of(eventId, 0L));
    }

    private NewEventDto newEvent() {
        return NewEventDto.builder()
                .annotation("Concert with a long annotation")
                .category(10L)
                .description("Concert description text")
                .eventDate(LocalDateTime.now().plusDays(1).format(FORMATTER))
                .location(new LocationDto(1.0F, 2.0F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Concert")
                .build();
    }

    private Event event(EventState state) {
        return Event.builder()
                .id(100L)
                .annotation("Concert with a long annotation")
                .categoryId(10L)
                .createdOn(LocalDateTime.now().minusDays(1))
                .description("Concert description text")
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiatorId(1L)
                .location(new Location(1.0F, 2.0F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(state)
                .title("Concert")
                .build();
    }

    private CategoryDto category() {
        return new CategoryDto(10L, "Concerts");
    }

    private UserDto user() {
        return new UserDto(1L, "user@mail.ru", "User");
    }
}
