package ru.practicum.ewm.common.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.NotFoundException;
import ru.practicum.ewm.common.exception.ServiceUnavailableException;
import ru.practicum.ewm.common.exception.ValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = readMessage(response, methodKey);
        return switch (response.status()) {
            case 400 -> new ValidationException(message);
            case 404 -> new NotFoundException(message);
            case 409 -> new ConflictException(message);
            case 503 -> new ServiceUnavailableException(message);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    private String readMessage(Response response, String methodKey) {
        if (response.body() == null) {
            return "Remote service call failed: " + methodKey;
        }
        try (InputStream stream = response.body().asInputStream()) {
            String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return body.isBlank() ? "Remote service call failed: " + methodKey : body;
        } catch (IOException e) {
            return "Remote service call failed: " + methodKey + " (" + HttpStatus.valueOf(response.status()) + ")";
        }
    }
}
