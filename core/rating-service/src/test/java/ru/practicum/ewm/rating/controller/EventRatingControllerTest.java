package ru.practicum.ewm.rating.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.rating.model.RatingType;
import ru.practicum.ewm.rating.service.RatingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventRatingControllerTest {

    private final RatingService ratingService = mock(RatingService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new EventRatingController(ratingService)).build();

    @Test
    void likeReturnsNoContent() throws Exception {
        mvc.perform(put("/users/1/events/100/like"))
                .andExpect(status().isNoContent());

        verify(ratingService).setRating(1L, 100L, RatingType.LIKE);
    }

    @Test
    void dislikeReturnsNoContent() throws Exception {
        mvc.perform(put("/users/1/events/100/dislike"))
                .andExpect(status().isNoContent());

        verify(ratingService).setRating(1L, 100L, RatingType.DISLIKE);
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/users/1/events/100/rating"))
                .andExpect(status().isNoContent());

        verify(ratingService).deleteRating(1L, 100L);
    }
}
