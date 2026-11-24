package ru.practicum.ewm.location.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.EventShortDto;
import ru.practicum.ewm.common.dto.LocationSearchRequest;
import ru.practicum.ewm.location.service.LocationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicLocationControllerTest {

    private final LocationService locationService = mock(LocationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicLocationController(locationService)).build();

    @Test
    void getEventsInLocationReturnsEvents() throws Exception {
        when(locationService.getEventsInLocation(any(LocationSearchRequest.class)))
                .thenReturn(List.of(EventShortDto.builder().id(100L).title("Event").build()));

        mvc.perform(get("/locations")
                        .param("lat", "55.7558")
                        .param("lon", "37.6173")
                        .param("radius", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }
}
