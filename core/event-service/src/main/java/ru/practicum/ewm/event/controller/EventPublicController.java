package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventPublicController {

    private final EventService eventService;

    @GetMapping("/events")
    public List<EventShortDto> getPublished(@RequestParam(required = false) String text,
                                            @RequestParam(required = false) List<Long> categories,
                                            @RequestParam(required = false) Boolean paid,
                                            @RequestParam(required = false) String rangeStart,
                                            @RequestParam(required = false) String rangeEnd,
                                            @RequestParam(required = false) Boolean onlyAvailable,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "0") Integer from,
                                            @RequestParam(defaultValue = "10") Integer size,
                                             @RequestHeader(value = "X-EWM-UserId", required = false) Long userId) {
        return eventService.getPublished(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable, sort, from, size, userId);
    }

    @GetMapping("/events/{id}")
    public EventFullDto getPublishedById(@PathVariable Long id,
                                         @RequestHeader(value = "X-EWM-UserId", required = false) Long userId) {
        return eventService.getPublishedById(id, userId);
    }
}
