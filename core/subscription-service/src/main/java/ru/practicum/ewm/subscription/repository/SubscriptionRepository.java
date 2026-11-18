package ru.practicum.ewm.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
