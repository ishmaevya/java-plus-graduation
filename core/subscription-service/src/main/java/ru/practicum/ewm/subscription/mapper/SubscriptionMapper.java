package ru.practicum.ewm.subscription.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.subscription.model.Subscription;

import java.util.Map;

@Component
public class SubscriptionMapper {

    public SubscriptionDto toDto(Subscription subscription, Map<Long, UserShortDto> users) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .follower(users.get(subscription.getFollowerId()))
                .followed(users.get(subscription.getFollowedId()))
                .status(subscription.getStatus().name())
                .created(subscription.getCreated().toString())
                .build();
    }
}
