package ru.practicum.ewm.analyzer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(long eventA, long eventB);

    List<EventSimilarity> findByEventAOrEventB(long eventA, long eventB);

    List<EventSimilarity> findByEventAInOrEventBIn(Collection<Long> eventAs, Collection<Long> eventBs);
}
