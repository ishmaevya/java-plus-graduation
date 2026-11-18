package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.service.CommentService;
import ru.practicum.ewm.common.dto.CommentDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentPublicController {

    private final CommentService commentService;

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getByEvent(@PathVariable Long eventId,
                                       @RequestParam(defaultValue = "0") Integer from,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getByEvent(eventId, from, size);
    }

    @GetMapping("/events/{eventId}/comments/{commentId}")
    public CommentDto getOne(@PathVariable Long eventId, @PathVariable Long commentId) {
        return commentService.getOne(eventId, commentId);
    }
}
