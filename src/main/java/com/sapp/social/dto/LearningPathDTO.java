package com.sapp.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathDTO {
    private Long id;
    private String name;
    private Integer tag;
    private Long userId;
    private String userName;
    private int completedCount;
    private int totalContentCount;
    private List<LearningPathContentDTO> contents;
}
