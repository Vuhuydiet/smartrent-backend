package com.smartrent.service.listing.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MyListingsFilterRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.enums.PostSource;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AddressRepository;
import com.smartrent.infra.repository.AddressMetadataRepository;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.VipTierDetailRepository;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.infra.repository.specification.ListingSpecification;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.listing.cache.ListingRequestCacheService;
import com.smartrent.service.pricing.LocationPricingService;
import com.smartrent.service.quota.QuotaService;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.transaction.TransactionService;
import com.smartrent.service.address.AddressCreationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingServiceImpl implements ListingService {

    ListingRepository listingRepository;
    MediaRepository mediaRepository;
    AddressRepository addressRepository;
    AddressMetadataRepository addressMetadataRepository;
    AdminRepository adminRepository;
    UserRepository userRepository;
    ListingMapper listingMapper;
    LocationPricingService locationPricingService;
    QuotaService quotaService;
    TransactionService transactionService;
    TransactionRepository transactionRepository;
    VipTierDetailRepository vipTierDetailRepository;
    PaymentService paymentService;
    AddressCreationService addressCreationService;
    ListingRequestCacheService listingRequestCacheService;

    @Override
    @Transactional
    public ListingCreationResponse createListing(ListingCreationRequest request) {
        log.info("Creating listing with address - User: {}, Title: {}, IsDraft: {}",
                request.getUserId(), request.getTitle(), request.getIsDraft());

        // For draft listings, skip validation and payment flow
        if (Boolean.TRUE.equals(request.getIsDraft())) {
            return createDraftListing(request);
        }

        // Validate required fields for non-draft listings
        validateNonDraftListing(request);

        // Check if this is a NORMAL listing with duration days requiring payment
        if (request.getDurationDays() != null && !Boolean.TRUE.equals(request.getUseMembershipQuota())) {
            log.info("NORMAL listing requires payment - initiating payment flow");

            // Validate duration days
            if (request.getDurationDays() <= 0) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Duration days must be greater than 0");
            }

            // Get VIP tier to calculate price (default to NORMAL if not specified)
            String vipType = request.getVipType() != null ? request.getVipType() : "NORMAL";
            VipTierDetail vipTier = vipTierDetailRepository.findByTierCode(vipType)
                    .orElseThrow(() -> new AppException(DomainCode.RESOURCE_NOT_FOUND,
                            "VIP tier not found: " + vipType));

            // Calculate price based on VIP tier and duration
            java.math.BigDecimal amount = vipTier.getPriceForDuration(request.getDurationDays());

            // Create PENDING transaction
            String transactionId = transactionService.createPostFeeTransaction(
                    request.getUserId(),
                    amount,
                    vipType,
                    request.getDurationDays(),
                    request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
            );

            // Cache the listing request in Redis (30 min TTL)
            listingRequestCacheService.storeNormalListingRequest(transactionId, request);
            log.info("Cached {} listing request for transaction: {}", vipType, transactionId);

            // Generate payment URL using PaymentService
            PaymentRequest paymentRequest = PaymentRequest.builder()
                    .transactionId(transactionId) // Reuse the transaction created above
                    .provider(com.smartrent.enums.PaymentProvider.valueOf(
                            request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                    .amount(amount)
                    .currency(PricingConstants.DEFAULT_CURRENCY)
                    .orderInfo(vipType + " Listing - " + request.getDurationDays() + " days: " + request.getTitle())
                    .build();

            PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

            log.info("Payment URL generated for {} listing transaction: {}", vipType, transactionId);

            // Return payment response
            return ListingCreationResponse.builder()
                    .paymentRequired(true)
                    .transactionId(transactionId)
                    .amount(amount.longValue())
                    .paymentUrl(paymentResponse.getPaymentUrl())
                    .message("Payment required. Complete payment to create listing.")
                    .build();
        }

        // Original flow for listings without payment requirement
        // Create address first (within the same transaction)
        Address address = createAddress(request.getAddress());

        // Create listing and set the address
        Listing listing = listingMapper.toEntity(request);
        listing.setAddress(address);
        // Set default values for required fields
        if (listing.getExpired() == null) {
            listing.setExpired(false);
        }

        // Save listing
        Listing saved = listingRepository.save(listing);
        log.info("Listing created successfully with id: {} and address id: {}",
                saved.getListingId(), address.getAddressId());

        // Link media to listing if provided (within same transaction)
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            linkMediaToListing(saved, request.getMediaIds(), request.getUserId());
            log.info("Linked {} media items to listing {}", request.getMediaIds().size(), saved.getListingId());
        }

        return listingMapper.toCreationResponse(saved);
    }

    @Override
    @Transactional
    public Object createVipListing(VipListingCreationRequest request) {
        log.info("Creating VIP listing for user: {}, vipType: {}", request.getUserId(), request.getVipType());

        // Determine benefit type based on vipType
        BenefitType benefitType = switch (request.getVipType().toUpperCase()) {
            case "SILVER" -> BenefitType.POST_SILVER;
            case "GOLD" -> BenefitType.POST_GOLD;
            case "DIAMOND" -> BenefitType.POST_DIAMOND;
            default -> BenefitType.POST_SILVER;
        };

        // Check if user wants to use quota and has quota available
        boolean useQuota = Boolean.TRUE.equals(request.getUseMembershipQuota());

        if (useQuota) {
            // Check quota availability
            var quotaStatus = quotaService.checkQuotaAvailability(request.getUserId(), benefitType);

            if (quotaStatus.getTotalAvailable() > 0) {
                // Use quota - create listing immediately
                log.info("Using quota for VIP listing creation");

                // Consume quota
                boolean consumed = quotaService.consumeQuota(request.getUserId(), benefitType, 1);
                if (!consumed) {
                    throw new RuntimeException("Failed to consume quota");
                }

                // Create address first (within the same transaction)
                Address address = createAddress(request.getAddress());

                // Create listing with postSource = QUOTA
                Listing listing = buildListingFromVipRequest(request, address);
                listing.setPostSource(PostSource.QUOTA);
                listing.setTransactionId(null);

                // Check if user has AUTO_APPROVE benefit
                // TODO: Implement auto-verification check

                Listing saved = listingRepository.save(listing);

                // Link media to listing if provided (within same transaction)
                if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
                    linkMediaToListing(saved, request.getMediaIds(), request.getUserId());
                    log.info("Linked {} media items to VIP listing {}", request.getMediaIds().size(), saved.getListingId());
                }

                // If Diamond, create shadow NORMAL listing
                if ("DIAMOND".equalsIgnoreCase(request.getVipType())) {
                    createShadowListing(saved);
                }

                return listingMapper.toCreationResponse(saved);
            }
        }

        // No quota or user chose not to use quota - require payment
        log.info("No quota available or user chose payment - initiating payment flow");

        // Calculate price based on vipType and duration
        int durationDays = request.getDurationDays() != null ? request.getDurationDays() : 30;
        java.math.BigDecimal amount = switch (request.getVipType()) {
            case "SILVER" -> PricingConstants.calculateSilverPostPrice(durationDays);
            case "GOLD" -> PricingConstants.calculateGoldPostPrice(durationDays);
            case "DIAMOND" -> PricingConstants.calculateDiamondPostPrice(durationDays);
            default -> PricingConstants.calculateNormalPostPrice(durationDays);
        };

        // Create PENDING transaction
        String transactionId = transactionService.createPostFeeTransaction(
                request.getUserId(),
                amount,
                request.getVipType(),
                durationDays,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        // Store listing request data in transaction metadata (for later creation)
        // For now, we'll need to pass the request data through the callback
        // TODO: Consider storing request data in a temporary table or cache

        // Generate payment URL - pass transactionId to reuse existing transaction
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId) // Reuse the transaction created above
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(amount)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("Post " + request.getVipType() + " listing: " + request.getTitle())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for VIP listing transaction: {}", transactionId);
        return paymentResponse;
    }

    @Override
    @Transactional
    public ListingCreationResponse completeVipListingCreation(String transactionId) {
        log.info("Completing VIP listing creation for transaction: {}", transactionId);

        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (!transaction.isPostFee()) {
            throw new RuntimeException("Transaction is not a post fee: " + transactionId);
        }

        // TODO: Retrieve listing request data from cache/temp table
        // For now, this method needs to be called with the listing data
        // This is a limitation that should be addressed by storing request data

        throw new RuntimeException("Complete VIP listing creation requires listing data - not yet implemented");
    }

    private Listing buildListingFromVipRequest(VipListingCreationRequest request, Address address) {
        return Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(request.getUserId())
                .listingType(Listing.ListingType.valueOf(request.getListingType()))
                .vipType(Listing.VipType.valueOf(request.getVipType()))
                .categoryId(request.getCategoryId())
                .productType(Listing.ProductType.valueOf(request.getProductType()))
                .price(request.getPrice())
                .priceUnit(Listing.PriceUnit.valueOf(request.getPriceUnit()))
                .address(address)  // Use Address entity instead of addressId
                .area(request.getArea())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .direction(request.getDirection() != null ? Listing.Direction.valueOf(request.getDirection()) : null)
                .furnishing(request.getFurnishing() != null ? Listing.Furnishing.valueOf(request.getFurnishing()) : null)
                .propertyType(request.getPropertyType() != null ? Listing.PropertyType.valueOf(request.getPropertyType()) : null)
                .roomCapacity(request.getRoomCapacity())
                .verified(false)
                .isVerify(false)
                .expired(false)
                .build();
    }

    private void createShadowListing(Listing premiumListing) {
        log.info("Creating shadow NORMAL listing for Premium listing: {}", premiumListing.getListingId());

        Listing shadowListing = Listing.builder()
                .title(premiumListing.getTitle())
                .description(premiumListing.getDescription())
                .userId(premiumListing.getUserId())
                .listingType(premiumListing.getListingType())
                .vipType(Listing.VipType.NORMAL) // Shadow is always NORMAL
                .categoryId(premiumListing.getCategoryId())
                .productType(premiumListing.getProductType())
                .price(premiumListing.getPrice())
                .priceUnit(premiumListing.getPriceUnit())
                .address(premiumListing.getAddress())  // Use same Address entity
                .area(premiumListing.getArea())
                .bedrooms(premiumListing.getBedrooms())
                .bathrooms(premiumListing.getBathrooms())
                .direction(premiumListing.getDirection())
                .furnishing(premiumListing.getFurnishing())
                .propertyType(premiumListing.getPropertyType())
                .roomCapacity(premiumListing.getRoomCapacity())
                .verified(premiumListing.getVerified())
                .isVerify(premiumListing.getIsVerify())
                .expired(premiumListing.getExpired() != null ? premiumListing.getExpired() : false)
                .isShadow(true)
                .parentListingId(premiumListing.getListingId())
                .postSource(premiumListing.getPostSource()) // Same source as parent
                .transactionId(premiumListing.getTransactionId()) // Same transaction as parent
                .build();

        listingRepository.save(shadowListing);
        log.info("Shadow listing created successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Build basic response
        ListingResponse response = listingMapper.toResponse(listing);

        // Populate owner's Zalo contact information
        populateOwnerZaloInfo(response, listing.getUserId());

        // Add location pricing if address is available
        Address address = listing.getAddress();
        if (address != null) {
            // Get address metadata to retrieve location IDs
            addressMetadataRepository.findByAddress_AddressId(address.getAddressId())
                    .ifPresent(metadata -> {
                        // Use old structure for pricing if available
                        if (metadata.getAddressType() == AddressMetadata.AddressType.OLD) {
                            response.setLocationPricing(
                                    locationPricingService.getLocationPricing(
                                            listing,
                                            metadata.getWardId(),
                                            metadata.getDistrictId(),
                                            metadata.getProvinceId()
                                    )
                            );
                        }
                        // For new structure, location pricing may not be applicable
                        // as it uses a different province/ward system
                    });
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsByIds(Set<Long> ids) {
        return listingRepository.findByIdsWithAmenities(ids).stream()
                .map(listing -> {
                    ListingResponse response = listingMapper.toResponse(listing);
                    populateOwnerZaloInfo(response, listing.getUserId());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListings(int page, int size) {
        // Convert 1-based page to 0-based for Spring Data
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // cap size to 100
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Listing> pageResult = listingRepository.findAll(pageable);
        return pageResult.getContent().stream()
                .map(listing -> {
                    ListingResponse response = listingMapper.toResponse(listing);
                    populateOwnerZaloInfo(response, listing.getUserId());
                    return response;
                })
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
        ListingResponse response = listingMapper.toResponse(saved);
        populateOwnerZaloInfo(response, saved.getUserId());
        return response;
    }

    @Override
    @Transactional
    public void deleteListing(Long id) {
        if (!listingRepository.existsById(id)) {
            throw new RuntimeException("Listing not found");
        }
        listingRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingResponseWithAdmin getListingByIdWithAdmin(Long id, String adminId) {
        log.info("Admin {} requesting listing {} with verification info", adminId, id);

        // Get listing with amenities
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Get admin information if updatedBy is set
        Admin verifyingAdmin = null;
        if (listing.getUpdatedBy() != null) {
            // Convert Long updatedBy to String adminId for lookup
            verifyingAdmin = adminRepository.findById(String.valueOf(listing.getUpdatedBy()))
                    .orElse(null);
        }

        // Determine verification status based on listing state
        String verificationStatus;
        if (listing.getVerified()) {
            verificationStatus = "APPROVED";
        } else if (listing.getIsVerify()) {
            verificationStatus = "PENDING";
        } else {
            verificationStatus = "PENDING";
        }

        // For now, verification notes can be null - in the future, this could be stored in a separate table
        String verificationNotes = null;

        return listingMapper.toResponseWithAdmin(listing, verifyingAdmin, verificationStatus, verificationNotes);
    }

    /**
     * Helper method to create an Address from AddressCreationRequest.
     * Delegates to AddressCreationService which handles both old and new address structures.
     * This ensures that address creation is part of the same transaction as listing creation,
     * preventing orphaned data.
     *
     * @param addressRequest the address creation request
     * @return the created and persisted Address entity
     * @throws IllegalArgumentException if validation fails
     */
    private Address createAddress(com.smartrent.dto.request.AddressCreationRequest addressRequest) {
        log.info("Creating address for listing - Type: {}", addressRequest.getAddressType());
        return addressCreationService.createAddress(addressRequest);
    }

    /**
     * Helper method to link existing media to a listing.
     * Ensures data integrity by validating:
     * - Media exists and is ACTIVE
     * - Media belongs to the same user (ownership validation)
     * - Media is not already linked to another listing
     *
     * This operation is part of the listing creation transaction,
     * ensuring atomicity and consistency.
     *
     * @param listing the listing to attach media to
     * @param mediaIds set of media IDs to link
     * @param userId user ID for ownership validation
     * @throws AppException if validation fails
     */
    private void linkMediaToListing(Listing listing, Set<Long> mediaIds, String userId) {
        log.info("Linking {} media items to listing {} for user {}",
                mediaIds.size(), listing.getListingId(), userId);

        for (Long mediaId : mediaIds) {
            // Fetch media
            Media media = mediaRepository.findById(mediaId)
                    .orElseThrow(() -> new AppException(DomainCode.ADDRESS_NOT_FOUND,
                            "Media not found with ID: " + mediaId));

            // Validate ownership
            if (!media.getUserId().equals(userId)) {
                throw new AppException(DomainCode.UNAUTHORIZED,
                        "Media " + mediaId + " does not belong to user " + userId);
            }

            // Validate media status
            if (media.getStatus() != Media.MediaStatus.ACTIVE) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Media " + mediaId + " is not active (status: " + media.getStatus() + ")");
            }

            // Validate media is not already linked to another listing
            if (media.getListing() != null) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Media " + mediaId + " is already linked to listing " +
                        media.getListing().getListingId());
            }

            // Link media to listing
            media.setListing(listing);
            mediaRepository.save(media);

            log.debug("Media {} successfully linked to listing {}", mediaId, listing.getListingId());
        }

        log.info("Successfully linked all {} media items to listing {}",
                mediaIds.size(), listing.getListingId());
    }

    /**
     * Helper method to populate owner's contact information in ListingResponse
     * Only sets contactAvailable=true if phone is present AND verified
     */
    private void populateOwnerZaloInfo(ListingResponse response, String userId) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                String contactPhone = user.getContactPhoneNumber();
                Boolean contactVerified = user.getContactPhoneVerified();

                response.setOwnerContactPhoneNumber(contactPhone);
                response.setOwnerContactPhoneVerified(contactVerified);

                // Only make contact available if phone exists AND is verified
                boolean isPhonePresent = contactPhone != null && !contactPhone.isEmpty();
                boolean isVerified = Boolean.TRUE.equals(contactVerified);

                if (isPhonePresent && isVerified) {
                    response.setOwnerZaloLink("https://zalo.me/" + contactPhone);
                    response.setContactAvailable(true);
                } else {
                    response.setOwnerZaloLink(null);
                    response.setContactAvailable(false);
                }
            });
        }
    }

    @Override
    @Transactional
    public ListingCreationResponse completeListingCreationAfterPayment(String transactionId) {
        log.info("Completing listing creation after payment for transaction: {}", transactionId);

        // Check for idempotency - if listing already created, return it
        return listingRepository.findByTransactionId(transactionId)
                .map(existingListing -> {
                    log.warn("Listing already created for transaction {}, returning existing listing",
                            transactionId);
                    return listingMapper.toCreationResponse(existingListing);
                })
                .orElseGet(() -> createListingFromCache(transactionId));
    }

    /**
     * Create listing from cache - checks both NORMAL and VIP caches
     */
    private ListingCreationResponse createListingFromCache(String transactionId) {
        // Try NORMAL listing first
        if (listingRequestCacheService.normalListingRequestExists(transactionId)) {
            return createNormalListingFromCache(transactionId);
        }

        // Try VIP listing
        if (listingRequestCacheService.vipListingRequestExists(transactionId)) {
            return createVipListingFromCache(transactionId);
        }

        // Cache expired or not found
        throw new AppException(DomainCode.LISTING_CREATION_CACHE_NOT_FOUND,
                "Listing request not found in cache for transaction: " + transactionId +
                ". Cache may have expired (30 min TTL). Please create listing again.");
    }

    /**
     * Create NORMAL listing from cached request
     */
    private ListingCreationResponse createNormalListingFromCache(String transactionId) {
        log.info("Creating NORMAL listing from cache for transaction: {}", transactionId);

        ListingCreationRequest request = listingRequestCacheService.getNormalListingRequest(transactionId);
        if (request == null) {
            throw new AppException(DomainCode.LISTING_CREATION_CACHE_NOT_FOUND,
                    "Failed to retrieve NORMAL listing request from cache");
        }

        try {
            // Create address
            Address address = createAddress(request.getAddress());

            // Create listing
            Listing listing = listingMapper.toEntity(request);
            listing.setAddress(address);
            listing.setPostSource(PostSource.DIRECT_PAYMENT);
            listing.setTransactionId(transactionId);
            // Set default values for required fields if not provided
            if (listing.getExpired() == null) {
                listing.setExpired(false);
            }

            Listing saved = listingRepository.save(listing);
            log.info("NORMAL listing created successfully with id: {} for transaction: {}",
                    saved.getListingId(), transactionId);

            // Link media if provided
            if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
                linkMediaToListing(saved, request.getMediaIds(), request.getUserId());
            }

            // Remove from cache
            listingRequestCacheService.removeNormalListingRequest(transactionId);

            return listingMapper.toCreationResponse(saved);

        } catch (Exception e) {
            log.error("Error creating NORMAL listing from cache for transaction: {}", transactionId, e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to create listing after payment: " + e.getMessage());
        }
    }

    /**
     * Create VIP listing from cached request
     */
    private ListingCreationResponse createVipListingFromCache(String transactionId) {
        log.info("Creating VIP listing from cache for transaction: {}", transactionId);

        VipListingCreationRequest request = listingRequestCacheService.getVipListingRequest(transactionId);
        if (request == null) {
            throw new AppException(DomainCode.LISTING_CREATION_CACHE_NOT_FOUND,
                    "Failed to retrieve VIP listing request from cache");
        }

        try {
            // Create address
            Address address = createAddress(request.getAddress());

            // Build and save VIP listing
            Listing vipListing = buildListingFromVipRequest(request, address);
            vipListing.setPostSource(PostSource.DIRECT_PAYMENT);
            vipListing.setTransactionId(transactionId);
            // Set default values for required fields if not provided
            if (vipListing.getExpired() == null) {
                vipListing.setExpired(false);
            }

            Listing savedVipListing = listingRepository.save(vipListing);
            log.info("VIP listing created with id: {} for transaction: {}",
                    savedVipListing.getListingId(), transactionId);

            // Link media if provided
            if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
                linkMediaToListing(savedVipListing, request.getMediaIds(), request.getUserId());
            }

            // Create shadow listing for DIAMOND tier
            if (savedVipListing.isDiamond()) {
                createShadowListing(savedVipListing);
            }

            // Remove from cache
            listingRequestCacheService.removeVipListingRequest(transactionId);

            return listingMapper.toCreationResponse(savedVipListing);

        } catch (Exception e) {
            log.error("Error creating VIP listing from cache for transaction: {}", transactionId, e);
            throw new AppException(DomainCode.UNKNOWN_ERROR,
                    "Failed to create VIP listing after payment: " + e.getMessage());
        }
    }

    /**
     * Validate required fields for non-draft listings
     */
    private void validateNonDraftListing(ListingCreationRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            missingFields.add("title");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            missingFields.add("description");
        }
        if (request.getListingType() == null || request.getListingType().isBlank()) {
            missingFields.add("listingType");
        }
        if (request.getVipType() == null || request.getVipType().isBlank()) {
            missingFields.add("vipType");
        }
        if (request.getCategoryId() == null) {
            missingFields.add("categoryId");
        }
        if (request.getProductType() == null || request.getProductType().isBlank()) {
            missingFields.add("productType");
        }
        if (request.getPrice() == null) {
            missingFields.add("price");
        }
        if (request.getPriceUnit() == null || request.getPriceUnit().isBlank()) {
            missingFields.add("priceUnit");
        }
        if (request.getAddress() == null) {
            missingFields.add("address");
        }

        if (!missingFields.isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Missing required fields for non-draft listing: " + String.join(", ", missingFields));
        }
    }

    /**
     * Create a draft listing with minimal validation
     */
    private ListingCreationResponse createDraftListing(ListingCreationRequest request) {
        log.info("Creating draft listing for user: {}", request.getUserId());

        // Create listing entity with available data
        Listing listing = Listing.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .description(request.getDescription())
                .listingType(request.getListingType() != null ?
                        Listing.ListingType.valueOf(request.getListingType()) : null)
                .vipType(request.getVipType() != null ?
                        Listing.VipType.valueOf(request.getVipType()) : Listing.VipType.NORMAL)
                .categoryId(request.getCategoryId())
                .productType(request.getProductType() != null ?
                        Listing.ProductType.valueOf(request.getProductType()) : null)
                .price(request.getPrice())
                .priceUnit(request.getPriceUnit() != null ?
                        Listing.PriceUnit.valueOf(request.getPriceUnit()) : null)
                .area(request.getArea())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .direction(request.getDirection() != null ?
                        Listing.Direction.valueOf(request.getDirection()) : null)
                .furnishing(request.getFurnishing() != null ?
                        Listing.Furnishing.valueOf(request.getFurnishing()) : null)
                .propertyType(request.getPropertyType() != null ?
                        Listing.PropertyType.valueOf(request.getPropertyType()) : null)
                .roomCapacity(request.getRoomCapacity())
                .isDraft(true)
                .verified(false)
                .isVerify(false)
                .expired(false)
                .build();

        // Create address if provided
        if (request.getAddress() != null) {
            Address address = createAddress(request.getAddress());
            listing.setAddress(address);
        }

        // Save draft listing
        Listing saved = listingRepository.save(listing);
        log.info("Draft listing created successfully with id: {}", saved.getListingId());

        // Link media to listing if provided
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            linkMediaToListing(saved, request.getMediaIds(), request.getUserId());
            log.info("Linked {} media items to draft listing {}", request.getMediaIds().size(), saved.getListingId());
        }

        return listingMapper.toCreationResponse(saved);
    }

    private String formatDiscountDescription(BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            return "No discount";
        }
        BigDecimal percentage = discount.multiply(new BigDecimal("100"));
        return percentage.stripTrailingZeros().toPlainString() + "% off";
    }

    private String formatSavingsDescription(BigDecimal amount, BigDecimal percentage) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return "No savings";
        }
        BigDecimal percentValue = percentage.multiply(new BigDecimal("100"));
        return String.format("Save %s %s (%.1f%%)",
                amount.toBigInteger(),
                PricingConstants.DEFAULT_CURRENCY,
                percentValue);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingListResponse searchListings(ListingFilterRequest filter) {
        boolean isMyListings = filter.getUserId() != null && !filter.getUserId().isEmpty();

        log.info("Unified search - UserId: {}, Category: {}, Province: {}/{}, isDraft: {}, Page: {}, Size: {}",
                filter.getUserId(), filter.getCategoryId(), filter.getProvinceId(), filter.getProvinceCode(),
                filter.getIsDraft(), filter.getPage(), filter.getSize());

        // Build specification from filter
        Specification<Listing> spec = ListingSpecification.fromFilterRequest(filter);

        // Create pageable with sorting
        Sort sort = createSort(filter.getSortBy(), filter.getSortDirection());
        Pageable pageable = PageRequest.of(
                Math.max(filter.getPage(), 0),
                Math.min(Math.max(filter.getSize(), 1), 100),
                sort
        );

        // Execute query with count
        Page<Listing> page = listingRepository.findAll(spec, pageable);

        // Convert to response DTOs
        List<ListingResponse> listings = page.getContent().stream()
                .map(listing -> {
                    ListingResponse response = listingMapper.toResponse(listing);
                    populateOwnerZaloInfo(response, listing.getUserId());
                    return response;
                })
                .collect(Collectors.toList());

        // Get recommendations (only for public search, not for my listings)
        List<ListingResponse> recommendations = isMyListings
                ? Collections.emptyList()
                : getRecommendations(filter, 5);

        return ListingListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .recommendations(recommendations)
                .filterCriteria(filter)
                .build();
    }

    /**
     * Get recommended listings based on filter criteria
     * TODO: Implement proper recommendation algorithm using ML or collaborative filtering
     */
    private List<ListingResponse> getRecommendations(ListingFilterRequest filter, int limit) {
        // For now, return recent high-tier listings (GOLD/DIAMOND) as recommendations
        Specification<Listing> recommendationSpec = (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            // High-tier listings
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("vipType"), Listing.VipType.GOLD),
                    criteriaBuilder.equal(root.get("vipType"), Listing.VipType.DIAMOND)
            ));

            // Verified and not expired
            predicates.add(criteriaBuilder.equal(root.get("verified"), true));
            predicates.add(criteriaBuilder.equal(root.get("expired"), false));
            predicates.add(criteriaBuilder.equal(root.get("isDraft"), false));
            predicates.add(criteriaBuilder.equal(root.get("isShadow"), false));

            // Same category if provided
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), filter.getCategoryId()));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "postDate"));
        Page<Listing> recommendedListings = listingRepository.findAll(recommendationSpec, pageable);

        return recommendedListings.getContent().stream()
                .map(listing -> {
                    ListingResponse response = listingMapper.toResponse(listing);
                    populateOwnerZaloInfo(response, listing.getUserId());
                    return response;
                })
                .collect(Collectors.toList());
    }

    /**
     * Create Sort object from sort field and direction
     */
    private Sort createSort(String sortBy, String sortDirection) {
        String sortField = sortBy != null ? sortBy : "postDate";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // Map sortBy field to entity field
        return switch (sortField) {
            case "price" -> Sort.by(direction, "price");
            case "area" -> Sort.by(direction, "area");
            case "createdAt" -> Sort.by(direction, "createdAt");
            case "updatedAt" -> Sort.by(direction, "updatedAt");
            default -> Sort.by(direction, "postDate");
        };
    }
}
