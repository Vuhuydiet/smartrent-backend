package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Admin request to block or unblock a listing author from posting new listings.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to block or unblock a user from posting listings")
public class PostingBlockRequest {

    @NotNull(message = "blocked is required")
    @Schema(description = "true to block the user from posting, false to unblock", example = "true", required = true)
    Boolean blocked;

    @Schema(description = "Reason for the block (recommended when blocking)",
            example = "Nhiều tin đăng vi phạm bị báo cáo và đã được duyệt")
    String reason;
}
