package ru.practicum.ewm.subscription.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.SubscriptionInputDto;
import ru.practicum.ewm.common.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.subscription.exception.SubscriptionNotFoundException;
import ru.practicum.ewm.subscription.mapper.SubscriptionMapper;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;
import ru.practicum.ewm.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserClient userClient;

    @Spy
    private SubscriptionMapper mapper = new SubscriptionMapper();

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    void createSavesPendingSubscription() {
        when(userClient.getById(1L)).thenReturn(user(1L, "One"));
        when(userClient.getById(2L)).thenReturn(user(2L, "Two"));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0);
            subscription.setId(10L);
            return subscription;
        });

        SubscriptionDto dto = subscriptionService.create(1L, new SubscriptionInputDto(2L));

        assertEquals(10L, dto.getId());
        assertEquals("PENDING", dto.getStatus());
        assertEquals("One", dto.getFollower().getName());
        assertEquals("Two", dto.getFollowed().getName());
    }

    @Test
    void createRejectsSelfSubscription() {
        when(userClient.getById(1L)).thenReturn(user(1L, "One"));
        when(userClient.getById(1L)).thenReturn(user(1L, "One"));

        assertThrows(ConflictException.class,
                () -> subscriptionService.create(1L, new SubscriptionInputDto(1L)));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void confirmUpdatesPendingSubscription() {
        when(subscriptionRepository.findById(10L))
                .thenReturn(Optional.of(subscription(10L, 1L, 2L, SubscriptionStatus.PENDING)));

        SubscriptionStatusUpdateResult result = subscriptionService.confirm(2L, 10L);

        assertEquals(10L, result.getId());
        assertEquals("CONFIRMED", result.getStatus());
    }

    @Test
    void confirmRejectsWrongFollowedUser() {
        when(subscriptionRepository.findById(10L))
                .thenReturn(Optional.of(subscription(10L, 1L, 2L, SubscriptionStatus.PENDING)));

        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.confirm(3L, 10L));
    }

    @Test
    void cancelSetsCanceledStatus() {
        when(subscriptionRepository.findById(10L))
                .thenReturn(Optional.of(subscription(10L, 1L, 2L, SubscriptionStatus.CONFIRMED)));

        subscriptionService.cancel(1L, 10L);

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void getFollowersReturnsConfirmedFollowers() {
        when(userClient.getById(2L)).thenReturn(user(2L, "Two"));
        when(subscriptionRepository.findByFollowedIdAndStatus(2L, SubscriptionStatus.CONFIRMED))
                .thenReturn(List.of(subscription(10L, 1L, 2L, SubscriptionStatus.CONFIRMED)));
        when(userClient.getByIds(List.of(1L))).thenReturn(List.of(user(1L, "One")));

        List<UserShortDto> followers = subscriptionService.getFollowers(2L);

        assertEquals(1, followers.size());
        assertEquals("One", followers.get(0).getName());
    }

    @Test
    void isSubscribedChecksConfirmedStatus() {
        when(subscriptionRepository.existsByFollowerIdAndFollowedIdAndStatus(
                1L, 2L, SubscriptionStatus.CONFIRMED)).thenReturn(true);

        assertTrue(subscriptionService.isSubscribed(1L, 2L));
        assertFalse(subscriptionService.isSubscribed(1L, 1L));
    }

    @Test
    void getFollowedIdsReturnsConfirmedSubscriptions() {
        when(subscriptionRepository.findFollowedIds(
                1L, List.of(2L, 3L), SubscriptionStatus.CONFIRMED)).thenReturn(List.of(2L));

        assertEquals(List.of(2L), subscriptionService.getFollowedIds(1L, List.of(2L, 3L)));
    }

    private static UserDto user(Long id, String name) {
        return UserDto.builder().id(id).name(name).build();
    }

    private static Subscription subscription(Long id, Long followerId, Long followedId, SubscriptionStatus status) {
        return Subscription.builder()
                .id(id)
                .followerId(followerId)
                .followedId(followedId)
                .status(status)
                .created(LocalDateTime.now())
                .build();
    }
}
