package com.smartrent.service.listing.impl;

import com.smartrent.controller.dto.request.ListingCreationRequest;
import com.smartrent.controller.dto.request.ListingRequest;
import com.smartrent.controller.dto.response.ListingCreationResponse;
import com.smartrent.controller.dto.response.ListingResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.service.listing.ListingService;
import com.smartrent.controller.dto.request.ListingFilterRequest;
import com.smartrent.controller.dto.response.ListingFilterResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingServiceImpl implements ListingService {

    ListingRepository listingRepository;
    ListingMapper listingMapper;

    @Override
    @Transactional
    public ListingCreationResponse createListing(ListingCreationRequest request) {
        Listing listing = listingMapper.toEntity(request);
        Listing saved = listingRepository.save(listing);
        return listingMapper.toCreationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        return listingMapper.toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsByIds(Set<Long> ids) {
        return listingRepository.findByListingIdIn(ids).stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListings(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // cap size to 100
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Listing> pageResult = listingRepository.findAll(pageable);
        return pageResult.getContent().stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ListingResponse updateListing(Long id, ListingRequest request) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        // Update fields from request (null-safe for partial update)
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getExpiryDate() != null) existing.setExpiryDate(request.getExpiryDate());
        if (request.getVerified() != null) existing.setVerified(request.getVerified());
        if (request.getIsVerify() != null) existing.setIsVerify(request.getIsVerify());
        if (request.getExpired() != null) existing.setExpired(request.getExpired());
        if (request.getPrice() != null) existing.setPrice(request.getPrice());
        if (request.getArea() != null) existing.setArea(request.getArea());
        if (request.getBedrooms() != null) existing.setBedrooms(request.getBedrooms());
        if (request.getBathrooms() != null) existing.setBathrooms(request.getBathrooms());
        if (request.getRoomCapacity() != null) existing.setRoomCapacity(request.getRoomCapacity());
        // ...add more fields as needed, null-safe
        Listing saved = listingRepository.save(existing);
        return listingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteListing(Long id) {
        if (!listingRepository.existsById(id)) {
            throw new RuntimeException("Listing not found");
        }
        listingRepository.deleteById(id);
    }

    // Filter listings (feature)
    @Override
    @Transactional(readOnly = true)
    public ListingFilterResponse filterListings(ListingFilterRequest filterRequest) {
        int safePage = Math.max(filterRequest.getPage() != null ? filterRequest.getPage() : 0, 0);
        int safeSize = Math.min(Math.max(filterRequest.getSize() != null ? filterRequest.getSize() : 20, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Listing> pageResult = listingRepository.findAll(com.smartrent.infra.repository.ListingSpecification.filter(filterRequest), pageable);
        ListingFilterResponse resp = new ListingFilterResponse();
        resp.setContent(pageResult.getContent().stream().map(listingMapper::toResponse).collect(Collectors.toList()));
        resp.setTotalElements(pageResult.getTotalElements());
        resp.setTotalPages(pageResult.getTotalPages());
        resp.setPage(safePage);
        resp.setSize(safeSize);
        return resp;
    }
}