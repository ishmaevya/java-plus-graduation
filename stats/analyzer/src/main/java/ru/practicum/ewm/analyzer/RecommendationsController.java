package ru.practicum.ewm.analyzer;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.HasUserInteractionRequestProto;
import ru.practicum.ewm.stats.proto.HasUserInteractionResponseProto;
import ru.practicum.ewm.stats.proto.HasUserViewedEventRequestProto;
import ru.practicum.ewm.stats.proto.HasUserViewedEventResponseProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

@GrpcService
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final AnalyzerState analyzerState;

    public RecommendationsController(AnalyzerState analyzerState) {
        this.analyzerState = analyzerState;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        List<ScoredEvent> recommendations = analyzerState.getRecommendations(
                request.getUserId(), request.getMaxResults());
        recommendations.stream()
                .map(RecommendationsController::toProto)
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        List<ScoredEvent> similarEvents = analyzerState.getSimilarEvents(
                request.getEventId(), request.getUserId(), request.getMaxResults());
        similarEvents.stream()
                .map(RecommendationsController::toProto)
                .forEach(responseObserver::onNext);
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        Map<Long, Double> counts = analyzerState.getInteractionsCount(request.getEventIdList());
        for (Long eventId : request.getEventIdList()) {
            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(counts.getOrDefault(eventId, 0.0))
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void hasUserInteraction(HasUserInteractionRequestProto request,
                                   StreamObserver<HasUserInteractionResponseProto> responseObserver) {
        responseObserver.onNext(HasUserInteractionResponseProto.newBuilder()
                .setInteracted(analyzerState.hasAnyInteraction(request.getUserId(), request.getEventId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void hasUserViewedEvent(HasUserViewedEventRequestProto request,
                                   StreamObserver<HasUserViewedEventResponseProto> responseObserver) {
        responseObserver.onNext(HasUserViewedEventResponseProto.newBuilder()
                .setViewed(analyzerState.hasViewedEvent(request.getUserId(), request.getEventId()))
                .build());
        responseObserver.onCompleted();
    }

    private static RecommendedEventProto toProto(ScoredEvent event) {
        return RecommendedEventProto.newBuilder()
                .setEventId(event.eventId())
                .setScore(event.score())
                .build();
    }
}
