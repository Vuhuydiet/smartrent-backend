package com.smartrent.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateUploadUrlResponse {
    private Long mediaId;
    private String uploadUrl;
    private Integer expiresIn; // seconds
    private String storageKey;
    private String message;
}
