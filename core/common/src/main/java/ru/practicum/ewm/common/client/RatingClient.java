package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.ewm.common.dto.RatingStatsDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "rating-service")
public interface RatingClient {

    @GetMapping("/internal/ratings/events/{eventId}")
    RatingStatsDto getStats(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/ratings/events/batch")
    Map<Long, RatingStatsDto> getStatsBatch(@RequestBody List<Long> eventIds);
}
