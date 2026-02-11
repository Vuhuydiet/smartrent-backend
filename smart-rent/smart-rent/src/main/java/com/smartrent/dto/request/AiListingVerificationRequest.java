package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smartrent.enums.HousingPropertyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request payload for AI listing verification")
public class AiListingVerificationRequest {

    @JsonProperty("title")
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    @Schema(description = "Listing title", example = "Cho thuê căn hộ 2PN Vinhomes Central Park", required = true)
    private String title;

    @JsonProperty("description")
    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    @Schema(description = "Listing description", example = "Căn hộ 2 phòng ngủ, đầy đủ nội thất, view sông đẹp", required = true)
    private String description;

    @JsonProperty("price")
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Schema(description = "Listing price", example = "20000000", required = true)
    private BigDecimal price;

    @JsonProperty("address")
    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    @Schema(description = "Property address", example = "208 Nguyễn Hữu Cảnh, Bình Thạnh, TP.HCM", required = true)
    private String address;

    @JsonProperty("property_type")
    @Schema(description = "Property type", example = "APARTMENT")
    private HousingPropertyType propertyType;

    @JsonProperty("area")
    @DecimalMin(value = "0.01", message = "Area must be greater than 0")
    @Schema(description = "Property area in square meters", example = "75")
    private Double area;

    @JsonProperty("amenities")
    @Schema(description = "List of amenities")
    private List<String> amenities;

    @JsonProperty("images")
    @Size(max = 20, message = "Maximum 20 images allowed")
    @Schema(description = "List of image URLs")
    private List<String> images;

    @JsonProperty("videos")
    @Size(max = 5, message = "Maximum 5 videos allowed")
    @Schema(description = "List of video objects")
    private List<VideoObject> videos;

    @JsonProperty("metadata")
    @Schema(description = "Additional property metadata")
    private PropertyMetadata metadata;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Video object structure")
    public static class VideoObject {
        @JsonProperty("url")
        @NotBlank(message = "Video URL is required")
        @Schema(description = "Video URL", example = "https://example.com/video.mp4", required = true)
        private String url;

        @JsonProperty("thumbnail_url")
        @Schema(description = "Video thumbnail URL", example = "https://example.com/thumb.jpg")
        private String thumbnailUrl;

        @JsonProperty("duration_seconds")
        @Schema(description = "Video duration in seconds", example = "60")
        private Integer durationSeconds;

        @JsonProperty("caption")
        @Schema(description = "Video caption", example = "Video tour")
        private String caption;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Property metadata")
    public static class PropertyMetadata {
        @JsonProperty("bedrooms")
        @Min(value = 0, message = "Bedrooms must be non-negative")
        @Schema(description = "Number of bedrooms", example = "2")
        private Integer bedrooms;

        @JsonProperty("bathrooms")
        @Min(value = 0, message = "Bathrooms must be non-negative")
        @Schema(description = "Number of bathrooms", example = "1")
        private Integer bathrooms;

        @JsonProperty("floor")
        @Min(value = 0, message = "Floor must be non-negative")
        @Schema(description = "Floor number", example = "5")
        private Integer floor;

        @JsonProperty("total_floors")
        @Min(value = 1, message = "Total floors must be at least 1")
        @Schema(description = "Total number of floors in building", example = "10")
        private Integer totalFloors;
    }
}