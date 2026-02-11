package com.smartrent.service.listing.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.AddressCreationRequest;
import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.request.DraftListingRequest;
import com.smartrent.dto.request.LegacyAddressData;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingFilterRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.MapBoundsRequest;
import com.smartrent.dto.request.NewAddressData;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.CategoryListingStatsResponse;
import com.smartrent.dto.response.DraftListingResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.ProvinceListingStatsResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.enums.PostSource;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AddressRepository;
import com.smartrent.infra.repository.AddressMetadataRepository;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.AmenityRepository;
import com.smartrent.infra.repository.CategoryRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyWardRepository;
import com.smartrent.infra.repository.ListingDraftRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.ProvinceRepository;
import com.smartrent.infra.repository.WardRepository;
import com.smartrent.infra.repository.ProjectRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.VipTierDetailRepository;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.listing.ListingQueryService;
import com.smartrent.service.listing.cache.ListingRequestCacheService;
import com.smartrent.util.TextNormalizer;
// import com.smartrent.service.pricing.LocationPricingService;  // DISABLED: Not in use
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
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListingServiceImpl implements ListingService {

    ListingRepository listingRepository;
    ListingDraftRepository listingDraftRepository;
    MediaRepository mediaRepository;
    AmenityRepository amenityRepository;
    CategoryRepository categoryRepository;
    AddressRepository addressRepository;
    AddressMetadataRepository addressMetadataRepository;
    AdminRepository adminRepository;
    UserRepository userRepository;
    LegacyProvinceRepository legacyProvinceRepository;
    LegacyDistrictRepository legacyDistrictRepository;
    LegacyWardRepository legacyWardRepository;
    ProvinceRepository provinceRepository;
    WardRepository wardRepository;
    ProjectRepository projectRepository;
    ListingMapper listingMapper;
    com.smartrent.mapper.MediaMapper mediaMapper;
    com.smartrent.mapper.AmenityMapper amenityMapper;
    com.smartrent.mapper.AddressMapper addressMapper;
    com.smartrent.mapper.UserMapper userMapper;
    // LocationPricingService locationPricingService;  // DISABLED: Service not in use
    QuotaService quotaService;
    TransactionService transactionService;
    TransactionRepository transactionRepository;
    VipTierDetailRepository vipTierDetailRepository;
    PaymentService paymentService;
    AddressCreationService addressCreationService;
    ListingRequestCacheService listingRequestCacheService;
    ListingQueryService listingQueryService;

    @Override
    @Transactional
    public ListingCreationResponse createListing(ListingCreationRequest request) {
        log.info("Creating listing with address - User: {}, Title: {}, UseMembershipQuota: {}",
                request.getUserId(), request.getTitle(), request.getUseMembershipQuota());

        // Validate required fields for non-draft listings
        validateNonDraftListing(request);

        // Check if user wants to use membership quota with specific benefit IDs
        if (Boolean.TRUE.equals(request.getUseMembershipQuota())) {
            // This method will fall back to payment flow if quota is not available
            return createListingWithMembershipQuota(request);
        }

        // For direct payment flow (useMembershipQuota=false or null)
        return createListingWithPayment(request);
    }

    /**
     * Create listing using membership quota with specific benefit IDs
     * This allows users to specify which membership benefits to use for creating a listing
     */
    private ListingCreationResponse createListingWithMembershipQuota(ListingCreationRequest request) {
        log.info("Creating listing with membership quota for user: {}, benefitIds: {}",
                request.getUserId(), request.getBenefitIds());

        // Validate benefitIds are provided
        if (request.getBenefitIds() == null || request.getBenefitIds().isEmpty()) {
            log.warn("No benefitIds provided for user {} with useMembershipQuota=true, falling back to payment flow",
                    request.getUserId());
            // Fall back to payment flow when no benefitIds provided
            return createListingWithPayment(request);
        }

        // Get the first benefit to determine the benefit type and infer vipType
        Long firstBenefitId = request.getBenefitIds().iterator().next();
        UserMembershipBenefit firstBenefit;
        try {
            firstBenefit = quotaService.getBenefitById(request.getUserId(), firstBenefitId);
        } catch (IllegalArgumentException e) {
            log.warn("Benefit not found for user {}: {}, falling back to payment flow",
                    request.getUserId(), e.getMessage());
            // Fall back to payment flow when benefit not found
            return createListingWithPayment(request);
        } catch (IllegalStateException e) {
            log.warn("Benefit expired for user {}: {}, falling back to payment flow",
                    request.getUserId(), e.getMessage());
            // Fall back to payment flow when benefit expired
            return createListingWithPayment(request);
        }

        BenefitType benefitType = firstBenefit.getBenefitType();

        // Validate benefit type is a posting type (POST_SILVER, POST_GOLD, POST_DIAMOND)
        String vipType;
        try {
            vipType = switch (benefitType) {
                case POST_SILVER -> "SILVER";
                case POST_GOLD -> "GOLD";
                case POST_DIAMOND -> "DIAMOND";
                default -> throw new AppException(DomainCode.BENEFIT_TYPE_MISMATCH,
                        "Benefit type " + benefitType + " cannot be used for creating listings. Only POST_SILVER, POST_GOLD, POST_DIAMOND are supported.");
            };
        } catch (AppException e) {
            log.warn("Invalid benefit type for user {}: {}, falling back to payment flow",
                    request.getUserId(), e.getMessage());
            // Fall back to payment flow when benefit type is invalid for listing creation
            return createListingWithPayment(request);
        }

        log.info("Inferred vipType {} from benefit type {}", vipType, benefitType);

        // Consume quota from specified benefit IDs (this also validates all benefits have the same type)
        try {
            quotaService.consumeQuotaByBenefitIds(request.getUserId(), request.getBenefitIds(), benefitType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid benefit IDs for user {}: {}, falling back to payment flow",
                    request.getUserId(), e.getMessage());
            // Fall back to payment flow when benefit IDs are invalid
            return createListingWithPayment(request);
        } catch (IllegalStateException e) {
            log.warn("Insufficient quota for user {}: {}, falling back to payment flow",
                    request.getUserId(), e.getMessage());
            // Fall back to payment flow when quota is insufficient
            return createListingWithPayment(request);
        }

        // Set the inferred vipType on the request so the mapper uses it
        request.setVipType(vipType);

        // Create address first (within the same transaction)
        Address address = createAddress(request.getAddress());

        // Create listing with postSource = QUOTA
        Listing listing = listingMapper.toEntity(request);
        listing.setAddress(address);
        listing.setPostSource(PostSource.QUOTA);
        listing.setTransactionId(null);
        listing.setUseMembershipQuota(true);

        // Set default values for required fields
        if (listing.getExpired() == null) {
            listing.setExpired(false);
        }

        // Save listing
        Listing saved = listingRepository.save(listing);
        log.info("Listing created successfully with quota - id: {}, vipType: {}, benefitIds: {}",
                saved.getListingId(), vipType, request.getBenefitIds());

        // Link media to listing if provided (within same transaction)
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            linkMediaToListing(saved, request.getMediaIds(), request.getUserId());
            log.info("Linked {} media items to listing {}", request.getMediaIds().size(), saved.getListingId());
        }

        // Link amenities to listing if provided (within same transaction)
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            linkAmenitiesToListing(saved, request.getAmenityIds());
            log.info("Linked {} amenities to listing {}", request.getAmenityIds().size(), saved.getListingId());
        }

        // If Diamond, create shadow NORMAL listing
        if ("DIAMOND".equalsIgnoreCase(vipType)) {
            createShadowListing(saved);
        }

        return listingMapper.toCreationResponse(saved);
    }

    /**
     * Create listing with payment flow (fallback when quota is not available)
     * This method handles the payment flow for listing creation when:
     * - User doesn't have quota
     * - User's quota is insufficient
     * - User's benefits are expired or invalid
     */
    private ListingCreationResponse createListingWithPayment(ListingCreationRequest request) {
        log.info("Creating listing with payment flow for user: {}", request.getUserId());

        // Validate duration days
        Integer durationDays = request.getDurationDays() != null ? request.getDurationDays() : 30;
        if (durationDays <= 0) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Duration days must be greater than 0");
        }

        // Get VIP tier to calculate price (default to NORMAL if not specified)
        String vipType = request.getVipType() != null ? request.getVipType() : "NORMAL";
        VipTierDetail vipTier = vipTierDetailRepository.findByTierCode(vipType)
                .orElseThrow(() -> new AppException(DomainCode.RESOURCE_NOT_FOUND,
                        "VIP tier not found: " + vipType));

        // Calculate price based on VIP tier and duration
        BigDecimal amount = vipTier.getPriceForDuration(durationDays);

        // Create PENDING transaction (no listing yet - will be created after payment)
        String transactionId = transactionService.createPostFeeTransaction(
                request.getUserId(),
                amount,
                vipType,
                durationDays,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        // Cache the listing request in Redis for payment callback (30 min TTL)
        // The listing will be created from this cached request after payment completion
        listingRequestCacheService.storeNormalListingRequest(transactionId, request);
        log.info("Cached {} listing request for transaction: {} (listing will be created after payment)",
                vipType, transactionId);

        // Generate payment URL using PaymentService
        // Use simple English orderInfo to avoid VNPay encoding issues with Vietnamese/special chars
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId)
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(amount)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent " + vipType + " Listing " + durationDays + " days")
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for {} listing transaction: {}", vipType, transactionId);

        // Return payment response (no listing ID yet - will be created after payment)
        return ListingCreationResponse.builder()
                .paymentRequired(true)
                .transactionId(transactionId)
                .amount(amount.longValue())
                .paymentUrl(paymentResponse.getPaymentUrl())
                .message("Payment required. Complete payment to create listing.")
                .build();
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

                // Link amenities to listing if provided (within same transaction)
                if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
                    linkAmenitiesToListing(saved, request.getAmenityIds());
                    log.info("Linked {} amenities to VIP listing {}", request.getAmenityIds().size(), saved.getListingId());
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

        // Cache the VIP listing request in Redis for payment callback (30 min TTL)
        listingRequestCacheService.storeVipListingRequest(transactionId, request);
        log.info("Cached VIP listing request for transaction: {}", transactionId);

        // Generate payment URL - pass transactionId to reuse existing transaction
        // Use simple English orderInfo to avoid VNPay encoding issues with Vietnamese/special chars
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId) // Reuse the transaction created above
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(amount)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent " + request.getVipType() + " Listing " + request.getDurationDays() + " days")
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for VIP listing transaction: {}", transactionId);

        // Return consistent response with payment info
        return ListingCreationResponse.builder()
                .paymentRequired(true)
                .transactionId(transactionId)
                .amount(amount.longValue())
                .paymentUrl(paymentResponse.getPaymentUrl())
                .message("Payment required. Complete payment to activate VIP listing.")
                .build();
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
        Listing.VipType vipType = Listing.VipType.valueOf(request.getVipType());
        return Listing.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(request.getUserId())
                .listingType(Listing.ListingType.valueOf(request.getListingType()))
                .vipType(vipType)
                .vipTypeSortOrder(Listing.getVipTypeSortOrder(vipType))
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
                .roomCapacity(request.getRoomCapacity())
                .verified(false)
                .isVerify(true)
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
                .vipTypeSortOrder(Listing.getVipTypeSortOrder(Listing.VipType.NORMAL)) // NORMAL = 4
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
        // Fetch listing with amenities first
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Fetch media separately to avoid MultipleBagFetchException
        listingRepository.findByIdWithMedia(id);

        // Build user and address responses
        com.smartrent.dto.response.UserCreationResponse user = buildUserResponse(listing.getUserId());
        com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(listing.getAddress());

        // Build basic response with user and address
        ListingResponse response = listingMapper.toResponse(listing, user, addressResponse);

        // Populate owner's Zalo contact information
        populateOwnerZaloInfo(response, listing.getUserId());

        // DISABLED: Location pricing feature not currently in use
        // Add location pricing if address is available
        /*
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
        */

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsByIds(Set<Long> ids) {
        List<Listing> listings = listingRepository.findByListingIdIn(ids);
        return batchMapListings(listings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListings(int page, int size) {
        // Convert 1-based page to 0-based for Spring Data
        int safePage = Math.max(page - 1, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                        .and(Sort.by(Sort.Direction.DESC, "updatedAt")));

        // Use filtered query with address JOIN FETCH (1 query instead of N+1)
        Page<Listing> pageResult = listingRepository.findPublicListings(pageable);

        return batchMapListings(pageResult.getContent());
    }

    @Override
    @Transactional
    public ListingResponse updateListing(Long id, ListingRequest request) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        // Update fields from request (null-safe for partial update)
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getPostDate() != null) existing.setPostDate(request.getPostDate());
        if (request.getExpiryDate() != null) existing.setExpiryDate(request.getExpiryDate());
        if (request.getListingType() != null) existing.setListingType(Listing.ListingType.valueOf(request.getListingType()));
        // Verification status can only be changed by admin via admin APIs
        // if (request.getVerified() != null) existing.setVerified(request.getVerified());
        // if (request.getIsVerify() != null) existing.setIsVerify(request.getIsVerify());
        if (request.getExpired() != null) existing.setExpired(request.getExpired());
        if (request.getVipType() != null) existing.setVipType(Listing.VipType.valueOf(request.getVipType()));
        if (request.getCategoryId() != null) existing.setCategoryId(request.getCategoryId());
        if (request.getProductType() != null) existing.setProductType(Listing.ProductType.valueOf(request.getProductType()));
        if (request.getPrice() != null) existing.setPrice(request.getPrice());
        if (request.getPriceUnit() != null) existing.setPriceUnit(Listing.PriceUnit.valueOf(request.getPriceUnit()));
        if (request.getArea() != null) existing.setArea(request.getArea());
        if (request.getBedrooms() != null) existing.setBedrooms(request.getBedrooms());
        if (request.getBathrooms() != null) existing.setBathrooms(request.getBathrooms());
        if (request.getDirection() != null) existing.setDirection(Listing.Direction.valueOf(request.getDirection()));
        if (request.getFurnishing() != null) existing.setFurnishing(Listing.Furnishing.valueOf(request.getFurnishing()));
        if (request.getRoomCapacity() != null) existing.setRoomCapacity(request.getRoomCapacity());
        if (request.getWaterPrice() != null) existing.setWaterPrice(request.getWaterPrice());
        if (request.getElectricityPrice() != null) existing.setElectricityPrice(request.getElectricityPrice());
        if (request.getInternetPrice() != null) existing.setInternetPrice(request.getInternetPrice());
        if (request.getServiceFee() != null) existing.setServiceFee(request.getServiceFee());

        // Handle media update if provided
        if (request.getMediaIds() != null) {
            log.info("Updating media for listing {}: {} media items", id, request.getMediaIds().size());

            // Unlink existing media (set listing to null)
            List<Media> existingMedia = mediaRepository.findByListing_ListingIdAndStatusOrderBySortOrderAsc(
                    id, Media.MediaStatus.ACTIVE);
            for (Media media : existingMedia) {
                media.setListing(null);
                mediaRepository.save(media);
                log.debug("Unlinked media {} from listing {}", media.getMediaId(), id);
            }

            // Link new media if provided (empty set will just unlink all)
            if (!request.getMediaIds().isEmpty()) {
                linkMediaToListing(existing, request.getMediaIds(), existing.getUserId());
                log.info("Linked {} new media items to listing {}", request.getMediaIds().size(), id);
            }
        }

        Listing saved = listingRepository.save(existing);
        com.smartrent.dto.response.UserCreationResponse user = buildUserResponse(saved.getUserId());
        com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(saved.getAddress());
        ListingResponse response = listingMapper.toResponse(saved, user, addressResponse);
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

        // Fetch media separately to avoid MultipleBagFetchException
        listingRepository.findByIdWithMedia(id);

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

        // Fetch user information
        com.smartrent.infra.repository.entity.User userEntity = userRepository.findById(listing.getUserId())
                .orElse(null);
        com.smartrent.dto.response.UserCreationResponse user = userEntity != null
                ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity)
                : null;

        return listingMapper.toResponseWithAdmin(listing, user, verifyingAdmin, verificationStatus, verificationNotes);
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
     * Helper method to link amenities to a listing.
     * Ensures data integrity by validating:
     * - Amenities exist
     * - Amenities are active
     *
     * This operation is part of the listing creation transaction,
     * ensuring atomicity and consistency.
     *
     * @param listing the listing to attach amenities to
     * @param amenityIds set of amenity IDs to link
     * @throws AppException if validation fails
     */
    private void linkAmenitiesToListing(Listing listing, Set<Long> amenityIds) {
        if (amenityIds == null || amenityIds.isEmpty()) {
            return;
        }

        log.info("Linking {} amenities to listing {}",
                amenityIds.size(), listing.getListingId());

        List<Amenity> amenities = new ArrayList<>();
        for (Long amenityId : amenityIds) {
            // Fetch amenity
            Amenity amenity = amenityRepository.findById(amenityId)
                    .orElseThrow(() -> new AppException(DomainCode.RESOURCE_NOT_FOUND,
                            "Amenity not found with ID: " + amenityId));

            // Validate amenity is active
            if (!Boolean.TRUE.equals(amenity.getIsActive())) {
                throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                        "Amenity " + amenityId + " is not active");
            }

            amenities.add(amenity);
            log.debug("Amenity {} will be linked to listing {}", amenityId, listing.getListingId());
        }

        // Set amenities to listing - JPA will handle the join table
        listing.setAmenities(amenities);

        log.info("Successfully linked {} amenities to listing {}",
                amenities.size(), listing.getListingId());
    }

    /**
     * Helper method to build UserCreationResponse from userId
     * @param userId User ID
     * @return UserCreationResponse or null if user not found
     */
    private com.smartrent.dto.response.UserCreationResponse buildUserResponse(String userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(userMapper::mapFromUserEntityToUserCreationResponse)
                .orElse(null);
    }

    /**
     * Helper method to build AddressResponse from Address entity
     * @param address Address entity
     * @return AddressResponse or null if address is null
     */
    private com.smartrent.dto.response.AddressResponse buildAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return addressMapper.toResponse(address);
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

    /**
     * Populate owner contact info from a pre-loaded User object (avoids extra DB query)
     */
    private void populateOwnerZaloInfoFromUser(ListingResponse response, User user) {
        if (user == null) return;
        String contactPhone = user.getContactPhoneNumber();
        Boolean contactVerified = user.getContactPhoneVerified();
        response.setOwnerContactPhoneNumber(contactPhone);
        response.setOwnerContactPhoneVerified(contactVerified);
        boolean isPhonePresent = contactPhone != null && !contactPhone.isEmpty();
        boolean isVerified = Boolean.TRUE.equals(contactVerified);
        if (isPhonePresent && isVerified) {
            response.setOwnerZaloLink("https://zalo.me/" + contactPhone);
            response.setContactAvailable(true);
        } else {
            response.setOwnerZaloLink(null);
            response.setContactAvailable(false);
        }
    }

    /**
     * Batch-load all relationships (users, amenities, media) and map listings to responses.
     * Reduces N+1 from ~33 queries to 4 queries for a page of 10 listings.
     */
    private List<ListingResponse> batchMapListings(List<Listing> listings) {
        if (listings.isEmpty()) return Collections.emptyList();

        List<Long> listingIds = listings.stream()
                .map(Listing::getListingId)
                .collect(Collectors.toList());

        // 1 query: batch-load all users
        Set<String> userIds = listings.stream()
                .map(Listing::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 1 query: batch-load amenities (populates Hibernate session cache)
        listingRepository.findByIdsWithAmenities(listingIds);

        // 1 query: batch-load media (populates Hibernate session cache)
        listingRepository.findByIdsWithMedia(listingIds);

        // Map to DTOs - amenities/media are now in Hibernate L1 cache, no extra queries
        return listings.stream()
                .map(listing -> {
                    User user = userMap.get(listing.getUserId());
                    com.smartrent.dto.response.UserCreationResponse userResponse =
                            user != null ? userMapper.mapFromUserEntityToUserCreationResponse(user) : null;
                    com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(listing.getAddress());
                    ListingResponse response = listingMapper.toResponse(listing, userResponse, addressResponse);
                    populateOwnerZaloInfoFromUser(response, user);
                    return response;
                })
                .collect(Collectors.toList());
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
        log.info("Checking cache for listing request - transactionId: {}", transactionId);

        // Try NORMAL listing first
        boolean normalExists = listingRequestCacheService.normalListingRequestExists(transactionId);
        log.info("NORMAL listing cache exists for {}: {}", transactionId, normalExists);
        if (normalExists) {
            return createNormalListingFromCache(transactionId);
        }

        // Try VIP listing
        boolean vipExists = listingRequestCacheService.vipListingRequestExists(transactionId);
        log.info("VIP listing cache exists for {}: {}", transactionId, vipExists);
        if (vipExists) {
            return createVipListingFromCache(transactionId);
        }

        // Cache expired or not found
        log.error("Listing request NOT FOUND in cache for transaction: {}. Cache may have expired (30 min TTL).", transactionId);
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

            // Link amenities if provided
            if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
                linkAmenitiesToListing(saved, request.getAmenityIds());
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

            // Link amenities if provided
            if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
                linkAmenitiesToListing(savedVipListing, request.getAmenityIds());
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
        // vipType is optional when:
        // 1. Using membership quota with benefitIds (will be inferred from benefit type)
        // 2. Using membership quota without benefitIds (will fall back to payment flow with default NORMAL)
        // In both cases, the service layer will handle the vipType appropriately
        boolean usingMembershipQuota = Boolean.TRUE.equals(request.getUseMembershipQuota());
        boolean hasVipType = request.getVipType() != null && !request.getVipType().isBlank();

        // vipType is required only when NOT using membership quota AND vipType is not provided
        if (!usingMembershipQuota && !hasVipType) {
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

    @Override
    @Transactional
    public DraftListingResponse createDraftListing(DraftListingRequest request) {
        log.info("Creating draft listing for user: {}", request.getUserId());

        // Create draft entity with available data
        ListingDraft draft = ListingDraft.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .description(request.getDescription())
                .listingType(request.getListingType())
                .vipType(request.getVipType())
                .categoryId(request.getCategoryId())
                .productType(request.getProductType())
                .price(request.getPrice())
                .priceUnit(request.getPriceUnit())
                .area(request.getArea())
                .bedrooms(request.getBedrooms())
                .bathrooms(request.getBathrooms())
                .direction(request.getDirection())
                .furnishing(request.getFurnishing())
                .roomCapacity(request.getRoomCapacity())
                .waterPrice(request.getWaterPrice())
                .electricityPrice(request.getElectricityPrice())
                .internetPrice(request.getInternetPrice())
                .serviceFee(request.getServiceFee())
                .build();

        // Extract address fields if provided (support BOTH legacy AND new structures)
        // ALWAYS save both structures if both are provided (like Listing does with Address entity)
        if (request.getAddress() != null) {
            var addressReq = request.getAddress();

            // ALWAYS extract and save legacy address data if provided
            if (addressReq.getLegacy() != null && addressReq.getLegacy().isValid()) {
                draft.setProvinceId(addressReq.getLegacy().getProvinceId() != null
                        ? addressReq.getLegacy().getProvinceId().longValue() : null);
                draft.setDistrictId(addressReq.getLegacy().getDistrictId() != null
                        ? addressReq.getLegacy().getDistrictId().longValue() : null);
                draft.setWardId(addressReq.getLegacy().getWardId() != null
                        ? addressReq.getLegacy().getWardId().longValue() : null);
            }

            // ALWAYS extract and save new address data if provided
            if (addressReq.getNewAddress() != null && addressReq.getNewAddress().isValid()) {
                draft.setProvinceCode(addressReq.getNewAddress().getProvinceCode());
                draft.setWardCode(addressReq.getNewAddress().getWardCode());
            }

            // ALWAYS save both legacy and new street fields separately if provided
            if (addressReq.getLegacy() != null && addressReq.getLegacy().getStreet() != null) {
                draft.setStreet(addressReq.getLegacy().getStreet());
            }
            if (addressReq.getNewAddress() != null && addressReq.getNewAddress().getStreet() != null) {
                draft.setNewStreet(addressReq.getNewAddress().getStreet());
            }

            // Set address type to indicate which structure is primary (priority: legacy > new if both provided)
            // Note: Both structures are still saved, addressType only indicates which one is primary
            if (addressReq.isLegacyStructure()) {
                draft.setAddressType("OLD");
            } else if (addressReq.isNewStructure()) {
                draft.setAddressType("NEW");
            }

            draft.setProjectId(addressReq.getProjectId() != null
                    ? addressReq.getProjectId().longValue() : null);
            draft.setLatitude(addressReq.getLatitude() != null
                    ? addressReq.getLatitude().doubleValue() : null);
            draft.setLongitude(addressReq.getLongitude() != null
                    ? addressReq.getLongitude().doubleValue() : null);
        }

        // Store amenity IDs as comma-separated string
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            draft.setAmenityIds(request.getAmenityIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        // Store media IDs as comma-separated string
        if (request.getMediaIds() != null && !request.getMediaIds().isEmpty()) {
            draft.setMediaIds(request.getMediaIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        // Save draft
        ListingDraft saved = listingDraftRepository.save(draft);
        log.info("Draft listing created successfully with id: {}", saved.getDraftId());

        return mapDraftToResponse(saved);
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
    @Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_SEARCH,
            key = "T(com.smartrent.util.CacheKeyBuilder).listingSearchKey(#filter)",
            unless = "#result == null")
    public ListingListResponse searchListings(ListingFilterRequest filter) {
        log.info("Unified search - UserId: {}, Category: {}, Province: {}/{}, isDraft: {}, Page: {}, Size: {}",
                filter.getUserId(), filter.getCategoryId(), filter.getProvinceId(), filter.getProvinceCode(),
                filter.getIsDraft(), filter.getPage(), filter.getSize());

        String normalizedKeyword = TextNormalizer.normalize(filter.getKeyword());
        if (filter.getKeyword() != null && (normalizedKeyword == null || normalizedKeyword.length() < 3)) {
            return ListingListResponse.builder()
                    .listings(Collections.emptyList())
                    .totalCount(0L)
                    .currentPage(1)
                    .pageSize(filter.getSize() != null ? filter.getSize() : 20)
                    .totalPages(0)
                    .filterCriteria(filter)
                    .build();
        }

        // Execute query using shared query service
        Page<Listing> page = listingQueryService.executeQuery(filter);

        // Batch-load all relationships and map to DTOs (4 queries total)
        List<ListingResponse> listings = batchMapListings(page.getContent());

        return ListingListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)  // Convert from 0-based (Spring Data) to 1-based (frontend)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .filterCriteria(filter)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.smartrent.dto.response.ListingAutocompleteResponse> autocompleteListings(String query, int limit) {
        String normalized = TextNormalizer.normalize(query);
        if (normalized == null || normalized.length() < 2) {
            return Collections.emptyList();
        }

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        Pageable pageable = PageRequest.of(0, safeLimit,
                Sort.by(Sort.Direction.ASC, "vipTypeSortOrder")
                        .and(Sort.by(Sort.Direction.DESC, "updatedAt")));

        Page<Listing> page = listingRepository.findAutocomplete(normalized, pageable);

        return page.getContent().stream()
                .map(listing -> com.smartrent.dto.response.ListingAutocompleteResponse.builder()
                        .listingId(listing.getListingId())
                        .title(listing.getTitle())
                        .address(listing.getAddress() != null ? listing.getAddress().getDisplayAddress() : null)
                        .price(listing.getPrice())
                        .priceUnit(listing.getPriceUnit())
                        .vipType(listing.getVipType())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceListingStatsResponse> getProvinceStats(ProvinceStatsRequest request) {
        log.info("Getting province stats - provinceIds: {}, provinceCodes: {}, verifiedOnly: {}",
                request.getProvinceIds(), request.getProvinceCodes(), request.getVerifiedOnly());

        // Validate request - must have either provinceIds or provinceCodes
        if ((request.getProvinceIds() == null || request.getProvinceIds().isEmpty()) &&
            (request.getProvinceCodes() == null || request.getProvinceCodes().isEmpty())) {
            log.warn("Province stats request missing both provinceIds and provinceCodes");
            return Collections.emptyList();
        }

        List<ProvinceListingStatsResponse> results = new ArrayList<>();

        // Handle old structure (63 provinces)
        if (request.getProvinceIds() != null && !request.getProvinceIds().isEmpty()) {
            log.info("Processing old structure with {} provinces", request.getProvinceIds().size());

            List<Object[]> statsData = listingRepository.getListingStatsByProvinceIds(request.getProvinceIds());

            // Map to response objects
            for (Object[] row : statsData) {
                Integer provinceId = (Integer) row[0];
                Long totalCount = (Long) row[1];
                Long verifiedCount = (Long) row[2];
                Long vipCount = (Long) row[3];

                // Get province name
                String provinceName = legacyProvinceRepository.findById(provinceId)
                        .map(com.smartrent.infra.repository.entity.LegacyProvince::getName)
                        .orElse("Unknown Province");

                // Filter if verifiedOnly is requested
                if (Boolean.TRUE.equals(request.getVerifiedOnly()) && verifiedCount == 0) {
                    continue;
                }

                results.add(ProvinceListingStatsResponse.builder()
                        .provinceId(provinceId)
                        .provinceCode(null)
                        .provinceName(provinceName)
                        .totalListings(totalCount)
                        .verifiedListings(verifiedCount)
                        .vipListings(vipCount)
                        .build());
            }
        }

        // Handle new structure (34 provinces)
        if (request.getProvinceCodes() != null && !request.getProvinceCodes().isEmpty()) {
            log.info("Processing new structure with {} provinces", request.getProvinceCodes().size());

            List<Object[]> statsData = listingRepository.getListingStatsByProvinceCodes(request.getProvinceCodes());

            // Map to response objects
            for (Object[] row : statsData) {
                String provinceCode = (String) row[0];
                Long totalCount = (Long) row[1];
                Long verifiedCount = (Long) row[2];
                Long vipCount = (Long) row[3];

                // Get province name
                String provinceName = provinceRepository.findByCode(provinceCode)
                        .map(Province::getName)
                        .orElse("Unknown Province");

                // Filter if verifiedOnly is requested
                if (Boolean.TRUE.equals(request.getVerifiedOnly()) && verifiedCount == 0) {
                    continue;
                }

                results.add(ProvinceListingStatsResponse.builder()
                        .provinceId(null)
                        .provinceCode(provinceCode)
                        .provinceName(provinceName)
                        .totalListings(totalCount)
                        .verifiedListings(verifiedCount)
                        .vipListings(vipCount)
                        .build());
            }
        }

        // Sort results to match input order
        if (request.getProvinceIds() != null && !request.getProvinceIds().isEmpty()) {
            results.sort((a, b) -> {
                int indexA = request.getProvinceIds().indexOf(a.getProvinceId());
                int indexB = request.getProvinceIds().indexOf(b.getProvinceId());
                return Integer.compare(indexA, indexB);
            });
        } else if (request.getProvinceCodes() != null && !request.getProvinceCodes().isEmpty()) {
            results.sort((a, b) -> {
                int indexA = request.getProvinceCodes().indexOf(a.getProvinceCode());
                int indexB = request.getProvinceCodes().indexOf(b.getProvinceCode());
                return Integer.compare(indexA, indexB);
            });
        }

        log.info("Province stats retrieved successfully - {} results", results.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryListingStatsResponse> getCategoryStats(CategoryStatsRequest request) {
        log.info("Getting category stats - categoryIds: {}, verifiedOnly: {}",
                request.getCategoryIds(), request.getVerifiedOnly());

        // Validate request - must have categoryIds
        if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
            log.warn("Category stats request missing categoryIds");
            return Collections.emptyList();
        }

        List<CategoryListingStatsResponse> results = new ArrayList<>();

        // Get stats from repository
        List<Object[]> statsData = listingRepository.getListingStatsByCategoryIds(request.getCategoryIds());

        // Map to response objects
        for (Object[] row : statsData) {
            Long categoryId = (Long) row[0];
            Long totalCount = (Long) row[1];
            Long verifiedCount = (Long) row[2];
            Long vipCount = (Long) row[3];

            // Get category details
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category == null) {
                log.warn("Category not found: {}", categoryId);
                continue;
            }

            // Filter if verifiedOnly is requested
            if (Boolean.TRUE.equals(request.getVerifiedOnly()) && verifiedCount == 0) {
                continue;
            }

            results.add(CategoryListingStatsResponse.builder()
                    .categoryId(categoryId)
                    .categoryName(category.getName())
                    .categorySlug(category.getSlug())
                    .categoryIcon(category.getIcon())
                    .totalListings(totalCount)
                    .verifiedListings(verifiedCount)
                    .vipListings(vipCount)
                    .build());
        }

        // Sort results to match input order
        results.sort((a, b) -> {
            int indexA = request.getCategoryIds().indexOf(a.getCategoryId());
            int indexB = request.getCategoryIds().indexOf(b.getCategoryId());
            return Integer.compare(indexA, indexB);
        });

        log.info("Category stats retrieved successfully - {} results", results.size());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public com.smartrent.dto.response.ListingResponseForOwner getMyListingDetail(Long id, String userId) {
        log.info("Owner {} requesting detail for listing {}", userId, id);

        // Get listing with amenities
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

        // Fetch media separately to avoid MultipleBagFetchException (though we fetch it again below)
        listingRepository.findByIdWithMedia(id);

        // Validate ownership
        if (!listing.getUserId().equals(userId)) {
            throw new AppException(DomainCode.UNAUTHORIZED,
                    "You don't have permission to view this listing's detail");
        }

        // Build user response
        com.smartrent.dto.response.UserCreationResponse user = buildUserResponse(listing.getUserId());

        // Get media for this listing
        List<Media> mediaList = mediaRepository.findByListing_ListingIdAndStatusOrderBySortOrderAsc(
                id, Media.MediaStatus.ACTIVE);
        List<com.smartrent.dto.response.MediaResponse> mediaResponses = mediaList.stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());

        // Get full address information
        com.smartrent.dto.response.AddressResponse addressResponse = null;
        if (listing.getAddress() != null) {
            addressResponse = addressMapper.toResponse(listing.getAddress());
        }

        // Get payment info if listing was created via payment
        com.smartrent.dto.response.ListingResponseForOwner.PaymentInfo paymentInfo = null;
        if (listing.getTransactionId() != null) {
            transactionRepository.findById(listing.getTransactionId()).ifPresent(transaction -> {
                // Build payment info from transaction
            });
            // For now, payment info is optional - can be enhanced later
        }

        // Build statistics - placeholder for now, can be enhanced with actual view/contact counts
        com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics statistics =
                com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics.builder()
                        .viewCount(0L)
                        .contactCount(0L)
                        .saveCount(0L)
                        .reportCount(0L)
                        .build();

        // Verification notes and rejection reason - can be retrieved from a verification table if exists
        String verificationNotes = null;
        String rejectionReason = null;

        return listingMapper.toResponseForOwner(
                listing,
                user,
                mediaResponses,
                addressResponse,
                paymentInfo,
                statistics,
                verificationNotes,
                rejectionReason
        );
    }

    @Override
    @Transactional(readOnly = true)
    public com.smartrent.dto.response.AdminListingListResponse getAllListingsForAdmin(
            ListingFilterRequest filter, String adminId) {
        log.info("Admin {} requesting all listings - Category: {}, Province: {}, Page: {}, Size: {}",
                adminId, filter.getCategoryId(), filter.getProvinceId(), filter.getPage(), filter.getSize());

        // Validate admin exists
        adminRepository.findById(adminId)
                .orElseThrow(() -> new AppException(DomainCode.UNAUTHORIZED, "Admin not found"));

        // Execute query using shared query service
        Page<Listing> page = listingQueryService.executeQuery(filter);

        // Build specification for statistics calculation
        Specification<Listing> spec = listingQueryService.buildSpecification(filter);

        // Convert to response DTOs with admin info
        List<com.smartrent.dto.response.ListingResponseWithAdmin> listings = page.getContent().stream()
                .map(listing -> {
                    // Get admin info if listing was updated by an admin
                    Admin verifyingAdmin = null;
                    if (listing.getUpdatedBy() != null) {
                        verifyingAdmin = adminRepository.findById(String.valueOf(listing.getUpdatedBy()))
                                .orElse(null);
                    }

                    // Determine verification status
                    String verificationStatus;
                    if (listing.getVerified()) {
                        verificationStatus = "APPROVED";
                    } else if (listing.getIsVerify()) {
                        verificationStatus = "PENDING";
                    } else {
                        verificationStatus = "NOT_SUBMITTED";
                    }

                    // Fetch user information
                    com.smartrent.infra.repository.entity.User userEntity = userRepository.findById(listing.getUserId())
                            .orElse(null);
                    com.smartrent.dto.response.UserCreationResponse user = userEntity != null
                            ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity)
                            : null;

                    return listingMapper.toResponseWithAdmin(listing, user, verifyingAdmin, verificationStatus, null);
                })
                .collect(Collectors.toList());

        // Calculate statistics for the filtered results
        com.smartrent.dto.response.AdminListingListResponse.AdminStatistics statistics =
                calculateAdminStatistics(spec);

        return com.smartrent.dto.response.AdminListingListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)  // Convert from 0-based (Spring Data) to 1-based (frontend)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .filterCriteria(filter)
                .statistics(statistics)
                .build();
    }

    /**
     * Calculate statistics for admin dashboard
     */
    private com.smartrent.dto.response.AdminListingListResponse.AdminStatistics calculateAdminStatistics(
            Specification<Listing> baseSpec) {
        // Count listings by different criteria
        Specification<Listing> pendingVerificationSpec = baseSpec.and((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("isVerify"), true),
                        cb.equal(root.get("verified"), false)
                ));

        Specification<Listing> verifiedSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("verified"), true));

        Specification<Listing> expiredSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("expired"), true));

        Specification<Listing> rejectedSpec = baseSpec.and((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("verified"), false),
                        cb.equal(root.get("isVerify"), false)
                ));

        Specification<Listing> draftSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("isDraft"), true));

        Specification<Listing> shadowSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("isShadow"), true));

        // Count by VIP tier
        Specification<Listing> normalSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.NORMAL));

        Specification<Listing> silverSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.SILVER));

        Specification<Listing> goldSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.GOLD));

        Specification<Listing> diamondSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.DIAMOND));

        return com.smartrent.dto.response.AdminListingListResponse.AdminStatistics.builder()
                .pendingVerification(listingRepository.count(pendingVerificationSpec))
                .verified(listingRepository.count(verifiedSpec))
                .expired(listingRepository.count(expiredSpec))
                .rejected(listingRepository.count(rejectedSpec))
                .drafts(listingRepository.count(draftSpec))
                .shadows(listingRepository.count(shadowSpec))
                .normalListings(listingRepository.count(normalSpec))
                .silverListings(listingRepository.count(silverSpec))
                .goldListings(listingRepository.count(goldSpec))
                .diamondListings(listingRepository.count(diamondSpec))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.smartrent.dto.response.OwnerListingListResponse getMyListings(
            ListingFilterRequest filter, String userId) {
        log.info("Owner {} requesting listings - Page: {}, Size: {}, Filters: verified={}, expired={}, isDraft={}",
                userId, filter.getPage(), filter.getSize(), filter.getVerified(), filter.getExpired(), filter.getIsDraft());

        // Set userId in filter to get only owner's listings
        filter.setUserId(userId);

        // Execute query using shared query service
        Page<Listing> page = listingQueryService.executeQuery(filter);

        // Build specification for statistics calculation
        Specification<Listing> spec = listingQueryService.buildSpecification(filter);

        // Convert to ListingResponseForOwner with full details
        List<com.smartrent.dto.response.ListingResponseForOwner> listings = page.getContent().stream()
                .map(listing -> {
                    // Build user response
                    com.smartrent.dto.response.UserCreationResponse user = buildUserResponse(listing.getUserId());

                    // Get media for this listing
                    List<Media> mediaList = mediaRepository.findByListing_ListingIdAndStatusOrderBySortOrderAsc(
                            listing.getListingId(), Media.MediaStatus.ACTIVE);
                    List<com.smartrent.dto.response.MediaResponse> mediaResponses = mediaList.stream()
                            .map(mediaMapper::toResponse)
                            .collect(Collectors.toList());

                    // Get full address information
                    com.smartrent.dto.response.AddressResponse addressResponse = null;
                    if (listing.getAddress() != null) {
                        addressResponse = addressMapper.toResponse(listing.getAddress());
                    }

                    // Get payment info if listing was created via payment
                    com.smartrent.dto.response.ListingResponseForOwner.PaymentInfo paymentInfo = null;
                    if (listing.getTransactionId() != null) {
                        // For now, payment info is optional - can be enhanced later
                    }

                    // Build statistics - placeholder for now
                    com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics statistics =
                            com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics.builder()
                                    .viewCount(0L)
                                    .contactCount(0L)
                                    .saveCount(0L)
                                    .reportCount(0L)
                                    .build();

                    // Verification notes and rejection reason - can be retrieved from a verification table if exists
                    String verificationNotes = null;
                    String rejectionReason = null;

                    return listingMapper.toResponseForOwner(
                            listing,
                            user,
                            mediaResponses,
                            addressResponse,
                            paymentInfo,
                            statistics,
                            verificationNotes,
                            rejectionReason
                    );
                })
                .collect(Collectors.toList());

        // Calculate owner statistics
        com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics statistics =
                calculateOwnerStatistics(spec);

        return com.smartrent.dto.response.OwnerListingListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)  // Convert from 0-based (Spring Data) to 1-based (frontend)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .filterCriteria(filter)
                .statistics(statistics)
                .build();
    }

    /**
     * Calculate statistics for owner dashboard
     */
    private com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics calculateOwnerStatistics(
            Specification<Listing> baseSpec) {
        // Count listings by different criteria
        Specification<Listing> draftSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("isDraft"), true));

        Specification<Listing> pendingVerificationSpec = baseSpec.and((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("isVerify"), true),
                        cb.equal(root.get("verified"), false),
                        cb.equal(root.get("isDraft"), false)
                ));

        Specification<Listing> rejectedSpec = baseSpec.and((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("verified"), false),
                        cb.equal(root.get("isVerify"), false),
                        cb.equal(root.get("isDraft"), false)
                ));

        Specification<Listing> activeSpec = baseSpec.and((root, query, cb) ->
                cb.and(
                        cb.equal(root.get("verified"), true),
                        cb.equal(root.get("expired"), false),
                        cb.equal(root.get("isDraft"), false)
                ));

        Specification<Listing> expiredSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("expired"), true));

        // Count by VIP tier
        Specification<Listing> normalSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.NORMAL));

        Specification<Listing> silverSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.SILVER));

        Specification<Listing> goldSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.GOLD));

        Specification<Listing> diamondSpec = baseSpec.and((root, query, cb) ->
                cb.equal(root.get("vipType"), Listing.VipType.DIAMOND));

        return com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics.builder()
                .drafts(listingRepository.count(draftSpec))
                .pendingVerification(listingRepository.count(pendingVerificationSpec))
                .rejected(listingRepository.count(rejectedSpec))
                .active(listingRepository.count(activeSpec))
                .expired(listingRepository.count(expiredSpec))
                .normalListings(listingRepository.count(normalSpec))
                .silverListings(listingRepository.count(silverSpec))
                .goldListings(listingRepository.count(goldSpec))
                .diamondListings(listingRepository.count(diamondSpec))
                .build();
    }

    // ============ DRAFT MANAGEMENT METHODS IMPLEMENTATION ============

    @Override
    @Transactional
    public DraftListingResponse updateDraft(Long draftId, DraftListingRequest request, String userId) {
        log.info("Updating draft listing {} for user {}", draftId, userId);

        // Get draft and verify ownership
        ListingDraft draft = listingDraftRepository.findByDraftIdAndUserId(draftId, userId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND,
                        "Draft not found with id: " + draftId));

        // Update fields if provided (partial update)
        if (request.getTitle() != null) {
            draft.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            draft.setDescription(request.getDescription());
        }
        if (request.getListingType() != null) {
            draft.setListingType(request.getListingType());
        }
        if (request.getVipType() != null) {
            draft.setVipType(request.getVipType());
        }
        if (request.getCategoryId() != null) {
            draft.setCategoryId(request.getCategoryId());
        }
        if (request.getProductType() != null) {
            draft.setProductType(request.getProductType());
        }
        if (request.getPrice() != null) {
            draft.setPrice(request.getPrice());
        }
        if (request.getPriceUnit() != null) {
            draft.setPriceUnit(request.getPriceUnit());
        }
        if (request.getArea() != null) {
            draft.setArea(request.getArea());
        }
        if (request.getBedrooms() != null) {
            draft.setBedrooms(request.getBedrooms());
        }
        if (request.getBathrooms() != null) {
            draft.setBathrooms(request.getBathrooms());
        }
        if (request.getDirection() != null) {
            draft.setDirection(request.getDirection());
        }
        if (request.getFurnishing() != null) {
            draft.setFurnishing(request.getFurnishing());
        }
        if (request.getRoomCapacity() != null) {
            draft.setRoomCapacity(request.getRoomCapacity());
        }
        if (request.getWaterPrice() != null) {
            draft.setWaterPrice(request.getWaterPrice());
        }
        if (request.getElectricityPrice() != null) {
            draft.setElectricityPrice(request.getElectricityPrice());
        }
        if (request.getInternetPrice() != null) {
            draft.setInternetPrice(request.getInternetPrice());
        }
        if (request.getServiceFee() != null) {
            draft.setServiceFee(request.getServiceFee());
        }

        // Update address if provided (support BOTH legacy AND new structures)
        // ALWAYS save both structures if both are provided (like Listing does with Address entity)
        if (request.getAddress() != null) {
            var addressReq = request.getAddress();

            // ALWAYS update and save legacy address data if provided
            if (addressReq.getLegacy() != null && addressReq.getLegacy().isValid()) {
                draft.setProvinceId(addressReq.getLegacy().getProvinceId() != null
                        ? addressReq.getLegacy().getProvinceId().longValue() : null);
                draft.setDistrictId(addressReq.getLegacy().getDistrictId() != null
                        ? addressReq.getLegacy().getDistrictId().longValue() : null);
                draft.setWardId(addressReq.getLegacy().getWardId() != null
                        ? addressReq.getLegacy().getWardId().longValue() : null);
            }

            // ALWAYS update and save new address data if provided
            if (addressReq.getNewAddress() != null && addressReq.getNewAddress().isValid()) {
                draft.setProvinceCode(addressReq.getNewAddress().getProvinceCode());
                draft.setWardCode(addressReq.getNewAddress().getWardCode());
            }

            // ALWAYS update both legacy and new street fields separately if provided
            if (addressReq.getLegacy() != null && addressReq.getLegacy().getStreet() != null) {
                draft.setStreet(addressReq.getLegacy().getStreet());
            }
            if (addressReq.getNewAddress() != null && addressReq.getNewAddress().getStreet() != null) {
                draft.setNewStreet(addressReq.getNewAddress().getStreet());
            }

            // Set address type to indicate which structure is primary (priority: legacy > new if both provided)
            // Note: Both structures are still saved, addressType only indicates which one is primary
            if (addressReq.isLegacyStructure()) {
                draft.setAddressType("OLD");
            } else if (addressReq.isNewStructure()) {
                draft.setAddressType("NEW");
            }

            if (addressReq.getProjectId() != null) {
                draft.setProjectId(addressReq.getProjectId().longValue());
            }
            if (addressReq.getLatitude() != null) {
                draft.setLatitude(addressReq.getLatitude().doubleValue());
            }
            if (addressReq.getLongitude() != null) {
                draft.setLongitude(addressReq.getLongitude().doubleValue());
            }
        }

        // Update amenity IDs if provided
        if (request.getAmenityIds() != null) {
            draft.setAmenityIds(request.getAmenityIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        // Update media IDs if provided
        if (request.getMediaIds() != null) {
            draft.setMediaIds(request.getMediaIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }

        // Save draft
        ListingDraft savedDraft = listingDraftRepository.save(draft);
        log.info("Draft listing {} updated successfully", draftId);

        return mapDraftToResponse(savedDraft);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DraftListingResponse> getMyDrafts(String userId) {
        log.info("Getting draft listings for user {}", userId);

        List<ListingDraft> drafts = listingDraftRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        log.info("Found {} draft listings for user {}", drafts.size(), userId);

        return drafts.stream()
                .map(this::mapDraftToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DraftListingResponse getDraftById(Long draftId, String userId) {
        log.info("Getting draft listing {} for user {}", draftId, userId);

        ListingDraft draft = listingDraftRepository.findByDraftIdAndUserId(draftId, userId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND,
                        "Draft not found with id: " + draftId));

        return mapDraftToResponse(draft);
    }

    @Override
    @Transactional
    public ListingCreationResponse publishDraft(Long draftId, ListingCreationRequest request, String userId) {
        log.info("Publishing draft listing {} for user {}", draftId, userId);

        // Get draft and verify ownership
        ListingDraft draft = listingDraftRepository.findByDraftIdAndUserId(draftId, userId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND,
                        "Draft not found with id: " + draftId));

        // Merge draft data with request (request takes precedence)
        ListingCreationRequest mergedRequest = mergeDraftWithRequest(draft, request);
        mergedRequest.setUserId(userId);

        // Validate required fields
        validateDraftForPublish(mergedRequest);

        // Create the listing using the normal flow
        ListingCreationResponse response = createListing(mergedRequest);

        // Delete the draft after successful publish
        listingDraftRepository.delete(draft);
        log.info("Draft {} deleted after successful publish, listing created with id: {}",
                draftId, response.getListingId());

        return response;
    }

    @Override
    @Transactional
    public void deleteDraft(Long draftId, String userId) {
        log.info("Deleting draft listing {} for user {}", draftId, userId);

        // Get draft and verify ownership
        ListingDraft draft = listingDraftRepository.findByDraftIdAndUserId(draftId, userId)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND,
                        "Draft not found with id: " + draftId));

        listingDraftRepository.delete(draft);
        log.info("Draft listing {} deleted successfully", draftId);
    }

    /**
     * Map ListingDraft entity to DraftListingResponse
     */
    private DraftListingResponse mapDraftToResponse(ListingDraft draft) {
        Set<com.smartrent.dto.response.AmenityResponse> amenities = null;
        if (draft.getAmenityIds() != null && !draft.getAmenityIds().isEmpty()) {
            Set<Long> amenityIds = java.util.Arrays.stream(draft.getAmenityIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());

            if (!amenityIds.isEmpty()) {
                List<Amenity> amenityEntities = amenityRepository.findAllById(amenityIds);
                amenities = amenityEntities.stream()
                        .map(amenityMapper::toResponse)
                        .collect(Collectors.toSet());
            }
        }

        Set<com.smartrent.dto.response.MediaResponse> media = null;
        if (draft.getMediaIds() != null && !draft.getMediaIds().isEmpty()) {
            Set<Long> mediaIds = java.util.Arrays.stream(draft.getMediaIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet());

            if (!mediaIds.isEmpty()) {
                List<Media> mediaEntities = mediaRepository.findAllById(mediaIds);
                media = mediaEntities.stream()
                        .map(mediaMapper::toResponse)
                        .collect(Collectors.toSet());
            }
        }

        // Build AddressResponse from draft address fields
        com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponseFromDraft(draft);

        return DraftListingResponse.builder()
                .draftId(draft.getDraftId())
                .userId(draft.getUserId())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .listingType(draft.getListingType())
                .vipType(draft.getVipType())
                .categoryId(draft.getCategoryId())
                .productType(draft.getProductType())
                .price(draft.getPrice())
                .priceUnit(draft.getPriceUnit())
                .address(addressResponse)
                .area(draft.getArea())
                .bedrooms(draft.getBedrooms())
                .bathrooms(draft.getBathrooms())
                .direction(draft.getDirection())
                .furnishing(draft.getFurnishing())
                .roomCapacity(draft.getRoomCapacity())
                .waterPrice(draft.getWaterPrice())
                .electricityPrice(draft.getElectricityPrice())
                .internetPrice(draft.getInternetPrice())
                .serviceFee(draft.getServiceFee())
                .amenities(amenities)
                .media(media)
                .createdAt(draft.getCreatedAt())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }

    /**
     * Build AddressResponse from ListingDraft address fields
     */
    private com.smartrent.dto.response.AddressResponse buildAddressResponseFromDraft(ListingDraft draft) {
        // If no address data, return null
        if (draft.getAddressType() == null) {
            return null;
        }

        com.smartrent.dto.response.AddressResponse.AddressResponseBuilder builder =
                com.smartrent.dto.response.AddressResponse.builder();

        // Set common fields
        if (draft.getLatitude() != null && draft.getLongitude() != null) {
            builder.latitude(java.math.BigDecimal.valueOf(draft.getLatitude()))
                   .longitude(java.math.BigDecimal.valueOf(draft.getLongitude()));
        }

        // Set address type based on which structure was marked as primary
        if ("OLD".equals(draft.getAddressType())) {
            builder.addressType(com.smartrent.infra.repository.entity.AddressMetadata.AddressType.OLD);
        } else if ("NEW".equals(draft.getAddressType())) {
            builder.addressType(com.smartrent.infra.repository.entity.AddressMetadata.AddressType.NEW);
        }

        // ALWAYS populate legacy fields if they exist (regardless of addressType)
        // This ensures both structures are returned when both are provided
        if (draft.getProvinceId() != null) {
            builder.legacyProvinceId(draft.getProvinceId().intValue());
            // Fetch and populate province name
            legacyProvinceRepository.findById(draft.getProvinceId().intValue())
                    .ifPresent(province -> builder.legacyProvinceName(province.getName()));
        }
        if (draft.getDistrictId() != null) {
            builder.legacyDistrictId(draft.getDistrictId().intValue());
            // Fetch and populate district name
            legacyDistrictRepository.findById(draft.getDistrictId().intValue())
                    .ifPresent(district -> builder.legacyDistrictName(district.getName()));
        }
        if (draft.getWardId() != null) {
            builder.legacyWardId(draft.getWardId().intValue());
            // Fetch and populate ward name
            legacyWardRepository.findById(draft.getWardId().intValue())
                    .ifPresent(ward -> builder.legacyWardName(ward.getName()));
        }

        // ALWAYS populate new address fields if they exist (regardless of addressType)
        // This ensures both structures are returned when both are provided
        if (draft.getProvinceCode() != null) {
            builder.newProvinceCode(draft.getProvinceCode());
            // Fetch and populate province name
            provinceRepository.findByCode(draft.getProvinceCode())
                    .ifPresent(province -> builder.newProvinceName(province.getName()));
        }
        if (draft.getWardCode() != null) {
            builder.newWardCode(draft.getWardCode());
            // Fetch and populate ward name
            wardRepository.findByCode(draft.getWardCode())
                    .ifPresent(ward -> builder.newWardName(ward.getName()));
        }

        // ALWAYS populate both street fields if they exist (regardless of addressType)
        // This ensures both structures are returned when both are provided
        if (draft.getStreet() != null) {
            builder.legacyStreet(draft.getStreet());
        }
        if (draft.getNewStreet() != null) {
            builder.newStreet(draft.getNewStreet());
        }

        // Build fullAddress from legacy structure if exists
        if (draft.getProvinceId() != null || draft.getDistrictId() != null || draft.getWardId() != null) {
            String fullAddress = buildLegacyFullAddressFromDraft(draft);
            builder.fullAddress(fullAddress);
        }

        // Build fullNewAddress from new structure if exists
        if (draft.getProvinceCode() != null || draft.getWardCode() != null) {
            String fullNewAddress = buildNewFullAddressFromDraft(draft);
            builder.fullNewAddress(fullNewAddress);
        }

        return builder.build();
    }

    /**
     * Merge draft data with publish request (request takes precedence)
     */
    private ListingCreationRequest mergeDraftWithRequest(ListingDraft draft, ListingCreationRequest request) {
        ListingCreationRequest merged = new ListingCreationRequest();

        // Use request value if provided, otherwise use draft value
        merged.setTitle(request.getTitle() != null ? request.getTitle() : draft.getTitle());
        merged.setDescription(request.getDescription() != null ? request.getDescription() : draft.getDescription());
        merged.setListingType(request.getListingType() != null ? request.getListingType() : draft.getListingType());
        merged.setVipType(request.getVipType() != null ? request.getVipType() : draft.getVipType());
        merged.setCategoryId(request.getCategoryId() != null ? request.getCategoryId() : draft.getCategoryId());
        merged.setProductType(request.getProductType() != null ? request.getProductType() : draft.getProductType());
        merged.setPrice(request.getPrice() != null ? request.getPrice() : draft.getPrice());
        merged.setPriceUnit(request.getPriceUnit() != null ? request.getPriceUnit() : draft.getPriceUnit());
        merged.setArea(request.getArea() != null ? request.getArea() : draft.getArea());
        merged.setBedrooms(request.getBedrooms() != null ? request.getBedrooms() : draft.getBedrooms());
        merged.setBathrooms(request.getBathrooms() != null ? request.getBathrooms() : draft.getBathrooms());
        merged.setDirection(request.getDirection() != null ? request.getDirection() : draft.getDirection());
        merged.setFurnishing(request.getFurnishing() != null ? request.getFurnishing() : draft.getFurnishing());
        merged.setRoomCapacity(request.getRoomCapacity() != null ? request.getRoomCapacity() : draft.getRoomCapacity());
        merged.setWaterPrice(request.getWaterPrice() != null ? request.getWaterPrice() : draft.getWaterPrice());
        merged.setElectricityPrice(request.getElectricityPrice() != null ? request.getElectricityPrice() : draft.getElectricityPrice());
        merged.setInternetPrice(request.getInternetPrice() != null ? request.getInternetPrice() : draft.getInternetPrice());
        merged.setServiceFee(request.getServiceFee() != null ? request.getServiceFee() : draft.getServiceFee());

        // Copy payment/quota related fields from request only
        merged.setDurationDays(request.getDurationDays());
        merged.setUseMembershipQuota(request.getUseMembershipQuota());
        merged.setBenefitIds(request.getBenefitIds());
        merged.setPaymentProvider(request.getPaymentProvider());

        // Handle address - use request if provided, otherwise build from draft
        if (request.getAddress() != null) {
            merged.setAddress(request.getAddress());
        } else if (draft.getAddressType() != null) {
            AddressCreationRequest addressReq = new AddressCreationRequest();
            if ("OLD".equals(draft.getAddressType())) {
                LegacyAddressData legacy = new LegacyAddressData();
                legacy.setProvinceId(draft.getProvinceId() != null ? draft.getProvinceId().intValue() : null);
                legacy.setDistrictId(draft.getDistrictId() != null ? draft.getDistrictId().intValue() : null);
                legacy.setWardId(draft.getWardId() != null ? draft.getWardId().intValue() : null);
                legacy.setStreet(draft.getStreet());
                addressReq.setLegacy(legacy);
            } else if ("NEW".equals(draft.getAddressType())) {
                NewAddressData newAddr = new NewAddressData();
                newAddr.setProvinceCode(draft.getProvinceCode());
                newAddr.setWardCode(draft.getWardCode());
                newAddr.setStreet(draft.getStreet());
                addressReq.setNewAddress(newAddr);
            }
            addressReq.setProjectId(draft.getProjectId() != null ? draft.getProjectId().intValue() : null);
            addressReq.setLatitude(draft.getLatitude() != null ? BigDecimal.valueOf(draft.getLatitude()) : null);
            addressReq.setLongitude(draft.getLongitude() != null ? BigDecimal.valueOf(draft.getLongitude()) : null);
            merged.setAddress(addressReq);
        }

        // Handle amenity IDs
        if (request.getAmenityIds() != null) {
            merged.setAmenityIds(request.getAmenityIds());
        } else if (draft.getAmenityIds() != null && !draft.getAmenityIds().isEmpty()) {
            merged.setAmenityIds(java.util.Arrays.stream(draft.getAmenityIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet()));
        }

        // Handle media IDs
        if (request.getMediaIds() != null) {
            merged.setMediaIds(request.getMediaIds());
        } else if (draft.getMediaIds() != null && !draft.getMediaIds().isEmpty()) {
            merged.setMediaIds(java.util.Arrays.stream(draft.getMediaIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toSet()));
        }

        return merged;
    }

    /**
     * Validate draft has all required fields before publishing
     */
    private void validateDraftForPublish(ListingCreationRequest request) {
        List<String> missingFields = new ArrayList<>();

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            missingFields.add("title");
        }
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            missingFields.add("description");
        }
        if (request.getListingType() == null) {
            missingFields.add("listingType");
        }
        if (request.getProductType() == null) {
            missingFields.add("productType");
        }
        if (request.getPrice() == null) {
            missingFields.add("price");
        }
        if (request.getPriceUnit() == null) {
            missingFields.add("priceUnit");
        }
        if (request.getAddress() == null) {
            missingFields.add("address");
        }
        if (request.getCategoryId() == null) {
            missingFields.add("categoryId");
        }

        // Check vipType only if not using membership quota
        if (!Boolean.TRUE.equals(request.getUseMembershipQuota()) &&
            (request.getBenefitIds() == null || request.getBenefitIds().isEmpty())) {
            if (request.getVipType() == null) {
                missingFields.add("vipType");
            }
        }

        if (!missingFields.isEmpty()) {
            throw new AppException(DomainCode.BAD_REQUEST_ERROR,
                    "Cannot publish draft. Missing required fields: " + String.join(", ", missingFields));
        }
    }

    /**
     * Build legacy full address string from ListingDraft
     * Format: [Street/Project], [Ward Type] [Ward Name], [District Type] [District Name], [Province Name]
     */
    private String buildLegacyFullAddressFromDraft(ListingDraft draft) {
        StringBuilder sb = new StringBuilder();

        // Add street or project name
        if (draft.getStreet() != null && !draft.getStreet().isEmpty()) {
            sb.append(draft.getStreet()).append(", ");
        } else if (draft.getProjectId() != null) {
            projectRepository.findById(draft.getProjectId().intValue())
                    .ifPresent(project -> sb.append(project.getName()).append(", "));
        }

        // Add ward
        if (draft.getWardId() != null) {
            legacyWardRepository.findById(draft.getWardId().intValue())
                    .ifPresent(ward -> {
                        if (ward.getType() != null && !ward.getType().isEmpty()) {
                            sb.append(ward.getType()).append(" ");
                        }
                        sb.append(ward.getName()).append(", ");
                    });
        }

        // Add district
        if (draft.getDistrictId() != null) {
            legacyDistrictRepository.findById(draft.getDistrictId().intValue())
                    .ifPresent(district -> {
                        if (district.getType() != null && !district.getType().isEmpty()) {
                            sb.append(district.getType()).append(" ");
                        }
                        sb.append(district.getName()).append(", ");
                    });
        }

        // Add province
        if (draft.getProvinceId() != null) {
            legacyProvinceRepository.findById(draft.getProvinceId().intValue())
                    .ifPresent(province -> sb.append(province.getName()));
        }

        return sb.toString().trim();
    }

    /**
     * Build new full address string from ListingDraft
     * Format: [Street/Project], [Ward Name], [Province Name]
     */
    private String buildNewFullAddressFromDraft(ListingDraft draft) {
        StringBuilder sb = new StringBuilder();

        // Add street or project name
        if (draft.getNewStreet() != null && !draft.getNewStreet().isEmpty()) {
            sb.append(draft.getNewStreet()).append(", ");
        } else if (draft.getProjectId() != null) {
            projectRepository.findById(draft.getProjectId().intValue())
                    .ifPresent(project -> sb.append(project.getName()).append(", "));
        }

        // Add ward
        if (draft.getWardCode() != null && !draft.getWardCode().isEmpty()) {
            wardRepository.findByCode(draft.getWardCode())
                    .ifPresent(ward -> sb.append(ward.getName()).append(", "));
        }

        // Add province
        if (draft.getProvinceCode() != null && !draft.getProvinceCode().isEmpty()) {
            provinceRepository.findByCode(draft.getProvinceCode())
                    .ifPresent(province -> sb.append(province.getName()));
        }

        return sb.toString().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public com.smartrent.dto.response.MapListingsResponse getListingsByMapBounds(
            MapBoundsRequest request) {
        log.info("Getting listings by map bounds - NE: ({}, {}), SW: ({}, {}), zoom: {}, limit: {}",
                request.getNeLat(), request.getNeLng(), request.getSwLat(), request.getSwLng(),
                request.getZoom(), request.getLimit());

        // Query listings within map bounds using ListingQueryService
        Page<Listing> page = listingQueryService.queryByMapBounds(
                request.getNeLat(),
                request.getNeLng(),
                request.getSwLat(),
                request.getSwLng(),
                request.getLimit() != null ? request.getLimit() : 100,
                request.getVerifiedOnly(),
                request.getCategoryId(),
                request.getVipType()
        );

        // Convert to response DTOs
        List<ListingResponse> listings = page.getContent().stream()
                .map(listing -> {
                    com.smartrent.dto.response.UserCreationResponse user = buildUserResponse(listing.getUserId());
                    com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(listing.getAddress());
                    ListingResponse response = listingMapper.toResponse(listing, user, addressResponse);
                    populateOwnerZaloInfo(response, listing.getUserId());
                    return response;
                })
                .collect(Collectors.toList());

        // Build bounds info
        com.smartrent.dto.response.MapListingsResponse.MapBoundsInfo boundsInfo =
                com.smartrent.dto.response.MapListingsResponse.MapBoundsInfo.builder()
                        .neLat(request.getNeLat())
                        .neLng(request.getNeLng())
                        .swLat(request.getSwLat())
                        .swLng(request.getSwLng())
                        .zoom(request.getZoom())
                        .build();

        // Build response
        com.smartrent.dto.response.MapListingsResponse response =
                com.smartrent.dto.response.MapListingsResponse.builder()
                        .listings(listings)
                        .totalCount(page.getTotalElements())
                        .returnedCount(listings.size())
                        .hasMore(page.getTotalElements() > listings.size())
                        .bounds(boundsInfo)
                        .build();

        log.info("Map bounds query returned {} listings out of {} total",
                listings.size(), page.getTotalElements());

        return response;
    }
}
