package com.sapp.social.service;


import com.sapp.social.model.Content;
import com.sapp.social.model.Post;
import com.sapp.social.model.User;
import com.sapp.social.repository.PostRepository;
import com.sapp.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Post createPost(Long userId, String description, List<MultipartFile> mediaFiles) throws IOException {
        // Validate user existence
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (mediaFiles != null && mediaFiles.size() > 3) {
            throw new IllegalArgumentException("Maximum 3 media files are allowed per post");
        }

        // Create new Post
        Post post = new Post();
        post.setUser(user);
        post.setPostType("skill_sharing");
        post.setDescription(description);
        post.setContents(new HashSet<>()); // Important to match your entity Set<Content>

        // Save Post early to get ID (if needed for storage naming)
        post = postRepository.save(post);

        // Process media files if any
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile file : mediaFiles) {
                String fileName = fileStorageService.storeFile(file);

                Content content = new Content();
                content.setPath(fileName);
                content.setContentType(file.getContentType());
                content.setFileName(file.getOriginalFilename());
                content.setFileSize(file.getSize());
                content.setPost(post);

                // File type handling
                if (fileStorageService.isImageFile(file.getContentType())) {
                    content.setFileType("image");
                } else if (fileStorageService.isVideoFile(file.getContentType())) {
                    content.setFileType("video");
                    content.setDuration(0); // Placeholder for video duration
                } else {
                    content.setFileType("unknown");
                }

                post.getContents().add(content);
            }
        }

        // Save updated post with contents attached
        post = postRepository.save(post);

        return post;
    }



    public List<Post> getPostsByUserId(Long userId) {
        return postRepository.findByUserUserIdOrderByPostIdDesc(userId);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByPostIdDesc();
    }

    public Optional<Post> getPostById(Long postId) {
        return postRepository.findById(postId);
    }

    public boolean deletePost(Long postId, Long userId) {
        Optional<Post> postOpt = postRepository.findById(postId);

        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (!post.getUser().getUserId().equals(userId)) {
                throw new SecurityException("Unauthorized: You can only delete your own posts");
            }
            postRepository.delete(post);
            return true;
        } else {
            return false;
        }
    }

    public Post updatePostDescription(Long postId, Long userId, String newDescription) {
        Optional<Post> postOpt = postRepository.findById(postId);

        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (!post.getUser().getUserId().equals(userId)) {
                throw new SecurityException("Unauthorized: You can only update your own posts");
            }
            post.setDescription(newDescription);
            return postRepository.save(post);
        } else {
            throw new RuntimeException("Post not found with ID: " + postId);
        }
    }


}