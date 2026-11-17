package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.common.dto.EventSnapshotDto;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventSnapshotDto getById(@PathVariable("eventId") Long eventId);
}
