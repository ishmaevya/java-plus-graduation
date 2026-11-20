package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.client.CategoryClient;
import ru.practicum.ewm.common.client.CommentClient;
import ru.practicum.ewm.common.client.RatingClient;
import ru.practicum.ewm.common.client.RequestClient;
import ru.practicum.ewm.common.client.SubscriptionClient;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.NewEventDto;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.common.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.common.dto.UpdateEventUserRequest;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.event.exception.CategoryNotFoundException;
import ru.practicum.ewm.event.exception.EventNotFoundException;
import ru.practicum.ewm.event.exception.UserNotFoundException;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.event.repository.EventSpecification;
import ru.practicum.ewm.stats.clients.StatsClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final String PUBLISH_EVENT = "PUBLISH_EVENT";
    private static final String REJECT_EVENT = "REJECT_EVENT";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EventRepository eventRepository;
    private final EventSpecification eventSpecification;
    private final EventMapper mapper;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final RequestClient requestClient;
    private final RatingClient ratingClient;
    private final CommentClient commentClient;
    private final SubscriptionClient subscriptionClient;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        UserDto initiator = userClient.getById(userId);
        CategoryDto category = getCategory(dto.getCategory());
        validateEventDate(dto.getEventDate(), 2);

        Event event = eventRepository.save(mapper.toEvent(dto, category.getId(), initiator.getId()));
        log.debug("Event created: {}", event.getId());
        return toFullDto(event, category, toShort(initiator));
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        userClient.getById(userId);
        validatePaging(from, size);
        List<Event> events = eventRepository.findByInitiatorId(userId, PageRequest.of(from / size, size)).getContent();
        return toShortDtos(events);
    }

    @Override
    public EventFullDto getByUserAndEvent(Long userId, Long eventId) {
        userClient.getById(userId);
        Event event = findEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new EventNotFoundException(eventId);
        }
        return toFullDto(event, getCategory(event.getCategoryId()),
                toShort(userClient.getById(userId)));
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        userClient.getById(userId);
        Event event = findEvent(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new EventNotFoundException(eventId);
        }
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate(), 2);
        }

        CategoryDto category = dto.getCategory() != null ? getCategory(dto.getCategory()) : null;
        mapper.updateFromUserRequest(event, dto, category != null ? category.getId() : null);
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new ValidationException("Unknown state action: " + dto.getStateAction());
            }
        }

        eventRepository.save(event);
        return toFullDto(event, category != null ? category : getCategory(event.getCategoryId()),
                toShort(userClient.getById(userId)));
    }

    @Override
    public List<EventFullDto> getByAdmin(List<Long> users, List<String> states, List<Long> categories,
                                         String rangeStart, String rangeEnd, Integer from, Integer size) {
        validatePaging(from, size);
        List<EventState> parsedStates = states == null ? List.of() : states.stream()
                .map(state -> {
                    try {
                        return EventState.valueOf(state);
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Unknown state: " + state);
                    }
                })
                .toList();
        LocalDateTime start = rangeStart != null ? parseDate(rangeStart, "rangeStart") : null;
        LocalDateTime end = rangeEnd != null ? parseDate(rangeEnd, "rangeEnd") : null;
        validateRange(start, end, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findAll(
                eventSpecification.eventsByAdmin(users, parsedStates, categories, start, end),
                PageRequest.of(from / size, size)
        ).getContent();
        return events.stream().map(this::toFullDto).toList();
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = findEvent(eventId);
        CategoryDto category = dto.getCategory() != null ? getCategory(dto.getCategory()) : null;
        if (dto.getEventDate() != null) {
            validateEventDate(dto.getEventDate(), 1);
        }

        mapper.updateFromAdminRequest(event, dto, category != null ? category.getId() : null);
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Event date must be at least 1 hour from now");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                }
                default -> throw new ValidationException("Unknown state action: " + dto.getStateAction());
            }
        }

        eventRepository.save(event);
        return toFullDto(event, category != null ? category : getCategory(event.getCategoryId()),
                toShort(userClient.getById(event.getInitiatorId())));
    }

    @Override
    public List<EventShortDto> getPublished(String text, List<Long> categories, Boolean paid,
                                            String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                            String sort, Integer from, Integer size, Long userId) {
        validatePaging(from, size);
        LocalDateTime start = rangeStart != null ? parseDate(rangeStart, "rangeStart") : null;
        LocalDateTime end = rangeEnd != null ? parseDate(rangeEnd, "rangeEnd") : null;
        validateRange(start, end, rangeStart, rangeEnd);
        if (start == null && end == null) {
            start = LocalDateTime.now();
        }

        boolean commentsSort = "COMMENTS".equals(sort);
        boolean ratingSort = "RATING".equals(sort);
        boolean noPaginationSort = commentsSort || ratingSort;

        List<Event> events;
        if (noPaginationSort) {
            events = eventRepository.findAll(
                    eventSpecification.publishedEvents(text, categories, paid, start, end));
        } else {
            events = eventRepository.findAll(
                    eventSpecification.publishedEvents(text, categories, paid, start, end),
                    PageRequest.of(from / size, size)
            ).getContent();
        }

        List<EventShortDto> subscribed = new ArrayList<>();
        List<EventShortDto> other = new ArrayList<>();
        for (EventShortDto dto : toShortDtos(events)) {
            if (Boolean.TRUE.equals(onlyAvailable) && dto.getConfirmedRequests() > 0
                    && eventLimitReached(events, dto)) {
                continue;
            }
            if (userId != null && isSubscribed(userId, dto.getInitiator().getId())) {
                subscribed.add(dto);
            } else {
                other.add(dto);
            }
        }

        sortEvents(subscribed, commentsSort, ratingSort, sort);
        sortEvents(other, commentsSort, ratingSort, sort);

        List<EventShortDto> result = new ArrayList<>(subscribed);
        result.addAll(other);
        if (!noPaginationSort) {
            return result;
        }
        int startIndex = Math.min(from, result.size());
        int endIndex = Math.min(startIndex + size, result.size());
        return new ArrayList<>(result.subList(startIndex, endIndex));
    }

    @Override
    public EventFullDto getPublishedById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new EventNotFoundException(id));
        return toFullDto(event, getCategory(event.getCategoryId()),
                toShort(userClient.getById(event.getInitiatorId())));
    }

    @Override
    public EventSnapshotDto getSnapshot(Long eventId) {
        return mapper.toSnapshot(findEvent(eventId));
    }

    @Override
    public List<EventShortDto> getByIds(List<Long> ids) {
        Map<Long, Event> byId = eventRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        List<Event> ordered = new ArrayList<>();
        for (Long id : ids) {
            Event event = byId.get(id);
            if (event != null) {
                ordered.add(event);
            }
        }
        return toShortDtos(ordered);
    }

    @Override
    public long countByCategoryId(Long categoryId) {
        return eventRepository.countByCategoryId(categoryId);
    }

    private boolean eventLimitReached(List<Event> events, EventShortDto dto) {
        return events.stream()
                .filter(event -> event.getId().equals(dto.getId()))
                .findFirst()
                .map(event -> event.getParticipantLimit() > 0
                        && dto.getConfirmedRequests() >= event.getParticipantLimit())
                .orElse(false);
    }

    private void sortEvents(List<EventShortDto> events, boolean commentsSort, boolean ratingSort, String sort) {
        if (commentsSort) {
            events.sort(Comparator.comparingLong(EventShortDto::getCommentCount).reversed());
        } else if (ratingSort) {
            events.sort(Comparator.comparingLong(EventShortDto::getRating).reversed());
        } else if ("VIEWS".equals(sort)) {
            events.sort(Comparator.comparingLong(EventShortDto::getViews));
        }
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, CategoryDto> categories = getCategories(events);
        Map<Long, UserShortDto> users = getUsers(events);
        Map<Long, RatingStatsDto> ratings = getRatings(ids);
        Map<Long, Long> commentCounts = getCommentCounts(ids);

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            CategoryDto category = categories.getOrDefault(event.getCategoryId(),
                    new CategoryDto(event.getCategoryId(), null));
            UserShortDto initiator = users.getOrDefault(event.getInitiatorId(),
                    new UserShortDto(event.getInitiatorId(), null));
            RatingStatsDto rating = ratings.getOrDefault(event.getId(), RatingStatsDto.EMPTY);
            result.add(mapper.toShortDto(
                    event,
                    category,
                    initiator,
                    getViews(event.getId()),
                    getConfirmed(event.getId()),
                    rating,
                    commentCounts.getOrDefault(event.getId(), 0L)
            ));
        }
        return result;
    }

    private EventFullDto toFullDto(Event event) {
        return toFullDto(event, getCategory(event.getCategoryId()),
                toShort(userClient.getById(event.getInitiatorId())));
    }

    private EventFullDto toFullDto(Event event, CategoryDto category, UserShortDto initiator) {
        return mapper.toFullDto(
                event,
                category,
                initiator,
                getViews(event.getId()),
                getConfirmed(event.getId()),
                getRating(event.getId()),
                getCommentCount(event.getId())
        );
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private CategoryDto getCategory(Long categoryId) {
        CategoryDto category = categoryClient.getById(categoryId);
        if (category == null) {
            throw new CategoryNotFoundException(categoryId);
        }
        return category;
    }

    private UserShortDto toShort(UserDto user) {
        if (user == null) {
            throw new UserNotFoundException(null);
        }
        return new UserShortDto(user.getId(), user.getName());
    }

    private Map<Long, CategoryDto> getCategories(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getCategoryId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<CategoryDto> categories = categoryClient.getByIds(ids);
            return categories == null ? Map.of() : categories.stream()
                    .collect(Collectors.toMap(CategoryDto::getId, Function.identity(), (first, second) -> first));
        } catch (Exception e) {
            log.warn("Failed to load categories: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, UserShortDto> getUsers(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserDto> users = userClient.getByIds(ids);
            if (users == null) {
                return Map.of();
            }
            return users.stream().map(this::toShort).collect(Collectors.toMap(
                    UserShortDto::getId, Function.identity(), (first, second) -> first));
        } catch (Exception e) {
            log.warn("Failed to load users: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, RatingStatsDto> getRatings(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            Map<Long, RatingStatsDto> stats = ratingClient.getStatsBatch(ids);
            return stats == null ? Map.of() : stats;
        } catch (Exception e) {
            log.warn("Failed to load ratings: {}", e.getMessage());
            return Map.of();
        }
    }

    private Map<Long, Long> getCommentCounts(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        try {
            Map<Long, Long> counts = commentClient.getCounts(ids);
            return counts == null ? Map.of() : counts;
        } catch (Exception e) {
            log.warn("Failed to load comment counts: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long getViews(Long eventId) {
        try {
            var stats = statsClient.getStats(
                    LocalDateTime.of(2020, 1, 1, 0, 0),
                    LocalDateTime.now(),
                    List.of("/events/" + eventId),
                    true
            );
            return stats == null || stats.isEmpty() ? 0L : stats.getFirst().getHits();
        } catch (Exception e) {
            log.warn("Failed to get views for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private Long getConfirmed(Long eventId) {
        try {
            Long count = requestClient.getConfirmedCount(eventId);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("Failed to get confirmed requests for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private RatingStatsDto getRating(Long eventId) {
        try {
            RatingStatsDto stats = ratingClient.getStats(eventId);
            return stats == null ? RatingStatsDto.EMPTY : stats;
        } catch (Exception e) {
            log.warn("Failed to get rating for event {}: {}", eventId, e.getMessage());
            return RatingStatsDto.EMPTY;
        }
    }

    private Long getCommentCount(Long eventId) {
        try {
            Long count = commentClient.getCount(eventId);
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("Failed to get comment count for event {}: {}", eventId, e.getMessage());
            return 0L;
        }
    }

    private boolean isSubscribed(Long userId, Long initiatorId) {
        if (userId.equals(initiatorId)) {
            return false;
        }
        try {
            Boolean subscribed = subscriptionClient.isSubscribed(userId, initiatorId);
            return Boolean.TRUE.equals(subscribed);
        } catch (Exception e) {
            log.warn("Failed to check subscription: {}", e.getMessage());
            return false;
        }
    }

    private void validateEventDate(String value, int hours) {
        LocalDateTime eventDate = parseDate(value, "eventDate");
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
            throw new ValidationException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: "
                    + value);
        }
    }

    private LocalDateTime parseDate(String value, String field) {
        try {
            return LocalDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Field: " + field + ". Error: incorrect date format. Value: " + value);
        }
    }

    private void validateRange(LocalDateTime start, LocalDateTime end, String rawStart, String rawEnd) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Field: rangeStart. Error: start must be before end. Value: rangeStart="
                    + rawStart + ", rangeEnd=" + rawEnd);
        }
    }

    private void validatePaging(Integer from, Integer size) {
        if (from < 0 || size <= 0) {
            throw new ValidationException("Incorrectly made request: from must be >= 0 and size must be > 0");
        }
    }
}
