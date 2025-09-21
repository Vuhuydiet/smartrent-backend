package com.smartrent.controller.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedListingResponse {
    
    String userId;
    
    Long listingId;
    
    LocalDateTime createdAt;
    
    LocalDateTime updatedAt;
    
    // Optional: Include basic listing information for convenience
    ListingResponse listing;
}
