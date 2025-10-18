package com.smartrent.service.listing.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.ListingCreationRequest;
import com.smartrent.dto.request.ListingRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.request.VipListingCreationRequest;
import com.smartrent.dto.response.ListingCreationResponse;
import com.smartrent.dto.response.ListingResponse;
import com.smartrent.dto.response.ListingResponseWithAdmin;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.enums.BenefitType;
import com.smartrent.enums.PostSource;
import com.smartrent.infra.repository.AdminRepository;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.entity.Admin;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.mapper.ListingMapper;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.quota.QuotaService;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.transaction.TransactionService;
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
    AdminRepository adminRepository;
    ListingMapper listingMapper;
    QuotaService quotaService;
    TransactionService transactionService;
    TransactionRepository transactionRepository;
    PaymentService paymentService;

    @Override
    @Transactional
    public ListingCreationResponse createListing(ListingCreationRequest request) {
        Listing listing = listingMapper.toEntity(request);
        Listing saved = listingRepository.save(listing);
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

                // Create listing with postSource = QUOTA
                Listing listing = buildListingFromVipRequest(request);
                listing.setPostSource(PostSource.QUOTA);
                listing.setTransactionId(null);

                // Check if user has AUTO_APPROVE benefit
                // TODO: Implement auto-verification check

                Listing saved = listingRepository.save(listing);

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

        // Generate payment URL
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(amount)
                .orderInfo("Post " + request.getVipType() + " listing: " + request.getTitle())
                .returnUrl(request.getReturnUrl())
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

    private Listing buildListingFromVipRequest(VipListingCreationRequest request) {
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
                .addressId(request.getAddressId())
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
                .addressId(premiumListing.getAddressId())
                .area(premiumListing.getArea())
                .bedrooms(premiumListing.getBedrooms())
                .bathrooms(premiumListing.getBathrooms())
                .direction(premiumListing.getDirection())
                .furnishing(premiumListing.getFurnishing())
                .propertyType(premiumListing.getPropertyType())
                .roomCapacity(premiumListing.getRoomCapacity())
                .verified(premiumListing.getVerified())
                .isVerify(premiumListing.getIsVerify())
                .expired(premiumListing.getExpired())
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
        return listingMapper.toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> getListingsByIds(Set<Long> ids) {
        return listingRepository.findByIdsWithAmenities(ids).stream()
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
}