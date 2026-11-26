package ru.practicum.ewm.analyzer;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
public class AnalyzerState {

    private static final int SIBLINGS_LIMIT = 20;

    private final Map<Long, Map<Long, UserInteraction>> userInteractions = new HashMap<>();
    private final Map<Long, Double> eventRatingSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> similarityByEvent = new HashMap<>();

    public synchronized void registerAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double weight = ActionWeights.get(action.getActionType());
        Instant timestamp = action.getTimestamp();

        Map<Long, UserInteraction> interactions = userInteractions.computeIfAbsent(
                userId, key -> new HashMap<>());
        UserInteraction current = interactions.get(eventId);
        boolean shouldUpdate = current == null
                || weight > current.rating()
                || (weight == current.rating() && timestamp.isAfter(current.timestamp()));

        if (shouldUpdate) {
            double previous = current == null ? 0.0 : current.rating();
            interactions.put(eventId, new UserInteraction(userId, eventId, weight, timestamp));
            eventRatingSums.merge(eventId, weight - previous, Double::sum);
        }
    }

    public synchronized void registerSimilarity(EventSimilarityAvro similarity) {
        long eventA = similarity.getEventA();
        long eventB = similarity.getEventB();

        similarityByEvent.computeIfAbsent(eventA, key -> new HashMap<>()).put(eventB, similarity.getScore());
        similarityByEvent.computeIfAbsent(eventB, key -> new HashMap<>()).put(eventA, similarity.getScore());
    }

    public synchronized List<ScoredEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        Map<Long, UserInteraction> interactions = userInteractions.getOrDefault(userId, Map.of());
        if (maxResults <= 0 || interactions.containsKey(eventId)) {
            return List.of();
        }
        return siblings(eventId, userId, false, maxResults);
    }

    public synchronized List<ScoredEvent> getRecommendations(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        Map<Long, UserInteraction> interactions = userInteractions.getOrDefault(userId, Map.of());
        Map<Long, double[]> accumulators = new HashMap<>();

        for (Long candidate : similarityByEvent.keySet()) {
            if (interactions.containsKey(candidate)) {
                continue;
            }

            double numerator = 0.0;
            double denominator = 0.0;
            for (ScoredEvent similar : siblings(candidate, userId, true, SIBLINGS_LIMIT)) {
                UserInteraction interaction = interactions.get(similar.eventId());
                numerator += similar.score() * interaction.rating();
                denominator += similar.score();
            }

            if (denominator > 0.0) {
                accumulators.put(candidate, new double[]{numerator, denominator});
            }
        }

        return accumulators.entrySet().stream()
                .sorted(Comparator
                        .comparingDouble((Map.Entry<Long, double[]> entry) -> entry.getValue()[0]
                                / entry.getValue()[1])
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(maxResults)
                .map(entry -> new ScoredEvent(entry.getKey(), entry.getValue()[0] / entry.getValue()[1]))
                .toList();
    }

    public synchronized Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        Map<Long, Double> counts = new HashMap<>();
        for (Long eventId : new LinkedHashSet<>(eventIds)) {
            counts.put(eventId, eventRatingSums.getOrDefault(eventId, 0.0));
        }
        return counts;
    }

    public synchronized boolean hasLike(long userId, long eventId) {
        UserInteraction interaction = userInteractions.getOrDefault(userId, Map.of()).get(eventId);
        return interaction != null && interaction.rating() >= 1.0;
    }

    private List<ScoredEvent> siblings(long eventId, long userId, boolean requireInteraction, int limit) {
        Map<Long, UserInteraction> interactions = userInteractions.getOrDefault(userId, Map.of());
        return similarityByEvent.getOrDefault(eventId, Map.of()).entrySet().stream()
                .filter(entry -> interactions.containsKey(entry.getKey()) == requireInteraction)
                .sorted(Comparator
                        .comparingDouble((Map.Entry<Long, Double> entry) -> entry.getValue())
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(entry -> new ScoredEvent(entry.getKey(), entry.getValue()))
                .toList();
    }
}
