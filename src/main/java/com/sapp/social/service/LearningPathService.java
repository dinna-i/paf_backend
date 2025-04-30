package com.sapp.social.service;


import com.sapp.social.dto.LearningPathContentDTO;
import com.sapp.social.dto.LearningPathDTO;
import com.sapp.social.model.LearningPath;
import com.sapp.social.model.LearningPathContent;
import com.sapp.social.model.User;
import com.sapp.social.repository.LearningPathContentRepository;
import com.sapp.social.repository.LearningPathRepository;
import com.sapp.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LearningPathService {

    @Autowired
    private LearningPathRepository learningPathRepository;

    @Autowired
    private LearningPathContentRepository learningPathContentRepository;

    @Autowired
    private UserRepository userRepository;

    public List<LearningPathDTO> getLearningPathsByUserId(Long userId) {
        List<LearningPath> learningPaths = learningPathRepository.findByUserUserId(userId);
        return learningPaths.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public LearningPathDTO getLearningPathById(Long pathId) {
        LearningPath learningPath = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Learning path not found with id: " + pathId));
        return convertToDto(learningPath);
    }

    @Transactional
    public LearningPathDTO createLearningPath(Long userId, String name, Integer tag, List<LearningPathContentDTO> contents) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        LearningPath learningPath = new LearningPath();
        learningPath.setName(name);
        learningPath.setTag(tag);
        learningPath.setUser(user);

        // Save learning path to get ID
        learningPath = learningPathRepository.save(learningPath);

        // Create contents if provided
        if (contents != null && !contents.isEmpty()) {
            for (LearningPathContentDTO contentDto : contents) {
                LearningPathContent content = new LearningPathContent();
                content.setIsCompleted(false); // Default value
                content.setDate(LocalDateTime.now());
                content.setContentTitle(contentDto.getContentTitle());
                content.setContentDescription(contentDto.getContentDescription());
                content.setContentUrl(contentDto.getContentUrl());
                content.setOrdinal(contentDto.getOrdinal());
                content.setLearningPath(learningPath);
                learningPathContentRepository.save(content);
            }
        }

        return convertToDto(learningPathRepository.findById(learningPath.getId()).orElse(learningPath));
    }

    @Transactional
    public LearningPathDTO updateLearningPath(Long pathId, String name, Integer tag) {
        LearningPath learningPath = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Learning path not found with id: " + pathId));

        learningPath.setName(name);
        learningPath.setTag(tag);

        return convertToDto(learningPathRepository.save(learningPath));
    }

    @Transactional
    public void deleteLearningPath(Long pathId, Long userId) {
        try {
            // Verify the learning path exists and belongs to the current user
            LearningPath learningPath = learningPathRepository.findById(pathId)
                    .orElseThrow(() -> new RuntimeException("Learning path not found with id: " + pathId));

            if (!learningPath.getUser().getUserId().equals(userId)) {
                throw new SecurityException("Unauthorized: You can only delete your own learning paths");
            }

            // Execute delete in correct order to avoid foreign key constraint issues
            // 1. First, delete all contents using JPQL (avoid loading the entities)
            learningPathContentRepository.deleteByLearningPathId(pathId);

            // 2. Clear the collection to avoid concurrent modification
            if (learningPath.getLearningPathContents() != null) {
                learningPath.getLearningPathContents().clear();
            }

            // 3. Now it's safe to delete the learning path itself
            learningPathRepository.deleteById(pathId);
        } catch (Exception e) {
            // Log the exception for better debugging
            e.printStackTrace();
            throw e;
        }
    }

    @Transactional
    public LearningPathContentDTO addContent(Long pathId, LearningPathContentDTO contentDto) {
        LearningPath learningPath = learningPathRepository.findById(pathId)
                .orElseThrow(() -> new RuntimeException("Learning path not found with id: " + pathId));

        LearningPathContent content = new LearningPathContent();
        content.setIsCompleted(false);
        content.setDate(LocalDateTime.now());
        content.setContentTitle(contentDto.getContentTitle());
        content.setContentDescription(contentDto.getContentDescription());
        content.setContentUrl(contentDto.getContentUrl());
        content.setOrdinal(contentDto.getOrdinal());
        content.setLearningPath(learningPath);

        content = learningPathContentRepository.save(content);

        return convertContentToDto(content);
    }

    @Transactional
    public LearningPathContentDTO updateContentCompletion(Long contentId, Boolean isCompleted, Long userId) {
        LearningPathContent content = learningPathContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Learning path content not found with id: " + contentId));

        // Check if the user owns this learning path
        if (!content.getLearningPath().getUser().getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You can only update your own learning paths");
        }

        content.setIsCompleted(isCompleted);
        content.setDate(LocalDateTime.now()); // Update the timestamp when status changes

        return convertContentToDto(learningPathContentRepository.save(content));
    }

    @Transactional
    public void deleteContent(Long contentId, Long userId) {
        LearningPathContent content = learningPathContentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Learning path content not found with id: " + contentId));

        // Check if the user owns this learning path
        if (!content.getLearningPath().getUser().getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized: You can only delete your own learning path contents");
        }

        learningPathContentRepository.delete(content);
    }

    private LearningPathDTO convertToDto(LearningPath learningPath) {
        LearningPathDTO dto = new LearningPathDTO();
        dto.setId(learningPath.getId());
        dto.setName(learningPath.getName());
        dto.setTag(learningPath.getTag());
        dto.setUserId(learningPath.getUser().getUserId());
        dto.setUserName(learningPath.getUser().getUserName());

        // Get completion counts directly from repository
        int totalContentCount = learningPathContentRepository.countByLearningPathId(learningPath.getId());
        int completedCount = learningPathContentRepository.countByLearningPathIdAndIsCompletedTrue(learningPath.getId());

        dto.setCompletedCount(completedCount);
        dto.setTotalContentCount(totalContentCount);

        // Get ordered contents
        List<LearningPathContent> contents = learningPathContentRepository.findByLearningPathIdOrderByOrdinalAsc(learningPath.getId());

        // Convert contents to DTOs
        dto.setContents(contents.stream()
                .map(this::convertContentToDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private LearningPathContentDTO convertContentToDto(LearningPathContent content) {
        LearningPathContentDTO dto = new LearningPathContentDTO();
        dto.setId(content.getId());
        dto.setIsCompleted(content.getIsCompleted());
        dto.setDate(content.getDate());
        dto.setLearningPathId(content.getLearningPath().getId());
        dto.setContentTitle(content.getContentTitle());
        dto.setContentDescription(content.getContentDescription());
        dto.setContentUrl(content.getContentUrl());
        dto.setOrdinal(content.getOrdinal());
        return dto;
    }

    /**
     * Calculate the completion percentage for a learning path
     * @param pathId The learning path ID
     * @return The completion percentage (0-100)
     */
    public int calculateCompletionPercentage(Long pathId) {
        // Check if learning path exists
        if (!learningPathRepository.existsById(pathId)) {
            throw new RuntimeException("Learning path not found with id: " + pathId);
        }

        // More efficient implementation using repository count methods
        int totalCount = learningPathContentRepository.countByLearningPathId(pathId);

        if (totalCount == 0) {
            return 0;
        }

        int completedCount = learningPathContentRepository.countByLearningPathIdAndIsCompletedTrue(pathId);

        return (int) ((completedCount * 100) / totalCount);
    }

    /**
     * Batch update completion status for multiple content items
     * @param contentIds List of content IDs to update
     * @param isCompleted New completion status
     * @param userId User ID for authorization check
     * @return List of updated content DTOs
     */
    @Transactional
    public List<LearningPathContentDTO> batchUpdateContentCompletion(List<Long> contentIds, Boolean isCompleted, Long userId) {
        List<LearningPathContent> contentsToUpdate = contentIds.stream()
                .map(id -> learningPathContentRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Learning path content not found with id: " + id)))
                .collect(Collectors.toList());

        // Check if all contents belong to the user
        for (LearningPathContent content : contentsToUpdate) {
            if (!content.getLearningPath().getUser().getUserId().equals(userId)) {
                throw new SecurityException("Unauthorized: You can only update your own learning paths");
            }
        }

        // Update all contents
        contentsToUpdate.forEach(content -> {
            content.setIsCompleted(isCompleted);
            content.setDate(LocalDateTime.now());
        });

        List<LearningPathContent> updatedContents = learningPathContentRepository.saveAll(contentsToUpdate);

        return updatedContents.stream()
                .map(this::convertContentToDto)
                .collect(Collectors.toList());
    }
}
