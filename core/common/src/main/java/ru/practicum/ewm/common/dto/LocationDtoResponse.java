package ru.practicum.ewm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDtoResponse {

    private Long id;
    private Long eventId;
    private String name;
    private Float lat;
    private Float lon;
}
