package com.smartrent.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

public class AIRecommendationRequest {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ListingFeatureDto {
        @JsonProperty("listing_id")
        Long listingId;
        
        @JsonProperty("product_type")
        String productType;
        
        @JsonProperty("listing_type")
        String listingType;
        
        @JsonProperty("price")
        Double price;
        
        @JsonProperty("area")
        Double area;
        
        @JsonProperty("bedrooms")
        Integer bedrooms;
        
        @JsonProperty("province_code")
        String provinceCode;
        
        @JsonProperty("district_id")
        Integer districtId;
        
        @JsonProperty("vip_type")
        String vipType;
        
        @JsonProperty("post_date_days_ago")
        Integer postDateDaysAgo;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InteractionEntryDto {
        @JsonProperty("user_id")
        String userId;
        
        @JsonProperty("listing_id")
        Long listingId;
        
        @JsonProperty("weight")
        Double weight;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SimilarListingAiRequest {
        @JsonProperty("target")
        ListingFeatureDto target;
        
        @JsonProperty("candidates")
        List<ListingFeatureDto> candidates;
        
        @JsonProperty("top_n")
        Integer top_n;
        
        @JsonProperty("alpha")
        Double alpha;

        @JsonProperty("user_interactions")
        List<InteractionEntryDto> userInteractions; // Optional: null for anonymous users

        @JsonProperty("interaction_features")
        List<ListingFeatureDto> interactionFeatures;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PersonalizedFeedAiRequest {
        @JsonProperty("user_id")
        String user_id;
        
        @JsonProperty("user_interactions")
        List<InteractionEntryDto> user_interactions;
        
        @JsonProperty("all_interactions")
        List<InteractionEntryDto> all_interactions;
        
        @JsonProperty("candidates")
        List<ListingFeatureDto> candidates;
        
        @JsonProperty("top_n")
        Integer top_n;
        
        @JsonProperty("alpha")
        Double alpha;

        @JsonProperty("interaction_features")
        List<ListingFeatureDto> interactionFeatures;
    }
}
