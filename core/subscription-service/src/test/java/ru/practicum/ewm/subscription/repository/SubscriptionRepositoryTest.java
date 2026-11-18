package ru.practicum.ewm.subscription.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void savesAndFindsSubscriptions() {
        subscriptionRepository.save(Subscription.builder()
                .followerId(1L)
                .followedId(2L)
                .status(SubscriptionStatus.CONFIRMED)
                .created(LocalDateTime.now())
                .build());

        assertTrue(subscriptionRepository.existsByFollowerIdAndFollowedId(1L, 2L));
        assertTrue(subscriptionRepository.existsByFollowerIdAndFollowedIdAndStatus(
                1L, 2L, SubscriptionStatus.CONFIRMED));
        assertEquals(1, subscriptionRepository.findByFollowedIdAndStatus(2L, SubscriptionStatus.CONFIRMED).size());
        assertEquals(1, subscriptionRepository.findByFollowerId(1L).size());
    }
}
