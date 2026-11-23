package ru.practicum.ewm.request.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    void countsConfirmedRequestsByEventIds() {
        requestRepository.save(request(10L, 21L, RequestStatus.CONFIRMED));
        requestRepository.save(request(10L, 22L, RequestStatus.CONFIRMED));
        requestRepository.save(request(10L, 23L, RequestStatus.PENDING));
        requestRepository.save(request(11L, 24L, RequestStatus.CONFIRMED));

        List<Object[]> result = requestRepository.countByEventIdsAndStatus(
                List.of(10L, 11L, 12L), RequestStatus.CONFIRMED);

        assertEquals(2, result.size());
        assertEquals(Map.of(10L, 2L, 11L, 1L), result.stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue())));
    }

    private ParticipationRequest request(Long eventId, Long requesterId, RequestStatus status) {
        return ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(requesterId)
                .status(status)
                .build();
    }
}
