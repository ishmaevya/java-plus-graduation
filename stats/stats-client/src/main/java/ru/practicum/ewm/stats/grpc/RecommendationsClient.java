package ru.practicum.ewm.stats.grpc;

import java.util.List;
import java.util.Map;

public interface RecommendationsClient {

    List<RecommendedEvent> getRecommendations(long userId, int maxResults);

    List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults);

    Map<Long, Double> getInteractionsCount(List<Long> eventIds);

    boolean hasUserInteraction(long userId, long eventId);
}
