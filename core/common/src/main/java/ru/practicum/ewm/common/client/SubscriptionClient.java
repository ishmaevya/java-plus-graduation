package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "subscription-service")
public interface SubscriptionClient {

    @GetMapping("/internal/subscriptions/{followerId}/followed/{followedId}")
    Boolean isSubscribed(@PathVariable("followerId") Long followerId,
                         @PathVariable("followedId") Long followedId);

    @PostMapping("/internal/subscriptions/{followerId}/followed")
    List<Long> getFollowed(@PathVariable("followerId") Long followerId,
                           @RequestBody List<Long> followedIds);
}
