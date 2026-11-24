package ru.practicum.ewm.compilation.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.model.CompilationEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class CompilationRepositoryTest {

    @Autowired
    private CompilationRepository compilationRepository;

    @Autowired
    private CompilationEventRepository compilationEventRepository;

    @Test
    void savesCompilationWithEvents() {
        Compilation compilation = compilationRepository.save(Compilation.builder()
                .title("Compilation")
                .pinned(true)
                .build());
        compilationEventRepository.saveAll(List.of(
                CompilationEvent.builder().compilationId(compilation.getId()).eventId(100L).build(),
                CompilationEvent.builder().compilationId(compilation.getId()).eventId(200L).build()));

        assertEquals(1, compilationRepository.findWithOffset(true, PageRequest.of(0, 10)).size());
        assertEquals(2, compilationEventRepository.findAllByCompilationIdOrderById(compilation.getId()).size());
    }
}
