package ru.practicum.ewm.location.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.practicum.ewm.common.dto.LocationDtoResponse;
import ru.practicum.ewm.common.dto.NewLocationDto;
import ru.practicum.ewm.location.service.LocationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminLocationControllerTest {

    private final LocationService locationService = mock(LocationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminLocationController(locationService)).build();

    @Test
    void createReturnsCreatedLocation() throws Exception {
        when(locationService.create(any(NewLocationDto.class)))
                .thenReturn(LocationDtoResponse.builder()
                        .id(1L)
                        .eventId(100L)
                        .name("Moscow")
                        .lat(55.7558f)
                        .lon(37.6173f)
                        .build());

        mvc.perform(post("/admin/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventId\":100,\"name\":\"Moscow\",\"lat\":55.7558,\"lon\":37.6173}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(100));
    }

    @Test
    void getByIdReturnsLocation() throws Exception {
        when(locationService.getById(1L))
                .thenReturn(LocationDtoResponse.builder().id(1L).eventId(100L).name("Moscow").build());

        mvc.perform(get("/admin/locations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
