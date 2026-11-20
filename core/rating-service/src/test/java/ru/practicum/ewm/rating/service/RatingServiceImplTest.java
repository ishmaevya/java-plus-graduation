package ru.practicum.ewm.rating.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.rating.model.EventRating;
import ru.practicum.ewm.rating.model.RatingType;
import ru.practicum.ewm.rating.repository.EventRatingRepository;
import ru.practicum.ewm.rating.repository.RatingStatsView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private EventRatingRepository ratingRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private RatingServiceImpl ratingService;

    @Test
    void setRatingSavesLike() {
        when(eventClient.getById(100L)).thenReturn(publishedEvent(2L));
        when(ratingRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        ratingService.setRating(1L, 100L, RatingType.LIKE);

        ArgumentCaptor<EventRating> captor = ArgumentCaptor.forClass(EventRating.class);
        verify(ratingRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(100L, captor.getValue().getEventId());
        assertEquals(RatingType.LIKE, captor.getValue().getType());
    }

    @Test
    void setRatingRejectsUnpublishedEvent() {
        EventSnapshotDto event = publishedEvent(2L);
        event.setState("PENDING");
        when(eventClient.getById(100L)).thenReturn(event);

        assertThrows(ConflictException.class,
                () -> ratingService.setRating(1L, 100L, RatingType.LIKE));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void setRatingRejectsInitiator() {
        when(eventClient.getById(100L)).thenReturn(publishedEvent(1L));

        assertThrows(ConflictException.class,
                () -> ratingService.setRating(1L, 100L, RatingType.DISLIKE));
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void deleteRatingRemovesExistingRating() {
        EventRating rating = EventRating.builder().eventId(100L).userId(1L).type(RatingType.LIKE).build();
        when(ratingRepository.findByEventIdAndUserId(100L, 1L)).thenReturn(Optional.of(rating));

        ratingService.deleteRating(1L, 100L);

        verify(ratingRepository).delete(rating);
    }

    @Test
    void getStatsMapsRepositoryViews() {
        when(ratingRepository.findStatsByEventIds(List.of(100L, 200L)))
                .thenReturn(List.of(new StatsView(100L, 3L, 1L)));

        Map<Long, RatingStatsDto> stats = ratingService.getStats(List.of(100L, 200L));

        assertEquals(3L, stats.get(100L).getLikes());
        assertEquals(1L, stats.get(100L).getDislikes());
        assertEquals(2L, stats.get(100L).getRating());
    }

    @Test
    void getStatsForEventReturnsEmptyWhenNoRatings() {
        when(ratingRepository.findStatsByEventIds(List.of(100L))).thenReturn(List.of());

        RatingStatsDto stats = ratingService.getStats(100L);

        assertEquals(0L, stats.getLikes());
        assertEquals(0L, stats.getDislikes());
    }

    private static EventSnapshotDto publishedEvent(Long initiatorId) {
        return EventSnapshotDto.builder()
                .id(100L)
                .initiatorId(initiatorId)
                .state("PUBLISHED")
                .build();
    }

    private static class StatsView implements RatingStatsView {

        private final Long eventId;
        private final Long likes;
        private final Long dislikes;

        private StatsView(Long eventId, Long likes, Long dislikes) {
            this.eventId = eventId;
            this.likes = likes;
            this.dislikes = dislikes;
        }

        @Override
        public Long getEventId() {
            return eventId;
        }

        @Override
        public Long getLikes() {
            return likes;
        }

        @Override
        public Long getDislikes() {
            return dislikes;
        }
    }
}
