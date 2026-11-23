package ru.practicum.ewm.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.transaction.TransactionRunner;
import ru.practicum.ewm.request.exception.EventNotFoundException;
import ru.practicum.ewm.request.exception.RequestNotFoundException;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestMapper mapper;

    private TransactionRunner transactionRunner = new TransactionRunner();

    @Autowired
    public void setTransactionRunner(TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Override
    public ParticipationRequestDto create(Long userId, Long eventId) {
        userClient.getById(userId);
        EventSnapshotDto event = eventClient.getById(eventId);

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Initiator cannot add request to own event");
        }
        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Cannot participate in unpublished event");
        }

        boolean limitEnabled = isLimitEnabled(event.getParticipantLimit());
        RequestStatus status = limitEnabled && Boolean.TRUE.equals(event.getRequestModeration())
                ? RequestStatus.PENDING
                : RequestStatus.CONFIRMED;

        ParticipationRequest request = transactionRunner.execute(() -> {
            if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
                throw new ConflictException("Request already exists");
            }
            if (limitEnabled) {
                long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
                if (confirmed >= event.getParticipantLimit()) {
                    throw new ConflictException("The participant limit has been reached");
                }
            }
            return requestRepository.save(ParticipationRequest.builder()
                    .created(LocalDateTime.now())
                    .eventId(eventId)
                    .requesterId(userId)
                    .status(status)
                    .build());
        });

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
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        ParticipationRequest request = transactionRunner.execute(() -> {
            ParticipationRequest existing = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RequestNotFoundException(requestId));
            if (!existing.getRequesterId().equals(userId)) {
                throw new RequestNotFoundException(requestId);
            }
            existing.setStatus(RequestStatus.CANCELED);
            return requestRepository.save(existing);
        });
        log.debug("Request canceled: {}", requestId);
        return mapper.toDto(request);
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

        return transactionRunner.execute(() -> {
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
        });
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
    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (eventIds.isEmpty()) {
            return counts;
        }
        for (Long eventId : eventIds) {
            counts.put(eventId, 0L);
        }
        for (Object[] row : requestRepository.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
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
