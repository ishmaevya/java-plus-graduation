package ru.practicum.ewm.compilation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.dto.CompilationDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.NewCompilationDto;
import ru.practicum.ewm.common.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.exception.CompilationNotFoundException;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.model.CompilationEvent;
import ru.practicum.ewm.compilation.repository.CompilationEventRepository;
import ru.practicum.ewm.compilation.repository.CompilationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private CompilationEventRepository compilationEventRepository;

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    @Test
    void createSavesCompilationAndResolvesEvents() {
        when(compilationRepository.save(any(Compilation.class))).thenAnswer(invocation -> {
            Compilation compilation = invocation.getArgument(0);
            compilation.setId(1L);
            return compilation;
        });
        when(eventClient.getByIds(List.of(100L, 200L))).thenReturn(List.of(event(100L), event(200L)));
        when(compilationEventRepository.findAllByCompilationIdOrderById(1L)).thenReturn(List.of(
                CompilationEvent.builder().compilationId(1L).eventId(100L).build(),
                CompilationEvent.builder().compilationId(1L).eventId(200L).build()));

        CompilationDto dto = compilationService.create(
                new NewCompilationDto(List.of(100L, 200L), true, "Title"));

        assertEquals(1L, dto.getId());
        assertEquals(2, dto.getEvents().size());
        verify(compilationEventRepository).saveAll(any());
    }

    @Test
    void patchReplacesEvents() {
        Compilation compilation = Compilation.builder().id(1L).title("Old").pinned(false).build();
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(compilation));
        when(eventClient.getByIds(List.of(300L))).thenReturn(List.of(event(300L)));
        when(compilationRepository.save(compilation)).thenReturn(compilation);
        when(compilationEventRepository.findAllByCompilationIdOrderById(1L)).thenReturn(List.of(
                CompilationEvent.builder().compilationId(1L).eventId(300L).build()));

        CompilationDto dto = compilationService.patch(1L,
                new UpdateCompilationRequest(List.of(300L), true, "New"));

        assertEquals("New", dto.getTitle());
        assertEquals(1, dto.getEvents().size());
        verify(compilationEventRepository).deleteAllByCompilationId(1L);
        verify(compilationEventRepository).saveAll(any());
    }

    @Test
    void getCompilationsReturnsEmptyListWhenNoCompilations() {
        when(compilationRepository.findWithOffset(null, PageRequest.of(0, 10))).thenReturn(List.of());

        assertEquals(List.of(), compilationService.getCompilations(null, 0, 10));
        verify(eventClient, never()).getByIds(any());
    }

    @Test
    void deleteRejectsUnknownCompilation() {
        when(compilationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(CompilationNotFoundException.class, () -> compilationService.delete(10L));
        verify(compilationEventRepository, never()).deleteAllByCompilationId(any());
    }

    private static EventShortDto event(Long id) {
        return EventShortDto.builder().id(id).title("Event " + id).build();
    }
}
