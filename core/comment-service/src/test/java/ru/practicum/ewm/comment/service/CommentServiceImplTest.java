package ru.practicum.ewm.comment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @Spy
    private CommentMapper mapper = new CommentMapper();

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void createSavesCommentForPublishedEvent() {
        when(userClient.getById(1L)).thenReturn(user(1L));
        when(eventClient.getById(100L)).thenReturn(event("PUBLISHED"));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(10L);
            return comment;
        });

        CommentDto dto = commentService.create(1L, 100L, new NewCommentDto("text"));

        assertEquals(10L, dto.getId());
        assertEquals("User", dto.getAuthorName());
        assertEquals(100L, dto.getEventId());
        assertNotNull(dto.getCreated());
    }

    @Test
    void createRejectsUnpublishedEvent() {
        when(userClient.getById(1L)).thenReturn(user(1L));
        when(eventClient.getById(100L)).thenReturn(event("PENDING"));

        assertThrows(EventNotFoundException.class,
                () -> commentService.create(1L, 100L, new NewCommentDto("text")));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateRejectsNonAuthor() {
        when(userClient.getById(1L)).thenReturn(user(1L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, 100L, 2L, "old")));

        assertThrows(ConflictException.class,
                () -> commentService.update(1L, 10L, new UpdateCommentDto("new")));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateChangesTextByAuthor() {
        when(userClient.getById(2L)).thenReturn(user(2L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, 100L, 2L, "old")));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentDto dto = commentService.update(2L, 10L, new UpdateCommentDto("new"));

        assertEquals("new", dto.getText());
        assertNotNull(dto.getUpdated());
    }

    @Test
    void deleteUserCommentRemovesOwnComment() {
        when(userClient.getById(2L)).thenReturn(user(2L));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, 100L, 2L, "old")));

        commentService.deleteUserComment(2L, 10L);

        verify(commentRepository).delete(any(Comment.class));
    }

    @Test
    void getByEventResolvesAuthorNames() {
        when(eventClient.getById(100L)).thenReturn(event("PUBLISHED"));
        when(commentRepository.findByEventIdOrderByCreatedDesc(100L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(comment(10L, 100L, 2L, "old"))));
        when(userClient.getByIds(List.of(2L))).thenReturn(List.of(user(2L)));

        List<CommentDto> comments = commentService.getByEvent(100L, 0, 10);

        assertEquals(1, comments.size());
        assertEquals("User", comments.get(0).getAuthorName());
    }

    @Test
    void getOneRejectsCommentFromAnotherEvent() {
        when(eventClient.getById(100L)).thenReturn(event("PUBLISHED"));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, 200L, 2L, "old")));

        assertThrows(CommentNotFoundException.class, () -> commentService.getOne(100L, 10L));
    }

    @Test
    void getCountsMapsRepositoryViews() {
        when(commentRepository.countByEventIdIn(List.of(100L, 200L)))
                .thenReturn(List.of(new CountView(100L, 3L)));

        Map<Long, Long> counts = commentService.getCounts(List.of(100L, 200L));

        assertEquals(3L, counts.get(100L));
    }

    private static UserDto user(Long id) {
        return UserDto.builder().id(id).name("User").build();
    }

    private static EventSnapshotDto event(String state) {
        return EventSnapshotDto.builder().id(100L).state(state).build();
    }

    private static Comment comment(Long id, Long eventId, Long authorId, String text) {
        return Comment.builder()
                .id(id)
                .eventId(eventId)
                .authorId(authorId)
                .text(text)
                .created(LocalDateTime.now())
                .build();
    }

    private static class CountView implements CommentRepository.CommentCountView {

        private final Long eventId;
        private final Long cnt;

        private CountView(Long eventId, Long cnt) {
            this.eventId = eventId;
            this.cnt = cnt;
        }

        @Override
        public Long getEventId() {
            return eventId;
        }

        @Override
        public Long getCnt() {
            return cnt;
        }
    }
}
