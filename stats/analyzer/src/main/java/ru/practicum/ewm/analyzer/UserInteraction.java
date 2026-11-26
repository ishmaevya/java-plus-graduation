package ru.practicum.ewm.analyzer;

import java.time.Instant;

public record UserInteraction(long userId, long eventId, double rating, Instant timestamp) {
}
