package ru.practicum.ewm.user.service;

import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.user.dto.NewUserRequest;

import java.util.List;

public interface UserService {

    UserDto create(NewUserRequest request);

    List<UserDto> getAll(List<Long> ids, Integer from, Integer size);

    UserDto getById(Long userId);

    UserShortDto getShortById(Long userId);

    List<UserDto> getByIds(List<Long> ids);

    void delete(Long userId);
}
