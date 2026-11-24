package ru.practicum.ewm.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.common.client.UserClient;
import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.SubscriptionInputDto;
import ru.practicum.ewm.common.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.transaction.TransactionRunner;
import ru.practicum.ewm.subscription.exception.SubscriptionNotFoundException;
import ru.practicum.ewm.subscription.mapper.SubscriptionMapper;
import ru.practicum.ewm.subscription.model.Subscription;
import ru.practicum.ewm.subscription.model.SubscriptionStatus;
import ru.practicum.ewm.subscription.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;
    private final SubscriptionMapper mapper;

    private TransactionRunner transactionRunner = new TransactionRunner();

    @Autowired
    public void setTransactionRunner(TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Override
    public SubscriptionDto create(Long userId, SubscriptionInputDto dto) {
        UserDto follower = userClient.getById(userId);
        UserDto followed = userClient.getById(dto.getTargetUserId());
        if (userId.equals(dto.getTargetUserId())) {
            throw new ConflictException("Cannot subscribe to yourself");
        }

        Subscription subscription = transactionRunner.execute(() -> {
            if (subscriptionRepository.existsByFollowerIdAndFollowedId(userId, dto.getTargetUserId())) {
                throw new ConflictException("Subscription already exists");
            }
            return subscriptionRepository.save(Subscription.builder()
                    .followerId(userId)
                    .followedId(dto.getTargetUserId())
                    .status(SubscriptionStatus.PENDING)
                    .created(LocalDateTime.now())
                    .build());
        });
        log.debug("Subscription created: {} by user {} to {}", subscription.getId(), userId, dto.getTargetUserId());
        return mapper.toDto(subscription,
                Map.of(userId, toShort(follower), dto.getTargetUserId(), toShort(followed)));
    }

    @Override
    public SubscriptionStatusUpdateResult confirm(Long userId, Long subscriptionId) {
        return transactionRunner.execute(() -> updateStatus(userId, subscriptionId, SubscriptionStatus.CONFIRMED));
    }

    @Override
    public SubscriptionStatusUpdateResult reject(Long userId, Long subscriptionId) {
        return transactionRunner.execute(() -> updateStatus(userId, subscriptionId, SubscriptionStatus.REJECTED));
    }

    @Override
    public List<SubscriptionDto> getAllByUser(Long userId) {
        userClient.getById(userId);
        return toDtos(subscriptionRepository.findByFollowerId(userId));
    }

    @Override
    public List<UserShortDto> getFollowers(Long userId) {
        userClient.getById(userId);
        List<Long> followerIds = subscriptionRepository
                .findByFollowedIdAndStatus(userId, SubscriptionStatus.CONFIRMED).stream()
                .map(Subscription::getFollowerId)
                .collect(Collectors.toList());
        if (followerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userClient.getByIds(followerIds).stream()
                .map(SubscriptionServiceImpl::toShort)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionDto> getIncoming(Long userId) {
        userClient.getById(userId);
        return toDtos(subscriptionRepository.findByFollowedIdAndStatus(userId, SubscriptionStatus.PENDING));
    }

    @Override
    public List<SubscriptionDto> getOutgoing(Long userId) {
        userClient.getById(userId);
        return toDtos(subscriptionRepository.findByFollowerIdAndStatus(userId, SubscriptionStatus.PENDING));
    }

    @Override
    public void cancel(Long userId, Long subscriptionId) {
        transactionRunner.execute(() -> {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(SubscriptionNotFoundException::new);
            if (!subscription.getFollowerId().equals(userId)) {
                throw new SubscriptionNotFoundException();
            }
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);
        });
        log.debug("Subscription canceled: {}", subscriptionId);
    }

    @Override
    public boolean isSubscribed(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            return false;
        }
        return subscriptionRepository.existsByFollowerIdAndFollowedIdAndStatus(
                followerId, followedId, SubscriptionStatus.CONFIRMED);
    }

    @Override
    public List<Long> getFollowedIds(Long followerId, List<Long> followedIds) {
        if (followedIds.isEmpty()) {
            return List.of();
        }
        return subscriptionRepository.findFollowedIds(
                followerId, followedIds, SubscriptionStatus.CONFIRMED);
    }

    private SubscriptionStatusUpdateResult updateStatus(Long userId,
                                                        Long subscriptionId,
                                                        SubscriptionStatus status) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(SubscriptionNotFoundException::new);
        if (!subscription.getFollowedId().equals(userId)) {
            throw new SubscriptionNotFoundException();
        }
        if (subscription.getStatus() != SubscriptionStatus.PENDING) {
            throw new ConflictException("Subscription must be in PENDING status");
        }
        subscription.setStatus(status);
        subscriptionRepository.save(subscription);
        log.debug("Subscription {} set to {}", subscriptionId, status);
        return new SubscriptionStatusUpdateResult(subscriptionId, status.name());
    }

    private List<SubscriptionDto> toDtos(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, UserShortDto> users = getUsers(subscriptions);
        return subscriptions.stream()
                .map(subscription -> mapper.toDto(subscription, users))
                .collect(Collectors.toList());
    }

    private Map<Long, UserShortDto> getUsers(Collection<Subscription> subscriptions) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Subscription subscription : subscriptions) {
            ids.add(subscription.getFollowerId());
            ids.add(subscription.getFollowedId());
        }
        return userClient.getByIds(new ArrayList<>(ids)).stream()
                .collect(Collectors.toMap(UserDto::getId, SubscriptionServiceImpl::toShort));
    }

    private static UserShortDto toShort(UserDto user) {
        return UserShortDto.builder().id(user.getId()).name(user.getName()).build();
    }
}
