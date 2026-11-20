package ru.practicum.ewm.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.event.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventSnapshotDto getById(@PathVariable("eventId") Long eventId) {
        return eventService.getSnapshot(eventId);
    }

    @PostMapping("/batch")
    public List<EventShortDto> getByIds(@RequestBody List<Long> ids) {
        return eventService.getByIds(ids);
    }

    @GetMapping("/count-by-category")
    public long countByCategoryId(@RequestParam("categoryId") Long categoryId) {
        return eventService.countByCategoryId(categoryId);
    }
}
