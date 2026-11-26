package ru.practicum.ewm.aggregator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Service
public class SimilarityProcessor {

    private final Map<Long, Map<Long, Double>> eventWeights = new HashMap<>();
    private final Map<Long, Double> eventNormSums = new HashMap<>();
    private final Map<EventPair, Double> pairMinSums = new HashMap<>();

    public synchronized List<EventSimilarityAvro> process(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double actionWeight = ActionWeights.get(action.getActionType());

        Map<Long, Double> users = eventWeights.computeIfAbsent(eventId, key -> new HashMap<>());
        double oldWeight = users.getOrDefault(userId, 0.0);
        double newWeight = Math.max(oldWeight, actionWeight);
        double delta = newWeight - oldWeight;

        if (delta <= 0.0) {
            return List.of();
        }

        users.put(userId, newWeight);
        eventNormSums.merge(eventId, delta, Double::sum);

        List<EventSimilarityAvro> similarities = new ArrayList<>();
        Instant timestamp = Instant.now();

        for (Map.Entry<Long, Map<Long, Double>> entry : eventWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) {
                continue;
            }

            double otherWeight = entry.getValue().getOrDefault(userId, 0.0);
            if (otherWeight <= 0.0) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double minDelta = newMin - oldMin;
            if (minDelta <= 0.0) {
                continue;
            }

            EventPair pair = EventPair.of(eventId, otherEventId);
            double minSum = pairMinSums.merge(pair, minDelta, Double::sum);
            double score = minSum / (Math.sqrt(eventNormSums.get(eventId))
                    * Math.sqrt(eventNormSums.get(otherEventId)));

            similarities.add(EventSimilarityAvro.newBuilder()
                    .setEventA(pair.eventA())
                    .setEventB(pair.eventB())
                    .setScore(score)
                    .setTimestamp(timestamp)
                    .build());
        }

        return similarities;
    }
}
