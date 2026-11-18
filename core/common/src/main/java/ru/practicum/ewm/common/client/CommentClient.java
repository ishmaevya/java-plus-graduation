package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "comment-service")
public interface CommentClient {

    @GetMapping("/internal/comments/events/{eventId}/count")
    Long getCount(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/comments/events/counts")
    Map<Long, Long> getCounts(@RequestBody List<Long> eventIds);
}
