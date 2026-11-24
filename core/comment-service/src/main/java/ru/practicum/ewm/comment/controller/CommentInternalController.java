package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
public class CommentInternalController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}/count")
    public long getCount(@PathVariable("eventId") Long eventId) {
        return commentService.getCount(eventId);
    }

    @PostMapping("/events/counts")
    public Map<Long, Long> getCounts(@RequestBody List<Long> eventIds) {
        return commentService.getCounts(eventIds);
    }
}
