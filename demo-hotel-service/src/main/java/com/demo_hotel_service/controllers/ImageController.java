package com.demo_hotel_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo_hotel_service.services.ImageService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @DeleteMapping("/api/image")
    public ResponseEntity<?> deleteImage(@RequestParam Long imageId, @RequestParam String imageUrl){
        return ResponseEntity.ok(imageService.removeImage(imageId, imageUrl));
    }
}
