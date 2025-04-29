package com.sapp.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {
    private Long postId;
    private String content;
    private Long parentCommentId; // Optional, null for top-level comments
}
