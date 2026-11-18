package ru.practicum.ewm.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.LocationSearchRequest;
import ru.practicum.ewm.location.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
public class PublicLocationController {

    private final LocationService locationService;

    @GetMapping
    public List<EventShortDto> getEventsInLocation(@Valid LocationSearchRequest request) {
        return locationService.getEventsInLocation(request);
    }
}
