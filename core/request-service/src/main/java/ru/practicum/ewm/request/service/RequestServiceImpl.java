package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.request.exception.EventNotFoundException;
import ru.practicum.ewm.request.exception.RequestNotFoundException;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestMapper mapper;

    @Override
    @Transactional
    public ParticipationRequestDto create(Long userId, Long eventId) {
        userClient.getById(userId);
        EventSnapshotDto event = eventClient.getById(eventId);

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Initiator cannot add request to own event");
        }
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Cannot participate in unpublished event");
        }
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Request already exists");
        }

        boolean limitEnabled = isLimitEnabled(event.getParticipantLimit());
        if (limitEnabled) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        RequestStatus status = limitEnabled && Boolean.TRUE.equals(event.getRequestModeration())
                ? RequestStatus.PENDING
                : RequestStatus.CONFIRMED;

        ParticipationRequest request = requestRepository.save(ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(status)
                .build());

        log.debug("Request created: {} for event {}", request.getId(), eventId);
        return mapper.toDto(request);
    }

    @Override
    public List<ParticipationRequestDto> getAllByUser(Long userId) {
        userClient.getById(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        if (!request.getRequesterId().equals(userId)) {
            throw new RequestNotFoundException(requestId);
        }

        request.setStatus(RequestStatus.CANCELED);
        log.debug("Request canceled: {}", requestId);
        return mapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getByEvent(Long userId, Long eventId) {
        EventSnapshotDto event = eventClient.getById(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new EventNotFoundException(eventId);
        }
        return requestRepository.findByEventId(eventId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest request) {
        EventSnapshotDto event = eventClient.getById(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new EventNotFoundException(eventId);
        }
        if (!isLimitEnabled(event.getParticipantLimit())
                || !Boolean.TRUE.equals(event.getRequestModeration())) {
            throw new ConflictException("Confirmation of requests is not required");
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long limit = event.getParticipantLimit();
        List<Long> ids = request.getRequestIds();

        List<ParticipationRequest> found = requestRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            List<Long> missing = new ArrayList<>(ids);
            missing.removeAll(found.stream().map(ParticipationRequest::getId).toList());
            throw new RequestNotFoundException(missing);
        }

        Map<Long, ParticipationRequest> byId = found.stream()
                .collect(Collectors.toMap(ParticipationRequest::getId, Function.identity()));

        for (Long id : ids) {
            ParticipationRequest participationRequest = byId.get(id);
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        boolean approve = "CONFIRMED".equals(request.getStatus());
        for (Long id : ids) {
            ParticipationRequest participationRequest = byId.get(id);
            if (approve && confirmed < limit) {
                participationRequest.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
            }
        }
        requestRepository.saveAll(byId.values());

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();
        for (Long id : ids) {
            ParticipationRequest participationRequest = byId.get(id);
            if (participationRequest.getStatus() == RequestStatus.CONFIRMED) {
                confirmedRequests.add(mapper.toDto(participationRequest));
            } else {
                rejectedRequests.add(mapper.toDto(participationRequest));
            }
        }

        if (confirmed >= limit) {
            List<ParticipationRequest> pending = requestRepository.findByEventIdAndStatus(
                    eventId, RequestStatus.PENDING);
            pending.forEach(r -> r.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(pending);
            pending.forEach(r -> rejectedRequests.add(mapper.toDto(r)));
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests)
                .rejectedRequests(rejectedRequests)
                .build();
    }

    @Override
    public ParticipationRequestDto getById(Long requestId) {
        return requestRepository.findById(requestId)
                .map(mapper::toDto)
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    @Override
    public List<ParticipationRequestDto> getByIds(List<Long> ids) {
        Map<Long, ParticipationRequest> byId = requestRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ParticipationRequest::getId, Function.identity()));
        Map<Long, ParticipationRequest> ordered = new LinkedHashMap<>();
        for (Long id : ids) {
            ParticipationRequest request = byId.get(id);
            if (request != null) {
                ordered.put(id, request);
            }
        }
        return ordered.values().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public long getConfirmedCount(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public List<ParticipationRequestDto> getConfirmed(Long eventId) {
        return requestRepository.findByEventIdAndStatus(eventId, RequestStatus.CONFIRMED).stream()
                .map(mapper::toDto)
                .toList();
    }

    private boolean isLimitEnabled(Integer participantLimit) {
        return participantLimit != null && participantLimit > 0;
    }
}
