package ru.practicum.ewm.compilation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.compilation.model.CompilationEvent;

import java.util.List;

public interface CompilationEventRepository extends JpaRepository<CompilationEvent, Long> {

    List<CompilationEvent> findAllByCompilationIdOrderById(Long compilationId);

    void deleteAllByCompilationId(Long compilationId);
}
