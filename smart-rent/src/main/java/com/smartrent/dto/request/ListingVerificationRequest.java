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

    @JsonProperty("price_unit")
    private String priceUnit;

    @JsonProperty("listing_type")
    private String listingType;

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

    @JsonProperty("direction")
    private String direction;

    @JsonProperty("furnishing")
    private String furnishing;

    @JsonProperty("room_capacity")
    private Integer roomCapacity;

    @JsonProperty("water_price")
    private String waterPrice;

    @JsonProperty("electricity_price")
    private String electricityPrice;

    @JsonProperty("internet_price")
    private String internetPrice;

    @JsonProperty("service_fee")
    private String serviceFee;

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
