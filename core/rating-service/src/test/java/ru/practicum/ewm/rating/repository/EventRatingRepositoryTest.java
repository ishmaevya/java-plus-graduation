package ru.practicum.ewm.rating.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.rating.model.EventRating;
import ru.practicum.ewm.rating.model.RatingType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class EventRatingRepositoryTest {

    @Autowired
    private EventRatingRepository ratingRepository;

    @Test
    void savesAndCountsRatings() {
        ratingRepository.save(EventRating.builder()
                .eventId(100L)
                .userId(1L)
                .type(RatingType.LIKE)
                .build());
        ratingRepository.save(EventRating.builder()
                .eventId(100L)
                .userId(2L)
                .type(RatingType.DISLIKE)
                .build());

        assertTrue(ratingRepository.findByEventIdAndUserId(100L, 1L).isPresent());
        List<RatingStatsView> stats = ratingRepository.findStatsByEventIds(List.of(100L));
        assertEquals(1, stats.size());
        assertEquals(100L, stats.get(0).getEventId());
        assertEquals(1L, stats.get(0).getLikes());
        assertEquals(1L, stats.get(0).getDislikes());
    }
}
