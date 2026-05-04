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
import com.smartrent.dto.response.AddressConversionResponse;
import com.smartrent.dto.response.DraftListingResponse;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingCardListResponse;
import com.smartrent.dto.response.ListingCardResponse;
import com.smartrent.dto.response.ListingListResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.ProvinceListingStatsResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.enums.ModerationStatus;
import com.smartrent.enums.PostSource;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.DomainException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.AddressRepository;
// AddressMetadataRepository removed — queries now use addresses table directly
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.AmenityRepository;
import com.smartrent.infra.repository.CategoryRepository;
import com.smartrent.infra.repository.LegacyProvinceRepository;
import com.smartrent.infra.repository.LegacyDistrictRepository;
import com.smartrent.infra.repository.LegacyWardRepository;
import com.smartrent.infra.repository.ListingDraftRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.MediaRepository;
import com.smartrent.infra.repository.AddressMappingRepository;
import com.smartrent.infra.repository.ProvinceRepository;
import com.smartrent.infra.repository.WardRepository;
import com.smartrent.infra.repository.ProjectRepository;
import com.smartrent.infra.repository.SavedListingRepository;
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
import com.smartrent.service.address.AddressService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    AdminRepository adminRepository;
    UserRepository userRepository;
    LegacyProvinceRepository legacyProvinceRepository;
    LegacyDistrictRepository legacyDistrictRepository;
    LegacyWardRepository legacyWardRepository;
    ProvinceRepository provinceRepository;
    AddressMappingRepository addressMappingRepository;
    WardRepository wardRepository;
    ProjectRepository projectRepository;
    SavedListingRepository savedListingRepository;
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
    AddressService addressService;
    ListingRequestCacheService listingRequestCacheService;
    ListingQueryService listingQueryService;
    com.smartrent.service.moderation.ListingModerationService listingModerationService;

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
    @Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_DETAIL,
            key = "#id",
            unless = "#result == null")
    public ListingResponse getListingById(Long id) {
        log.info("getListingById called for id={}", id);

        // Fetch listing with amenities first
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        // Fetch media separately to avoid MultipleBagFetchException
        listingRepository.findByIdWithMedia(id);

        // Load user once — reuse for both user response and Zalo info
        User userEntity = listing.getUserId() != null
                ? userRepository.findById(listing.getUserId()).orElse(null) : null;
        com.smartrent.dto.response.UserCreationResponse user = userEntity != null
                ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity) : null;
        com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(listing.getAddress());

        // Build basic response with user and address
        ListingResponse response = listingMapper.toResponse(listing, user, addressResponse);

        log.info("getListingById completed for id={}, title={}", id, response.getTitle());
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
    @Cacheable(cacheNames = com.smartrent.config.Constants.CacheNames.LISTING_BROWSE,
            key = "'page:' + #page + ':size:' + #size",
            unless = "#result == null || #result.isEmpty()")
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
    public ListingResponse updateListing(Long id, ListingRequest request, String userId) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new DomainException(DomainCode.LISTING_NOT_FOUND));

        // Validate ownership
        if (!existing.getUserId().equals(userId)) {
            throw new DomainException(DomainCode.NOT_LISTING_OWNER);
        }

        // Block updates on SUSPENDED listings
        if (existing.getModerationStatus() == ModerationStatus.SUSPENDED) {
            throw new DomainException(DomainCode.UPDATE_NOT_ALLOWED);
        }
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
        return listingMapper.toResponse(saved, user, addressResponse);
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
     * Batch-load all relationships (users, amenities, media) and map listings to responses.
     * Reduces N+1 from ~33 queries to 4 queries for a page of 10 listings.
     */
    private List<ListingResponse> batchMapListings(List<Listing> listings) {
        if (listings.isEmpty()) return Collections.emptyList();

        List<Listing> validListings = listings.stream()
                .filter(l -> l.getListingId() != null)
                .collect(Collectors.toList());
        if (validListings.size() < listings.size()) {
            log.warn("batchMapListings: {} listing(s) had null listingId and were excluded",
                    listings.size() - validListings.size());
        }
        listings = validListings;

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
                    return listingMapper.toResponse(listing, userResponse, addressResponse);
                })
                .collect(Collectors.toList());
    }

    private List<ListingCardResponse> batchMapCardListings(List<Listing> listings) {
        if (listings.isEmpty()) return Collections.emptyList();

        List<Listing> validListings = listings.stream()
                .filter(l -> l.getListingId() != null)
                .collect(Collectors.toList());
        listings = validListings;

        List<Long> listingIds = listings.stream()
                .map(Listing::getListingId)
                .collect(Collectors.toList());

        // 1 query: batch-load users
        Set<String> userIds = listings.stream()
                .map(Listing::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 1 query: batch-load media (no amenities needed for cards)
        listingRepository.findByIdsWithMedia(listingIds);

        return listings.stream()
                .map(listing -> {
                    User user = userMap.get(listing.getUserId());
                    com.smartrent.dto.response.UserCreationResponse userResponse =
                            user != null ? userMapper.mapFromUserEntityToUserCreationResponse(user) : null;
                    com.smartrent.dto.response.AddressResponse addressResponse = buildAddressResponse(listing.getAddress());
                    return listingMapper.toCardResponse(listing, userResponse, addressResponse);
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
    public ListingCardListResponse searchListings(ListingFilterRequest filter) {
        log.info("Unified search - UserId: {}, Category: {}, Province: {}/{}, isDraft: {}, Page: {}, Size: {}",
                filter.getUserId(), filter.getCategoryId(), filter.getProvinceId(), filter.getProvinceCodes(),
                filter.getIsDraft(), filter.getPage(), filter.getSize());

        String normalizedKeyword = TextNormalizer.normalize(filter.getKeyword());
        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()
                && (normalizedKeyword == null || normalizedKeyword.length() < 3)) {
            return ListingCardListResponse.builder()
                    .listings(Collections.emptyList())
                    .totalCount(0L)
                    .currentPage(1)
                    .pageSize(filter.getSize() != null ? filter.getSize() : 20)
                    .totalPages(0)
                    .build();
        }

        // Merge single provinceCode into provinceCodes list (FE typically sends provinceCode singular)
        if (filter.getProvinceCode() != null && !filter.getProvinceCode().isBlank()) {
            List<String> merged = new ArrayList<>();
            if (filter.getProvinceCodes() != null) merged.addAll(filter.getProvinceCodes());
            if (!merged.contains(filter.getProvinceCode())) merged.add(filter.getProvinceCode());
            filter.setProvinceCodes(merged);
        }

        // Resolve legacy province IDs so the spec can match old-structure listings too.
        // address_mapping stores province codes with leading zeros (e.g. "01" for Hanoi),
        // but FE typically sends without leading zeros ("1"). Pass both formats to cover both cases.
        if (filter.getProvinceCodes() != null && !filter.getProvinceCodes().isEmpty()) {
            Set<String> codesToQuery = new java.util.LinkedHashSet<>();
            for (String code : filter.getProvinceCodes()) {
                String stripped = code.replaceFirst("^0+(?!$)", "");
                codesToQuery.add(stripped);
                // Also add zero-padded version (2-digit) in case address_mapping uses that format
                try {
                    codesToQuery.add(String.format("%02d", Integer.parseInt(stripped)));
                } catch (NumberFormatException ignored) {}
            }

            List<Object[]> pairs = addressMappingRepository
                    .findNewCodeToLegacyIdPairs(new ArrayList<>(codesToQuery));
            List<Integer> legacyIds;
            if (!pairs.isEmpty()) {
                legacyIds = pairs.stream()
                        .map(p -> ((Number) p[1]).intValue())
                        .distinct()
                        .collect(Collectors.toList());
                log.info("Resolved province codes {} → legacy IDs {} (via address_mapping)", codesToQuery, legacyIds);
            } else {
                // Fallback: address_mapping has no data or code format mismatch.
                // Directly find LegacyProvince by code (handles non-merged provinces).
                List<LegacyProvince> directMatches = legacyProvinceRepository.findByCodeIn(new ArrayList<>(codesToQuery));
                legacyIds = directMatches.stream()
                        .map(LegacyProvince::getId)
                        .distinct()
                        .collect(Collectors.toList());
                if (!legacyIds.isEmpty()) {
                    log.info("Resolved province codes {} → legacy IDs {} (via direct LegacyProvince lookup)", codesToQuery, legacyIds);
                } else {
                    log.warn("No legacy IDs resolved for province codes {} — old-structure listings won't be included", codesToQuery);
                }
            }
            if (!legacyIds.isEmpty()) {
                filter.setResolvedLegacyProvinceIds(legacyIds);
            }
            log.info("Search province filter: provinceCodes={}, resolvedLegacyIds={}", filter.getProvinceCodes(), filter.getResolvedLegacyProvinceIds());
        }

        // Reverse direction: resolve legacy provinceId → new province codes so the
        // spec can also match listings stored under the new structure (their
        // addresses.legacy_province_id may be a different merged-province ID, e.g.
        // a Bình Dương listing whose new_province_code is HCM after the 2025 merger).
        // Without this, filtering by old provinceId returns 0 for listings that
        // only carry new_province_code.
        if (filter.getProvinceId() != null && !filter.getProvinceId().isBlank()) {
            try {
                Integer legacyId = Integer.parseInt(filter.getProvinceId());
                List<String> newCodes = addressMappingRepository
                        .findNewProvinceCodesByLegacyProvinceIds(List.of(legacyId));
                if (newCodes.isEmpty()) {
                    // Fallback: address_mapping has no row for this legacy province.
                    // Handles non-merged provinces whose code is unchanged (e.g. HCM '79' before/after).
                    LegacyProvince lp = legacyProvinceRepository.findById(legacyId).orElse(null);
                    if (lp != null) {
                        List<String> direct = provinceRepository
                                .findByCodeIn(java.util.Collections.singletonList(lp.getCode()))
                                .stream().map(Province::getCode).distinct().toList();
                        if (!direct.isEmpty()) {
                            newCodes = direct;
                            log.info("Resolved legacy provinceId {} (code {}) → new codes {} (via direct Province.findByCode fallback)",
                                    legacyId, lp.getCode(), newCodes);
                        }
                    }
                }
                if (!newCodes.isEmpty()) {
                    filter.setResolvedNewProvinceCodes(newCodes);
                    log.info("Resolved legacy provinceId {} → new province codes {}", legacyId, newCodes);
                } else {
                    log.warn("No new province codes resolved for legacy provinceId {} — new-structure listings won't be matched", legacyId);
                }
            } catch (NumberFormatException ignored) {
                log.warn("provinceId {} is not a valid integer — skipping legacy→new resolution", filter.getProvinceId());
            }
        }

        // Resolve newWardCode to legacy ward IDs so the spec can match old-structure
        // listings (whose addresses.new_ward_code is NULL because they predate the
        // 2-tier reform or weren't backfilled). Without this, filtering by newWardCode
        // returns 0 rows for any pre-existing legacy listing.
        if (filter.getNewWardCode() != null && !filter.getNewWardCode().isBlank()) {
            // Pass the same normalized province-code set we used for province resolution
            List<String> wardScopeCodes = filter.getProvinceCodes();
            if (wardScopeCodes != null && !wardScopeCodes.isEmpty()) {
                Set<String> normalized = new java.util.LinkedHashSet<>();
                for (String code : wardScopeCodes) {
                    String stripped = code.replaceFirst("^0+(?!$)", "");
                    normalized.add(stripped);
                    try {
                        normalized.add(String.format("%02d", Integer.parseInt(stripped)));
                    } catch (NumberFormatException ignored) {}
                }
                wardScopeCodes = new ArrayList<>(normalized);
            }
            List<Integer> legacyWardIds = addressMappingRepository
                    .findLegacyWardIdsByNewWardCode(filter.getNewWardCode(), wardScopeCodes);
            if (!legacyWardIds.isEmpty()) {
                filter.setResolvedLegacyWardIds(legacyWardIds);
                log.info("Resolved newWardCode {} (provinces {}) → legacy ward IDs {}",
                        filter.getNewWardCode(), wardScopeCodes, legacyWardIds);
            } else {
                log.warn("No legacy ward IDs resolved for newWardCode {} — old-structure listings won't be matched",
                        filter.getNewWardCode());
            }
        }

        // Reverse direction: resolve legacy districtId → new ward codes so the
        // spec can match new-structure listings (which have NULL legacy_district_id
        // because the 2-tier reform removed the district level). Without this,
        // any listing created in NEW mode is filtered out the moment user picks
        // a district in LEGACY mode.
        if (filter.getDistrictId() != null) {
            List<String> newWardCodes = addressMappingRepository
                    .findNewWardCodesByLegacyDistrictId(filter.getDistrictId());
            if (!newWardCodes.isEmpty()) {
                filter.setResolvedNewWardCodesForDistrict(newWardCodes);
                log.info("Resolved legacy districtId {} → {} new ward codes (used for OR with new-structure listings)",
                        filter.getDistrictId(), newWardCodes.size());
            } else {
                log.warn("No new ward codes resolved for legacy districtId {} — new-structure listings won't be matched",
                        filter.getDistrictId());
            }
        }

        // Reverse direction: resolve legacy wardId → new ward codes so the spec
        // can also match listings stored under the new structure when FE filters
        // by old-structure ward.
        if (filter.getWardId() != null && !filter.getWardId().isBlank()) {
            try {
                Integer legacyWardIdInt = Integer.parseInt(filter.getWardId());
                List<String> newWardCodes = addressMappingRepository
                        .findNewWardCodesByLegacyWardIds(List.of(legacyWardIdInt));
                if (!newWardCodes.isEmpty()) {
                    filter.setResolvedNewWardCodes(newWardCodes);
                    log.info("Resolved legacy wardId {} → new ward codes {}", legacyWardIdInt, newWardCodes);
                } else {
                    log.warn("No new ward codes resolved for legacy wardId {} — new-structure listings won't be matched", legacyWardIdInt);
                }
            } catch (NumberFormatException ignored) {
                log.warn("wardId {} is not a valid integer — skipping legacy→new resolution", filter.getWardId());
            }
        }

        // Execute query using shared query service
        Page<Listing> page = listingQueryService.executeQuery(filter);

        // Batch-load relationships and map to card DTOs (3 queries: users + media, no amenities)
        List<ListingCardResponse> listings = batchMapCardListings(page.getContent());

        return ListingCardListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .build();
    }

            @Override
            @Transactional(readOnly = true)
            public ListingCardListResponse getTopSavedListingsByUser(String userId, int limit) {
            int safeLimit = Math.min(Math.max(limit, 1), 20);

            List<Object[]> rows = savedListingRepository.findTopSavedListingIdsForOwner(
                userId,
                PageRequest.of(0, safeLimit)
            );

            if (rows.isEmpty()) {
                return ListingCardListResponse.builder()
                    .listings(Collections.emptyList())
                    .totalCount(0L)
                    .currentPage(1)
                    .pageSize(safeLimit)
                    .totalPages(0)
                    .build();
            }

            List<Long> listingIds = rows.stream()
                .map(row -> ((Number) row[0]).longValue())
                .collect(Collectors.toList());

            List<Listing> listingEntities = listingRepository.findByListingIdIn(listingIds);
            Map<Long, Listing> listingMap = listingEntities.stream()
                .collect(Collectors.toMap(Listing::getListingId, Function.identity(), (a, b) -> a));

            List<Listing> orderedListings = listingIds.stream()
                .map(listingMap::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            List<ListingCardResponse> listingResponses = batchMapCardListings(orderedListings);

            return ListingCardListResponse.builder()
                .listings(listingResponses)
                .totalCount((long) listingResponses.size())
                .currentPage(1)
                .pageSize(safeLimit)
                .totalPages(listingResponses.isEmpty() ? 0 : 1)
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

        // Build a unified set of provinceCodes (new 34-province codes) preserving input order
        Set<String> provinceCodeSet = new LinkedHashSet<>();
        if (request.getProvinceCodes() != null) {
            provinceCodeSet.addAll(request.getProvinceCodes());
        }

        // Convert any legacy provinceIds to codes via address_mapping table
        if (request.getProvinceIds() != null && !request.getProvinceIds().isEmpty()) {
            List<LegacyProvince> legacyProvinces = legacyProvinceRepository.findAllById(request.getProvinceIds());
            List<String> legacyCodes = legacyProvinces.stream()
                    .map(LegacyProvince::getCode).collect(Collectors.toList());
            if (!legacyCodes.isEmpty()) {
                List<String> resolvedCodes = addressMappingRepository
                        .findNewProvinceCodesByLegacyProvinceCodes(legacyCodes);
                provinceCodeSet.addAll(resolvedCodes);
            }
        }

        if (provinceCodeSet.isEmpty()) {
            log.warn("Province stats request: no valid provinceCodes or provinceIds resolved");
            return Collections.emptyList();
        }

        List<String> provinceCodes = new ArrayList<>(provinceCodeSet);

        // Build canonical (stripped) code → original code mapping for lookup normalization.
        // address_mapping may store codes with leading zeros ("01") while FE sends "1".
        // We query both formats and then normalize the result back to the original key.
        Map<String, String> strippedToOriginal = new LinkedHashMap<>();
        Set<String> codesToQuery = new java.util.LinkedHashSet<>();
        for (String code : provinceCodes) {
            String stripped = code.replaceFirst("^0+(?!$)", "");
            strippedToOriginal.put(stripped, code);
            codesToQuery.add(stripped);
            try { codesToQuery.add(String.format("%02d", Integer.parseInt(stripped))); } catch (NumberFormatException ignored) {}
        }

        // Try to resolve legacyId → provinceCode mapping via address_mapping
        List<Object[]> codeIdPairs = addressMappingRepository.findNewCodeToLegacyIdPairs(new ArrayList<>(codesToQuery));
        Map<Integer, String> legacyIdToCode = new HashMap<>();
        List<Integer> allLegacyIds = new ArrayList<>();
        for (Object[] pair : codeIdPairs) {
            String rawCode = (String) pair[0];
            String strippedRaw = rawCode.replaceFirst("^0+(?!$)", "");
            String originalCode = strippedToOriginal.getOrDefault(strippedRaw, rawCode);
            Integer legacyId = ((Number) pair[1]).intValue();
            legacyIdToCode.putIfAbsent(legacyId, originalCode);
            allLegacyIds.add(legacyId);
        }

        // Accumulator: provinceCode → [total, verified, vip]
        Map<String, long[]> aggregated = new LinkedHashMap<>();
        for (String code : provinceCodes) {
            aggregated.put(code, new long[]{0L, 0L, 0L});
        }

        // Fallback: if address_mapping has no data, use provinceIds from request directly.
        // FE sends provinceIds: [1, 79, ...] and provinceCodes: ["1", "79", ...] with matching numeric values.
        if (allLegacyIds.isEmpty() && request.getProvinceIds() != null && !request.getProvinceIds().isEmpty()) {
            for (Integer id : request.getProvinceIds()) {
                String codeKey = strippedToOriginal.getOrDefault(String.valueOf(id), String.valueOf(id));
                if (aggregated.containsKey(codeKey)) {
                    legacyIdToCode.put(id, codeKey);
                    allLegacyIds.add(id);
                }
            }
            log.debug("address_mapping empty — built legacyIdToCode directly from request provinceIds: {}", legacyIdToCode);
        }

        // Aggregate new-structure listings (addresses.new_province_code)
        boolean verifiedOnly = Boolean.TRUE.equals(request.getVerifiedOnly());
        List<Object[]> newStats = listingRepository.getListingStatsByProvinceCodes(
                provinceCodes, verifiedOnly);
        for (Object[] row : newStats) {
            String code = (String) row[0];
            long[] acc = aggregated.get(code);
            if (acc != null) {
                acc[0] += toLong(row[1]);
                acc[1] += toLong(row[2]);
                acc[2] += toLong(row[3]);
            }
        }

        // Aggregate old-structure listings (addresses.legacy_province_id)
        // Exclude listings already counted via new_province_code to avoid double-counting
        if (!allLegacyIds.isEmpty()) {
            List<Object[]> oldStats = listingRepository.getListingStatsByProvinceIdsWithoutNewCode(
                    allLegacyIds, verifiedOnly);
            for (Object[] row : oldStats) {
                Integer legacyId = ((Number) row[0]).intValue();
                String code = legacyIdToCode.get(legacyId);
                long[] acc = (code != null) ? aggregated.get(code) : null;
                if (acc != null) {
                    acc[0] += toLong(row[1]);
                    acc[1] += toLong(row[2]);
                    acc[2] += toLong(row[3]);
                }
            }
        }

        // Load province names for new structure
        Map<String, String> provinceNames = provinceRepository.findByCodeIn(provinceCodes)
                .stream().collect(Collectors.toMap(Province::getCode, Province::getName));

        // Create reverse mapping for response
        Map<String, Integer> codeToLegacyId = new HashMap<>();
        for (Map.Entry<Integer, String> entry : legacyIdToCode.entrySet()) {
            codeToLegacyId.putIfAbsent(entry.getValue(), entry.getKey());
        }

        // Build response list (preserves insertion order from LinkedHashMap)
        List<ProvinceListingStatsResponse> results = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : aggregated.entrySet()) {
            String code = entry.getKey();
            long[] counts = entry.getValue();
            long verified = counts[1];

            if (Boolean.TRUE.equals(request.getVerifiedOnly()) && verified == 0) {
                continue;
            }

            results.add(ProvinceListingStatsResponse.builder()
                    .provinceId(codeToLegacyId.get(code))
                    .provinceCode(code)
                    .provinceName(provinceNames.getOrDefault(code, "Unknown Province"))
                    .totalListings(counts[0])
                    .verifiedListings(verified)
                    .vipListings(counts[2])
                    .build());
        }

        log.info("Province stats retrieved successfully - {} results (aggregated old+new structures)", results.size());
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
        boolean verifiedOnly = Boolean.TRUE.equals(request.getVerifiedOnly());
        List<Object[]> statsData = listingRepository.getListingStatsByCategoryIds(
                request.getCategoryIds(), verifiedOnly);

        // Batch-load all categories in 1 query (avoids N+1)
        Map<Long, Category> categoryMap = categoryRepository.findAllById(request.getCategoryIds())
                .stream().collect(Collectors.toMap(Category::getCategoryId, Function.identity()));

        // Map to response objects
        for (Object[] row : statsData) {
            Long categoryId = toLong(row[0]);
            Long totalCount = toLong(row[1]);
            Long verifiedCount = toLong(row[2]);
            Long vipCount = toLong(row[3]);

            Category category = categoryMap.get(categoryId);
            if (category == null) {
                log.warn("Category not found: {}", categoryId);
                continue;
            }

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

        // Get listing with amenities (also loads address via JOIN)
        Listing listing = listingRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new AppException(DomainCode.LISTING_NOT_FOUND, "Listing not found"));

        // Validate ownership
        if (!listing.getUserId().equals(userId)) {
            throw new AppException(DomainCode.UNAUTHORIZED,
                    "You don't have permission to view this listing's detail");
        }

        // Load user once — same owner for all cases
        com.smartrent.dto.response.UserCreationResponse user = userRepository.findById(userId)
                .map(userMapper::mapFromUserEntityToUserCreationResponse)
                .orElse(null);

        // Get active media for this listing (single query, filtered by status)
        List<com.smartrent.dto.response.MediaResponse> mediaResponses =
                mediaRepository.findByListing_ListingIdAndStatusOrderBySortOrderAsc(id, Media.MediaStatus.ACTIVE)
                        .stream().map(mediaMapper::toResponse).collect(Collectors.toList());

        com.smartrent.dto.response.AddressResponse addressResponse =
                listing.getAddress() != null ? addressMapper.toResponse(listing.getAddress()) : null;

        com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics statistics =
                com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics.builder()
                        .viewCount(0L).contactCount(0L).saveCount(0L).reportCount(0L).build();

        com.smartrent.dto.response.ListingResponseForOwner response = listingMapper.toResponseForOwner(
                listing, user, mediaResponses, addressResponse,
                null, statistics,
                listing.getLastModerationReasonText(),
                listing.getLastModerationReasonCode()
        );

        // Populate moderation context (pendingOwnerAction & moderationTimeline)
        response.setPendingOwnerAction(listingModerationService.getOwnerPendingAction(id));
        response.setModerationTimeline(listingModerationService.getModerationTimeline(id));

        return response;
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

        List<Listing> content = page.getContent();

        List<com.smartrent.dto.response.ListingResponseWithAdmin> listings;

        if (!content.isEmpty()) {
            List<Long> listingIds = content.stream()
                    .map(Listing::getListingId).collect(Collectors.toList());

            // ---- Batch-load all related data (avoids N+1) ----

            // 1. Batch-load addresses into Hibernate session cache
            listingRepository.findByIdsWithAmenities(listingIds);

            // 2. Batch-load all distinct users — 1 query
            Set<String> userIds = content.stream()
                    .map(Listing::getUserId).filter(id -> id != null).collect(Collectors.toSet());
            Map<String, com.smartrent.infra.repository.entity.User> userMap = userIds.isEmpty()
                    ? Collections.emptyMap()
                    : userRepository.findAllById(userIds).stream()
                            .collect(Collectors.toMap(
                                    com.smartrent.infra.repository.entity.User::getUserId,
                                    Function.identity()));

            // 3. Batch-load all distinct admins who updated listings — 1 query
            Set<String> adminIds = content.stream()
                    .map(Listing::getUpdatedBy).filter(id -> id != null)
                    .map(String::valueOf).collect(Collectors.toSet());
            Map<String, Admin> adminMap = adminIds.isEmpty()
                    ? Collections.emptyMap()
                    : adminRepository.findAllById(adminIds).stream()
                            .collect(Collectors.toMap(Admin::getAdminId, Function.identity()));

            // ---- Map to response DTOs ----
            listings = content.stream()
                    .map(listing -> {
                        // Look up admin from batch-loaded map
                        Admin verifyingAdmin = listing.getUpdatedBy() != null
                                ? adminMap.get(String.valueOf(listing.getUpdatedBy()))
                                : null;

                        String verificationStatus;
                        if (listing.getVerified()) {
                            verificationStatus = "APPROVED";
                        } else if (listing.getIsVerify()) {
                            verificationStatus = "PENDING";
                        } else {
                            verificationStatus = "NOT_SUBMITTED";
                        }

                        // Look up user from batch-loaded map
                        com.smartrent.infra.repository.entity.User userEntity = userMap.get(listing.getUserId());
                        com.smartrent.dto.response.UserCreationResponse user = userEntity != null
                                ? userMapper.mapFromUserEntityToUserCreationResponse(userEntity)
                                : null;

                        return listingMapper.toResponseWithAdmin(listing, user, verifyingAdmin, verificationStatus, null);
                    })
                    .collect(Collectors.toList());
        } else {
            listings = Collections.emptyList();
        }

        // Calculate statistics — 1 query instead of 10
        com.smartrent.dto.response.AdminListingListResponse.AdminStatistics statistics =
                calculateAdminStatistics();

        return com.smartrent.dto.response.AdminListingListResponse.builder()
                .listings(listings)
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .filterCriteria(filter)
                .statistics(statistics)
                .build();
    }

    /**
     * Calculate admin statistics in a single query instead of 10 separate COUNT queries
     */
    private com.smartrent.dto.response.AdminListingListResponse.AdminStatistics calculateAdminStatistics() {
        List<Object[]> results = listingRepository.getAdminStatistics();
        if (results == null || results.isEmpty()) {
            return com.smartrent.dto.response.AdminListingListResponse.AdminStatistics.builder()
                    .pendingVerification(0L).verified(0L).expired(0L).rejected(0L)
                    .drafts(0L).shadows(0L)
                    .normalListings(0L).silverListings(0L).goldListings(0L).diamondListings(0L)
                    .build();
        }
        Object[] row = results.get(0);
        return com.smartrent.dto.response.AdminListingListResponse.AdminStatistics.builder()
                .pendingVerification(toLong(row[0]))
                .verified(toLong(row[1]))
                .expired(toLong(row[2]))
                .rejected(toLong(row[3]))
                .drafts(toLong(row[4]))
                .shadows(toLong(row[5]))
                .normalListings(toLong(row[6]))
                .silverListings(toLong(row[7]))
                .goldListings(toLong(row[8]))
                .diamondListings(toLong(row[9]))
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

        // The seller dashboard must show ALL of the owner's listings — including
        // expired / taken-down ones — so the "All" and "Expired" tabs aren't
        // silently emptied. The DTO defaults excludeExpired=true (intended for
        // public search), so we override it here. Status filtering for the seller
        // dashboard is driven by `listingStatus`, not `excludeExpired`.
        filter.setExcludeExpired(false);

        // Execute query using shared query service
        Page<Listing> page = listingQueryService.executeQuery(filter);

        List<Listing> content = page.getContent();

        // ---- Batch-load all related data in bulk (avoids N+1) ----

        // 1. Single user query — all listings belong to the same owner
        com.smartrent.dto.response.UserCreationResponse ownerResponse = userRepository.findById(userId)
                .map(userMapper::mapFromUserEntityToUserCreationResponse)
                .orElse(null);

        if (!content.isEmpty()) {
            List<Long> listingIds = content.stream()
                    .map(Listing::getListingId).collect(Collectors.toList());

            // 2. Batch-load addresses into Hibernate session cache
            listingRepository.findByIdsWithAmenities(listingIds);

            // 3. Batch-load active media for all listings — 1 query
            Map<Long, List<com.smartrent.dto.response.MediaResponse>> mediaByListing =
                    mediaRepository.findActiveMediaByListingIds(listingIds).stream()
                            .collect(Collectors.groupingBy(
                                    m -> m.getListing().getListingId(),
                                    Collectors.mapping(mediaMapper::toResponse, Collectors.toList())
                            ));

            // 4. Batch-load pending owner actions — 1 query
            Map<Long, com.smartrent.dto.response.OwnerActionResponse> pendingActions =
                    listingModerationService.getOwnerPendingActions(listingIds);

            // ---- Map to response DTOs ----
            List<com.smartrent.dto.response.ListingResponseForOwner> listings = content.stream()
                    .map(listing -> {
                        Long lid = listing.getListingId();

                        com.smartrent.dto.response.AddressResponse addressResp =
                                listing.getAddress() != null ? addressMapper.toResponse(listing.getAddress()) : null;

                        List<com.smartrent.dto.response.MediaResponse> mediaResponses =
                                mediaByListing.getOrDefault(lid, Collections.emptyList());

                        com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics statistics =
                                com.smartrent.dto.response.ListingResponseForOwner.ListingStatistics.builder()
                                        .viewCount(0L).contactCount(0L).saveCount(0L).reportCount(0L).build();

                        com.smartrent.dto.response.ListingResponseForOwner ownerListingResponse = listingMapper.toResponseForOwner(
                                listing, ownerResponse, mediaResponses, addressResp,
                                null, statistics,
                                listing.getLastModerationReasonText(),
                                listing.getLastModerationReasonCode()
                        );

                        ownerListingResponse.setPendingOwnerAction(pendingActions.get(lid));

                        return ownerListingResponse;
                    })
                    .collect(Collectors.toList());

            // 5. Calculate owner statistics — 1 query instead of 9
            com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics ownerStats =
                    calculateOwnerStatistics(userId);

            return com.smartrent.dto.response.OwnerListingListResponse.builder()
                    .listings(listings)
                    .totalCount(page.getTotalElements())
                    .currentPage(page.getNumber() + 1)
                    .pageSize(page.getSize())
                    .totalPages(page.getTotalPages())
                    .filterCriteria(filter)
                    .statistics(ownerStats)
                    .build();
        }

        // Empty page — still need statistics
        com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics ownerStats =
                calculateOwnerStatistics(userId);

        return com.smartrent.dto.response.OwnerListingListResponse.builder()
                .listings(Collections.emptyList())
                .totalCount(page.getTotalElements())
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .filterCriteria(filter)
                .statistics(ownerStats)
                .build();
    }

    /**
     * Calculate owner statistics in a single query instead of 9 separate COUNT queries
     */
    private com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics calculateOwnerStatistics(String userId) {
        List<Object[]> results = listingRepository.getOwnerStatistics(userId);
        if (results == null || results.isEmpty()) {
            return com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics.builder()
                    .drafts(0L).pendingVerification(0L).rejected(0L).active(0L).expired(0L)
                    .normalListings(0L).silverListings(0L).goldListings(0L).diamondListings(0L)
                    .build();
        }
        Object[] row = results.get(0);
        return com.smartrent.dto.response.OwnerListingListResponse.OwnerStatistics.builder()
                .drafts(toLong(row[0]))
                .pendingVerification(toLong(row[1]))
                .rejected(toLong(row[2]))
                .active(toLong(row[3]))
                .expired(toLong(row[4]))
                .normalListings(toLong(row[5]))
                .silverListings(toLong(row[6]))
                .goldListings(toLong(row[7]))
                .diamondListings(toLong(row[8]))
                .build();
    }

    private static Long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
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
     * Build AddressResponse from ListingDraft address fields (unified format)
     */
    private com.smartrent.dto.response.AddressResponse buildAddressResponseFromDraft(ListingDraft draft) {
        Integer legacyProvinceId = draft.getProvinceId() != null ? draft.getProvinceId().intValue() : null;
        Integer legacyDistrictId = draft.getDistrictId() != null ? draft.getDistrictId().intValue() : null;
        Integer legacyWardId = draft.getWardId() != null ? draft.getWardId().intValue() : null;

        String legacyStreet = trimToNull(draft.getStreet());
        String newStreet = trimToNull(draft.getNewStreet());
        if (!hasText(newStreet)) {
            newStreet = legacyStreet;
        }

        String newProvinceCode = trimToNull(draft.getProvinceCode());
        String newWardCode = trimToNull(draft.getWardCode());

        LegacyProvince legacyProvince = null;
        District legacyDistrict = null;
        LegacyWard legacyWard = null;
        Province newProvince = null;
        Ward newWard = null;
        AddressMapping resolvedMapping = null;
        AddressConversionResponse conversionResponse = null;

        if (legacyProvinceId != null) {
            legacyProvince = legacyProvinceRepository.findById(legacyProvinceId).orElse(null);
        }
        if (legacyDistrictId != null) {
            legacyDistrict = legacyDistrictRepository.findById(legacyDistrictId).orElse(null);
        }
        if (legacyWardId != null) {
            legacyWard = legacyWardRepository.findById(legacyWardId).orElse(null);
        }

        // If draft only has legacy triplet, auto-convert to new structure for FE mapping.
        if ((!hasText(newProvinceCode) || !hasText(newWardCode))
                && legacyProvinceId != null
                && legacyDistrictId != null
                && legacyWardId != null) {
            try {
                conversionResponse = addressService.convertLegacyToNew(
                        legacyProvinceId,
                        legacyDistrictId,
                        legacyWardId);

                if (conversionResponse != null
                        && conversionResponse.getNewAddress() != null) {
                    if (!hasText(newProvinceCode)
                            && conversionResponse.getNewAddress().getProvince() != null) {
                        newProvinceCode = trimToNull(
                                conversionResponse.getNewAddress().getProvince().getId());
                    }
                    if (!hasText(newWardCode)
                            && conversionResponse.getNewAddress().getWard() != null) {
                        newWardCode = trimToNull(
                                conversionResponse.getNewAddress().getWard().getCode());
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not convert legacy draft {} to new address codes: {}",
                        draft.getDraftId(), ex.getMessage());
            }

            // Fallback to direct mapping lookup if conversion service cannot provide data.
            if ((!hasText(newProvinceCode) || !hasText(newWardCode))
                    && legacyProvince != null
                    && legacyDistrict != null
                    && legacyWard != null) {
                AddressMapping bestMapping = addressMappingRepository
                        .findBestByLegacyAddress(
                                legacyProvince.getCode(),
                                legacyDistrict.getCode(),
                                legacyWard.getCode())
                        .orElse(null);

                if (bestMapping != null) {
                    resolvedMapping = bestMapping;
                    if (!hasText(newProvinceCode)) {
                        newProvinceCode = trimToNull(bestMapping.getNewProvinceCode());
                    }
                    if (!hasText(newWardCode)) {
                        newWardCode = trimToNull(bestMapping.getNewWardCode());
                    }
                }
            }
        }

        // If draft only has new structure, backfill legacy IDs for compatibility.
        if ((legacyProvinceId == null || legacyDistrictId == null || legacyWardId == null)
                && hasText(newProvinceCode)
                && hasText(newWardCode)) {
            AddressMapping reverseMapping = findBestReverseMappingForDraft(newProvinceCode, newWardCode);

            if (reverseMapping != null) {
                resolvedMapping = resolvedMapping != null ? resolvedMapping : reverseMapping;

                if (legacyProvinceId == null && reverseMapping.getLegacyProvince() != null) {
                    legacyProvinceId = reverseMapping.getLegacyProvince().getId();
                }
                if (legacyDistrictId == null && reverseMapping.getLegacyDistrict() != null) {
                    legacyDistrictId = reverseMapping.getLegacyDistrict().getId();
                }
                if (legacyWardId == null && reverseMapping.getLegacyWard() != null) {
                    legacyWardId = reverseMapping.getLegacyWard().getId();
                }
            }
        }

        if (legacyProvince == null && legacyProvinceId != null) {
            legacyProvince = legacyProvinceRepository.findById(legacyProvinceId).orElse(null);
        }
        if (legacyDistrict == null && legacyDistrictId != null) {
            legacyDistrict = legacyDistrictRepository.findById(legacyDistrictId).orElse(null);
        }
        if (legacyWard == null && legacyWardId != null) {
            legacyWard = legacyWardRepository.findById(legacyWardId).orElse(null);
        }

        if (hasText(newProvinceCode)) {
            newProvince = findProvinceForDraft(newProvinceCode);
        }
        if (hasText(newWardCode)) {
            newWard = findWardForDraft(newWardCode);
        }

        String legacyProvinceName = legacyProvince != null
                ? legacyProvince.getName()
                : (resolvedMapping != null ? trimToNull(resolvedMapping.getLegacyProvinceName()) : null);
        String legacyDistrictName = legacyDistrict != null
                ? legacyDistrict.getName()
                : (resolvedMapping != null ? trimToNull(resolvedMapping.getLegacyDistrictName()) : null);
        String legacyWardName = legacyWard != null
                ? legacyWard.getName()
                : (resolvedMapping != null ? trimToNull(resolvedMapping.getLegacyWardName()) : null);

        String convertedNewProvinceName =
            conversionResponse != null
                && conversionResponse.getNewAddress() != null
                && conversionResponse.getNewAddress().getProvince() != null
                ? trimToNull(conversionResponse.getNewAddress().getProvince().getName())
                : null;
        String convertedNewWardName =
            conversionResponse != null
                && conversionResponse.getNewAddress() != null
                && conversionResponse.getNewAddress().getWard() != null
                ? trimToNull(conversionResponse.getNewAddress().getWard().getName())
                : null;

        String newProvinceName = newProvince != null
                ? newProvince.getName()
            : (hasText(convertedNewProvinceName)
                ? convertedNewProvinceName
                : (resolvedMapping != null ? trimToNull(resolvedMapping.getNewProvinceName()) : null));
        String newWardName = newWard != null
                ? newWard.getName()
            : (hasText(convertedNewWardName)
                ? convertedNewWardName
                : (resolvedMapping != null ? trimToNull(resolvedMapping.getNewWardName()) : null));

        String fullAddress = joinAddressParts(
                legacyStreet,
                legacyWardName,
                legacyDistrictName,
                legacyProvinceName);

        if (!hasText(fullAddress) && (legacyProvinceId != null || legacyDistrictId != null || legacyWardId != null)) {
            ListingDraft legacySource = ListingDraft.builder()
                    .provinceId(legacyProvinceId != null ? legacyProvinceId.longValue() : null)
                    .districtId(legacyDistrictId != null ? legacyDistrictId.longValue() : null)
                    .wardId(legacyWardId != null ? legacyWardId.longValue() : null)
                    .street(legacyStreet)
                    .projectId(draft.getProjectId())
                    .build();
            fullAddress = trimToNull(buildLegacyFullAddressFromDraft(legacySource));
        }

        String fullNewAddress = joinAddressParts(
                newStreet,
                newWardName,
                newProvinceName);

        if (!hasText(fullNewAddress) && (hasText(newProvinceCode) || hasText(newWardCode))) {
            ListingDraft newSource = ListingDraft.builder()
                    .provinceCode(newProvinceCode)
                    .wardCode(newWardCode)
                    .newStreet(newStreet)
                    .projectId(draft.getProjectId())
                    .build();
            fullNewAddress = trimToNull(buildNewFullAddressFromDraft(newSource));
        }

        boolean hasLegacyData = legacyProvinceId != null || legacyDistrictId != null || legacyWardId != null || hasText(legacyStreet);
        boolean hasNewData = hasText(newProvinceCode) || hasText(newWardCode) || hasText(newStreet);
        boolean hasCoordinates = draft.getLatitude() != null || draft.getLongitude() != null;

        if (!hasLegacyData && !hasNewData && !hasCoordinates) {
            return null;
        }

        String effectiveAddressType;
        if (hasNewData) {
            effectiveAddressType = "NEW";
        } else if (hasLegacyData) {
            effectiveAddressType = "OLD";
        } else {
            effectiveAddressType = hasText(draft.getAddressType()) ? draft.getAddressType() : "OLD";
        }

        String unifiedProvinceCode = "NEW".equals(effectiveAddressType) && hasText(newProvinceCode)
            ? newProvinceCode
            : (legacyProvinceId != null ? String.valueOf(legacyProvinceId) : newProvinceCode);
        String unifiedProvinceName = "NEW".equals(effectiveAddressType) && hasText(newProvinceName)
            ? newProvinceName
            : (hasText(legacyProvinceName) ? legacyProvinceName : newProvinceName);
        String unifiedDistrictCode = "NEW".equals(effectiveAddressType)
            ? null
            : (legacyDistrictId != null ? String.valueOf(legacyDistrictId) : null);
        String unifiedDistrictName = "NEW".equals(effectiveAddressType)
            ? null
            : legacyDistrictName;
        String unifiedWardCode = "NEW".equals(effectiveAddressType) && hasText(newWardCode)
            ? newWardCode
            : (legacyWardId != null ? String.valueOf(legacyWardId) : newWardCode);
        String unifiedWardName = "NEW".equals(effectiveAddressType) && hasText(newWardName)
            ? newWardName
            : (hasText(legacyWardName) ? legacyWardName : newWardName);
        String unifiedStreet = "NEW".equals(effectiveAddressType) ? newStreet : legacyStreet;
        if (!hasText(unifiedStreet)) {
            unifiedStreet = hasText(newStreet) ? newStreet : legacyStreet;
        }

        Integer projectId = draft.getProjectId() != null ? draft.getProjectId().intValue() : null;
        String projectName = null;
        if (projectId != null) {
            projectName = projectRepository.findById(projectId)
                    .map(Project::getName)
                    .orElse(null);
        }

        return com.smartrent.dto.response.AddressResponse.builder()
                .addressId(null)
                .fullAddress(trimToNull(fullAddress))
                .fullNewAddress(trimToNull(fullNewAddress))
                .latitude(draft.getLatitude() != null ? java.math.BigDecimal.valueOf(draft.getLatitude()) : null)
                .longitude(draft.getLongitude() != null ? java.math.BigDecimal.valueOf(draft.getLongitude()) : null)
                .provinceCode(trimToNull(unifiedProvinceCode))
                .provinceName(trimToNull(unifiedProvinceName))
                .districtCode(trimToNull(unifiedDistrictCode))
                .districtName(trimToNull(unifiedDistrictName))
                .wardCode(trimToNull(unifiedWardCode))
                .wardName(trimToNull(unifiedWardName))
                .street(trimToNull(unifiedStreet))
                .addressType(effectiveAddressType)
                .legacyProvinceId(legacyProvinceId)
                .legacyProvinceName(trimToNull(legacyProvinceName))
                .legacyDistrictId(legacyDistrictId)
                .legacyDistrictName(trimToNull(legacyDistrictName))
                .legacyWardId(legacyWardId)
                .legacyWardName(trimToNull(legacyWardName))
                .legacyStreet(trimToNull(legacyStreet))
                .newProvinceCode(trimToNull(newProvinceCode))
                .newProvinceName(trimToNull(newProvinceName))
                .newWardCode(trimToNull(newWardCode))
                .newWardName(trimToNull(newWardName))
                .newStreet(trimToNull(newStreet))
                .streetId(draft.getStreetId() != null ? draft.getStreetId().intValue() : null)
                .streetName(trimToNull(unifiedStreet))
                .projectId(projectId)
                .projectName(trimToNull(projectName))
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Address mapping can contain multiple rows per new address. Draft flow must pick a stable
     * candidate instead of throwing non-unique result exceptions.
     */
    private AddressMapping findBestReverseMappingForDraft(String newProvinceCode, String newWardCode) {
        List<AddressMapping> candidates = addressMappingRepository.findByNewAddress(newProvinceCode, newWardCode);
        if (candidates.isEmpty()) {
            return null;
        }

        List<AddressMapping> defaults = candidates.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsDefaultNewWard()))
                .collect(Collectors.toList());

        if (defaults.size() > 1) {
            log.warn("Found {} default address mappings for new address ({}, {}), using first",
                    defaults.size(), newProvinceCode, newWardCode);
            return defaults.get(0);
        }

        if (defaults.size() == 1) {
            return defaults.get(0);
        }

        if (candidates.size() > 1) {
            log.warn("Found {} address mappings for new address ({}, {}) without default, using first",
                    candidates.size(), newProvinceCode, newWardCode);
        }
        return candidates.get(0);
    }

    private Province findProvinceForDraft(String provinceCode) {
        List<Province> candidates = provinceRepository.findByCodeIn(Collections.singletonList(provinceCode));
        if (candidates.size() > 1) {
            log.warn("Found {} provinces for code {}, using first in draft response",
                    candidates.size(), provinceCode);
        }
        return candidates.stream().findFirst().orElse(null);
    }

    private Ward findWardForDraft(String wardCode) {
        List<Ward> candidates = wardRepository.findAllByCode(wardCode);
        if (candidates.size() > 1) {
            log.warn("Found {} wards for code {}, using first in draft response",
                    candidates.size(), wardCode);
        }
        return candidates.stream().findFirst().orElse(null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String joinAddressParts(String... parts) {
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String value = trimToNull(part);
            if (value != null) {
                normalized.add(value);
            }
        }
        return normalized.isEmpty() ? null : String.join(", ", normalized);
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
            Ward ward = findWardForDraft(draft.getWardCode());
            if (ward != null) {
                sb.append(ward.getName()).append(", ");
            }
        }

        // Add province
        if (draft.getProvinceCode() != null && !draft.getProvinceCode().isEmpty()) {
            Province province = findProvinceForDraft(draft.getProvinceCode());
            if (province != null) {
                sb.append(province.getName());
            }
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

        // Batch-load all relationships and map to DTOs (avoids N+1)
        List<ListingResponse> listings = batchMapListings(page.getContent());

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
