package ru.practicum.ewm.user.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUser() {
        User saved = userRepository.save(User.builder()
                .email("user@example.com")
                .name("User")
                .build());

        assertTrue(userRepository.existsByEmail("user@example.com"));
        assertEquals(saved.getId(), userRepository.findById(saved.getId()).orElseThrow().getId());
    }

    @Test
    void findsUsersByIdsWithPaging() {
        User first = userRepository.save(User.builder()
                .email("first@example.com")
                .name("First")
                .build());
        User second = userRepository.save(User.builder()
                .email("second@example.com")
                .name("Second")
                .build());

        List<User> users = userRepository.findByIdIn(List.of(first.getId(), second.getId()), PageRequest.of(0, 1))
                .getContent();

        assertEquals(1, users.size());
        assertFalse(users.isEmpty());
    }
}
