package com.smartrent.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ListingVerificationResponse {
    @JsonProperty("is_valid")
    private boolean isValid;
    
    @JsonProperty("score")
    private double score;
    
    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("suggested_status")
    private String suggestedStatus;
    
    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("processing_time_seconds")
    private Double processingTimeSeconds;
    
    @JsonProperty("image_validation")
    private ValidationResult imageValidation;
    
    @JsonProperty("video_validation")
    private ValidationResult videoValidation;
    
    @JsonProperty("content_validation")
    private ValidationResult contentValidation;
    
    @JsonProperty("completeness_validation")
    private ValidationResult completenessValidation;
    
    @JsonProperty("violations")
    private List<ViolationDto> violations;
    
    @JsonProperty("suggestions")
    private List<SuggestionDto> suggestions;
    
    @JsonProperty("reason")
    private StructuredReasonDto reason;
    
    @JsonProperty("verification_timestamp")
    private java.time.LocalDateTime verificationTimestamp;
    
    @JsonProperty("violation_codes")
    private List<String> violationCodes;

    @Data
    @NoArgsConstructor
    public static class ValidationResult {
        @JsonProperty("is_valid")
        private boolean isValid;

        @JsonProperty("quality_score")
        private double qualityScore;

        @JsonProperty("issues")
        private List<String> issues;

        @JsonProperty("total_images")
        private Integer totalImages;

        @JsonProperty("valid_images")
        private Integer validImages;

        @JsonProperty("total_videos")
        private Integer totalVideos;

        @JsonProperty("valid_videos")
        private Integer validVideos;

        @JsonProperty("is_rental_related")
        private Boolean isRentalRelated;

        @JsonProperty("category_match")
        private Boolean categoryMatch;

        @JsonProperty("content_score")
        private Double contentScore;

        @JsonProperty("is_complete")
        private Boolean isComplete;

        @JsonProperty("completeness_score")
        private Double completenessScore;

        @JsonProperty("missing_fields")
        private List<String> missingFields;

        @JsonProperty("quality_issues")
        private List<String> qualityIssues;
    }

    @Data
    @NoArgsConstructor
    public static class ViolationDto {
        private String category;
        private String severity;
        private String message;
        private String field;
    }

    @Data
    @NoArgsConstructor
    public static class SuggestionDto {
        private String category;
        private String message;
        private String field;
        private String priority;
    }

    @Data
    @NoArgsConstructor
    public static class StructuredReasonDto {
        @JsonProperty("blurriness_issue")
        private boolean blurrinessIssue;

        @JsonProperty("missing_fields")
        private List<String> missingFields;

        @JsonProperty("inconsistent_info")
        private boolean inconsistentInfo;

        @JsonProperty("watermark_or_phone")
        private boolean watermarkOrPhone;

        @JsonProperty("stock_photo")
        private boolean stockPhoto;

        @JsonProperty("details")
        private String details;
    }
}
