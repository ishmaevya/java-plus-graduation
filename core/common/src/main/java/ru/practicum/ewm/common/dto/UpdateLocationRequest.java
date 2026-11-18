package ru.practicum.ewm.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {

    private Long eventId;

    @Size(min = 1, max = 100)
    private String name;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Float lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Float lon;
}
