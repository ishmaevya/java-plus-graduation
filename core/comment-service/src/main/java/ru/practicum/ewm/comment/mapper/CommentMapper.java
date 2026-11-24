package ru.practicum.ewm.comment.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.common.dto.CommentDto;
import ru.practicum.ewm.common.dto.NewCommentDto;

import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public Comment toComment(NewCommentDto dto, Long eventId, Long authorId) {
        return Comment.builder()
                .text(dto.getText())
                .eventId(eventId)
                .authorId(authorId)
                .created(LocalDateTime.now())
                .build();
    }

    public CommentDto toCommentDto(Comment comment, String authorName) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthorId())
                .authorName(authorName)
                .eventId(comment.getEventId())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .build();
    }
}
