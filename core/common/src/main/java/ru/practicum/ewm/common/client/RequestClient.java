package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.ewm.common.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/{requestId}")
    ParticipationRequestDto getById(@PathVariable("requestId") Long requestId);

    @PostMapping("/internal/requests/batch")
    List<ParticipationRequestDto> getByIds(@RequestBody List<Long> ids);

    @GetMapping("/internal/requests/event/{eventId}/confirmed-count")
    Long getConfirmedCount(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/requests/confirmed-counts")
    Map<Long, Long> getConfirmedCounts(@RequestBody List<Long> eventIds);

    @GetMapping("/internal/requests/event/{eventId}/confirmed")
    List<ParticipationRequestDto> getConfirmed(@PathVariable("eventId") Long eventId);
}
