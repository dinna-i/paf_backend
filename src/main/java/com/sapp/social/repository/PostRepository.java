package com.sapp.social.repository;

import com.sapp.social.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserUserIdOrderByPostIdDesc(Long userId);
    List<Post> findAllByOrderByPostIdDesc();
    void deleteByPostIdAndUserUserId(Long postId, Long userId);
}
