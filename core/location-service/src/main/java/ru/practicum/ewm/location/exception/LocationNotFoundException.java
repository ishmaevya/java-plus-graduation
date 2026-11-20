package ru.practicum.ewm.location.exception;

import ru.practicum.ewm.common.exception.NotFoundException;

public class LocationNotFoundException extends NotFoundException {

    public LocationNotFoundException(Long id) {
        super("Location with id=" + id + " was not found");
    }
}
