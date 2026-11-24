package ru.practicum.ewm.location.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.location.model.Location;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class LocationRepositoryTest {

    @Autowired
    private LocationRepository locationRepository;

    @Test
    void savesAndFindsLocations() {
        locationRepository.save(Location.builder()
                .eventId(100L)
                .name("Moscow")
                .lat(55.7558f)
                .lon(37.6173f)
                .build());

        List<Location> locations = locationRepository.findAllWithOffset(PageRequest.of(0, 10));
        assertEquals(1, locations.size());
        assertEquals(100L, locations.get(0).getEventId());
    }
}
