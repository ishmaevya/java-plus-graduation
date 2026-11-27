package ru.practicum.ewm.stats.grpc;

import io.grpc.Channel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.HasUserInteractionRequestProto;
import ru.practicum.ewm.stats.proto.HasUserViewedEventRequestProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

@Component
public class RecommendationsClientImpl implements RecommendationsClient {

    @GrpcClient("analyzer")
    private Channel channel;

    @Override
    public List<RecommendedEvent> getRecommendations(long userId, int maxResults) {
        Iterator<RecommendedEventProto> iterator = stub().getRecommendationsForUser(
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build());
        return toEvents(iterator);
    }

    @Override
    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        Iterator<RecommendedEventProto> iterator = stub().getSimilarEvents(
                SimilarEventsRequestProto.newBuilder()
                        .setEventId(eventId)
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build());
        return toEvents(iterator);
    }

    @Override
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        Iterator<RecommendedEventProto> iterator = stub().getInteractionsCount(
                InteractionsCountRequestProto.newBuilder()
                        .addAllEventId(eventIds)
                        .build());
        Map<Long, Double> counts = new LinkedHashMap<>();
        while (iterator.hasNext()) {
            RecommendedEventProto event = iterator.next();
            counts.put(event.getEventId(), event.getScore());
        }
        return counts;
    }

    @Override
    public boolean hasUserInteraction(long userId, long eventId) {
        return stub().hasUserInteraction(HasUserInteractionRequestProto.newBuilder()
                        .setUserId(userId)
                        .setEventId(eventId)
                        .build())
                .getInteracted();
    }

    @Override
    public boolean hasUserViewedEvent(long userId, long eventId) {
        return stub().hasUserViewedEvent(HasUserViewedEventRequestProto.newBuilder()
                        .setUserId(userId)
                        .setEventId(eventId)
                        .build())
                .getViewed();
    }

    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub() {
        return RecommendationsControllerGrpc.newBlockingStub(channel)
                .withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private static List<RecommendedEvent> toEvents(Iterator<RecommendedEventProto> iterator) {
        List<RecommendedEvent> events = new ArrayList<>();
        while (iterator.hasNext()) {
            RecommendedEventProto event = iterator.next();
            events.add(new RecommendedEvent(event.getEventId(), event.getScore()));
        }
        return events;
    }
}
