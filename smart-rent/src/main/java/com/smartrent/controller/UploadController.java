package com.smartrent.controller;

import com.smartrent.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@Tag(name = "Uploads", description = "Upload images and videos to S3-compatible storage")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadController {
    StorageService storageService;

    @PostMapping("/image")
    @Operation(
        summary = "Upload image",
        description = "Uploads an image file and returns a public URL",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "multipart/form-data",
                schema = @Schema(type = "object", requiredProperties = {"file"})
            )
        )
    )
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
    @Operation(
        summary = "Upload video",
        description = "Uploads a video file and returns a public URL",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "multipart/form-data",
                schema = @Schema(type = "object", requiredProperties = {"file"})
            )
        )
    )
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
