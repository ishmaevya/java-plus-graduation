package ru.practicum.ewm.subscription.service;

import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.SubscriptionInputDto;
import ru.practicum.ewm.common.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.common.dto.UserShortDto;

import java.util.List;

public interface SubscriptionService {

    SubscriptionDto create(Long userId, SubscriptionInputDto dto);

    SubscriptionStatusUpdateResult confirm(Long userId, Long subscriptionId);

    SubscriptionStatusUpdateResult reject(Long userId, Long subscriptionId);

    List<SubscriptionDto> getAllByUser(Long userId);

    List<UserShortDto> getFollowers(Long userId);

    List<SubscriptionDto> getIncoming(Long userId);

    List<SubscriptionDto> getOutgoing(Long userId);

    void cancel(Long userId, Long subscriptionId);

    boolean isSubscribed(Long followerId, Long followedId);
}
