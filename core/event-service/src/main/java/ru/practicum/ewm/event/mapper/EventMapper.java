package ru.practicum.ewm.event.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.dto.EventFullDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.LocationDto;
import ru.practicum.ewm.common.dto.NewEventDto;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.common.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.common.dto.UpdateEventUserRequest;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Event toEvent(NewEventDto dto, Long categoryId, Long initiatorId) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .categoryId(categoryId)
                .createdOn(LocalDateTime.now())
                .description(dto.getDescription())
                .eventDate(parse(dto.getEventDate()))
                .initiatorId(initiatorId)
                .location(new Location(dto.getLocation().getLat(), dto.getLocation().getLon()))
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .state(EventState.PENDING)
                .title(dto.getTitle())
                .build();
    }

    public void updateFromUserRequest(Event event, UpdateEventUserRequest dto, Long categoryId) {
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (categoryId != null) {
            event.setCategoryId(categoryId);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(parse(dto.getEventDate()));
        }
        if (dto.getLocation() != null) {
            event.setLocation(toLocation(dto.getLocation()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }

    public void updateFromAdminRequest(Event event, UpdateEventAdminRequest dto, Long categoryId) {
        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (categoryId != null) {
            event.setCategoryId(categoryId);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(parse(dto.getEventDate()));
        }
        if (dto.getLocation() != null) {
            event.setLocation(toLocation(dto.getLocation()));
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }

    public EventSnapshotDto toSnapshot(Event event) {
        return EventSnapshotDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .categoryId(event.getCategoryId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .eventDate(event.getEventDate())
                .publishedOn(event.getPublishedOn())
                .build();
    }

    public EventFullDto toFullDto(Event event, CategoryDto category, UserShortDto initiator,
                                   Long confirmedRequests, RatingStatsDto ratingStats, Double rating,
                                   Long commentCount) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(category)
                .confirmedRequests(confirmedRequests)
                .createdOn(format(event.getCreatedOn()))
                .description(event.getDescription())
                .eventDate(format(event.getEventDate()))
                .initiator(initiator)
                .location(toLocationDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(format(event.getPublishedOn()))
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .likes(ratingStats.getLikes())
                .dislikes(ratingStats.getDislikes())
                .rating(rating)
                .commentCount(commentCount)
                .build();
    }

    public EventShortDto toShortDto(Event event, CategoryDto category, UserShortDto initiator,
                                     Long confirmedRequests, RatingStatsDto ratingStats, Double rating,
                                     Long commentCount) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(category)
                .confirmedRequests(confirmedRequests)
                .eventDate(format(event.getEventDate()))
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .likes(ratingStats.getLikes())
                .dislikes(ratingStats.getDislikes())
                .rating(rating)
                .commentCount(commentCount)
                .build();
    }

    public LocalDateTime parse(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }

    private String format(LocalDateTime value) {
        return value != null ? value.format(FORMATTER) : null;
    }

    private Location toLocation(LocationDto dto) {
        return new Location(dto.getLat(), dto.getLon());
    }

    private LocationDto toLocationDto(Location location) {
        if (location == null) {
            return null;
        }
        return new LocationDto(location.getLat(), location.getLon());
    }
}
