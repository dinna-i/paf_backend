package com.sapp.social.repository;


import com.sapp.social.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostPostIdOrderByCreatedAtDesc(Long postId);
    List<Comment> findByParentCommentCommentIdOrderByCreatedAtAsc(Long parentCommentId);
    List<Comment> findByPostPostIdAndParentCommentIsNullOrderByCreatedAtDesc(Long postId);
    int countByPostPostId(Long postId);
}
