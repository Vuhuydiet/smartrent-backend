package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing AI listing verification results")
public class AiListingVerificationResponse {

    @JsonProperty("is_valid")
    @Schema(description = "Whether the listing is valid overall", example = "true")
    private Boolean isValid;

    @JsonProperty("score")
    @Schema(description = "Overall verification score (0.0 to 1.0)", example = "0.85")
    private Double score;

    @JsonProperty("confidence")
    @Schema(description = "Confidence level of the verification", example = "0.9")
    private Double confidence;

    @JsonProperty("image_validation")
    @Schema(description = "Image validation results")
    private ImageValidation imageValidation;

    @JsonProperty("video_validation")
    @Schema(description = "Video validation results")
    private VideoValidation videoValidation;

    @JsonProperty("content_validation")
    @Schema(description = "Content validation results")
    private ContentValidation contentValidation;

    @JsonProperty("completeness_validation")
    @Schema(description = "Completeness validation results")
    private CompletenessValidation completenessValidation;

    @JsonProperty("violations")
    @Schema(description = "List of violations found")
    private List<Violation> violations;

    @JsonProperty("suggestions")
    @Schema(description = "List of improvement suggestions")
    private List<Suggestion> suggestions;

    @JsonProperty("verification_timestamp")
    @Schema(description = "When the verification was performed", example = "2025-11-23T15:30:00")
    private LocalDateTime verificationTimestamp;

    @JsonProperty("model_used")
    @Schema(description = "AI model used for verification", example = "gemini-2.5-pro")
    private String modelUsed;

    @JsonProperty("processing_time_seconds")
    @Schema(description = "Time taken to process the verification", example = "6.2")
    private Double processingTimeSeconds;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Image validation results")
    public static class ImageValidation {
        @JsonProperty("is_valid")
        @Schema(description = "Whether images are valid", example = "true")
        private Boolean isValid;

        @JsonProperty("total_images")
        @Schema(description = "Total number of images", example = "3")
        private Integer totalImages;

        @JsonProperty("valid_images")
        @Schema(description = "Number of valid images", example = "3")
        private Integer validImages;

        @JsonProperty("issues")
        @Schema(description = "List of image issues")
        private List<String> issues;

        @JsonProperty("quality_score")
        @Schema(description = "Image quality score", example = "0.8")
        private Double qualityScore;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Video validation results")
    public static class VideoValidation {
        @JsonProperty("is_valid")
        @Schema(description = "Whether videos are valid", example = "true")
        private Boolean isValid;

        @JsonProperty("total_videos")
        @Schema(description = "Total number of videos analyzed", example = "2")
        private Integer totalVideos;

        @JsonProperty("valid_videos")
        @Schema(description = "Number of valid videos", example = "2")
        private Integer validVideos;

        @JsonProperty("quality_score")
        @Schema(description = "Overall video quality score", example = "0.85")
        private Double qualityScore;

        @JsonProperty("issues")
        @Schema(description = "List of video validation issues")
        private List<String> issues;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Content validation results")
    public static class ContentValidation {
        @JsonProperty("is_rental_related")
        @Schema(description = "Whether content is rental-related", example = "true")
        private Boolean isRentalRelated;

        @JsonProperty("category_match")
        @Schema(description = "Whether category matches content", example = "true")
        private Boolean categoryMatch;

        @JsonProperty("content_score")
        @Schema(description = "Content quality score", example = "0.9")
        private Double contentScore;

        @JsonProperty("issues")
        @Schema(description = "List of content issues")
        private List<String> issues;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Completeness validation results")
    public static class CompletenessValidation {
        @JsonProperty("is_complete")
        @Schema(description = "Whether listing is complete", example = "true")
        private Boolean isComplete;

        @JsonProperty("completeness_score")
        @Schema(description = "Completeness score", example = "0.8")
        private Double completenessScore;

        @JsonProperty("missing_fields")
        @Schema(description = "List of missing fields")
        private List<String> missingFields;

        @JsonProperty("quality_issues")
        @Schema(description = "List of quality issues")
        private List<String> qualityIssues;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Violation found in listing")
    public static class Violation {
        @JsonProperty("category")
        @Schema(description = "Violation category", example = "Suspicious Content")
        private String category;

        @JsonProperty("severity")
        @Schema(description = "Violation severity", example = "high")
        private String severity;

        @JsonProperty("message")
        @Schema(description = "Violation message", example = "Unrealistically high price")
        private String message;

        @JsonProperty("field")
        @Schema(description = "Related field", example = "price")
        private String field;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Improvement suggestion")
    public static class Suggestion {
        @JsonProperty("category")
        @Schema(description = "Suggestion category", example = "images")
        private String category;

        @JsonProperty("message")
        @Schema(description = "Suggestion message", example = "Add more photos of bathroom")
        private String message;

        @JsonProperty("field")
        @Schema(description = "Related field", example = "images")
        private String field;

        @JsonProperty("priority")
        @Schema(description = "Suggestion priority", example = "medium")
        private String priority;
    }
}