package ru.practicum.ewm.compilation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.CompilationDto;
import ru.practicum.ewm.common.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCompilationControllerTest {

    private final CompilationService compilationService = mock(CompilationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminCompilationController(compilationService))
            .build();

    @Test
    void createReturnsCreatedCompilation() throws Exception {
        when(compilationService.create(any(NewCompilationDto.class)))
                .thenReturn(CompilationDto.builder().id(1L).title("Title").pinned(false).events(List.of()).build());

        mvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Title\",\"pinned\":false,\"events\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/admin/compilations/1"))
                .andExpect(status().isNoContent());
    }
}
