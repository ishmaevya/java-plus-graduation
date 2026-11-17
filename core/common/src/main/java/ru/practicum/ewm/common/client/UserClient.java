package ru.practicum.ewm.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/internal/users/{userId}")
    UserDto getById(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users/{userId}/short")
    UserShortDto getShortById(@PathVariable("userId") Long userId);

    @PostMapping("/internal/users/batch")
    List<UserDto> getByIds(@RequestBody List<Long> ids);
}
