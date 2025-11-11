package com.agendalo.controller.image;

import com.agendalo.domain.Image;
import com.agendalo.services.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

//    @PostMapping("/users/{userId}")
//    public ResponseEntity<Long> uploadUserImage(
//            @PathVariable Long userId,
//            @RequestParam("image") MultipartFile file) {
//        try {
//            Long imageId = imageService.uploadUserImage(file, userId);
//            return ResponseEntity.ok(imageId);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body(null);
//        }
//    }

    @PostMapping("/companies/{companyId}/logo")
    public ResponseEntity<Long> uploadCompanyLogo(
            @PathVariable Long companyId,
            @RequestParam("logo") MultipartFile file) {
        try {
            Long imageId = imageService.uploadCompanyLogo(file, companyId);
            return ResponseEntity.ok(imageId);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

//    @PostMapping("/companies/gallery")
//    public ResponseEntity<List<Long>> uploadCompanyImages(
//            @RequestParam("images") MultipartFile file) {
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            String email = authentication.getName();
//
//            List<Long> imageIds = imageService.uploadCompanyImages(file, email);
//            return ResponseEntity.ok(imageIds);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().body(null);
//        }
//    }

    @GetMapping("/{imageId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long imageId) {
        Image image = imageService.getImage(imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getType()))
                .body(image.getData());
    }
} 