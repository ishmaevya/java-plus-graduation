package ru.practicum.ewm.request.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ParticipationRequestRepositoryTest {

    @Autowired
    private ParticipationRequestRepository requestRepository;

    @Test
    void savesAndFindsRequests() {
        ParticipationRequest saved = requestRepository.save(ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(10L)
                .requesterId(20L)
                .status(RequestStatus.CONFIRMED)
                .build());

        assertTrue(requestRepository.existsByRequesterIdAndEventId(20L, 10L));
        assertEquals(1, requestRepository.findByEventId(10L).size());
        assertEquals(1, requestRepository.findByRequesterId(20L).size());
        assertEquals(1, requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED));
        assertEquals(saved.getId(), requestRepository.findById(saved.getId()).orElseThrow().getId());
        assertFalse(requestRepository.existsByRequesterIdAndEventId(20L, 11L));
    }
}
