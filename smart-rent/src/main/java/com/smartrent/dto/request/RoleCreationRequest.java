package com.smartrent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request object for creating a new role")
public class RoleCreationRequest {

    @NotBlank(message = "Role ID is required")
    @Size(max = 10, message = "Role ID must not exceed 10 characters")
    @Schema(
        description = "Unique identifier for the role (e.g., SA, UA, CM)",
        example = "MA",
        maxLength = 10,
        required = true
    )
    String roleId;

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    @Schema(
        description = "Human-readable name of the role",
        example = "Marketing Admin",
        maxLength = 100,
        required = true
    )
    String roleName;
}

