package ru.practicum.ewm.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.exception.CategoryNotFoundException;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.common.dto.CategoryDto;
import ru.practicum.ewm.common.handler.ErrorHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryAdminControllerTest {

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryAdminController(categoryService))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void createsCategory() throws Exception {
        NewCategoryDto request = NewCategoryDto.builder().name("Concerts").build();
        when(categoryService.create(any(NewCategoryDto.class))).thenReturn(category());

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Concerts"));
    }

    @Test
    void updatesCategory() throws Exception {
        when(categoryService.update(eq(1L), any(CategoryDto.class))).thenReturn(category());

        mockMvc.perform(patch("/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CategoryDto.builder().name("Concerts").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Concerts"));
    }

    @Test
    void deletesCategory() throws Exception {
        mockMvc.perform(delete("/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundForMissingCategory() throws Exception {
        doThrow(new CategoryNotFoundException(42L)).when(categoryService).delete(42L);

        mockMvc.perform(delete("/admin/categories/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    private CategoryDto category() {
        return CategoryDto.builder()
                .id(1L)
                .name("Concerts")
                .build();
    }
}
