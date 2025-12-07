package com.smartrent.mapper.impl;

import com.smartrent.dto.request.SavedListingRequest;
import com.smartrent.dto.response.AddressResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.SavedListingResponse;
import com.smartrent.dto.response.UserCreationResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.SavedListing;
import com.smartrent.infra.repository.entity.SavedListingId;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.AddressMapper;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.SavedListingMapper;
import com.smartrent.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SavedListingMapperImpl implements SavedListingMapper {

    private final ListingMapper listingMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Override
    public SavedListing toEntity(SavedListingRequest request, String userId) {
        if (request == null || userId == null) {
            return null;
        }

        SavedListingId id = new SavedListingId(userId, request.getListingId());

        // Fetch the User entity reference
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Fetch the Listing entity reference
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + request.getListingId()));

        return SavedListing.builder()
                .id(id)
                .user(user)
                .listing(listing)
                .build();
    }

    @Override
    public SavedListingResponse toResponse(SavedListing entity) {
        if (entity == null) {
            return null;
        }

        return SavedListingResponse.builder()
                .userId(entity.getId().getUserId())
                .listingId(entity.getId().getListingId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public SavedListingResponse toResponseWithListing(SavedListing entity) {
        if (entity == null) {
            return null;
        }

        SavedListingResponse response = SavedListingResponse.builder()
                .userId(entity.getId().getUserId())
                .listingId(entity.getId().getListingId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();

        // Map listing if available
        if (entity.getListing() != null) {
            Listing listing = entity.getListing();

            // Map user
            UserCreationResponse user = null;
            if (entity.getUser() != null) {
                user = userMapper.mapFromUserEntityToUserCreationResponse(entity.getUser());
            }

            // Map address
            AddressResponse address = null;
            if (listing.getAddress() != null) {
                address = addressMapper.toResponse(listing.getAddress());
            }

            // Map listing to response
            ListingResponse listingResponse = listingMapper.toResponse(listing, user, address);
            response.setListing(listingResponse);
        }

        return response;
    }
}
