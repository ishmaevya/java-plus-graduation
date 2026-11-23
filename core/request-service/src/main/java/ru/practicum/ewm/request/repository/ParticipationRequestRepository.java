package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.eventId, count(r) from ParticipationRequest r "
            + "where r.eventId in :eventIds and r.status = :status group by r.eventId")
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                            @Param("status") RequestStatus status);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);
}
