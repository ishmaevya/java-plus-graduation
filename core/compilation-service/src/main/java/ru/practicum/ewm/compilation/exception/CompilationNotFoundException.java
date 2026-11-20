package ru.practicum.ewm.compilation.exception;

import ru.practicum.ewm.common.exception.NotFoundException;

public class CompilationNotFoundException extends NotFoundException {

    public CompilationNotFoundException(Long id) {
        super("Compilation with id %d not found".formatted(id));
    }
}
