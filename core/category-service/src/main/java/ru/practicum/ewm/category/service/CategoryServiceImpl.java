package ru.practicum.ewm.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.exception.CategoryAlreadyExistException;
import ru.practicum.ewm.category.exception.CategoryNotFoundException;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.exception.ValidationException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Field: name. Error: must not be blank. Value: null");
        }

        Category category = mapper.toCategory(dto);
        try {
            category = categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new CategoryAlreadyExistException(dto.getName());
        }

        log.debug("Category created: {}", category.getId());
        return mapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryDto dto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));

        if (dto.getName() != null) {
            category.setName(dto.getName());
        }

        try {
            category = categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            throw new CategoryAlreadyExistException(dto.getName());
        }

        log.debug("Category updated: {}", catId);
        return mapper.toCategoryDto(category);
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId));
        categoryRepository.delete(category);
        categoryRepository.flush();
        log.debug("Category deleted: {}", catId);
    }

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        validatePaging(from, size);
        return categoryRepository.findAll(PageRequest.of(from / size, size)).getContent().stream()
                .map(mapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long catId) {
        return mapper.toCategoryDto(categoryRepository.findById(catId)
                .orElseThrow(() -> new CategoryNotFoundException(catId)));
    }

    @Override
    public CategoryDto getByName(String name) {
        return mapper.toCategoryDto(categoryRepository.findByName(name)
                .orElseThrow(() -> new CategoryNotFoundException(name)));
    }

    @Override
    public List<CategoryDto> getByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ValidationException("Category ids must not be empty.");
        }
        Map<Long, Category> categories = categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        return ids.stream()
                .map(categories::get)
                .filter(Objects::nonNull)
                .map(mapper::toCategoryDto)
                .toList();
    }

    private void validatePaging(Integer from, Integer size) {
        if (from == null || from < 0 || size == null || size <= 0) {
            throw new ValidationException("from must be >= 0 and size must be > 0.");
        }
    }
}
