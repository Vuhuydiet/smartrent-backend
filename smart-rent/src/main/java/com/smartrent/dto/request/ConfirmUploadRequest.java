package com.smartrent.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmUploadRequest {
    private String checksum; // Optional: for verification
    private String contentType; // Actual content type after upload
}