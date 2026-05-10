package com.smartrent.service.ai;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Handles Natural Language Free Text Search
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> searchByNaturalLanguage(SearchRequest request, Pageable pageable) {
        log.info("Processing natural language search query: {}", request.getQuery());
        
        // 1. Send natural language query to AI Server to get structured JSON
        AiParsedCriteriaDto criteria;
        try {
            criteria = aiServerClient.parseNaturalLanguage(request);
            log.info("AI parsed criteria: {}", criteria);
        } catch (Exception e) {
            log.error("Failed to parse query via AI Server. Falling back to FULLTEXT search.", e);
            // Fallback: If AI fails, use the raw query as a FULLTEXT search keyword
            criteria = new AiParsedCriteriaDto();
            criteria.setKeyword(request.getQuery());
        }

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
}
