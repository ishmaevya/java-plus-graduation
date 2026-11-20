package ru.practicum.ewm.location.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.EventSnapshotDto;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.LocationSearchRequest;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.common.dto.UpdateLocationRequest;
import ru.practicum.ewm.location.exception.LocationNotFoundException;
import ru.practicum.ewm.location.mapper.LocationMapper;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.location.repository.LocationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Spy
    private LocationMapper mapper = new LocationMapper();

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private LocationServiceImpl locationService;

    @Test
    void createValidatesEventAndSavesLocation() {
        when(eventClient.getById(100L)).thenReturn(EventSnapshotDto.builder().id(100L).build());
        when(locationRepository.save(any(Location.class))).thenAnswer(invocation -> {
            Location location = invocation.getArgument(0);
            location.setId(1L);
            return location;
        });

        LocationDtoResponse dto = locationService.create(
                new NewLocationDto(100L, "Moscow", 55.7558f, 37.6173f));

        assertEquals(1L, dto.getId());
        assertEquals(100L, dto.getEventId());
    }

    @Test
    void patchUpdatesOnlyProvidedFields() {
        Location location = Location.builder()
                .id(1L)
                .eventId(100L)
                .name("Old")
                .lat(1f)
                .lon(1f)
                .build();
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.save(location)).thenReturn(location);

        LocationDtoResponse dto = locationService.patch(1L,
                new UpdateLocationRequest(null, "New", null, null));

        assertEquals("New", dto.getName());
        assertEquals(100L, dto.getEventId());
    }

    @Test
    void deleteRejectsUnknownLocation() {
        when(locationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(LocationNotFoundException.class, () -> locationService.delete(10L));
    }

    @Test
    void getEventsInLocationFiltersByRadius() {
        when(locationRepository.findAll()).thenReturn(List.of(
                location(100L, 55.7558f, 37.6173f),
                location(200L, 0f, 0f)));
        when(eventClient.getByIds(List.of(100L)))
                .thenReturn(List.of(EventShortDto.builder().id(100L).build()));

        List<EventShortDto> events = locationService.getEventsInLocation(
                new LocationSearchRequest(55.7558f, 37.6173f, 1f));

        assertEquals(1, events.size());
        assertEquals(100L, events.get(0).getId());
        verify(eventClient).getByIds(List.of(100L));
    }

    private static Location location(Long eventId, Float lat, Float lon) {
        return Location.builder().id(eventId).eventId(eventId).lat(lat).lon(lon).build();
    }
}
