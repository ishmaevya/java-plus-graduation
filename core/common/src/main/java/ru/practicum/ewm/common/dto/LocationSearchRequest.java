package ru.practicum.ewm.common.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchRequest {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Float lat;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Float lon;

    @NotNull
    @DecimalMin("0.1")
    private Float radius;
}
