package ru.practicum.ewm.location.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.common.dto.UpdateLocationRequest;
import ru.practicum.ewm.location.model.Location;

@Component
public class LocationMapper {

    public Location toModel(NewLocationDto dto) {
        return Location.builder()
                .eventId(dto.getEventId())
                .name(dto.getName())
                .lat(dto.getLat())
                .lon(dto.getLon())
                .build();
    }

    public LocationDtoResponse toDto(Location location) {
        return LocationDtoResponse.builder()
                .id(location.getId())
                .eventId(location.getEventId())
                .name(location.getName())
                .lat(location.getLat())
                .lon(location.getLon())
                .build();
    }

    public void update(UpdateLocationRequest dto, Location location) {
        if (dto.getEventId() != null) {
            location.setEventId(dto.getEventId());
        }
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            location.setName(dto.getName());
        }
        if (dto.getLat() != null) {
            location.setLat(dto.getLat());
        }
        if (dto.getLon() != null) {
            location.setLon(dto.getLon());
        }
    }
}
