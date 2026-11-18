package ru.practicum.ewm.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.rating.model.EventRating;
import ru.practicum.ewm.rating.model.RatingType;
import ru.practicum.ewm.rating.repository.EventRatingRepository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingServiceImpl implements RatingService {

    private final EventRatingRepository ratingRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public void setRating(Long userId, Long eventId, RatingType type) {
        userClient.getById(userId);
        EventSnapshotDto event = eventClient.getById(eventId);

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Only published events can be rated");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("An initiator cannot rate their own event");
        }

        EventRating rating = ratingRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> EventRating.builder().eventId(eventId).userId(userId).build());
        rating.setType(type);
        ratingRepository.save(rating);
        log.debug("Rating set by user {} for event {}: {}", userId, eventId, type);
    }

    @Override
    @Transactional
    public void deleteRating(Long userId, Long eventId) {
        userClient.getById(userId);
        eventClient.getById(eventId);
        ratingRepository.findByEventIdAndUserId(eventId, userId).ifPresent(ratingRepository::delete);
    }

    @Override
    public Map<Long, RatingStatsDto> getStats(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return ratingRepository.findStatsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        view -> view.getEventId(),
                        view -> new RatingStatsDto(view.getLikes(), view.getDislikes())
                ));
    }

    @Override
    public RatingStatsDto getStats(Long eventId) {
        return getStats(List.of(eventId)).getOrDefault(eventId, RatingStatsDto.EMPTY);
    }
}
