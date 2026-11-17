package ru.practicum.ewm.common.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends ApiException {

    static final String REASON = "The requested service is temporarily unavailable.";

    public ServiceUnavailableException(String message) {
        super(REASON, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
