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

    private static final Duration AI_PARSE_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * Handles Natural Language Free Text Search
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchByNaturalLanguage(SearchRequest request, Pageable pageable) {
        log.info("Processing natural language search query: {}", request.getQuery());
        
        // 1. Send natural language query to AI Server to get structured JSON
        AiParsedCriteriaDto criteria = parseWithCache(request);

        // 2. Build dynamic JPA query from AI structured output
        org.springframework.data.jpa.domain.Specification<Listing> spec = ListingSpecification.matchesCriteria(criteria);

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
            if (criteria == null) {
                criteria = new AiParsedCriteriaDto();
                criteria.setKeyword(query);
            }
            log.info("AI parsed criteria: {}", criteria);
        } catch (Exception e) {
            log.error("Failed to parse query via AI Server. Falling back to FULLTEXT search.", e);
            criteria = new AiParsedCriteriaDto();
            criteria.setKeyword(query);
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
