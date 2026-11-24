package ru.practicum.ewm.location.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.common.dto.UpdateLocationRequest;
import ru.practicum.ewm.location.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/admin/locations")
@RequiredArgsConstructor
public class AdminLocationController {

    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDtoResponse create(@RequestBody @Valid NewLocationDto dto) {
        return locationService.create(dto);
    }

    @PatchMapping("/{locId}")
    public LocationDtoResponse patch(@PathVariable Long locId,
                                     @RequestBody @Valid UpdateLocationRequest dto) {
        return locationService.patch(locId, dto);
    }

    @DeleteMapping("/{locId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long locId) {
        locationService.delete(locId);
    }

    @GetMapping
    public List<LocationDtoResponse> getAll(Pageable pageable) {
        return locationService.getAll(pageable);
    }

    @GetMapping("/{locId}")
    public LocationDtoResponse getById(@PathVariable Long locId) {
        return locationService.getById(locId);
    }
}
