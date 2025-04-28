package com.sapp.social.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_path_contents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isCompleted;
    private LocalDateTime date;

    // Fields to better describe the content
    private String contentTitle;
    private String contentDescription;
    private String contentUrl;
    private Integer ordinal; // For ordering content in a learning path

    @ManyToOne
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;
}