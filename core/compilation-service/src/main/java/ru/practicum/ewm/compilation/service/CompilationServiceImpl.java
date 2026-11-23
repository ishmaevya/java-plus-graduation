package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.dto.CompilationDto;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.NewCompilationDto;
import ru.practicum.ewm.common.dto.UpdateCompilationRequest;
import ru.practicum.ewm.common.transaction.TransactionRunner;
import ru.practicum.ewm.compilation.exception.CompilationNotFoundException;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.model.CompilationEvent;
import ru.practicum.ewm.compilation.repository.CompilationEventRepository;
import ru.practicum.ewm.compilation.repository.CompilationRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationEventRepository compilationEventRepository;
    private final EventClient eventClient;

    private TransactionRunner transactionRunner = new TransactionRunner();

    @Autowired
    public void setTransactionRunner(TransactionRunner transactionRunner) {
        this.transactionRunner = transactionRunner;
    }

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        List<Long> eventIds = dto.getEvents() == null
                ? Collections.emptyList()
                : new ArrayList<>(new LinkedHashSet<>(dto.getEvents()));
        Map<Long, EventShortDto> events = resolveEvents(eventIds);

        Compilation compilation = transactionRunner.execute(() -> {
            Compilation saved = compilationRepository.save(Compilation.builder()
                    .title(dto.getTitle())
                    .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                    .build());
            saveCompilationEvents(saved.getId(), eventIds, events);
            return saved;
        });

        log.debug("Compilation created: {}", compilation.getId());
        return toDto(compilation, events);
    }

    @Override
    public CompilationDto patch(Long compId, UpdateCompilationRequest dto) {
        Compilation existing = findCompilation(compId);
        List<Long> eventIds = dto.getEvents() != null
                ? new ArrayList<>(new LinkedHashSet<>(dto.getEvents()))
                : getEventIds(compId);
        Map<Long, EventShortDto> events = resolveEvents(eventIds);

        Compilation compilation = transactionRunner.execute(() -> {
            if (dto.getEvents() != null) {
                compilationEventRepository.deleteAllByCompilationId(compId);
                saveCompilationEvents(compId, eventIds, events);
            }
            if (dto.getPinned() != null) {
                existing.setPinned(dto.getPinned());
            }
            if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
                existing.setTitle(dto.getTitle());
            }
            return compilationRepository.save(existing);
        });

        log.debug("Compilation patched: {}", compId);
        return toDto(compilation, events);
    }

    @Override
    public void delete(Long compId) {
        transactionRunner.execute(() -> {
            Compilation compilation = findCompilation(compId);
            compilationEventRepository.deleteAllByCompilationId(compId);
            compilationRepository.delete(compilation);
        });
        log.debug("Compilation deleted: {}", compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        List<Compilation> compilations = compilationRepository.findWithOffset(pinned, PageRequest.of(from / size, size));
        if (compilations.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> allEventIds = new LinkedHashSet<>();
        Map<Long, List<Long>> eventIdsByCompilation = new LinkedHashMap<>();
        for (Compilation compilation : compilations) {
            List<Long> eventIds = getEventIds(compilation.getId());
            eventIdsByCompilation.put(compilation.getId(), eventIds);
            allEventIds.addAll(eventIds);
        }
        Map<Long, EventShortDto> events = resolveEvents(new ArrayList<>(allEventIds));

        return compilations.stream()
                .map(compilation -> toDto(compilation,
                        eventIdsByCompilation.get(compilation.getId()), events))
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = findCompilation(compId);
        return toDto(compilation, resolveEvents(getEventIds(compId)));
    }

    private Compilation findCompilation(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new CompilationNotFoundException(compId));
    }

    private void saveCompilationEvents(Long compilationId, Collection<Long> eventIds,
                                       Map<Long, EventShortDto> resolvedEvents) {
        compilationEventRepository.saveAll(eventIds.stream()
                .map(resolvedEvents::get)
                .filter(event -> event != null)
                .map(event -> CompilationEvent.builder()
                        .compilationId(compilationId)
                        .eventId(event.getId())
                        .build())
                .collect(Collectors.toList()));
    }

    private List<Long> getEventIds(Long compilationId) {
        return compilationEventRepository.findAllByCompilationIdOrderById(compilationId).stream()
                .map(CompilationEvent::getEventId)
                .collect(Collectors.toList());
    }

    private Map<Long, EventShortDto> resolveEvents(Collection<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return eventClient.getByIds(new ArrayList<>(eventIds)).stream()
                .collect(Collectors.toMap(EventShortDto::getId, event -> event));
    }

    private CompilationDto toDto(Compilation compilation, Map<Long, EventShortDto> events) {
        return toDto(compilation, getEventIds(compilation.getId()), events);
    }

    private CompilationDto toDto(Compilation compilation,
                                 List<Long> eventIds,
                                 Map<Long, EventShortDto> events) {
        List<EventShortDto> compilationEvents = eventIds.stream()
                .map(events::get)
                .filter(event -> event != null)
                .collect(Collectors.toList());
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .events(compilationEvents)
                .build();
    }
}
