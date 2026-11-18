package ru.practicum.ewm.comment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.comment.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findsByEventAndAuthorAndCountsComments() {
        commentRepository.save(comment(100L, 1L, "first"));
        commentRepository.save(comment(100L, 2L, "second"));
        commentRepository.save(comment(200L, 1L, "third"));

        assertEquals(2, commentRepository.findByEventIdOrderByCreatedDesc(100L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, commentRepository.findByAuthorIdOrderByCreatedDesc(1L, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2L, commentRepository.countByEventId(100L));

        List<CommentRepository.CommentCountView> counts = commentRepository.countByEventIdIn(List.of(100L, 200L));
        assertEquals(2, counts.size());
    }

    private static Comment comment(Long eventId, Long authorId, String text) {
        return Comment.builder()
                .eventId(eventId)
                .authorId(authorId)
                .text(text)
                .created(LocalDateTime.now())
                .build();
    }
}
