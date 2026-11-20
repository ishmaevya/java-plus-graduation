package ru.practicum.ewm.subscription.exception;

import ru.practicum.ewm.common.exception.NotFoundException;

public class SubscriptionNotFoundException extends NotFoundException {

    public SubscriptionNotFoundException() {
        super("Subscription not found");
    }
}
