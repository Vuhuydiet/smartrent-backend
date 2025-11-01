package com.smartrent.service.housing.impl;

import com.smartrent.dto.request.HousingPredictorRequest;
import com.smartrent.dto.response.HousingPredictorResponse;
import com.smartrent.service.housing.HousingPredictorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Mock implementation of HousingPredictorService for development/testing
 * Activated when application.housing-predictor.mock.enabled=true
 */
@Service
@ConditionalOnProperty(name = "application.housing-predictor.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockHousingPredictorServiceImpl implements HousingPredictorService {

    private final Random random = new Random();

    @Override
    public HousingPredictorResponse predictPrice(HousingPredictorRequest request) {
        log.info("Using mock housing predictor service for development");
        
        // Simulate some basic price calculation logic
        double basePrice = calculateMockPrice(request);
        double confidence = 0.85 + random.nextDouble() * 0.1; // 0.85-0.95
        
        // Generate price range (±10% of predicted price)
        double minPrice = basePrice * 0.9;
        double maxPrice = basePrice * 1.1;
        
        // Price per square meter
        double pricePerSqm = basePrice / request.getArea();
        
        // Generate mock metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("prediction_method", "mock");
        metadata.put("calculation_time_ms", 10);
        metadata.put("location", request.getLatitude() + "," + request.getLongitude());
        
        // Create price range
        HousingPredictorResponse.PriceRange priceRange = HousingPredictorResponse.PriceRange.builder()
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .build();
        
        // Create address string
        String address = request.getDistrict() + ", " + request.getCity();
        
        return HousingPredictorResponse.builder()
                .address(address)
                .propertyType(request.getPropertyType())
                .predictedPrice(basePrice)
                .priceRange(priceRange)
                .confidence(confidence)
                .currency("VND_millions")
                .timestamp(LocalDateTime.now())
                .modelVersion("mock-v1.0.0")
                .metadata(metadata)
                .build();
    }

    @Override
    public boolean checkHealth() {
        log.info("Mock AI service health check - always healthy");
        return true;
    }

    private double getBasePriceByCityMock(String city, String propertyType) {
        log.debug("Getting base price for city: '{}', propertyType: '{}'", city, propertyType);
        
        // Add null safety for parameters
        String safeCity = city != null ? city : "Unknown";
        String safePropertyType = propertyType != null ? propertyType.toUpperCase() : "APARTMENT";
        
        // Base prices in VND millions based on city and property type
        Map<String, Double> cityBasePrices = Map.of(
            "Ho Chi Minh City", 15000.0,
            "Hanoi", 12000.0,
            "Da Nang", 8000.0,
            "Can Tho", 6000.0,
            "Hai Phong", 7000.0,
            "Bien Hoa", 5500.0,
            "Nha Trang", 7500.0
        );
        
        // Property type multipliers
        Map<String, Double> typeMultipliers = Map.of(
            "APARTMENT", 1.0,
            "HOUSE", 1.3,
            "TOWNHOUSE", 1.2,
            "VILLA", 2.0
        );
        
        double basePrice = cityBasePrices.getOrDefault(safeCity, 8000.0);
        double typeMultiplier = typeMultipliers.getOrDefault(safePropertyType, 1.0);
        
        log.debug("Calculated basePrice: {}, typeMultiplier: {}", basePrice, typeMultiplier);
        return basePrice * typeMultiplier;
    }

    private double calculateMockPrice(HousingPredictorRequest request) {
        // Base price varies by city and property type (in VND millions)
        double basePrice = getBasePriceByCityMock(request.getCity(), request.getPropertyType());
        
        // Add some location-based variation using coordinates
        double locationFactor = 1.0 + (Math.sin(request.getLatitude()) + Math.cos(request.getLongitude())) * 0.1;
        basePrice *= locationFactor;
        
        // Property type factor
        double price = basePrice;
        double typeMultiplier = getPropertyTypeMultiplier(request.getPropertyType());
        price *= typeMultiplier;
        
        // Area factor - smaller properties have higher price per sqm
        if (request.getArea() != null) {
            if (request.getArea() < 30) {
                price *= 1.2; // 20% premium for small properties
            } else if (request.getArea() > 100) {
                price *= 0.9; // 10% discount for large properties
            }
        }
        
        // Add some randomness for mock data
        double randomFactor = 0.9 + (random.nextDouble() * 0.2); // ±10% variation
        price *= randomFactor;
        
        return Math.round(price);
    }

    private double getLocationMultiplier(String district) {
        if (district == null) return 1.0;
        
        Map<String, Double> districtMultipliers = Map.of(
            "District 1", 2.5,
            "District 2", 1.8,
            "District 3", 2.0,
            "District 7", 1.6,
            "Binh Thanh", 1.4,
            "Thu Duc", 1.2
        );
        
        return districtMultipliers.getOrDefault(district, 1.0);
    }

    private double getPropertyTypeMultiplier(String propertyType) {
        if (propertyType == null) return 1.0;
        
        return switch (propertyType) {
            case "House" -> 1.2;
            case "Apartment" -> 1.0;
            case "Office" -> 0.9;
            case "Room" -> 0.8;
            default -> 1.0;
        };
    }



    private double generateConfidence() {
        // Generate confidence between 0.7 and 0.95
        double confidence = 0.7 + (random.nextDouble() * 0.25);
        return Math.round(confidence * 100.0) / 100.0; // Round to 2 decimal places
    }

    private String getRandomMarketTrend() {
        String[] trends = {"RISING", "STABLE", "DECLINING"};
        return trends[random.nextInt(trends.length)];
    }

    private double generateLocationScore(HousingPredictorRequest request) {
        // Generate score between 6.0 and 9.5 based on district
        double baseScore = 7.0;
        if ("District 1".equals(request.getDistrict()) || "District 2".equals(request.getDistrict())) {
            baseScore = 8.5;
        } else if ("District 3".equals(request.getDistrict()) || "District 7".equals(request.getDistrict())) {
            baseScore = 8.0;
        }
        
        double variation = (random.nextDouble() - 0.5) * 1.0; // ±0.5 variation
        double finalScore = Math.max(6.0, Math.min(9.5, baseScore + variation));
        
        return Math.round(finalScore * 10.0) / 10.0; // Round to 1 decimal place
    }

    private double generatePropertyScore(HousingPredictorRequest request) {
        double baseScore = 7.0;
        
        // Adjust based on property type
        if ("Apartment".equals(request.getPropertyType())) {
            baseScore += 0.5;
        } else if ("House".equals(request.getPropertyType())) {
            baseScore += 1.0;
        } else if ("Room".equals(request.getPropertyType())) {
            baseScore -= 0.5;
        }
        
        // Adjust based on area
        if (request.getArea() != null) {
            if (request.getArea() > 100) {
                baseScore += 0.8;
            } else if (request.getArea() < 30) {
                baseScore -= 0.3;
            }
        }
        
        double variation = (random.nextDouble() - 0.5) * 1.0;
        double finalScore = Math.max(5.0, Math.min(9.5, baseScore + variation));
        
        return Math.round(finalScore * 10.0) / 10.0; // Round to 1 decimal place
    }

    private Map<String, Object> generateMockInsights(HousingPredictorRequest request) {
        Map<String, Object> insights = new HashMap<>();
        insights.put("marketAnalysis", "Properties in " + request.getDistrict() + " are showing steady demand");
        insights.put("priceGrowthRate", "5.2% annually");
        insights.put("demandLevel", "High");
        insights.put("investmentPotential", "Good");
        insights.put("nearbyAmenities", Map.of(
            "schools", 3,
            "hospitals", 2,
            "shopping", 5,
            "transportation", 4
        ));
        return insights;
    }

    private Map<String, Object> generateMockComparables() {
        Map<String, Object> comparables = new HashMap<>();
        comparables.put("count", 5);
        comparables.put("avgPricePerSqm", "285000000");
        comparables.put("priceRange", Map.of(
            "min", "12000000000",
            "max", "18000000000"
        ));
        return comparables;
    }

    private Map<String, Double> generateInfluencingFactors(HousingPredictorRequest request) {
        Map<String, Double> factors = new HashMap<>();
        factors.put("location", 0.35);
        factors.put("size", 0.25);
        factors.put("condition", 0.15);
        factors.put("amenities", 0.12);
        factors.put("marketTrends", 0.08);
        factors.put("otherFactors", 0.05);
        return factors;
    }
}