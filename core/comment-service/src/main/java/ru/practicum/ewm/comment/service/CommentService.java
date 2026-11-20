package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.common.dto.CommentDto;
import ru.practicum.ewm.common.dto.NewCommentDto;
import ru.practicum.ewm.common.dto.UpdateCommentDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CommentService {

    CommentDto create(Long userId, Long eventId, NewCommentDto dto);

    CommentDto update(Long userId, Long commentId, UpdateCommentDto dto);

    void deleteUserComment(Long userId, Long commentId);

    void deleteByAdmin(Long commentId);

    List<CommentDto> getByUser(Long userId, Integer from, Integer size);

    List<CommentDto> getByEvent(Long eventId, Integer from, Integer size);

    CommentDto getOne(Long eventId, Long commentId);

    long getCount(Long eventId);

    Map<Long, Long> getCounts(Collection<Long> eventIds);
}
