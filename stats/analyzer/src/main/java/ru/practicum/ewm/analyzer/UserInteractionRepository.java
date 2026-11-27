package ru.practicum.ewm.analyzer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    Optional<UserInteraction> findByUserIdAndEventId(long userId, long eventId);

    List<UserInteraction> findByUserId(long userId);

    List<UserInteraction> findByUserIdOrderByTimestampDesc(long userId, Pageable pageable);

    @Query("select i.eventId from UserInteraction i where i.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") long userId);

    boolean existsByUserIdAndEventId(long userId, long eventId);

    boolean existsByUserIdAndEventIdAndViewedTrue(long userId, long eventId);

    @Query("select i.eventId as eventId, sum(i.weight) as totalWeight "
            + "from UserInteraction i where i.eventId in :eventIds group by i.eventId")
    List<InteractionSum> sumWeightsByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
