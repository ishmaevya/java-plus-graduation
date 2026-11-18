package ru.practicum.ewm.event.exception;

import ru.practicum.ewm.common.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {

    public CategoryNotFoundException(Long id) {
        super("Category with ID = %d not found".formatted(id));
    }
}
