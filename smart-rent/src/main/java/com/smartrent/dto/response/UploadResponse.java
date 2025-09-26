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
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object containing uploaded file information")
public class UploadResponse {

  @Schema(
      description = "Status of the upload operation",
      example = "success"
  )
  String status;

  @Schema(
      description = "Public URL of the uploaded file",
      example = "https://storage.example.com/images/12345.jpg"
  )
  String url;

  @Schema(
      description = "Error message if upload failed",
      example = "File type not supported"
  )
  String message;
}