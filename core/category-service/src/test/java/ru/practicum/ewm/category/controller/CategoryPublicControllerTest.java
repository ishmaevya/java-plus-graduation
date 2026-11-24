package ru.practicum.ewm.category.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.handler.ErrorHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryPublicControllerTest {

    private final CategoryService categoryService = mock(CategoryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryPublicController(categoryService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsCategories() throws Exception {
        when(categoryService.getAll(anyInt(), anyInt())).thenReturn(List.of(category()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Concerts"));
    }

    @Test
    void returnsCategoryById() throws Exception {
        when(categoryService.getById(1L)).thenReturn(category());

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private CategoryDto category() {
        return CategoryDto.builder()
                .id(1L)
                .name("Concerts")
                .build();
    }
}
