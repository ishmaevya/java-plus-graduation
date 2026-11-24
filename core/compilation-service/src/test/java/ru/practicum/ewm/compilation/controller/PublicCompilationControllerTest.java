package ru.practicum.ewm.compilation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicCompilationControllerTest {

    private final CompilationService compilationService = mock(CompilationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicCompilationController(compilationService))
            .build();

    @Test
    void getByIdReturnsCompilation() throws Exception {
        when(compilationService.getById(1L))
                .thenReturn(CompilationDto.builder().id(1L).title("Title").events(List.of()).build());

        mvc.perform(get("/compilations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCompilationsPassesDefaults() throws Exception {
        when(compilationService.getCompilations(null, 0, 10)).thenReturn(List.of());

        mvc.perform(get("/compilations"))
                .andExpect(status().isOk());
    }
}
