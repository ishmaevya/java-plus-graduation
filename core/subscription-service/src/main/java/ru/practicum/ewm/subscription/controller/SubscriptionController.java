package ru.practicum.ewm.subscription.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.SubscriptionInputDto;
import ru.practicum.ewm.common.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionDto create(@PathVariable Long userId,
                                  @Valid @RequestBody SubscriptionInputDto dto) {
        return subscriptionService.create(userId, dto);
    }

    @PatchMapping("/{subscriptionId}/confirm")
    public SubscriptionStatusUpdateResult confirm(@PathVariable Long userId, @PathVariable Long subscriptionId) {
        return subscriptionService.confirm(userId, subscriptionId);
    }

    @PatchMapping("/{subscriptionId}/reject")
    public SubscriptionStatusUpdateResult reject(@PathVariable Long userId, @PathVariable Long subscriptionId) {
        return subscriptionService.reject(userId, subscriptionId);
    }

    @GetMapping
    public List<SubscriptionDto> getAllByUser(@PathVariable Long userId) {
        return subscriptionService.getAllByUser(userId);
    }

    @GetMapping("/followers")
    public List<UserShortDto> getFollowers(@PathVariable Long userId) {
        return subscriptionService.getFollowers(userId);
    }

    @GetMapping("/incoming")
    public List<SubscriptionDto> getIncoming(@PathVariable Long userId) {
        return subscriptionService.getIncoming(userId);
    }

    @GetMapping("/outgoing")
    public List<SubscriptionDto> getOutgoing(@PathVariable Long userId) {
        return subscriptionService.getOutgoing(userId);
    }

    @DeleteMapping("/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long userId, @PathVariable Long subscriptionId) {
        subscriptionService.cancel(userId, subscriptionId);
    }
}
