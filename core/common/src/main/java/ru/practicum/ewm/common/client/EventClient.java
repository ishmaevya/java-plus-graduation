package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;

import java.util.List;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventSnapshotDto getById(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/events/batch")
    List<EventShortDto> getByIds(@RequestBody List<Long> ids);
}
