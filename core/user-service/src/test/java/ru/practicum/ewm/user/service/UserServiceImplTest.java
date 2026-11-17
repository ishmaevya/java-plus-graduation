package ru.practicum.ewm.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.common.dto.UserDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.ValidationException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.exception.UserNotFoundException;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserMapper mapper = new UserMapper();

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void createSavesUser() {
        NewUserRequest request = NewUserRequest.builder()
                .email("user@example.com")
                .name("User")
                .build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserDto result = service.create(request);

        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("User", result.getName());
    }

    @Test
    void createThrowsConflictForDuplicateEmail() {
        NewUserRequest request = NewUserRequest.builder()
                .email("user@example.com")
                .name("User")
                .build();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request));
    }

    @Test
    void getAllReturnsUsers() {
        User user = user(1L, "user@example.com", "User");
        when(userRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(user)));

        List<UserDto> result = service.getAll(null, 0, 10);

        assertEquals(List.of(1L), result.stream().map(UserDto::getId).toList());
    }

    @Test
    void getAllRejectsInvalidPaging() {
        assertThrows(ValidationException.class, () -> service.getAll(null, -1, 10));
        assertThrows(ValidationException.class, () -> service.getAll(null, 0, 0));
    }

    @Test
    void getByIdThrowsWhenUserMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getById(42L));
    }

    @Test
    void getByIdsKeepsRequestedOrderAndSkipsMissing() {
        User first = user(1L, "first@example.com", "First");
        User third = user(3L, "third@example.com", "Third");
        when(userRepository.findAllById(List.of(2L, 1L, 3L))).thenReturn(List.of(first, third));

        List<UserDto> result = service.getByIds(List.of(2L, 1L, 3L));

        assertEquals(List.of(1L, 3L), result.stream().map(UserDto::getId).toList());
    }

    @Test
    void getByIdsRejectsEmptyIds() {
        assertThrows(ValidationException.class, () -> service.getByIds(List.of()));
    }

    @Test
    void deleteRemovesExistingUser() {
        User user = user(1L, "user@example.com", "User");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        service.delete(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteThrowsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.delete(1L));
    }

    @Test
    void getShortByIdReturnsIdAndName() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, "user@example.com", "User")));

        assertEquals("User", service.getShortById(1L).getName());
        assertTrue(service.getShortById(1L).getId().equals(1L));
    }

    private User user(Long id, String email, String name) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .build();
    }
}
