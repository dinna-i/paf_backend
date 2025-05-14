package com.sapp.social.controller;

import com.sapp.social.dto.LikeRequest;
import com.sapp.social.model.Like;
import com.sapp.social.model.User;
import com.sapp.social.repository.UserRepository;
import com.sapp.social.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleLike(@RequestBody LikeRequest likeRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        Like like = likeService.toggleLike(user.getUserId(), likeRequest.getPostId());

        Map<String, Object> response = new HashMap<>();
        response.put("liked", like != null);
        response.put("likesCount", likeService.getLikesCount(likeRequest.getPostId()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getPostLikes(@PathVariable Long postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        List<Like> likes = likeService.getPostLikes(postId);
        boolean userHasLiked = likeService.hasUserLiked(userOpt.get().getUserId(), postId);

        Map<String, Object> response = new HashMap<>();
        response.put("likes", likes);
        response.put("count", likes.size());
        response.put("userHasLiked", userHasLiked);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<?> getLikesCount(@PathVariable Long postId) {
        int count = likeService.getLikesCount(postId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/post/{postId}/status")
    public ResponseEntity<?> getUserLikeStatus(@PathVariable Long postId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        boolean hasLiked = likeService.hasUserLiked(userOpt.get().getUserId(), postId);
        return ResponseEntity.ok(hasLiked);
    }
}