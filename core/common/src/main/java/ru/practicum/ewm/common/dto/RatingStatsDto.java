package ru.practicum.ewm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RatingStatsDto {

    public static final RatingStatsDto EMPTY = new RatingStatsDto(0L, 0L);

    private Long likes;
    private Long dislikes;

    public Long getRating() {
        return likes - dislikes;
    }
}
