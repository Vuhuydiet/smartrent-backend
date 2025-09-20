package com.smartrent.controller;

import com.smartrent.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadController {
    StorageService storageService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = storageService.uploadImage(file);
            result.put("status", "success");
            result.put("url", url);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Upload failed");
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/video")
    public ResponseEntity<Map<String, Object>> uploadVideo(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            String url = storageService.uploadVideo(file);
            result.put("status", "success");
            result.put("url", url);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Upload failed");
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
