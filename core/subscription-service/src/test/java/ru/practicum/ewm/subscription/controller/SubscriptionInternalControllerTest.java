package ru.practicum.ewm.subscription.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionInternalControllerTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SubscriptionInternalController(subscriptionService))
            .build();

    @Test
    void isSubscribedReturnsBoolean() throws Exception {
        when(subscriptionService.isSubscribed(1L, 2L)).thenReturn(true);

        mvc.perform(get("/internal/subscriptions/1/followed/2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getFollowedReturnsSubscribedUserIds() throws Exception {
        when(subscriptionService.getFollowedIds(1L, List.of(2L, 3L))).thenReturn(List.of(2L));

        mvc.perform(post("/internal/subscriptions/1/followed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[2,3]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2));
    }
}
