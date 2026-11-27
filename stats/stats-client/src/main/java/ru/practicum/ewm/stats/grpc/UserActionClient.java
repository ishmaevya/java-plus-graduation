package ru.practicum.ewm.stats.grpc;

import java.time.Instant;

public interface UserActionClient {

    void collect(long userId, long eventId, ActionType actionType, Instant timestamp);
}
