package ru.practicum.ewm.location.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.LocationSearchRequest;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.common.dto.UpdateLocationRequest;

import java.util.List;

public interface LocationService {

    LocationDtoResponse create(NewLocationDto dto);

    LocationDtoResponse patch(Long locId, UpdateLocationRequest dto);

    void delete(Long locId);

    List<LocationDtoResponse> getAll(Pageable pageable);

    LocationDtoResponse getById(Long locId);

    List<EventShortDto> getEventsInLocation(LocationSearchRequest request);
}
