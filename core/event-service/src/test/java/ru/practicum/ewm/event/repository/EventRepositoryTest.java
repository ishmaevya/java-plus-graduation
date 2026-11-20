package ru.practicum.ewm.event.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.Location;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    void savesAndFindsPublishedEvent() {
        Event saved = eventRepository.save(Event.builder()
                .annotation("Concert with a long annotation")
                .categoryId(10L)
                .createdOn(LocalDateTime.now())
                .description("Concert description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .initiatorId(1L)
                .location(new Location(1.0F, 2.0F))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("Concert")
                .build());

        assertTrue(eventRepository.findByIdAndState(saved.getId(), EventState.PUBLISHED).isPresent());
        assertEquals(1, eventRepository.findByInitiatorId(1L, org.springframework.data.domain.PageRequest.of(0, 10))
                .getTotalElements());
    }
}
