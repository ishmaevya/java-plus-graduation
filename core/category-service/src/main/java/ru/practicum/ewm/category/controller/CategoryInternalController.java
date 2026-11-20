package ru.practicum.ewm.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.common.dto.CategoryDto;

import java.util.List;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class CategoryInternalController {

    private final CategoryService categoryService;

    @GetMapping("/{categoryId}")
    public CategoryDto getById(@PathVariable Long categoryId) {
        return categoryService.getById(categoryId);
    }

    @GetMapping("/by-name")
    public CategoryDto getByName(@RequestParam String name) {
        return categoryService.getByName(name);
    }

    @PostMapping("/batch")
    public List<CategoryDto> getByIds(@RequestBody List<Long> ids) {
        return categoryService.getByIds(ids);
    }
}
