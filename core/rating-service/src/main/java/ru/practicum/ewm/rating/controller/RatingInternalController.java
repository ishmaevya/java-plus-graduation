package ru.practicum.ewm.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.rating.service.RatingService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/ratings")
@RequiredArgsConstructor
public class RatingInternalController {

    private final RatingService ratingService;

    @GetMapping("/events/{eventId}")
    public RatingStatsDto getStats(@PathVariable("eventId") Long eventId) {
        return ratingService.getStats(eventId);
    }

    @PostMapping("/events/batch")
    public Map<Long, RatingStatsDto> getStatsBatch(@RequestBody List<Long> eventIds) {
        return ratingService.getStats(eventIds);
    }
}
