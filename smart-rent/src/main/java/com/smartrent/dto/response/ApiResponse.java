package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse <T> {

  @Builder.Default
  @Schema(
      description = "Response code indicating the status of the operation",
      example = "999999",
      defaultValue = "999999"
  )
  String code = "999999";

  @Schema(
      description = "Human-readable message describing the result",
      example = "Operation completed successfully"
  )
  String message;

  @Schema(
      description = "Response data payload"
  )
  T data;
}
