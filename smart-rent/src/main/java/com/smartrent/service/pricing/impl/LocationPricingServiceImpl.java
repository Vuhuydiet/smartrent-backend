package com.smartrent.service.pricing.impl;

import com.smartrent.dto.response.ListingPricingInfo;
import com.smartrent.dto.response.LocationPricingResponse;
import com.smartrent.dto.response.LocationPricingStatistics;
import com.smartrent.infra.repository.AddressRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Address;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.service.pricing.LocationPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
// @Service  // DISABLED: Not currently in use, causing errors
@RequiredArgsConstructor
public class LocationPricingServiceImpl implements LocationPricingService {

    private final ListingRepository listingRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional(readOnly = true)
    public LocationPricingResponse getLocationPricing(Listing listing, Integer wardId, Integer districtId, Integer provinceId) {
        log.info("Generating location pricing for listing {} in ward {}, district {}, province {}",
                listing.getListingId(), wardId, districtId, provinceId);

        // Build response
        LocationPricingResponse.LocationPricingResponseBuilder responseBuilder = LocationPricingResponse.builder();

        // Get ward-level pricing
        if (wardId != null) {
            LocationPricingStatistics wardPricing = getPricingStatistics(
                    wardId, "WARD", listing.getProductType(), listing.getPriceUnit());
            responseBuilder.wardPricing(wardPricing);

            // Get similar listings in ward
            List<ListingPricingInfo> similarInWard = getSimilarListings(
                    wardId, "WARD", listing.getProductType(), listing.getPriceUnit(), listing.getListingId());
            responseBuilder.similarListingsInWard(similarInWard);

            // Calculate price comparison
            if (wardPricing != null && wardPricing.getAveragePrice() != null) {
                String comparison = calculatePriceComparison(listing.getPrice(), wardPricing.getAveragePrice());
                responseBuilder.priceComparison(comparison);

                Double percentageDiff = calculatePercentageDifference(listing.getPrice(), wardPricing.getAveragePrice());
                responseBuilder.percentageDifferenceFromAverage(percentageDiff);
            }
        }

        // Get district-level pricing
        if (districtId != null) {
            LocationPricingStatistics districtPricing = getPricingStatistics(
                    districtId, "DISTRICT", listing.getProductType(), listing.getPriceUnit());
            responseBuilder.districtPricing(districtPricing);

            // Get similar listings in district
            List<ListingPricingInfo> similarInDistrict = getSimilarListings(
                    districtId, "DISTRICT", listing.getProductType(), listing.getPriceUnit(), listing.getListingId());
            responseBuilder.similarListingsInDistrict(similarInDistrict);
        }

        // Get province-level pricing
        if (provinceId != null) {
            LocationPricingStatistics provincePricing = getPricingStatistics(
                    provinceId, "PROVINCE", listing.getProductType(), listing.getPriceUnit());
            responseBuilder.provincePricing(provincePricing);
        }

        return responseBuilder.build();
    }

