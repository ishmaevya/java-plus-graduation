package ru.practicum.ewm.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.exception.CategoryAlreadyExistException;
import ru.practicum.ewm.category.exception.CategoryNotFoundException;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.client.EventClient;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.exception.ConflictException;
import ru.practicum.ewm.common.exception.ValidationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventClient eventClient;

    @Spy
    private CategoryMapper mapper = new CategoryMapper();

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void createSavesCategory() {
        NewCategoryDto request = NewCategoryDto.builder().name("Concerts").build();
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryDto result = service.create(request);

        assertEquals(1L, result.getId());
        assertEquals("Concerts", result.getName());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(ValidationException.class,
                () -> service.create(NewCategoryDto.builder().name(" ").build()));
    }

    @Test
    void createThrowsConflictForDuplicateName() {
        NewCategoryDto request = NewCategoryDto.builder().name("Concerts").build();
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(CategoryAlreadyExistException.class, () -> service.create(request));
    }

    @Test
    void updateChangesName() {
        Category category = category(1L, "Old");
        CategoryDto request = CategoryDto.builder().name("New").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.saveAndFlush(category)).thenReturn(category);

        CategoryDto result = service.update(1L, request);

        assertEquals("New", result.getName());
    }

    @Test
    void deleteThrowsWhenCategoryMissing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.delete(1L));
    }

    @Test
    void getAllRejectsInvalidPaging() {
        assertThrows(ValidationException.class, () -> service.getAll(-1, 10));
        assertThrows(ValidationException.class, () -> service.getAll(0, 0));
    }

    @Test
    void getAllReturnsCategories() {
        when(categoryRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(category(1L, "Concerts"))));

        List<CategoryDto> result = service.getAll(0, 10);

        assertEquals(List.of("Concerts"), result.stream().map(CategoryDto::getName).toList());
    }

    @Test
    void getByIdThrowsWhenCategoryMissing() {
        when(categoryRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.getById(42L));
    }

    @Test
    void getByNameThrowsWhenCategoryMissing() {
        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.getByName("Unknown"));
    }

    @Test
    void getByIdsKeepsRequestedOrderAndSkipsMissing() {
        Category first = category(1L, "First");
        Category third = category(3L, "Third");
        when(categoryRepository.findAllById(List.of(2L, 1L, 3L))).thenReturn(List.of(first, third));

        List<CategoryDto> result = service.getByIds(List.of(2L, 1L, 3L));

        assertEquals(List.of(1L, 3L), result.stream().map(CategoryDto::getId).toList());
    }

    @Test
    void getByIdsRejectsEmptyIds() {
        assertThrows(ValidationException.class, () -> service.getByIds(List.of()));
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category category = category(1L, "Concerts");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventClient.countByCategoryId(1L)).thenReturn(0L);

        service.delete(1L);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteThrowsConflictWhenCategoryHasEvents() {
        Category category = category(1L, "Concerts");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventClient.countByCategoryId(1L)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.delete(1L));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    private Category category(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .build();
    }
}
