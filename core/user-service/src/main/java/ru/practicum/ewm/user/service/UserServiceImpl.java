package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.exception.UserNotFoundException;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    @Transactional
    public UserDto create(NewUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email " + request.getEmail() + " already exists");
        }
        User user = userRepository.save(mapper.toUser(request));
        log.debug("User created: {}", user.getId());
        return mapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getAll(List<Long> ids, Integer from, Integer size) {
        validatePaging(from, size);
        PageRequest page = PageRequest.of(from / size, size);
        List<User> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findByIdIn(ids, page).getContent();
        } else {
            users = userRepository.findAll(page).getContent();
        }
        log.debug("Users found: {}", users.size());
        return users.stream()
                .map(mapper::toUserDto)
                .toList();
    }

    @Override
    public UserDto getById(Long userId) {
        return mapper.toUserDto(findUser(userId));
    }

    @Override
    public UserShortDto getShortById(Long userId) {
        return mapper.toUserShortDto(findUser(userId));
    }

    @Override
    public List<UserDto> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ValidationException("User ids must not be empty.");
        }
        Map<Long, User> users = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return ids.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .map(mapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        User user = findUser(userId);
        userRepository.delete(user);
        log.debug("User deleted: {}", userId);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void validatePaging(Integer from, Integer size) {
        if (from == null || from < 0 || size == null || size <= 0) {
            throw new ValidationException("from must be >= 0 and size must be > 0.");
        }
    }
}
