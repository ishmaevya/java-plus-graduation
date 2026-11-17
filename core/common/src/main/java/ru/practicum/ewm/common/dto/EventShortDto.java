package ru.practicum.ewm.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class EventShortDto {

    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String eventDate;

    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
    private Long likes;
    private Long dislikes;
    private Long rating;
    private Long commentCount;
}
