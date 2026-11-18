package ru.practicum.ewm.rating.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.RatingStatsDto;
import ru.practicum.ewm.rating.service.RatingService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingInternalControllerTest {

    private final RatingService ratingService = mock(RatingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RatingInternalController(ratingService)).build();

    @Test
    void getStatsReturnsStats() throws Exception {
        when(ratingService.getStats(100L)).thenReturn(new RatingStatsDto(5L, 2L));

        mvc.perform(get("/internal/ratings/events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(5))
                .andExpect(jsonPath("$.dislikes").value(2))
                .andExpect(jsonPath("$.rating").value(3));
    }

    @Test
    void getStatsBatchReturnsMap() throws Exception {
        when(ratingService.getStats(List.of(100L, 200L)))
                .thenReturn(Map.of(100L, new RatingStatsDto(1L, 0L)));

        mvc.perform(post("/internal/ratings/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[100,200]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['100'].likes").value(1));

        verify(ratingService).getStats(List.of(100L, 200L));
    }
}
