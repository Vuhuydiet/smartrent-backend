package com.smartrent.controller;

import com.smartrent.dto.request.PresignedUrlRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PresignedUrlResponse;
import com.smartrent.dto.response.UploadResponse;
import com.smartrent.service.storage.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
            description = "Tải lên hình ảnh thành công",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Phản hồi thành công",
                    value = """
                        {
                          "code": "999999",
                          "message": null,
                          "data": {
                            "status": "success",
                            "url": "https://smartrent-storage.vn/images/can-ho-q1-phong-ngu.jpg"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Định dạng tệp không hợp lệ hoặc lỗi tải lên",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Phản hồi lỗi",
                    value = """
                        {
                          "code": "400001",
                          "message": "ĐỊNH_DẠNG_TẾP_KHÔNG_HỢP_LỆ",
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
        summary = "Tải lên video",
        description = "Tải lên tệp video và trả về URL công khai",
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
            description = "Tải lên video thành công",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "Phản hồi thành công",
                    value = """
                        {
                          "code": "999999",
                          "message": null,
                          "data": {
                            "status": "success",
                            "url": "https://smartrent-storage.vn/videos/gioi-thieu-can-ho-q1.mp4"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Định dạng tệp không hợp lệ hoặc lỗi tải lên",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Phản hồi lỗi",
                    value = """
                        {
                          "code": "400001",
                          "message": "ĐỊNH_DẠNG_TẾP_KHÔNG_HỢP_LỆ",
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

    // ===== PRE-SIGNED URL ENDPOINTS =====

    @PostMapping("/presigned-url/image")
    @Operation(
        summary = "Generate pre-signed URL for image upload",
        description = "Returns a pre-signed URL that allows direct upload to S3 without going through the backend server",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlRequest.class),
                examples = @ExampleObject(
                    name = "Request Example",
                    value = """
                        {
                          "filename": "living-room.jpg",
                          "contentType": "image/jpeg",
                          "fileSize": 2048576
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Pre-signed URL generated successfully",
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
                            "uploadUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-living-room.jpg?X-Amz-Algorithm=...",
                            "fileUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/images/uuid-living-room.jpg",
                            "fileKey": "images/uuid-living-room.jpg",
                            "expiresAt": "2025-10-04T11:00:00",
                            "requiredHeaders": {
                              "contentType": "image/jpeg"
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<PresignedUrlResponse> generateImagePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = storageService.generatePresignedImageUploadUrl(request);
        return ApiResponse.<PresignedUrlResponse>builder()
                .data(response)
                .build();
    }

    @PostMapping("/presigned-url/video")
    @Operation(
        summary = "Generate pre-signed URL for video upload",
        description = "Returns a pre-signed URL that allows direct upload to S3 without going through the backend server",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresignedUrlRequest.class),
                examples = @ExampleObject(
                    name = "Request Example",
                    value = """
                        {
                          "filename": "apartment-tour.mp4",
                          "contentType": "video/mp4",
                          "fileSize": 10485760
                        }
                        """
                )
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Pre-signed URL generated successfully",
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
                            "uploadUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4?X-Amz-Algorithm=...",
                            "fileUrl": "https://pub-444e165e3cc34721a5620508f66c58b0.r2.dev/videos/uuid-tour.mp4",
                            "fileKey": "videos/uuid-tour.mp4",
                            "expiresAt": "2025-10-04T11:00:00",
                            "requiredHeaders": {
                              "contentType": "video/mp4"
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public ApiResponse<PresignedUrlResponse> generateVideoPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = storageService.generatePresignedVideoUploadUrl(request);
        return ApiResponse.<PresignedUrlResponse>builder()
                .data(response)
                .build();
    }
}
