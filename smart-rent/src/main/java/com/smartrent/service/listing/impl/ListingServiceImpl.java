package com.smartrent.service.listing.impl;

import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.infra.exception.InsufficientPermissionsException;
import com.smartrent.infra.exception.PhoneNotVerifiedException;
import com.smartrent.dto.request.ImageRequest;
import com.smartrent.dto.request.VideoRequest;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.entity.Image;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Video;
import com.smartrent.mapper.ImageMapper;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.mapper.VideoMapper;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.pricing.PricingHistoryService;
import com.smartrent.service.user.UserVerificationService;
import com.smartrent.utility.AuthorizationUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    UserVerificationService userVerificationService;
    PricingHistoryService pricingHistoryService;
    ImageMapper imageMapper;
    VideoMapper videoMapper;

    @Override
    @Transactional
    public ListingCreationResponse createListing(ListingCreationRequest request) {
        String currentUserId = AuthorizationUtil.getCurrentUserId();

        // Check phone verification status using authentication context
        if (!userVerificationService.isPhoneActive()) {
            throw new PhoneNotVerifiedException("User's phone number must be verified before creating listings");
        }

        log.info("Creating listing for user: {}", currentUserId);
        Listing listing = listingMapper.toEntity(request);
        Listing saved = listingRepository.save(listing);

        // Add images if provided
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (ImageRequest imgReq : request.getImages()) {
                Image image = imageMapper.toEntity(imgReq);
                image.setListing(saved);
                images.add(image);
            }
            saved.setImages(images);
            log.info("Added {} images to listing: {}", images.size(), saved.getListingId());
        }

        // Add videos if provided
        if (request.getVideos() != null && !request.getVideos().isEmpty()) {
            List<Video> videos = new ArrayList<>();
            for (VideoRequest vidReq : request.getVideos()) {
                Video video = videoMapper.toEntity(vidReq);
                video.setListing(saved);
                videos.add(video);
            }
            saved.setVideos(videos);
            log.info("Added {} videos to listing: {}", videos.size(), saved.getListingId());
        }

        // Save again to persist images and videos
        if ((request.getImages() != null && !request.getImages().isEmpty()) ||
            (request.getVideos() != null && !request.getVideos().isEmpty())) {
            saved = listingRepository.save(saved);
        }

        // Create initial pricing history
        pricingHistoryService.createInitialPricing(
                saved.getListingId(),
                saved.getPrice(),
                saved.getPriceUnit() != null ? saved.getPriceUnit().name() : null,
                currentUserId
        );
        log.info("Initial pricing history created for listing: {}", saved.getListingId());

        return listingMapper.toCreationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        // Authorization check: Admin can view all, users can view all (as per requirement)
        // Note: GET operations are allowed for all users to view all listings
        
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

        // Authorization check: Admin can update any listing, users can only update their own
        if (!AuthorizationUtil.canAccessListing(existing.getUserId())) {
            throw new InsufficientPermissionsException("You can only update your own listings");
        }

        log.info("Updating listing {} by user: {}", id, AuthorizationUtil.getCurrentUserId());

        // Track price changes before updating
        BigDecimal oldPrice = existing.getPrice();
        Listing.PriceUnit oldPriceUnit = existing.getPriceUnit();
        boolean priceChanged = false;

        // Handle status field restriction: only admins can modify status
        if (request.getStatus() != null) {
            if (!AuthorizationUtil.canModifyListingStatus()) {
                log.warn("Non-admin user attempted to modify listing status. Ignoring status change.");
                // Ignore status change for non-admin users
                // Alternative: throw new InsufficientPermissionsException("Only administrators can modify listing status");
            } else {
                // Admin can modify status
                existing.setStatus(Listing.ListingStatus.valueOf(request.getStatus()));
                log.info("Admin updated listing {} status to: {}", id, request.getStatus());
            }
        }

        // Update other fields from request (null-safe for partial update)
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getExpiryDate() != null) existing.setExpiryDate(request.getExpiryDate());
        if (request.getListingType() != null) existing.setListingType(Listing.ListingType.valueOf(request.getListingType()));
        if (request.getVerified() != null) existing.setVerified(request.getVerified());
        if (request.getIsVerify() != null) existing.setIsVerify(request.getIsVerify());
        if (request.getExpired() != null) existing.setExpired(request.getExpired());
        if (request.getVipType() != null) existing.setVipType(Listing.VipType.valueOf(request.getVipType()));
        if (request.getProductType() != null) existing.setProductType(Listing.ProductType.valueOf(request.getProductType()));

        // Check if price or price unit changed
        if (request.getPrice() != null && !request.getPrice().equals(oldPrice)) {
            existing.setPrice(request.getPrice());
            priceChanged = true;
        }
        if (request.getPriceUnit() != null && !request.getPriceUnit().equals(oldPriceUnit.name())) {
            existing.setPriceUnit(Listing.PriceUnit.valueOf(request.getPriceUnit()));
            priceChanged = true;
        }

        if (request.getArea() != null) existing.setArea(request.getArea());
        if (request.getBedrooms() != null) existing.setBedrooms(request.getBedrooms());
        if (request.getBathrooms() != null) existing.setBathrooms(request.getBathrooms());
        if (request.getDirection() != null) existing.setDirection(Listing.Direction.valueOf(request.getDirection()));
        if (request.getFurnishing() != null) existing.setFurnishing(Listing.Furnishing.valueOf(request.getFurnishing()));
        if (request.getPropertyType() != null) existing.setPropertyType(Listing.PropertyType.valueOf(request.getPropertyType()));
        if (request.getRoomCapacity() != null) existing.setRoomCapacity(request.getRoomCapacity());

        // Update images if provided
        if (request.getImages() != null) {
            // Clear existing images
            if (existing.getImages() != null) {
                existing.getImages().clear();
            }
            // Add new images
            if (!request.getImages().isEmpty()) {
                List<Image> images = new ArrayList<>();
                for (ImageRequest imgReq : request.getImages()) {
                    Image image = imageMapper.toEntity(imgReq);
                    image.setListing(existing);
                    images.add(image);
                }
                existing.setImages(images);
                log.info("Updated {} images for listing: {}", images.size(), id);
            }
        }

        // Update videos if provided
        if (request.getVideos() != null) {
            // Clear existing videos
            if (existing.getVideos() != null) {
                existing.getVideos().clear();
            }
            // Add new videos
            if (!request.getVideos().isEmpty()) {
                List<Video> videos = new ArrayList<>();
                for (VideoRequest vidReq : request.getVideos()) {
                    Video video = videoMapper.toEntity(vidReq);
                    video.setListing(existing);
                    videos.add(video);
                }
                existing.setVideos(videos);
                log.info("Updated {} videos for listing: {}", videos.size(), id);
            }
        }

        Listing saved = listingRepository.save(existing);

        // Record price change if price or price unit changed
        if (priceChanged) {
            String currentUserId = AuthorizationUtil.getCurrentUserId();
            com.smartrent.dto.request.PriceUpdateRequest priceUpdateRequest =
                com.smartrent.dto.request.PriceUpdateRequest.builder()
                    .newPrice(saved.getPrice())
                    .build();
            pricingHistoryService.updatePrice(
                    saved.getListingId(),
                    priceUpdateRequest,
                    currentUserId
            );
            log.info("Price change recorded for listing: {}", id);
        }

        return listingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteListing(Long id) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        
        // Authorization check: Admin can delete any listing, users can only delete their own
        if (!AuthorizationUtil.canAccessListing(existing.getUserId())) {
            throw new InsufficientPermissionsException("You can only delete your own listings");
        }
        
        log.info("Deleting listing {} by user: {}", id, AuthorizationUtil.getCurrentUserId());
        listingRepository.deleteById(id);
    }
}