package ru.practicum.ewm.collector;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@GrpcService
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<Long, SpecificRecordBase> kafkaTemplate;
    private final String userActionsTopic;

    public UserActionController(KafkaTemplate<Long, SpecificRecordBase> kafkaTemplate,
                                @Value("${collector.topic.user-actions}") String userActionsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.userActionsTopic = userActionsTopic;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            UserActionAvro action = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(mapActionType(request.getActionType()))
                    .setTimestamp(toInstant(request))
                    .build();

            kafkaTemplate.send(userActionsTopic, request.getUserId(), action).get();
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to collect user action: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private static Instant toInstant(UserActionProto request) {
        return Instant.ofEpochSecond(request.getTimestamp().getSeconds(), request.getTimestamp().getNanos());
    }

    private static ActionTypeAvro mapActionType(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }
}