    private LocationPricingStatistics getPricingStatistics(Integer locationId, String locationType,
                                                           Listing.ProductType productType, Listing.PriceUnit priceUnit) {
        try {
            Object[] stats = switch (locationType) {
                case "WARD" -> listingRepository.getPricingStatisticsByWard(locationId, productType, priceUnit);
                case "DISTRICT" -> listingRepository.getPricingStatisticsByDistrict(locationId, productType, priceUnit);
                case "PROVINCE" -> listingRepository.getPricingStatisticsByProvince(locationId, productType, priceUnit);
                default -> null;
            };

            if (stats == null || stats[0] == null) {
                return null;
            }

            // Parse statistics array: count, avg, min, max, avgArea, avgPricePerSqm
            Long count = ((Number) stats[0]).longValue();
            if (count == 0) {
                return null;
            }

            BigDecimal avgPrice = stats[1] != null ? new BigDecimal(stats[1].toString()) : null;
            BigDecimal minPrice = stats[2] != null ? new BigDecimal(stats[2].toString()) : null;
            BigDecimal maxPrice = stats[3] != null ? new BigDecimal(stats[3].toString()) : null;
            Double avgArea = stats[4] != null ? ((Number) stats[4]).doubleValue() : null;
            BigDecimal avgPricePerSqm = stats[5] != null ? new BigDecimal(stats[5].toString()) : null;

            // Get location name
            String locationName = getLocationName(locationId, locationType);

            return LocationPricingStatistics.builder()
                    .locationType(locationType)
                    .locationId(locationId)
                    .locationName(locationName)
                    .totalListings(count.intValue())
                    .averagePrice(avgPrice != null ? avgPrice.setScale(0, RoundingMode.HALF_UP) : null)
                    .minPrice(minPrice != null ? minPrice.setScale(0, RoundingMode.HALF_UP) : null)
                    .maxPrice(maxPrice != null ? maxPrice.setScale(0, RoundingMode.HALF_UP) : null)
                    .medianPrice(avgPrice != null ? avgPrice.setScale(0, RoundingMode.HALF_UP) : null) // Simplified - using average as median
                    .priceUnit(priceUnit.name())
                    .productType(productType.name())
                    .averageArea(avgArea)
                    .averagePricePerSqm(avgPricePerSqm != null ? avgPricePerSqm.setScale(0, RoundingMode.HALF_UP) : null)
                    .build();
        } catch (Exception e) {
            log.error("Error getting pricing statistics for {} {}: {}", locationType, locationId, e.getMessage());
            return null;
        }
    }

    private List<ListingPricingInfo> getSimilarListings(Integer locationId, String locationType,
                                                        Listing.ProductType productType, Listing.PriceUnit priceUnit,
                                                        Long excludeListingId) {
        try {
            List<Listing> listings = switch (locationType) {
                case "WARD" -> listingRepository.findByWardIdAndProductTypeAndPriceUnit(
                        locationId, productType, priceUnit, PageRequest.of(0, 10));
                case "DISTRICT" -> listingRepository.findByDistrictIdAndProductTypeAndPriceUnit(
                        locationId, productType, priceUnit, PageRequest.of(0, 10));
                default -> new ArrayList<>();
            };

            return listings.stream()
                    .filter(l -> !l.getListingId().equals(excludeListingId))
                    .map(this::convertToListingPricingInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting similar listings for {} {}: {}", locationType, locationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private ListingPricingInfo convertToListingPricingInfo(Listing listing) {
        BigDecimal pricePerSqm = null;
        if (listing.getArea() != null && listing.getArea() > 0) {
            pricePerSqm = listing.getPrice().divide(
                    BigDecimal.valueOf(listing.getArea()), 0, RoundingMode.HALF_UP);
        }

        return ListingPricingInfo.builder()
                .listingId(listing.getListingId())
                .title(listing.getTitle())
                .price(listing.getPrice())
                .priceUnit(listing.getPriceUnit().name())
                .area(listing.getArea())
                .pricePerSqm(pricePerSqm)
                .bedrooms(listing.getBedrooms())
                .bathrooms(listing.getBathrooms())
                .productType(listing.getProductType().name())
                .vipType(listing.getVipType().name())
                .verified(listing.getVerified())
                .build();
    }

    private String calculatePriceComparison(BigDecimal listingPrice, BigDecimal averagePrice) {
        if (listingPrice == null || averagePrice == null || averagePrice.compareTo(BigDecimal.ZERO) == 0) {
            return "AVERAGE";
        }

        double percentageDiff = calculatePercentageDifference(listingPrice, averagePrice);

        if (percentageDiff < -20) {
            return "WELL_BELOW_AVERAGE";
        } else if (percentageDiff < -5) {
            return "BELOW_AVERAGE";
        } else if (percentageDiff > 20) {
            return "WELL_ABOVE_AVERAGE";
        } else if (percentageDiff > 5) {
            return "ABOVE_AVERAGE";
        } else {
            return "AVERAGE";
        }
    }

    private Double calculatePercentageDifference(BigDecimal value, BigDecimal average) {
        if (value == null || average == null || average.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        BigDecimal diff = value.subtract(average);
        BigDecimal percentage = diff.divide(average, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return percentage.doubleValue();
    }

    private String getLocationName(Integer locationId, String locationType) {
        // This is a simplified version - you would fetch actual names from the database
        return locationType + " " + locationId;
    }
}