package com.smartrent.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.request.AiParsedCriteriaDto;
import com.smartrent.dto.request.SearchRequest;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.infra.client.AiServerClient;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.specification.ListingSpecification;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.mapper.AddressMapper;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.UserMapper;
import com.smartrent.service.discovery.AmenityResolver;
import com.smartrent.util.SearchQueryParser;
import com.smartrent.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSearchService {

    private final AiServerClient aiServerClient;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AmenityResolver amenityResolver;

    private static final Duration AI_PARSE_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Handles Natural Language Free Text Search
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchByNaturalLanguage(SearchRequest request, Pageable pageable) {
        log.info("Processing natural language search query: {}", request.getQuery());
        
        // 1. Send natural language query to AI Server to get structured JSON
        AiParsedCriteriaDto criteria = parseWithCache(request);

        // 1b. Resolve the colloquial amenity phrases ("máy lạnh", "wifi", …)
        //     to canonical amenity ids so they filter by id instead of the
        //     LIKE on amenity.name that never matched the stored canonical name.
        AmenityResolver.Resolved amenities = amenityResolver.resolve(criteria.getAmenities());

        // 2. Build dynamic JPA query from AI structured output
        org.springframework.data.jpa.domain.Specification<Listing> spec =
                ListingSpecification.matchesCriteria(
                        criteria, amenities.amenityIds(), amenities.unresolved());

        // 3. Execute query
        Page<Listing> listings = listingRepository.findAll(spec, pageable);
        
        // 4. Map to response
        List<ListingResponse> mappedListings = listings.getContent().stream().map(listing -> {
            User userEntity = listing.getUserId() != null 
                ? userRepository.findById(listing.getUserId()).orElse(null) : null;
            com.smartrent.dto.response.UserCreationResponse user = userEntity != null 
                ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity) : null;
            com.smartrent.dto.response.AddressResponse address = listing.getAddress() != null 
                ? addressMapper.toResponse(listing.getAddress()) : null;
            
            return listingMapper.toResponse(listing, user, address);
        }).collect(Collectors.toList());
        
        return new PageImpl<>(mappedListings, pageable, listings.getTotalElements());
    }

    /**
     * No-AI fallback: derive structured filters locally instead of dumping the
     * whole raw query into the keyword field. Dumping the raw query meant every
     * token (including "dưới", "5tr") was ANDed into the FULLTEXT match, which
     * matched zero listings. The local parser extracts price / type / location
     * so the search still returns relevant results when the AI server is down.
     */
    private AiParsedCriteriaDto localParse(String query) {
        SearchQueryParser.ParsedQuery p = SearchQueryParser.parse(query);
        AiParsedCriteriaDto c = new AiParsedCriteriaDto();
        c.setPropertyType(p.productType());
        c.setListingType(p.listingType());
        c.setMinPrice(p.minPrice());
        c.setMaxPrice(p.maxPrice());
        c.setMinArea(p.minArea());
        c.setMaxArea(p.maxArea());
        c.setBedrooms(p.bedrooms());
        if (p.locationText() != null && !p.locationText().isBlank()) {
            c.setDistrict(p.locationText());
        }
        if (p.amenities() != null && !p.amenities().isEmpty()) {
            c.setAmenities(p.amenities());
        }
        if (!p.hasStructuredFilter()) {
            // Nothing recognised — keep the legacy behaviour so at least a
            // FULLTEXT keyword search runs.
            c.setKeyword(query);
        }
        return c;
    }

    private boolean isEmptyCriteria(AiParsedCriteriaDto c) {
        return c.getPropertyType() == null && c.getListingType() == null
                && c.getMinPrice() == null && c.getMaxPrice() == null
                && c.getMinArea() == null && c.getMaxArea() == null && c.getBedrooms() == null
                && c.getDistrict() == null && c.getProvince() == null && c.getWard() == null
                && (c.getAmenities() == null || c.getAmenities().isEmpty())
                && (c.getKeyword() == null || c.getKeyword().isBlank())
                && (c.getPhoneticKeyword() == null || c.getPhoneticKeyword().isBlank());
    }

    private AiParsedCriteriaDto parseWithCache(SearchRequest request) {
        String query = request != null ? request.getQuery() : null;
        String normalized = TextNormalizer.normalize(query);
        String cacheKey = normalized == null ? null : "search:ai:parse:" + Integer.toHexString(normalized.hashCode());

        if (cacheKey != null) {
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    return objectMapper.readValue(cached, AiParsedCriteriaDto.class);
                }
            } catch (Exception e) {
                log.debug("AI parse cache read skipped: {}", e.getMessage());
            }
        }

        AiParsedCriteriaDto criteria;
        try {
            criteria = aiServerClient.parseNaturalLanguage(request);
            if (criteria == null || isEmptyCriteria(criteria)) {
                criteria = localParse(query);
            }
            log.info("AI parsed criteria: {}", criteria);
        } catch (Exception e) {
            log.error("Failed to parse query via AI Server. Falling back to local parser.", e);
            criteria = localParse(query);
        }

        if (cacheKey != null && criteria != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(criteria), AI_PARSE_CACHE_TTL);
            } catch (Exception e) {
                log.debug("AI parse cache write skipped: {}", e.getMessage());
            }
        }

        return criteria;
    }
}
