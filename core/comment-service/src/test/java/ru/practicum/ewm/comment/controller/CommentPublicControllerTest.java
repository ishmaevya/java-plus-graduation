package ru.practicum.ewm.comment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.comment.service.CommentService;
import ru.practicum.ewm.common.dto.CommentDto;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentPublicControllerTest {

    private final CommentService commentService = mock(CommentService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CommentPublicController(commentService)).build();

    @Test
    void getOneReturnsComment() throws Exception {
        when(commentService.getOne(100L, 10L)).thenReturn(CommentDto.builder()
                .id(10L)
                .text("hello")
                .authorId(1L)
                .authorName("User")
                .eventId(100L)
                .created(LocalDateTime.now())
                .build());

        mvc.perform(get("/events/100/comments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }
}
