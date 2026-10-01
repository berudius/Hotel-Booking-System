package com.demo_hotel_service.services;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class ImageStorageService {
    @Value("${image_storage.uploads.dir}")
    private String uploadsDir;
    @Value("${image_storage.uploads.url}")
    private String uploadsUrl;

    private Path uploadsDirPath;

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final List<String> ALLOWED_MIME_TYPES = List.of("image/jpeg", "image/png");

    @PostConstruct
    public void init() throws IOException {
        uploadsDirPath = Path.of(uploadsDir);
        if (!Files.exists(uploadsDirPath)) {
            Files.createDirectories(uploadsDirPath);
        }
    }

    public String store(MultipartFile imageFile) throws IOException {
        if (imageFile == null) {
            throw new IllegalArgumentException("File cant be null");
        }

        if (imageFile.isEmpty()) {
            throw new IOException("Failed to store empty file");
        }

        String originalFileName = imageFile.getOriginalFilename();
        String fileExtansion = StringUtils.getFilenameExtension(originalFileName);

        if (fileExtansion == null || !ALLOWED_EXTENSIONS.contains(fileExtansion)) {
            throw new IllegalArgumentException("Invalid file extension. Only JPG and PNG are allowed.");
        }

        String contentType = imageFile.getContentType();

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type. Only jpeg, png are allowed.");
        }

        String newFilename = UUID.randomUUID().toString() + "." + fileExtansion;

        Path target = uploadsDirPath.resolve(newFilename);

        Files.copy(imageFile.getInputStream(), target);

        String imageFileUrl = uploadsUrl + newFilename;
        return imageFileUrl;
    }

    public List<String> storeAll(List<MultipartFile> images) {
        List<String> savedImagesNames = new ArrayList<>();

        if (images == null || images.isEmpty()) {
            return savedImagesNames;
        }

        for (MultipartFile image : images) {
            try {
                String imageName = store(image);
                savedImagesNames.add(imageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (savedImagesNames.isEmpty()) {
            throw new RuntimeException("None of the files was stored, operation canceled");
        }

        return savedImagesNames;
    }

    public void remove(String imageUrl) throws IOException {
        Path target = getPathFromUrl(imageUrl);
        Files.deleteIfExists(target);
    }

    public void remove(List<String> imageUrls) {
        for (String url : imageUrls) {
            Path path = getPathFromUrl(url);
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Path getPathFromUrl(String imageUrl) {
        String filename = Paths.get(URI.create(imageUrl).getPath()).getFileName().toString();
        return Paths.get(uploadsDir).resolve(filename);
    }

}
