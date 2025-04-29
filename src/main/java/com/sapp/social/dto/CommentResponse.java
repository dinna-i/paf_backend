package com.sapp.social.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long commentId;
    private String content;
    private LocalDateTime createdAt;
    private Long userId;
    private String userName;
    private Long postId;
    private Long parentCommentId;
    private List<CommentResponse> replies;
}