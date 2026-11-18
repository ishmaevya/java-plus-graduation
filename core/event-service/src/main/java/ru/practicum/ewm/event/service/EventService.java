package ru.practicum.ewm.event.service;

import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.NewEventDto;
import ru.practicum.ewm.common.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.common.dto.UpdateEventUserRequest;

import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto dto);

    List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size);

    EventFullDto getByUserAndEvent(Long userId, Long eventId);

    EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<EventFullDto> getByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                  String rangeStart, String rangeEnd, Integer from, Integer size);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto);

    List<EventShortDto> getPublished(String text, List<Long> categories, Boolean paid,
                                     String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                     String sort, Integer from, Integer size, Long userId);

    EventFullDto getPublishedById(Long id);

    EventSnapshotDto getSnapshot(Long eventId);

    List<EventShortDto> getByIds(List<Long> ids);
}
