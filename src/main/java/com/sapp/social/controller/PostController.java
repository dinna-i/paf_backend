package com.sapp.social.controller;


import com.sapp.social.dto.UpdatePostRequest;
import com.sapp.social.model.Post;
import com.sapp.social.model.User;
import com.sapp.social.repository.UserRepository;
import com.sapp.social.service.FileStorageService;
import com.sapp.social.service.PostService;
import com.sapp.social.util.VideoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private static final Logger logger = Logger.getLogger(PostController.class.getName());

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoUtils videoUtil;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> createPost(
            @AuthenticationPrincipal Object principal,
            @RequestPart("description") String description,
            @RequestPart(value = "media", required = false) List<MultipartFile> mediaFiles) {

        try {
            // Debug info about the request
            logger.info("Creating post with description: " + description);
            logger.info("Media files: " + (mediaFiles != null ? mediaFiles.size() : "null"));

            // Get email from principal
            String email;
            if (principal instanceof OAuth2User oauth2User) {
                email = (String) oauth2User.getAttributes().get("email");
                logger.info("Authenticated via OAuth2: " + email);
            } else if (principal instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
                logger.info("Authenticated via UserDetails: " + email);
            } else {
                logger.warning("Unauthorized: Invalid principal type: " +
                        (principal != null ? principal.getClass().getName() : "null"));
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized: Invalid authentication");
            }

            // Lookup user in DB with proper error handling
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.warning("User not found for email: " + email);
                        return new RuntimeException("User not found for email: " + email);
                    });

            logger.info("Found user: " + user.getUserId() + " - " + user.getUserName());

            // Validate media file count
            if (mediaFiles != null && mediaFiles.size() > 3) {
                logger.warning("Too many files: " + mediaFiles.size());
                return ResponseEntity.badRequest().body("Maximum 3 files allowed per post");
            }

            if (mediaFiles == null ) {
                return ResponseEntity.badRequest().body("Post must need to Have atleast One content.");
            }
//            if (mediaFiles != null && !mediaFiles.isEmpty()) {
//                for (MultipartFile file : mediaFiles) {
//                    if (fileStorageService.isVideoFile(file.getContentType())) {
//                        if(!videoUtil.isVideoDurationValid(file)){
//                            return ResponseEntity.badRequest().body("Video Length is Long");
//                        }
//
//                    }
//                }
//            }
            // Create post with better error catching
            Post post = postService.createPost(user.getUserId(), description, mediaFiles);
            logger.info("Post created successfully with ID: " + post.getPostId());

            return ResponseEntity.status(HttpStatus.CREATED).body(post);

        } catch (IOException e) {
            logger.severe("IOException during post creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to process files: " + e.getMessage());
        } catch (Exception e) {
            logger.severe("Exception during post creation: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Post>> getAllPosts() {
        try {
            List<Post> posts = postService.getAllPosts();
            logger.info("Retrieved " + posts.size() + " posts");
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            logger.severe("Error retrieving all posts: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPostById(@PathVariable Long postId) {
        try {
            logger.info("Fetching post with ID: " + postId);
            return postService.getPostById(postId)
                    .map(post -> {
                        logger.info("Found post: " + post.getPostId());
                        return ResponseEntity.ok(post);
                    })
                    .orElseGet(() -> {
                        logger.warning("Post not found with ID: " + postId);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            logger.severe("Error retrieving post " + postId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving post: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId) {
        try {
            logger.info("Fetching posts for user ID: " + userId);
            List<Post> posts = postService.getPostsByUserId(userId);
            logger.info("Found " + posts.size() + " posts for user " + userId);
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            logger.severe("Error retrieving posts for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving posts: " + e.getMessage());
        }
    }

    // Optional: add a method to test file upload functionality separately
    @PostMapping("/test-upload")
    public ResponseEntity<?> testFileUpload(
            @RequestPart(value = "file", required = true) MultipartFile file) {
        try {
            logger.info("Testing file upload: " + file.getOriginalFilename() +
                    ", size: " + file.getSize() +
                    ", content type: " + file.getContentType());

            // Just check if file is readable, don't save it
            if (!file.isEmpty() && file.getBytes().length > 0) {
                return ResponseEntity.ok("File received successfully: " +
                        file.getOriginalFilename() +
                        " (" + file.getSize() + " bytes)");
            } else {
                return ResponseEntity.badRequest().body("Empty file received");
            }
        } catch (Exception e) {
            logger.severe("Test upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Test upload failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(@AuthenticationPrincipal Object principal,
                                        @PathVariable Long postId) {
        try {
            String email;
            if (principal instanceof OAuth2User oauth2User) {
                email = (String) oauth2User.getAttributes().get("email");
            } else if (principal instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized: Invalid authentication");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

            boolean deleted = postService.deletePost(postId, user.getUserId());

            if (deleted) {
                return ResponseEntity.ok("Post deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Post not found with ID: " + postId);
            }
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(se.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting post: " + e.getMessage());
        }
    }

    @PutMapping("/{postId}")
    public ResponseEntity<?> updatePostDescription(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long postId,
            @RequestBody UpdatePostRequest updateRequest) {

        try {
            String email;
            if (principal instanceof OAuth2User oauth2User) {
                email = (String) oauth2User.getAttributes().get("email");
            } else if (principal instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Unauthorized: Invalid authentication");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

            Post updatedPost = postService.updatePostDescription(postId, user.getUserId(), updateRequest.getDescription());

            return ResponseEntity.ok(updatedPost);

        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(se.getMessage());
        } catch (RuntimeException re) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(re.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating post: " + e.getMessage());
        }
    }

}