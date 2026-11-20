package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.exception.CommentNotFoundException;
import ru.practicum.ewm.comment.exception.EventNotFoundException;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.CommentDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.NewCommentDto;
import ru.practicum.ewm.common.dto.UpdateCommentDto;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.exception.ConflictException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentMapper mapper;

    @Override
    @Transactional
    public CommentDto create(Long userId, Long eventId, NewCommentDto dto) {
        UserDto author = userClient.getById(userId);
        EventSnapshotDto event = eventClient.getById(eventId);
        if (!"PUBLISHED".equals(event.getState())) {
            throw new EventNotFoundException(eventId);
        }

        Comment comment = mapper.toComment(dto, eventId, userId);
        comment = commentRepository.save(comment);
        log.debug("Comment created: {} by user {} on event {}", comment.getId(), userId, eventId);
        return mapper.toCommentDto(comment, author.getName());
    }

    @Override
    @Transactional
    public CommentDto update(Long userId, Long commentId, UpdateCommentDto dto) {
        UserDto author = userClient.getById(userId);
        Comment comment = findComment(commentId);
        checkAuthor(comment, userId, "edit");

        comment.setText(dto.getText());
        comment.setUpdated(LocalDateTime.now());
        comment = commentRepository.save(comment);
        log.debug("Comment updated: {}", commentId);
        return mapper.toCommentDto(comment, author.getName());
    }

    @Override
    @Transactional
    public void deleteUserComment(Long userId, Long commentId) {
        userClient.getById(userId);
        Comment comment = findComment(commentId);
        checkAuthor(comment, userId, "delete");

        commentRepository.delete(comment);
        log.debug("Comment deleted by author: {}", commentId);
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long commentId) {
        Comment comment = findComment(commentId);
        commentRepository.delete(comment);
        log.debug("Comment deleted by admin: {}", commentId);
    }

    @Override
    public List<CommentDto> getByUser(Long userId, Integer from, Integer size) {
        UserDto author = userClient.getById(userId);
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository.findByAuthorIdOrderByCreatedDesc(userId, page).stream()
                .map(comment -> mapper.toCommentDto(comment, author.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getByEvent(Long eventId, Integer from, Integer size) {
        eventClient.getById(eventId);
        PageRequest page = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByEventIdOrderByCreatedDesc(eventId, page).getContent();
        Map<Long, String> authorNames = getAuthorNames(comments);
        return comments.stream()
                .map(comment -> mapper.toCommentDto(comment, authorNames.get(comment.getAuthorId())))
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto getOne(Long eventId, Long commentId) {
        eventClient.getById(eventId);
        Comment comment = findComment(commentId);
        if (!comment.getEventId().equals(eventId)) {
            throw new CommentNotFoundException(commentId);
        }
        UserDto author = userClient.getById(comment.getAuthorId());
        return mapper.toCommentDto(comment, author.getName());
    }

    @Override
    public long getCount(Long eventId) {
        return commentRepository.countByEventId(eventId);
    }

    @Override
    public Map<Long, Long> getCounts(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository.countByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(
                        CommentRepository.CommentCountView::getEventId,
                        CommentRepository.CommentCountView::getCnt
                ));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void checkAuthor(Comment comment, Long userId, String action) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("Only the author can " + action + " the comment");
        }
    }

    private Map<Long, String> getAuthorNames(List<Comment> comments) {
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userClient.getByIds(List.copyOf(authorIds)).stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));
    }
}
