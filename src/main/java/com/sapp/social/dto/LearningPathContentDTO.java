package com.sapp.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathContentDTO {
    private Long id;
    private Boolean isCompleted;
    private LocalDateTime date;
    private Long learningPathId;
    private String contentTitle;
    private String contentDescription;
    private String contentUrl;
    private Integer ordinal;
}
