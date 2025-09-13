package com.smartrent.mapper.impl;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.response.ListingResponse;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.ListingMapper;
import org.springframework.stereotype.Component;

@Component
public class ListingMapperImpl implements ListingMapper {
    @Override
    public Listing toEntity(ListingCreationRequest req) {
        return Listing.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .userId(req.getUserId())
                .expiryDate(req.getExpiryDate())
                .listingType(req.getListingType() != null ? Listing.ListingType.valueOf(req.getListingType()) : null)
                .verified(req.getVerified())
                .expired(req.getExpired())
                .vipType(req.getVipType() != null ? Listing.VipType.valueOf(req.getVipType()) : null)
                .categoryId(req.getCategoryId())
                .productType(req.getProductType() != null ? Listing.ProductType.valueOf(req.getProductType()) : null)
                .price(req.getPrice())
                .priceUnit(req.getPriceUnit() != null ? Listing.PriceUnit.valueOf(req.getPriceUnit()) : null)
                .addressId(req.getAddressId())
                .area(req.getArea())
                .bedrooms(req.getBedrooms())
                .bathrooms(req.getBathrooms())
                .direction(req.getDirection() != null ? Listing.Direction.valueOf(req.getDirection()) : null)
                .furnishing(req.getFurnishing() != null ? Listing.Furnishing.valueOf(req.getFurnishing()) : null)
                .propertyType(req.getPropertyType() != null ? Listing.PropertyType.valueOf(req.getPropertyType()) : null)
                .roomCapacity(req.getRoomCapacity())
                .build();
    }

    @Override
    public ListingResponse toResponse(Listing entity) {
        return ListingResponse.builder()
                .listingId(entity.getListingId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .userId(entity.getUserId())
                .postDate(entity.getPostDate())
                .expiryDate(entity.getExpiryDate())
                .listingType(entity.getListingType() != null ? entity.getListingType().name() : null)
                .verified(entity.getVerified())
                .expired(entity.getExpired())
                .vipType(entity.getVipType() != null ? entity.getVipType().name() : null)
                .categoryId(entity.getCategoryId())
                .productType(entity.getProductType() != null ? entity.getProductType().name() : null)
                .price(entity.getPrice())
                .priceUnit(entity.getPriceUnit() != null ? entity.getPriceUnit().name() : null)
                .addressId(entity.getAddressId())
                .area(entity.getArea())
                .bedrooms(entity.getBedrooms())
                .bathrooms(entity.getBathrooms())
                .direction(entity.getDirection() != null ? entity.getDirection().name() : null)
                .furnishing(entity.getFurnishing() != null ? entity.getFurnishing().name() : null)
                .propertyType(entity.getPropertyType() != null ? entity.getPropertyType().name() : null)
                .roomCapacity(entity.getRoomCapacity())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public ListingCreationResponse toCreationResponse(Listing entity) {
        return ListingCreationResponse.builder()
                .listingId(entity.getListingId())
                .status("CREATED")
                .build();
    }
}
