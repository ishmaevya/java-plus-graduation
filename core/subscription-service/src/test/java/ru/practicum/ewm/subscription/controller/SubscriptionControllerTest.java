package ru.practicum.ewm.subscription.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.SubscriptionDto;
import ru.practicum.ewm.common.dto.SubscriptionInputDto;
import ru.practicum.ewm.common.dto.SubscriptionStatusUpdateResult;
import ru.practicum.ewm.common.dto.UserShortDto;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionControllerTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SubscriptionController(subscriptionService)).build();

    @Test
    void createReturnsCreatedSubscription() throws Exception {
        when(subscriptionService.create(eq(1L), any(SubscriptionInputDto.class)))
                .thenReturn(SubscriptionDto.builder()
                        .id(10L)
                        .follower(UserShortDto.builder().id(1L).name("One").build())
                        .followed(UserShortDto.builder().id(2L).name("Two").build())
                        .status("PENDING")
                        .created("2026-11-18T12:00:00")
                        .build());

        mvc.perform(post("/users/1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void confirmReturnsResult() throws Exception {
        when(subscriptionService.confirm(2L, 10L))
                .thenReturn(new SubscriptionStatusUpdateResult(10L, "CONFIRMED"));

        mvc.perform(patch("/users/2/subscriptions/10/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void getFollowersReturnsList() throws Exception {
        when(subscriptionService.getFollowers(2L))
                .thenReturn(java.util.List.of(UserShortDto.builder().id(1L).name("One").build()));

        mvc.perform(get("/users/2/subscriptions/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
