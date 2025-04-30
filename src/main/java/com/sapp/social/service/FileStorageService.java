package com.sapp.social.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${file.allowed-video-types}")
    private String allowedVideoTypes;

    @Value("${file.max-video-duration-seconds}")
    private int maxVideoDurationSeconds;

    public String storeFile(MultipartFile file) throws IOException {
        try {
            // Check if file is empty
            if (file == null) {
                throw new IOException("File is null");
            }

            if (file.isEmpty()) {
                throw new IOException("Failed to store empty file");
            }

            // Log file details
            System.out.println("Processing file: " + file.getOriginalFilename() +
                    ", size: " + file.getSize() +
                    ", content type: " + file.getContentType());

            // Check file type
            String fileType = file.getContentType();
            if (!isAllowedFileType(fileType)) {
                throw new IOException("File type not allowed: " + fileType);
            }

            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() +
                    (extension.isEmpty() ? "" : "." + extension);

            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            System.out.println("Upload path: " + uploadPath);
            Files.createDirectories(uploadPath);

            // Save the file
            Path targetLocation = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully to: " + targetLocation);

            return newFilename;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public boolean isAllowedFileType(String contentType) {
        List<String> allowedTypes = getAllowedFileTypes();
        return allowedTypes.contains(contentType);
    }

    public boolean isImageFile(String contentType) {
        return Arrays.asList(allowedImageTypes.split(",")).contains(contentType);
    }

    public boolean isVideoFile(String contentType) {
        return Arrays.asList(allowedVideoTypes.split(",")).contains(contentType);
    }

    private List<String> getAllowedFileTypes() {
        List<String> imageTypes = Arrays.asList(allowedImageTypes.split(","));
        List<String> videoTypes = Arrays.asList(allowedVideoTypes.split(","));
        return Stream.concat(imageTypes.stream(), videoTypes.stream())
                .collect(Collectors.toList());
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex > 0) ? filename.substring(dotIndex + 1) : "";
    }
}
