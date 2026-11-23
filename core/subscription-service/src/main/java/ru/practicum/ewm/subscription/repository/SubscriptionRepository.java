package ru.practicum.ewm.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByFollowerIdAndFollowedId(Long followerId, Long followedId);

    List<Subscription> findByFollowerId(Long followerId);

    List<Subscription> findByFollowerIdAndStatus(Long followerId, SubscriptionStatus status);

    List<Subscription> findByFollowedIdAndStatus(Long followedId, SubscriptionStatus status);

    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);

    boolean existsByFollowerIdAndFollowedIdAndStatus(Long followerId, Long followedId, SubscriptionStatus status);

    @Query("select s.followedId from Subscription s "
            + "where s.followerId = :followerId and s.followedId in :followedIds and s.status = :status")
    List<Long> findFollowedIds(@Param("followerId") Long followerId,
                               @Param("followedIds") List<Long> followedIds,
                               @Param("status") SubscriptionStatus status);
}
