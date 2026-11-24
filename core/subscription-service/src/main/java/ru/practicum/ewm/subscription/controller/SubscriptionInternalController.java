package ru.practicum.ewm.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class SubscriptionInternalController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/{followerId}/followed/{followedId}")
    public boolean isSubscribed(@PathVariable("followerId") Long followerId,
                                @PathVariable("followedId") Long followedId) {
        return subscriptionService.isSubscribed(followerId, followedId);
    }

    @PostMapping("/{followerId}/followed")
    public List<Long> getFollowed(@PathVariable("followerId") Long followerId,
                                  @RequestBody List<Long> followedIds) {
        return subscriptionService.getFollowedIds(followerId, followedIds);
    }
}
