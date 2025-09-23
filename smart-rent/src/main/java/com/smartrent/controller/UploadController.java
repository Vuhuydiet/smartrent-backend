package com.smartrent.controller;

import com.smartrent.controller.dto.response.ApiResponse;
import com.smartrent.controller.dto.response.UploadResponse;
import com.smartrent.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/upload")
@Tag(name = "File Upload", description = "Upload images and videos to S3-compatible storage")
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
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Image uploaded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "code": "999999",
                          "message": null,
                          "data": {
                            "status": "success",
                            "url": "https://storage.example.com/images/12345.jpg"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid file format or upload error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Error Response",
                    value = """
                        {
                          "code": "400001",
                          "message": "INVALID_FILE_FORMAT",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadImage(file);
        UploadResponse response = UploadResponse.builder()
            .status("success")
            .url(url)
            .build();
        return ApiResponse.<UploadResponse>builder()
            .data(response)
            .build();
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
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Video uploaded successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                        {
                          "code": "999999",
                          "message": null,
                          "data": {
                            "status": "success",
                            "url": "https://storage.example.com/videos/67890.mp4"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid file format or upload error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Error Response",
                    value = """
                        {
                          "code": "400001",
                          "message": "INVALID_FILE_FORMAT",
                          "data": null
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<UploadResponse> uploadVideo(@RequestParam("file") MultipartFile file) {
        String url = storageService.uploadVideo(file);
        UploadResponse response = UploadResponse.builder()
            .status("success")
            .url(url)
            .build();
        return ApiResponse.<UploadResponse>builder()
            .data(response)
            .build();
    }
}
