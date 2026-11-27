package ru.practicum.ewm.analyzer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
public class AnalyzerState {

    private static final int SIBLINGS_LIMIT = 20;

    private final UserInteractionRepository userInteractions;
    private final EventSimilarityRepository eventSimilarities;

    public AnalyzerState(UserInteractionRepository userInteractions,
                         EventSimilarityRepository eventSimilarities) {
        this.userInteractions = userInteractions;
        this.eventSimilarities = eventSimilarities;
    }

    @Transactional
    public void registerAction(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double weight = ActionWeights.get(action.getActionType());
        Instant timestamp = action.getTimestamp();
        boolean viewed = action.getActionType() == ActionTypeAvro.VIEW;

        UserInteraction current = userInteractions.findByUserIdAndEventId(userId, eventId).orElse(null);
        if (current == null) {
            UserInteraction interaction = new UserInteraction();
            interaction.setUserId(userId);
            interaction.setEventId(eventId);
            interaction.setWeight(weight);
            interaction.setTimestamp(timestamp);
            interaction.setViewed(viewed);
            userInteractions.save(interaction);
            return;
        }

        boolean newer = timestamp.isAfter(current.getTimestamp());
        boolean higherWeight = weight > current.getWeight();
        boolean sameWeightAndNewer = weight == current.getWeight() && newer;
        boolean shouldUpdate = higherWeight || sameWeightAndNewer || viewed && !current.getViewed();
        if (shouldUpdate) {
            current.setWeight(higherWeight || sameWeightAndNewer ? weight : current.getWeight());
            current.setTimestamp(newer ? timestamp : current.getTimestamp());
            current.setViewed(current.getViewed() || viewed);
            userInteractions.save(current);
        }
    }

    @Transactional
    public void registerSimilarity(EventSimilarityAvro similarity) {
        long eventA = Math.min(similarity.getEventA(), similarity.getEventB());
        long eventB = Math.max(similarity.getEventA(), similarity.getEventB());

        EventSimilarity current = eventSimilarities.findByEventAAndEventB(eventA, eventB).orElse(null);
        if (current == null) {
            current = new EventSimilarity();
            current.setEventA(eventA);
            current.setEventB(eventB);
        }
        current.setScore(similarity.getScore());
        current.setTimestamp(similarity.getTimestamp());
        eventSimilarities.save(current);
    }

    @Transactional(readOnly = true)
    public List<ScoredEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        Set<Long> interacted = new HashSet<>(userInteractions.findEventIdsByUserId(userId));
        return eventSimilarities.findByEventAOrEventB(eventId, eventId).stream()
                .map(similarity -> new ScoredEvent(
                        opposite(similarity, eventId), similarity.getScore()))
                .filter(scored -> scored.eventId() != eventId)
                .filter(scored -> !(interacted.contains(eventId)
                        && interacted.contains(scored.eventId())))
                .sorted(scoreComparator())
                .limit(maxResults)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoredEvent> getRecommendations(long userId, int maxResults) {
        if (maxResults <= 0) {
            return List.of();
        }

        List<UserInteraction> recent = userInteractions.findByUserIdOrderByTimestampDesc(
                userId, PageRequest.of(0, maxResults));
        if (recent.isEmpty()) {
            return List.of();
        }

        List<UserInteraction> all = userInteractions.findByUserId(userId);
        Map<Long, Double> weights = all.stream().collect(Collectors.toMap(
                UserInteraction::getEventId,
                UserInteraction::getWeight,
                (first, second) -> first));
        Set<Long> interacted = new HashSet<>(weights.keySet());
        Set<Long> seeds = recent.stream()
                .map(UserInteraction::getEventId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ScoredEvent> candidates = eventSimilarities.findByEventAInOrEventBIn(seeds, seeds).stream()
                .map(similarity -> new ScoredEvent(
                        oppositeFromSet(similarity, seeds), similarity.getScore()))
                .filter(candidate -> !interacted.contains(candidate.eventId()))
                .sorted(scoreComparator())
                .limit(maxResults)
                .toList();

        List<ScoredEvent> result = new ArrayList<>();
        for (ScoredEvent candidate : candidates) {
            double numerator = 0.0;
            double denominator = 0.0;
            List<ScoredEvent> siblings = eventSimilarities
                    .findByEventAOrEventB(candidate.eventId(), candidate.eventId()).stream()
                    .map(similarity -> new ScoredEvent(
                            opposite(similarity, candidate.eventId()), similarity.getScore()))
                    .filter(sibling -> interacted.contains(sibling.eventId()))
                    .sorted(scoreComparator())
                    .limit(SIBLINGS_LIMIT)
                    .toList();
            for (ScoredEvent sibling : siblings) {
                numerator += sibling.score() * weights.get(sibling.eventId());
                denominator += sibling.score();
            }
            if (denominator > 0.0) {
                result.add(new ScoredEvent(candidate.eventId(), round(numerator / denominator)));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getInteractionsCount(Collection<Long> eventIds) {
        Set<Long> requested = new LinkedHashSet<>(eventIds);
        Map<Long, Double> counts = new HashMap<>();
        for (Long eventId : requested) {
            counts.put(eventId, 0.0);
        }
        if (requested.isEmpty()) {
            return counts;
        }
        for (InteractionSum sum : userInteractions.sumWeightsByEventIds(requested)) {
            counts.put(sum.getEventId(), sum.getTotalWeight());
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public boolean hasLike(long userId, long eventId) {
        return userInteractions.findByUserIdAndEventId(userId, eventId)
                .map(interaction -> interaction.getWeight() >= 1.0)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasViewedEvent(long userId, long eventId) {
        return userInteractions.existsByUserIdAndEventIdAndViewedTrue(userId, eventId);
    }

    @Transactional(readOnly = true)
    public boolean hasAnyInteraction(long userId, long eventId) {
        return userInteractions.existsByUserIdAndEventId(userId, eventId);
    }

    private long opposite(EventSimilarity similarity, long eventId) {
        return similarity.getEventA() == eventId ? similarity.getEventB() : similarity.getEventA();
    }

    private long oppositeFromSet(EventSimilarity similarity, Set<Long> seeds) {
        return seeds.contains(similarity.getEventA()) ? similarity.getEventB() : similarity.getEventA();
    }

    private Comparator<ScoredEvent> scoreComparator() {
        return Comparator.comparingDouble(ScoredEvent::score)
                .reversed()
                .thenComparingLong(ScoredEvent::eventId);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
