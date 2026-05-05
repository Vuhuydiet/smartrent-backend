package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
public class ListingVerificationRequest {
    @JsonProperty("listing_id")
    private String listingId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("area")
    private Float area;

    @JsonProperty("address")
    private String address;

    @JsonProperty("property_type")
    private String propertyType;

    @JsonProperty("amenities")
    private List<String> amenities;

    @JsonProperty("images")
    private List<String> images;

    @JsonProperty("videos")
    private List<VideoDto> videos;

    @JsonProperty("metadata")
    private MetadataDto metadata;

    @Data
    @Builder
    public static class VideoDto {
        @JsonProperty("url")
        private String url;

        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;

        @JsonProperty("duration")
        private Float duration;
    }

    @Data
    @Builder
    public static class MetadataDto {
        @JsonProperty("bedrooms")
        private Integer bedrooms;

        @JsonProperty("bathrooms")
        private Integer bathrooms;

        @JsonProperty("floor")
        private String floor;
    }
}
