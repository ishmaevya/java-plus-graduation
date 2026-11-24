package ru.practicum.ewm.request.service;

import ru.practicum.ewm.common.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.common.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface RequestService {

    ParticipationRequestDto create(Long userId, Long eventId);

    List<ParticipationRequestDto> getAllByUser(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);

    List<ParticipationRequestDto> getByEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeStatus(Long userId, Long eventId,
                                                  EventRequestStatusUpdateRequest request);

    ParticipationRequestDto getById(Long requestId);

    List<ParticipationRequestDto> getByIds(List<Long> ids);

    long getConfirmedCount(Long eventId);

    Map<Long, Long> getConfirmedCounts(List<Long> eventIds);

    List<ParticipationRequestDto> getConfirmed(Long eventId);
}
