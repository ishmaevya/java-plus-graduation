package ru.practicum.ewm.comment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.comment.service.CommentService;
import ru.practicum.ewm.common.dto.CommentDto;
import ru.practicum.ewm.common.dto.NewCommentDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentPrivateControllerTest {

    private final CommentService commentService = mock(CommentService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CommentPrivateController(commentService)).build();

    @Test
    void createReturnsCreatedComment() throws Exception {
        when(commentService.create(eq(1L), eq(100L), any(NewCommentDto.class))).thenReturn(comment());

        mvc.perform(post("/users/1/events/100/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("hello"));
    }

    @Test
    void getByUserReturnsComments() throws Exception {
        when(commentService.getByUser(1L, 0, 10)).thenReturn(List.of(comment()));

        mvc.perform(get("/users/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value(1));

        verify(commentService).getByUser(1L, 0, 10);
    }

    private static CommentDto comment() {
        return CommentDto.builder()
                .id(10L)
                .text("hello")
                .authorId(1L)
                .authorName("User")
                .eventId(100L)
                .created(LocalDateTime.now())
                .build();
    }
}
