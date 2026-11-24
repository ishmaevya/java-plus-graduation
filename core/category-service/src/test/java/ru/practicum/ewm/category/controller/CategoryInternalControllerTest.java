package ru.practicum.ewm.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.handler.ErrorHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryInternalControllerTest {

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryInternalController(categoryService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void returnsCategoryById() throws Exception {
        when(categoryService.getById(1L)).thenReturn(category());

        mockMvc.perform(get("/internal/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Concerts"));
    }

    @Test
    void returnsCategoryByName() throws Exception {
        when(categoryService.getByName("Concerts")).thenReturn(category());

        mockMvc.perform(get("/internal/categories/by-name").param("name", "Concerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void returnsCategoriesByIds() throws Exception {
        when(categoryService.getByIds(anyList())).thenReturn(List.of(category()));

        mockMvc.perform(post("/internal/categories/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    private CategoryDto category() {
        return CategoryDto.builder()
                .id(1L)
                .name("Concerts")
                .build();
    }
}
