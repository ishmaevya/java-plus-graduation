package ru.practicum.ewm.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {

    private final RequestService requestService;

    @GetMapping("/{requestId}")
    public ParticipationRequestDto getById(@PathVariable("requestId") Long requestId) {
        return requestService.getById(requestId);
    }

    @PostMapping("/batch")
    public List<ParticipationRequestDto> getByIds(@RequestBody List<Long> ids) {
        return requestService.getByIds(ids);
    }

    @GetMapping("/event/{eventId}/confirmed-count")
    public Long getConfirmedCount(@PathVariable("eventId") Long eventId) {
        return requestService.getConfirmedCount(eventId);
    }

    @GetMapping("/event/{eventId}/confirmed")
    public List<ParticipationRequestDto> getConfirmed(@PathVariable("eventId") Long eventId) {
        return requestService.getConfirmed(eventId);
    }
}
