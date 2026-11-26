package ru.practicum.ewm.stats.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.Channel;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Component
public class UserActionClientImpl implements UserActionClient {

    @GrpcClient("collector")
    private Channel channel;

    @Override
    public void collect(long userId, long eventId, ActionType actionType, Instant timestamp) {
        Instant moment = timestamp == null ? Instant.now() : timestamp;
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(toProto(actionType))
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(moment.getEpochSecond())
                        .setNanos(moment.getNano()))
                .build();

        UserActionControllerGrpc.newBlockingStub(channel)
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .collectUserAction(request);
    }

    private static ActionTypeProto toProto(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }
}
