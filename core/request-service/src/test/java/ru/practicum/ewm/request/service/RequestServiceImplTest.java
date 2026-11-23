package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.request.exception.EventNotFoundException;
import ru.practicum.ewm.request.exception.RequestNotFoundException;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @Spy
    private RequestMapper mapper = new RequestMapper();

    @InjectMocks
    private RequestServiceImpl service;

    @Test
    void createReturnsPendingForModeratedEvent() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(20L, "PUBLISHED", 2, true));
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(100L);
            return request;
        });

        ParticipationRequestDto result = service.create(1L, 10L);

        assertEquals(100L, result.getId());
        assertEquals(RequestStatus.PENDING.name(), result.getStatus());
        assertEquals(10L, result.getEvent());
        assertEquals(1L, result.getRequester());
    }

    @Test
    void createReturnsConfirmedWithoutModeration() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(20L, "PUBLISHED", 0, true));
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest request = invocation.getArgument(0);
            request.setId(100L);
            return request;
        });

        ParticipationRequestDto result = service.create(1L, 10L);

        assertEquals(RequestStatus.CONFIRMED.name(), result.getStatus());
    }

    @Test
    void createRejectsInitiator() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(1L, "PUBLISHED", 2, true));

        assertThrows(ConflictException.class, () -> service.create(1L, 10L));
    }

    @Test
    void createRejectsUnpublishedEvent() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(20L, "PENDING", 2, true));

        assertThrows(ConflictException.class, () -> service.create(1L, 10L));
    }

    @Test
    void createRejectsDuplicateRequest() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(20L, "PUBLISHED", 2, true));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(1L, 10L));
    }

    @Test
    void createRejectsReachedLimit() {
        when(userClient.getById(1L)).thenReturn(new UserDto(1L, "user@mail.ru", "User"));
        when(eventClient.getById(10L)).thenReturn(event(20L, "PUBLISHED", 1, true));
        when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.create(1L, 10L));
    }

    @Test
    void cancelChangesStatus() {
        ParticipationRequest request = request(100L, 1L, 10L, RequestStatus.PENDING);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(requestRepository.save(request)).thenReturn(request);

        ParticipationRequestDto result = service.cancel(1L, 100L);

        assertEquals(RequestStatus.CANCELED.name(), result.getStatus());
    }

    @Test
    void cancelRejectsForeignRequest() {
        ParticipationRequest request = request(100L, 2L, 10L, RequestStatus.PENDING);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));

        assertThrows(RequestNotFoundException.class, () -> service.cancel(1L, 100L));
    }

    @Test
    void getByEventRejectsNonInitiator() {
        when(eventClient.getById(10L)).thenReturn(event(20L, "PUBLISHED", 2, true));

        assertThrows(EventNotFoundException.class, () -> service.getByEvent(1L, 10L));
    }

    @Test
    void changeStatusApprovesAndRejectsRequests() {
        ParticipationRequest first = request(1L, 2L, 10L, RequestStatus.PENDING);
        ParticipationRequest second = request(2L, 3L, 10L, RequestStatus.PENDING);
        ParticipationRequest pending = request(3L, 4L, 10L, RequestStatus.PENDING);
        when(eventClient.getById(10L)).thenReturn(event(1L, "PUBLISHED", 1, true));
        when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(requestRepository.findByEventIdAndStatus(10L, RequestStatus.PENDING))
                .thenReturn(List.of(pending));

        EventRequestStatusUpdateResult result = service.changeStatus(1L, 10L,
                new EventRequestStatusUpdateRequest(List.of(1L, 2L), "CONFIRMED"));

        assertEquals(List.of(RequestStatus.CONFIRMED.name()),
                result.getConfirmedRequests().stream().map(ParticipationRequestDto::getStatus).toList());
        assertEquals(List.of(RequestStatus.REJECTED.name(), RequestStatus.REJECTED.name()),
                result.getRejectedRequests().stream().map(ParticipationRequestDto::getStatus).toList());
    }

    @Test
    void changeStatusRejectsMissingRequests() {
        when(eventClient.getById(10L)).thenReturn(event(1L, "PUBLISHED", 2, true));
        when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(request(
                1L, 2L, 10L, RequestStatus.PENDING)));

        assertThrows(RequestNotFoundException.class,
                () -> service.changeStatus(1L, 10L,
                        new EventRequestStatusUpdateRequest(List.of(1L, 2L), "CONFIRMED")));
    }

    @Test
    void getByIdsKeepsRequestedOrder() {
        ParticipationRequest first = request(1L, 2L, 10L, RequestStatus.CONFIRMED);
        ParticipationRequest second = request(2L, 3L, 10L, RequestStatus.PENDING);
        when(requestRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(first, second));

        List<ParticipationRequestDto> result = service.getByIds(List.of(2L, 1L));

        assertEquals(List.of(2L, 1L), result.stream().map(ParticipationRequestDto::getId).toList());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RequestNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void getConfirmedCountDelegatesToRepository() {
        when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(3L);

        assertEquals(3L, service.getConfirmedCount(10L));
    }

    @Test
    void getConfirmedCountsDefaultsMissingEventsToZero() {
        when(requestRepository.countByEventIdsAndStatus(List.of(10L, 11L), RequestStatus.CONFIRMED))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        Map<Long, Long> result = service.getConfirmedCounts(List.of(10L, 11L));

        assertEquals(Map.of(10L, 3L, 11L, 0L), result);
    }

    private EventSnapshotDto event(Long initiatorId, String state, Integer limit, Boolean moderation) {
        return EventSnapshotDto.builder()
                .id(10L)
                .initiatorId(initiatorId)
                .state(state)
                .participantLimit(limit)
                .requestModeration(moderation)
                .build();
    }

    private ParticipationRequest request(Long id, Long requesterId, Long eventId, RequestStatus status) {
        return ParticipationRequest.builder()
                .id(id)
                .created(LocalDateTime.of(2025, 1, 1, 12, 0))
                .eventId(eventId)
                .requesterId(requesterId)
                .status(status)
                .build();
    }
}
