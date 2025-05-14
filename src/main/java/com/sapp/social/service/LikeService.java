package com.sapp.social.service;

import com.linkup.app.model.Like;
import com.linkup.app.model.Post;
import com.linkup.app.model.User;
import com.linkup.app.repository.LikeRepository;
import com.linkup.app.repository.PostRepository;
import com.linkup.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Transactional
    public Like toggleLike(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        Optional<Like> existingLike = likeRepository.findByUserUserIdAndPostPostId(userId, postId);

        if (existingLike.isPresent()) {
            // Unlike - remove the like
            likeRepository.delete(existingLike.get());
            return null;
        } else {
            // Like - create new like
            Like like = new Like();
            like.setUser(user);
            like.setPost(post);
            like.setCreatedAt(LocalDateTime.now());
            return likeRepository.save(like);
        }
    }

    public boolean hasUserLiked(Long userId, Long postId) {
        return likeRepository.existsByUserUserIdAndPostPostId(userId, postId);
    }

    public int getLikesCount(Long postId) {
        return likeRepository.countByPostPostId(postId);
    }

    public List<Like> getPostLikes(Long postId) {
        return likeRepository.findByPostPostId(postId);
    }
}
