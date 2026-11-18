package ru.practicum.ewm.subscription.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.subscription.service.SubscriptionService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
}
