package ru.practicum.ewm.location.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.LocationSearchRequest;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.common.dto.UpdateLocationRequest;
import ru.practicum.ewm.location.exception.LocationNotFoundException;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper mapper;
    private final EventClient eventClient;

    @Override
    @Transactional
    public LocationDtoResponse create(NewLocationDto dto) {
        eventClient.getById(dto.getEventId());
        Location location = locationRepository.save(mapper.toModel(dto));
        log.debug("Location created: {}", location.getId());
        return mapper.toDto(location);
    }

    @Override
    @Transactional
    public LocationDtoResponse patch(Long locId, UpdateLocationRequest dto) {
        Location location = findLocation(locId);
        if (dto.getEventId() != null) {
            eventClient.getById(dto.getEventId());
        }
        mapper.update(dto, location);
        location = locationRepository.save(location);
        log.debug("Location patched: {}", locId);
        return mapper.toDto(location);
    }

    @Override
    @Transactional
    public void delete(Long locId) {
        Location location = findLocation(locId);
        locationRepository.delete(location);
        log.debug("Location deleted: {}", locId);
    }

    @Override
    public List<LocationDtoResponse> getAll(Pageable pageable) {
        return locationRepository.findAllWithOffset(pageable).stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public LocationDtoResponse getById(Long locId) {
        return mapper.toDto(findLocation(locId));
    }

    @Override
    public List<EventShortDto> getEventsInLocation(LocationSearchRequest request) {
        List<Long> eventIds = locationRepository.findAll().stream()
                .filter(location -> distanceKm(
                        request.getLat(), request.getLon(), location.getLat(), location.getLon())
                        <= request.getRadius())
                .map(Location::getEventId)
                .collect(Collectors.toList());
        if (eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        return eventClient.getByIds(eventIds);
    }

    private Location findLocation(Long locId) {
        return locationRepository.findById(locId)
                .orElseThrow(() -> new LocationNotFoundException(locId));
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == lat2 && lon1 == lon2) {
            return 0;
        }
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double theta = lon1 - lon2;
        double radTheta = Math.toRadians(theta);
        double distance = Math.sin(radLat1) * Math.sin(radLat2)
                + Math.cos(radLat1) * Math.cos(radLat2) * Math.cos(radTheta);
        distance = Math.min(distance, 1);
        distance = Math.max(distance, -1);
        distance = Math.acos(distance);
        distance = Math.toDegrees(distance);
        return distance * 60 * 1.8524;
    }
}
