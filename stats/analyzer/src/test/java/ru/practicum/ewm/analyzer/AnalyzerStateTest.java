package ru.practicum.ewm.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@DataJpaTest
@Import(AnalyzerState.class)
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalyzerStateTest {

    @Autowired
    private AnalyzerState analyzerState;

    @Test
    void registerActionKeepsMaxWeightAndViewFlag() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");
        analyzerState.registerAction(action(1, 10, ActionTypeAvro.VIEW, timestamp));
        analyzerState.registerAction(action(1, 10, ActionTypeAvro.LIKE, timestamp.plusSeconds(1)));

        assertTrue(analyzerState.hasViewedEvent(1, 10));
        assertTrue(analyzerState.hasAnyInteraction(1, 10));
        assertEquals(1.0, analyzerState.getInteractionsCount(List.of(10L)).get(10L));
    }

    @Test
    void similarEventsExcludeOnlyPairsInteractedByUserOnBothEnds() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");
        analyzerState.registerSimilarity(similarity(1, 2, 0.9, timestamp));
        analyzerState.registerSimilarity(similarity(1, 3, 0.5, timestamp));
        analyzerState.registerAction(action(1, 1, ActionTypeAvro.VIEW, timestamp));

        assertEquals(List.of(new ScoredEvent(2, 0.9), new ScoredEvent(3, 0.5)),
                analyzerState.getSimilarEvents(1, 1, 10));

        analyzerState.registerAction(action(1, 2, ActionTypeAvro.VIEW, timestamp.plusSeconds(1)));

        assertEquals(List.of(new ScoredEvent(3, 0.5)),
                analyzerState.getSimilarEvents(1, 1, 10));
    }

    @Test
    void recommendationsUseRecentInteractionsOnly() {
        Instant timestamp = Instant.parse("2024-01-01T00:00:00Z");
        analyzerState.registerAction(action(2, 1, ActionTypeAvro.VIEW, timestamp));
        analyzerState.registerAction(action(2, 9, ActionTypeAvro.VIEW, timestamp.plusSeconds(1)));
        analyzerState.registerAction(action(2, 2, ActionTypeAvro.VIEW, timestamp.plusSeconds(2)));
        analyzerState.registerAction(action(2, 3, ActionTypeAvro.VIEW, timestamp.plusSeconds(3)));
        analyzerState.registerSimilarity(similarity(1, 4, 0.9, timestamp));
        analyzerState.registerSimilarity(similarity(2, 5, 0.8, timestamp));
        analyzerState.registerSimilarity(similarity(3, 6, 0.7, timestamp));

        assertEquals(List.of(new ScoredEvent(5, 0.4), new ScoredEvent(6, 0.4)),
                analyzerState.getRecommendations(2, 2));
    }

    private static UserActionAvro action(long userId, long eventId, ActionTypeAvro type, Instant timestamp) {
        return UserActionAvro.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(timestamp)
                .build();
    }

    private static EventSimilarityAvro similarity(long eventA, long eventB, double score, Instant timestamp) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(score)
                .setTimestamp(timestamp)
                .build();
    }
}
