package ru.practicum.ewm.comment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentInternalControllerTest {

    private final CommentService commentService = mock(CommentService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CommentInternalController(commentService)).build();

    @Test
    void getCountReturnsCount() throws Exception {
        when(commentService.getCount(100L)).thenReturn(3L);

        mvc.perform(get("/internal/comments/events/100/count"))
                .andExpect(status().isOk());
    }

    @Test
    void getCountsReturnsMap() throws Exception {
        when(commentService.getCounts(List.of(100L, 200L))).thenReturn(Map.of(100L, 2L));

        mvc.perform(post("/internal/comments/events/counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[100,200]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['100']").value(2));

        verify(commentService).getCounts(List.of(100L, 200L));
    }
}
