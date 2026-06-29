package com.smartrent.service.membership.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.MembershipPackageCreateRequest;
import com.smartrent.dto.request.MembershipPackageUpdateRequest;
import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.request.MembershipRenewalRequest;
import com.smartrent.dto.request.MembershipUpgradeRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.*;
import com.smartrent.infra.exception.AppException;
import com.smartrent.infra.exception.MembershipPackageExistingException;
import com.smartrent.infra.exception.MembershipPackageNotFoundException;
import com.smartrent.infra.exception.model.DomainCode;
import com.smartrent.infra.repository.*;
import com.smartrent.infra.repository.entity.*;
import com.smartrent.service.membership.MembershipService;
import com.smartrent.service.payment.PaymentService;
import com.smartrent.service.quota.QuotaService;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MembershipServiceImpl implements MembershipService {

    MembershipPackageRepository membershipPackageRepository;
    MembershipPackageBenefitRepository packageBenefitRepository;
    UserMembershipRepository userMembershipRepository;
    UserMembershipBenefitRepository userBenefitRepository;
    TransactionRepository transactionRepository;
    UserRepository userRepository;
    TransactionService transactionService;
    PaymentService paymentService;
    QuotaService quotaService;
    VipTierDetailRepository vipTierDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPackageResponse> getAllActivePackages() {
        log.info("Getting all active membership packages");
        return membershipPackageRepository.findByIsActiveTrueOrderByPackageLevelAsc()
                .stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MembershipPackageResponse> getAllActivePackages(int page, int size) {
        log.info("Getting all active membership packages with pagination - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MembershipPackage> packagePage = membershipPackageRepository.findAll(pageable);

        List<MembershipPackageResponse> packageResponses = packagePage.getContent().stream()
                .filter(MembershipPackage::getIsActive)
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} active membership packages", packageResponses.size());

        return PageResponse.<MembershipPackageResponse>builder()
                .page(page)
                .size(packagePage.getSize())
                .totalPages(packagePage.getTotalPages())
                .totalElements(packagePage.getTotalElements())
                .data(packageResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MembershipPackageResponse> getAllPackages(int page, int size) {
        log.info("Getting all membership packages (admin) with pagination - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<MembershipPackage> packagePage = membershipPackageRepository.findAll(pageable);

        List<MembershipPackageResponse> packageResponses = packagePage.getContent().stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} membership packages", packageResponses.size());

        return PageResponse.<MembershipPackageResponse>builder()
                .page(page)
                .size(packagePage.getSize())
                .totalPages(packagePage.getTotalPages())
                .totalElements(packagePage.getTotalElements())
                .data(packageResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPackageResponse getPackageById(Long membershipId) {
        log.info("Getting membership package by ID: {}", membershipId);
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);
        return mapToPackageResponse(membershipPackage);
    }

    @Override
    @Transactional
    public MembershipPackageResponse createPackage(MembershipPackageCreateRequest request) {
        log.info("Creating new membership package with code: {}", request.getPackageCode());

        // Check if package code already exists
        if (membershipPackageRepository.existsByPackageCode(request.getPackageCode())) {
            throw new MembershipPackageExistingException();
        }

        // Validate package level
        PackageLevel packageLevel;
        try {
            packageLevel = PackageLevel.valueOf(request.getPackageLevel().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid package level: " + request.getPackageLevel() + ". Must be one of: BASIC, STANDARD, ADVANCED");
        }

        // Create membership package
        MembershipPackage membershipPackage = MembershipPackage.builder()
                .packageCode(request.getPackageCode())
                .packageName(request.getPackageName())
                .packageLevel(packageLevel)
                .durationMonths(request.getDurationMonths())
                .originalPrice(request.getOriginalPrice())
                .salePrice(request.getSalePrice())
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .description(request.getDescription())
                .build();

        membershipPackage = membershipPackageRepository.save(membershipPackage);
        log.info("Successfully created membership package with ID: {}", membershipPackage.getMembershipId());

        return mapToPackageResponse(membershipPackage);
    }

    @Override
    @Transactional
    public MembershipPackageResponse updatePackage(Long membershipId, MembershipPackageUpdateRequest request) {
        log.info("Updating membership package with ID: {}", membershipId);

        // Find existing package
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        // Update fields if provided
        if (request.getPackageName() != null) {
            membershipPackage.setPackageName(request.getPackageName());
        }

        if (request.getPackageLevel() != null) {
            try {
                PackageLevel packageLevel = PackageLevel.valueOf(request.getPackageLevel().toUpperCase());
                membershipPackage.setPackageLevel(packageLevel);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid package level: " + request.getPackageLevel() + ". Must be one of: BASIC, STANDARD, ADVANCED");
            }
        }

        if (request.getDurationMonths() != null) {
            membershipPackage.setDurationMonths(request.getDurationMonths());
        }

        if (request.getOriginalPrice() != null) {
            membershipPackage.setOriginalPrice(request.getOriginalPrice());
        }

        if (request.getDiscountPercentage() != null) {
            membershipPackage.setDiscountPercentage(request.getDiscountPercentage());
        }

        // Auto-recalculate salePrice from originalPrice and discountPercentage
        // whenever either of them is touched. Admin never provides salePrice directly.
        if (request.getOriginalPrice() != null || request.getDiscountPercentage() != null) {
            membershipPackage.setSalePrice(
                    calculateSalePrice(membershipPackage.getOriginalPrice(),
                            membershipPackage.getDiscountPercentage()));
        }

        if (request.getIsActive() != null) {
            membershipPackage.setIsActive(request.getIsActive());
        }

        if (request.getDescription() != null) {
            membershipPackage.setDescription(request.getDescription());
        }

        membershipPackage = membershipPackageRepository.save(membershipPackage);
        log.info("Successfully updated membership package with ID: {} (salePrice={}, discountPercentage={})",
                membershipId, membershipPackage.getSalePrice(), membershipPackage.getDiscountPercentage());

        return mapToPackageResponse(membershipPackage);
    }

    @Override
    @Transactional
    public void deletePackage(Long membershipId) {
        log.info("Deleting membership package with ID: {}", membershipId);

        // Find existing package
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        // Delete the package
        membershipPackageRepository.delete(membershipPackage);
        log.info("Successfully deleted membership package with ID: {}", membershipId);
    }

    @Override
    @Transactional
    public UserMembershipResponse purchaseMembership(String userId, MembershipPurchaseRequest request) {
        log.info("User {} purchasing membership package {}", userId, request.getMembershipId());

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if user already has an active membership - they should use upgrade instead
        boolean hasActiveMembership = userMembershipRepository.hasActiveMembership(userId, LocalDateTime.now());
        if (hasActiveMembership) {
            log.warn("User {} already has an active membership, rejecting new purchase", userId);
            throw new AppException(DomainCode.ALREADY_HAS_ACTIVE_MEMBERSHIP);
        }

        // Get membership package
        MembershipPackage membershipPackage = membershipPackageRepository.findById(request.getMembershipId())
                .orElseThrow(MembershipPackageNotFoundException::new);

        if (!membershipPackage.getIsActive()) {
            throw new RuntimeException("Membership package is not active");
        }

        // Calculate dates
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(membershipPackage.getDurationMonths());
        int durationDays = membershipPackage.getDurationMonths() * 30;

        // Create transaction
        Transaction transaction = createMembershipTransaction(userId, membershipPackage, request.getPaymentProvider());
        transactionRepository.save(transaction);

        // Create user membership
        UserMembership userMembership = UserMembership.builder()
                .userId(userId)
                .membershipPackage(membershipPackage)
                .startDate(startDate)
                .endDate(endDate)
                .durationDays(durationDays)
                .status(MembershipStatus.ACTIVE)
                .totalPaid(membershipPackage.getSalePrice())
                .build();

        userMembership = userMembershipRepository.save(userMembership);

        // Grant all benefits immediately (ONE-TIME GRANT)
        List<UserMembershipBenefit> benefits = grantBenefits(userMembership, membershipPackage);
        userBenefitRepository.saveAll(benefits);

        // Mark transaction as completed
        transaction.complete();
        transactionRepository.save(transaction);

        log.info("Successfully purchased membership for user {}", userId);
        return mapToUserMembershipResponse(userMembership);
    }

    @Override
    @Transactional
    public PaymentResponse initiateMembershipPurchase(String userId, MembershipPurchaseRequest request) {
        log.info("Initiating membership purchase for user: {}, package: {}", userId, request.getMembershipId());

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if user already has an active membership - they should use upgrade instead
        boolean hasActiveMembership = userMembershipRepository.hasActiveMembership(userId, LocalDateTime.now());
        if (hasActiveMembership) {
            log.warn("User {} already has an active membership, rejecting new purchase", userId);
            throw new AppException(DomainCode.ALREADY_HAS_ACTIVE_MEMBERSHIP);
        }

        // Get membership package
        MembershipPackage membershipPackage = membershipPackageRepository.findById(request.getMembershipId())
                .orElseThrow(MembershipPackageNotFoundException::new);

        if (!membershipPackage.getIsActive()) {
            throw new RuntimeException("Membership package is not active");
        }

        // Create PENDING transaction
        String transactionId = transactionService.createMembershipTransaction(
                userId,
                request.getMembershipId(),
                membershipPackage.getSalePrice(),
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY"
        );

        // Generate payment URL - pass transactionId to reuse existing transaction
        // Use simple English orderInfo to avoid VNPay encoding issues
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId) // Reuse the transaction created above
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY"))
                .amount(membershipPackage.getSalePrice())
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent Membership " + membershipPackage.getPackageName())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        log.info("Payment URL generated for transaction: {}", transactionId);
        return paymentResponse;
    }

    @Override
    @Transactional
    public UserMembershipResponse completeMembershipPurchase(String transactionId) {
        log.info("Completing membership purchase for transaction: {}", transactionId);

        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (!transaction.isMembershipPurchase()) {
            throw new RuntimeException("Transaction is not a membership purchase: " + transactionId);
        }

        // Validate referenceId exists
        if (transaction.getReferenceId() == null || transaction.getReferenceId().isEmpty()) {
            throw new RuntimeException("Transaction referenceId is null or empty: " + transactionId);
        }

        // Check if membership already created for this transaction (idempotency)
        // This prevents duplicate memberships when both callback and IPN are processed
        Optional<UserMembership> existingMembership = userMembershipRepository.findByUserId(transaction.getUserId())
                .stream()
                .filter(um -> um.getCreatedAt().isAfter(transaction.getCreatedAt().minusMinutes(5)))
                .filter(um -> um.getStatus() == MembershipStatus.ACTIVE)
                .findFirst();

        if (existingMembership.isPresent()) {
            log.info("Membership already created for transaction: {}, returning existing membership", transactionId);
            return mapToUserMembershipResponse(existingMembership.get());
        }

        // Parse membership ID
        Long membershipId;
        try {
            membershipId = Long.parseLong(transaction.getReferenceId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid membership ID in transaction referenceId: " + transaction.getReferenceId(), e);
        }

        // Get membership package
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        // Check if user already has an active membership
        boolean hasActiveMembership = userMembershipRepository.hasActiveMembership(
                transaction.getUserId(),
                LocalDateTime.now()
        );

        if (hasActiveMembership) {
            log.warn("User {} already has an active membership, but proceeding with new purchase", transaction.getUserId());
            // Optionally: expire the old membership or extend it
            // For now, we'll allow multiple active memberships (they can stack)
        }

        // Calculate dates
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(membershipPackage.getDurationMonths());
        int durationDays = membershipPackage.getDurationMonths() * 30;

        // Create user membership
        UserMembership userMembership = UserMembership.builder()
                .userId(transaction.getUserId())
                .membershipPackage(membershipPackage)
                .startDate(startDate)
                .endDate(endDate)
                .durationDays(durationDays)
                .status(MembershipStatus.ACTIVE)
                .totalPaid(membershipPackage.getSalePrice())
                .build();

        userMembership = userMembershipRepository.save(userMembership);

        // Grant all benefits immediately (ONE-TIME GRANT)
        List<UserMembershipBenefit> benefits = grantBenefits(userMembership, membershipPackage);
        userBenefitRepository.saveAll(benefits);

        log.info("Membership activated for user: {}", transaction.getUserId());
        return mapToUserMembershipResponse(userMembership);
    }

    @Override
    @Transactional(readOnly = true)
    public UserMembershipResponse getActiveMembership(String userId) {
        log.info("Getting active membership for user: {}", userId);
        UserMembership userMembership = userMembershipRepository
                .findActiveUserMembership(userId, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("No active membership found for user: " + userId));
        return mapToUserMembershipResponse(userMembership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMembershipResponse> getMembershipHistory(String userId) {
        log.info("Getting membership history for user: {}", userId);
        return userMembershipRepository.findByUserId(userId)
                .stream()
                .map(this::mapToUserMembershipResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserMembershipResponse> getMembershipHistory(String userId, int page, int size) {
        log.info("Getting membership history for user: {} with pagination - page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<UserMembership> membershipPage = userMembershipRepository.findAll(pageable);

        // Filter by userId
        List<UserMembershipResponse> membershipResponses = membershipPage.getContent().stream()
                .filter(um -> um.getUserId().equals(userId))
                .map(this::mapToUserMembershipResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} membership history records", membershipResponses.size());

        return PageResponse.<UserMembershipResponse>builder()
                .page(page)
                .size(membershipPage.getSize())
                .totalPages(membershipPage.getTotalPages())
                .totalElements(membershipPage.getTotalElements())
                .data(membershipResponses)
                .build();
    }

    @Override
    @Transactional
    public int expireOldMemberships() {
        log.info("Expiring old memberships");
        int expiredCount = userMembershipRepository.expireOldMemberships(LocalDateTime.now());
        int expiredBenefitsCount = userBenefitRepository.expireOldBenefits(LocalDateTime.now());
        log.info("Expired {} memberships and {} benefits", expiredCount, expiredBenefitsCount);
        return expiredCount;
    }

    @Override
    @Transactional
    public void cancelMembership(String userId, Long userMembershipId) {
        log.info("Cancelling membership {} for user {}", userMembershipId, userId);
        UserMembership userMembership = userMembershipRepository.findById(userMembershipId)
                .orElseThrow(() -> new RuntimeException("User membership not found: " + userMembershipId));

        if (!userMembership.getUserId().equals(userId)) {
            throw new RuntimeException("Membership does not belong to user");
        }

        userMembership.cancel();
        userMembershipRepository.save(userMembership);

        // Expire all benefits
        List<UserMembershipBenefit> benefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(userMembershipId);
        benefits.forEach(UserMembershipBenefit::expire);
        userBenefitRepository.saveAll(benefits);

        log.info("Successfully cancelled membership {}", userMembershipId);
    }

    // Helper methods

    /**
     * Compute salePrice = originalPrice * (1 - discountPercentage / 100), rounded to 0 decimals.
     * Treats null discount as 0%. Result is clamped to >= 0 in case of out-of-range input.
     */
    private BigDecimal calculateSalePrice(BigDecimal originalPrice, BigDecimal discountPercentage) {
        BigDecimal discount = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.ONE.subtract(
                discount.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        BigDecimal salePrice = originalPrice.multiply(multiplier).max(BigDecimal.ZERO);
        return salePrice.setScale(0, RoundingMode.HALF_UP);
    }

    private Transaction createMembershipTransaction(String userId, MembershipPackage membershipPackage, String paymentProvider) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.MEMBERSHIP_PURCHASE)
                .amount(membershipPackage.getSalePrice())
                .referenceType(ReferenceType.MEMBERSHIP)
                .referenceId(membershipPackage.getMembershipId().toString())
                .status(TransactionStatus.PENDING)
                .paymentProvider(paymentProvider != null ? PaymentProvider.valueOf(paymentProvider) : PaymentProvider.SEPAY)
                .build();
    }

    private List<UserMembershipBenefit> grantBenefits(UserMembership userMembership, MembershipPackage membershipPackage) {
        List<MembershipPackageBenefit> packageBenefits = packageBenefitRepository
                .findByMembershipPackageMembershipId(membershipPackage.getMembershipId());

        List<UserMembershipBenefit> userBenefits = new ArrayList<>();
        for (MembershipPackageBenefit packageBenefit : packageBenefits) {
            // Calculate total quantity = quantity_per_month * duration_months
            int totalQuantity = packageBenefit.calculateTotalQuantity(membershipPackage.getDurationMonths());

            UserMembershipBenefit userBenefit = UserMembershipBenefit.builder()
                    .userMembership(userMembership)
                    .packageBenefit(packageBenefit)
                    .userId(userMembership.getUserId())
                    .benefitType(packageBenefit.getBenefitType())
                    .grantedAt(LocalDateTime.now())
                    .expiresAt(userMembership.getEndDate())
                    .totalQuantity(totalQuantity)
                    .quantityUsed(0)
                    .status(BenefitStatus.ACTIVE)
                    .build();

            userBenefits.add(userBenefit);
        }

        return userBenefits;
    }

    private MembershipPackageResponse mapToPackageResponse(MembershipPackage membershipPackage) {
        List<MembershipPackageBenefitResponse> benefits = packageBenefitRepository
                .findByMembershipPackageMembershipId(membershipPackage.getMembershipId())
                .stream()
                .map(this::mapToBenefitResponse)
                .collect(Collectors.toList());

        return MembershipPackageResponse.builder()
                .membershipId(membershipPackage.getMembershipId())
                .packageCode(membershipPackage.getPackageCode())
                .packageName(membershipPackage.getPackageName())
                .packageLevel(membershipPackage.getPackageLevel().name())
                .durationMonths(membershipPackage.getDurationMonths())
                .originalPrice(membershipPackage.getOriginalPrice())
                .salePrice(membershipPackage.getSalePrice())
                .discountPercentage(membershipPackage.getDiscountPercentage())
                .isActive(membershipPackage.getIsActive())
                .description(membershipPackage.getDescription())
                .benefits(benefits)
                .createdAt(membershipPackage.getCreatedAt())
                .updatedAt(membershipPackage.getUpdatedAt())
                .build();
    }

    private MembershipPackageBenefitResponse mapToBenefitResponse(MembershipPackageBenefit benefit) {
        String tierCode = resolveTierCode(benefit.getBenefitType());
        VipTierDetail tier = tierCode != null
                ? vipTierDetailRepository.findByTierCode(tierCode).orElse(null)
                : null;
        return MembershipPackageBenefitResponse.builder()
                .benefitId(benefit.getBenefitId())
                .benefitType(benefit.getBenefitType().name())
                .benefitNameDisplay(benefit.getBenefitNameDisplay())
                .quantityPerMonth(benefit.getQuantityPerMonth())
                .vipTierCode(tierCode)
                .maxImages(tier != null ? tier.getMaxImages() : null)
                .maxVideos(tier != null ? tier.getMaxVideos() : null)
                .createdAt(benefit.getCreatedAt())
                .build();
    }

    /**
     * Map a {@link BenefitType} to the VIP tier code the resulting listing will use.
     * Returns null for benefit types that don't create listings (e.g. PUSH).
     */
    private String resolveTierCode(BenefitType benefitType) {
        if (benefitType == null) return null;
        return switch (benefitType) {
            case POST_SILVER -> "SILVER";
            case POST_GOLD -> "GOLD";
            case POST_DIAMOND -> "DIAMOND";
            default -> null;
        };
    }

    private UserMembershipResponse mapToUserMembershipResponse(UserMembership userMembership) {
        List<UserMembershipBenefitResponse> benefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(userMembership.getUserMembershipId())
                .stream()
                .map(this::mapToUserBenefitResponse)
                .collect(Collectors.toList());

        return UserMembershipResponse.builder()
                .userMembershipId(userMembership.getUserMembershipId())
                .userId(userMembership.getUserId())
                .membershipId(userMembership.getMembershipPackage().getMembershipId())
                .packageName(userMembership.getMembershipPackage().getPackageName())
                .packageLevel(userMembership.getMembershipPackage().getPackageLevel().name())
                .startDate(userMembership.getStartDate())
                .endDate(userMembership.getEndDate())
                .durationDays(userMembership.getDurationDays())
                .daysRemaining(userMembership.getDaysRemaining())
                .status(userMembership.getStatus().name())
                .totalPaid(userMembership.getTotalPaid())
                .benefits(benefits)
                .createdAt(userMembership.getCreatedAt())
                .updatedAt(userMembership.getUpdatedAt())
                .build();
    }

    private UserMembershipBenefitResponse mapToUserBenefitResponse(UserMembershipBenefit benefit) {
        String tierCode = resolveTierCode(benefit.getBenefitType());
        VipTierDetail tier = tierCode != null
                ? vipTierDetailRepository.findByTierCode(tierCode).orElse(null)
                : null;
        return UserMembershipBenefitResponse.builder()
                .userBenefitId(benefit.getUserBenefitId())
                .benefitType(benefit.getBenefitType().name())
                .benefitNameDisplay(benefit.getPackageBenefit().getBenefitNameDisplay())
                .grantedAt(benefit.getGrantedAt())
                .expiresAt(benefit.getExpiresAt())
                .totalQuantity(benefit.getTotalQuantity())
                .quantityUsed(benefit.getQuantityUsed())
                .quantityRemaining(benefit.getQuantityRemaining())
                .status(benefit.getStatus().name())
                .vipTierCode(tierCode)
                .maxImages(tier != null ? tier.getMaxImages() : null)
                .maxVideos(tier != null ? tier.getMaxVideos() : null)
                .createdAt(benefit.getCreatedAt())
                .updatedAt(benefit.getUpdatedAt())
                .build();
    }

    // =====================================================
    // MEMBERSHIP UPGRADE METHODS
    // =====================================================

    @Override
    @Transactional(readOnly = true)
    public MembershipUpgradePreviewResponse getUpgradePreview(String userId, Long targetMembershipId) {
        log.info("Getting upgrade preview for user: {}, target membership: {}", userId, targetMembershipId);

        // Get user's active membership
        Optional<UserMembership> activeMembershipOpt = userMembershipRepository
                .findActiveUserMembership(userId, LocalDateTime.now());

        if (activeMembershipOpt.isEmpty()) {
            return MembershipUpgradePreviewResponse.builder()
                    .eligible(false)
                    .ineligibilityReason("No active membership found. Please purchase a membership first.")
                    .build();
        }

        UserMembership currentMembership = activeMembershipOpt.get();
        MembershipPackage currentPackage = currentMembership.getMembershipPackage();

        // Get target membership package
        MembershipPackage targetPackage = membershipPackageRepository.findById(targetMembershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        if (!targetPackage.getIsActive()) {
            return MembershipUpgradePreviewResponse.builder()
                    .eligible(false)
                    .ineligibilityReason("Target membership package is not active.")
                    .build();
        }

        // Check if upgrade is valid (target must be higher level)
        if (!isValidUpgrade(currentPackage.getPackageLevel(), targetPackage.getPackageLevel())) {
            String reason = currentPackage.getPackageLevel() == targetPackage.getPackageLevel()
                    ? "Target package is the same level as your current membership."
                    : "Cannot downgrade membership. Target package must be higher tier than current package.";
            return MembershipUpgradePreviewResponse.builder()
                    .currentMembershipId(currentMembership.getUserMembershipId())
                    .currentPackageName(currentPackage.getPackageName())
                    .currentPackageLevel(currentPackage.getPackageLevel().name())
                    .daysRemaining(currentMembership.getDaysRemaining())
                    .targetMembershipId(targetPackage.getMembershipId())
                    .targetPackageName(targetPackage.getPackageName())
                    .targetPackageLevel(targetPackage.getPackageLevel().name())
                    .eligible(false)
                    .ineligibilityReason(reason)
                    .build();
        }

        // Calculate discount based on remaining time
        List<UserMembershipBenefit> currentBenefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(currentMembership.getUserMembershipId());

        BigDecimal currentPrice = currentPackage.getSalePrice();
        BigDecimal targetPrice = targetPackage.getSalePrice();
        BigDecimal discountAmount = calculateUpgradeDiscount(currentMembership, currentBenefits, currentPrice, targetPrice);
        BigDecimal finalPrice = targetPrice.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal discountPercentage = targetPrice.compareTo(BigDecimal.ZERO) > 0
                ? discountAmount.divide(targetPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Build forfeited benefits list
        List<MembershipUpgradePreviewResponse.ForfeitedBenefitInfo> forfeitedBenefits = currentBenefits.stream()
                .filter(b -> b.getQuantityRemaining() > 0)
                .map(b -> MembershipUpgradePreviewResponse.ForfeitedBenefitInfo.builder()
                        .benefitType(b.getBenefitType().name())
                        .benefitName(b.getPackageBenefit().getBenefitNameDisplay())
                        .totalQuantity(b.getTotalQuantity())
                        .usedQuantity(b.getQuantityUsed())
                        .remainingQuantity(b.getQuantityRemaining())
                        .estimatedValue(calculateBenefitValue(b.getBenefitType(), b.getQuantityRemaining()))
                        .build())
                .collect(Collectors.toList());

        // Get new benefits from target package
        List<MembershipPackageBenefitResponse> newBenefits = packageBenefitRepository
                .findByMembershipPackageMembershipId(targetMembershipId)
                .stream()
                .map(this::mapToBenefitResponse)
                .collect(Collectors.toList());

        return MembershipUpgradePreviewResponse.builder()
                .currentMembershipId(currentMembership.getUserMembershipId())
                .currentPackageName(currentPackage.getPackageName())
                .currentPackageLevel(currentPackage.getPackageLevel().name())
                .daysRemaining(currentMembership.getDaysRemaining())
                .targetMembershipId(targetPackage.getMembershipId())
                .targetPackageName(targetPackage.getPackageName())
                .targetPackageLevel(targetPackage.getPackageLevel().name())
                .targetDurationDays(targetPackage.getDurationMonths() * 30)
                .targetPackagePrice(targetPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .discountPercentage(discountPercentage)
                .forfeitedBenefits(forfeitedBenefits)
                .newBenefits(newBenefits)
                .eligible(true)
                .ineligibilityReason(null)
                .build();
    }

    @Override
    @Transactional
    public MembershipUpgradeResponse initiateMembershipUpgrade(String userId, MembershipUpgradeRequest request) {
        log.info("Initiating membership upgrade for user: {}, target: {}", userId, request.getTargetMembershipId());

        // Validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Get user's active membership
        UserMembership currentMembership = userMembershipRepository
                .findActiveUserMembership(userId, LocalDateTime.now())
                .orElseThrow(() -> new AppException(DomainCode.NO_ACTIVE_MEMBERSHIP));

        MembershipPackage currentPackage = currentMembership.getMembershipPackage();

        // Get target membership package
        MembershipPackage targetPackage = membershipPackageRepository.findById(request.getTargetMembershipId())
                .orElseThrow(MembershipPackageNotFoundException::new);

        if (!targetPackage.getIsActive()) {
            throw new AppException(DomainCode.INVALID_UPGRADE_TARGET, "Target package is not active");
        }

        // Validate upgrade direction
        if (!isValidUpgrade(currentPackage.getPackageLevel(), targetPackage.getPackageLevel())) {
            if (currentPackage.getPackageLevel() == targetPackage.getPackageLevel()) {
                throw new AppException(DomainCode.SAME_MEMBERSHIP_LEVEL);
            }
            throw new AppException(DomainCode.CANNOT_DOWNGRADE_MEMBERSHIP);
        }

        // Calculate discount
        List<UserMembershipBenefit> currentBenefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(currentMembership.getUserMembershipId());
        BigDecimal currentPrice = currentPackage.getSalePrice();
        BigDecimal targetPrice = targetPackage.getSalePrice();
        BigDecimal discountAmount = calculateUpgradeDiscount(currentMembership, currentBenefits, currentPrice, targetPrice);
        BigDecimal finalAmount = targetPrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // Create upgrade transaction with metadata
        String transactionId = transactionService.createMembershipUpgradeTransaction(
                userId,
                request.getTargetMembershipId(),
                currentMembership.getUserMembershipId(),
                finalAmount,
                discountAmount,
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY"
        );

        // If final amount is 0 or less, complete upgrade immediately without payment
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Upgrade is free due to discount, completing immediately");
            // Mark transaction as completed
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
            transaction.complete();
            transactionRepository.save(transaction);

            // Complete the upgrade
            completeMembershipUpgrade(transactionId);

            return MembershipUpgradeResponse.builder()
                    .transactionRef(transactionId)
                    .paymentUrl(null)
                    .paymentProvider(request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY")
                    .previousMembershipId(currentMembership.getUserMembershipId())
                    .newMembershipPackageId(targetPackage.getMembershipId())
                    .newPackageName(targetPackage.getPackageName())
                    .newPackageLevel(targetPackage.getPackageLevel().name())
                    .originalPrice(targetPackage.getSalePrice())
                    .discountAmount(discountAmount)
                    .finalAmount(BigDecimal.ZERO)
                    .status("COMPLETED")
                    .message("Upgrade completed successfully. No payment required due to discount.")
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        // Generate payment URL
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId)
                .provider(PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY"))
                .amount(finalAmount)
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent Membership Upgrade to " + targetPackage.getPackageName())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);

        return MembershipUpgradeResponse.builder()
                .transactionRef(transactionId)
                .paymentUrl(paymentResponse.getPaymentUrl())
                // Carry the SePay hosted-checkout form fields so the frontend can POST them.
                // Without this the frontend only has paymentUrl and GETs the checkout URL -> 404.
                .providerData(paymentResponse.getProviderData())
                .paymentProvider(request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY")
                .previousMembershipId(currentMembership.getUserMembershipId())
                .newMembershipPackageId(targetPackage.getMembershipId())
                .newPackageName(targetPackage.getPackageName())
                .newPackageLevel(targetPackage.getPackageLevel().name())
                .originalPrice(targetPackage.getSalePrice())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .status("PENDING_PAYMENT")
                .message("Please complete payment to finalize upgrade.")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Override
    @Transactional
    public UserMembershipResponse completeMembershipUpgrade(String transactionId) {
        log.info("Completing membership upgrade for transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (transaction.getTransactionType() != TransactionType.MEMBERSHIP_UPGRADE) {
            throw new RuntimeException("Transaction is not a membership upgrade: " + transactionId);
        }

        // Parse target membership ID from referenceId
        Long targetMembershipId;
        try {
            targetMembershipId = Long.parseLong(transaction.getReferenceId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid membership ID in transaction: " + transaction.getReferenceId(), e);
        }

        // Get previous membership ID from transaction metadata (stored in description or separate field)
        Long previousMembershipId = transaction.getPreviousMembershipId();
        if (previousMembershipId == null) {
            throw new RuntimeException("Previous membership ID not found in transaction");
        }

        // Cancel the old membership
        UserMembership oldMembership = userMembershipRepository.findById(previousMembershipId)
                .orElseThrow(() -> new RuntimeException("Previous membership not found: " + previousMembershipId));

        oldMembership.setStatus(MembershipStatus.UPGRADED);
        userMembershipRepository.save(oldMembership);

        // Expire old benefits
        List<UserMembershipBenefit> oldBenefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(previousMembershipId);
        oldBenefits.forEach(UserMembershipBenefit::expire);
        userBenefitRepository.saveAll(oldBenefits);

        // Get target membership package
        MembershipPackage targetPackage = membershipPackageRepository.findById(targetMembershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        // Create new membership
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(targetPackage.getDurationMonths());
        int durationDays = targetPackage.getDurationMonths() * 30;

        UserMembership newMembership = UserMembership.builder()
                .userId(transaction.getUserId())
                .membershipPackage(targetPackage)
                .startDate(startDate)
                .endDate(endDate)
                .durationDays(durationDays)
                .status(MembershipStatus.ACTIVE)
                .totalPaid(transaction.getAmount())
                .upgradedFromMembershipId(previousMembershipId)
                .build();

        newMembership = userMembershipRepository.save(newMembership);

        // Grant new benefits
        List<UserMembershipBenefit> newBenefits = grantBenefits(newMembership, targetPackage);
        userBenefitRepository.saveAll(newBenefits);

        log.info("Membership upgrade completed for user: {}, from membership {} to {}",
                transaction.getUserId(), previousMembershipId, newMembership.getUserMembershipId());

        return mapToUserMembershipResponse(newMembership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipUpgradePreviewResponse> getAvailableUpgrades(String userId) {
        log.info("Getting available upgrades for user: {}", userId);

        // Get user's active membership
        Optional<UserMembership> activeMembershipOpt = userMembershipRepository
                .findActiveUserMembership(userId, LocalDateTime.now());

        if (activeMembershipOpt.isEmpty()) {
            log.info("User {} has no active membership, returning empty upgrade list", userId);
            return List.of();
        }

        UserMembership currentMembership = activeMembershipOpt.get();
        MembershipPackage currentPackage = currentMembership.getMembershipPackage();
        PackageLevel currentLevel = currentPackage.getPackageLevel();

        // Get current benefits for discount calculation
        List<UserMembershipBenefit> currentBenefits = userBenefitRepository
                .findByUserMembershipUserMembershipId(currentMembership.getUserMembershipId());

        // Get all active packages with higher tier
        List<MembershipPackage> allPackages = membershipPackageRepository.findByIsActiveTrueOrderByPackageLevelAsc();

        List<MembershipUpgradePreviewResponse> upgrades = allPackages.stream()
                .filter(pkg -> pkg.getPackageLevel().ordinal() > currentLevel.ordinal())
                .sorted((a, b) -> {
                    // Sort by level first, then by price
                    int levelCompare = Integer.compare(a.getPackageLevel().ordinal(), b.getPackageLevel().ordinal());
                    if (levelCompare != 0) return levelCompare;
                    return a.getSalePrice().compareTo(b.getSalePrice());
                })
                .map(targetPackage -> {
                    BigDecimal currentPrice = currentPackage.getSalePrice();
                    BigDecimal targetPrice = targetPackage.getSalePrice();
                    BigDecimal discountAmount = calculateUpgradeDiscount(currentMembership, currentBenefits, currentPrice, targetPrice);
                    BigDecimal finalPrice = targetPrice.subtract(discountAmount).max(BigDecimal.ZERO);
                    BigDecimal discountPercentage = targetPrice.compareTo(BigDecimal.ZERO) > 0
                            ? discountAmount.divide(targetPrice, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                            : BigDecimal.ZERO;

                    // Build forfeited benefits list
                    List<MembershipUpgradePreviewResponse.ForfeitedBenefitInfo> forfeitedBenefits = currentBenefits.stream()
                            .filter(b -> b.getQuantityRemaining() > 0)
                            .map(b -> MembershipUpgradePreviewResponse.ForfeitedBenefitInfo.builder()
                                    .benefitType(b.getBenefitType().name())
                                    .benefitName(b.getPackageBenefit().getBenefitNameDisplay())
                                    .totalQuantity(b.getTotalQuantity())
                                    .usedQuantity(b.getQuantityUsed())
                                    .remainingQuantity(b.getQuantityRemaining())
                                    .estimatedValue(calculateBenefitValue(b.getBenefitType(), b.getQuantityRemaining()))
                                    .build())
                            .collect(Collectors.toList());

                    // Get new benefits from target package
                    List<MembershipPackageBenefitResponse> newBenefits = packageBenefitRepository
                            .findByMembershipPackageMembershipId(targetPackage.getMembershipId())
                            .stream()
                            .map(this::mapToBenefitResponse)
                            .collect(Collectors.toList());

                    return MembershipUpgradePreviewResponse.builder()
                            .currentMembershipId(currentMembership.getUserMembershipId())
                            .currentPackageName(currentPackage.getPackageName())
                            .currentPackageLevel(currentPackage.getPackageLevel().name())
                            .daysRemaining(currentMembership.getDaysRemaining())
                            .targetMembershipId(targetPackage.getMembershipId())
                            .targetPackageName(targetPackage.getPackageName())
                            .targetPackageLevel(targetPackage.getPackageLevel().name())
                            .targetDurationDays(targetPackage.getDurationMonths() * 30)
                            .targetPackagePrice(targetPrice)
                            .discountAmount(discountAmount)
                            .finalPrice(finalPrice)
                            .discountPercentage(discountPercentage)
                            .forfeitedBenefits(forfeitedBenefits)
                            .newBenefits(newBenefits)
                            .eligible(true)
                            .ineligibilityReason(null)
                            .build();
                })
                .collect(Collectors.toList());

        log.info("Found {} available upgrades for user {}", upgrades.size(), userId);
        return upgrades;
    }

    // =====================================================
    // MEMBERSHIP RENEWAL METHODS
    // =====================================================

    @Override
    @Transactional
    public PaymentResponse initiateMembershipRenewal(String userId, MembershipRenewalRequest request) {
        log.info("Initiating membership renewal for user: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Accept active memberships OR memberships that expired within the last 7 days
        LocalDateTime renewalCutoff = LocalDateTime.now().minusDays(7);
        UserMembership currentMembership = userMembershipRepository
                .findByUserIdOrderByEndDateDesc(userId)
                .stream()
                .filter(um -> um.isActive()
                        || (um.isExpired() && um.getEndDate().isAfter(renewalCutoff)))
                .findFirst()
                .orElseThrow(() -> new AppException(DomainCode.NO_RENEWABLE_MEMBERSHIP));

        MembershipPackage pkg = currentMembership.getMembershipPackage();
        if (!pkg.getIsActive()) {
            throw new RuntimeException("Membership package is no longer active: " + pkg.getPackageName());
        }

        String provider = request.getPaymentProvider() != null ? request.getPaymentProvider() : "SEPAY";
        String transactionId = transactionService.createMembershipRenewalTransaction(
                userId,
                pkg.getMembershipId(),
                currentMembership.getUserMembershipId(),
                pkg.getSalePrice(),
                provider);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId)
                .provider(PaymentProvider.valueOf(provider))
                .amount(pkg.getSalePrice())
                .currency(PricingConstants.DEFAULT_CURRENCY)
                .orderInfo("SmartRent Membership Renewal " + pkg.getPackageName())
                .build();

        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest, null);
        log.info("Renewal payment URL generated for transaction: {}", transactionId);
        return paymentResponse;
    }

    @Override
    @Transactional
    public UserMembershipResponse completeMembershipRenewal(String transactionId) {
        log.info("Completing membership renewal for transaction: {}", transactionId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!transaction.isCompleted()) {
            throw new RuntimeException("Transaction is not completed: " + transactionId);
        }

        if (!transaction.isMembershipRenewal()) {
            throw new RuntimeException("Transaction is not a membership renewal: " + transactionId);
        }

        Long membershipId = Long.parseLong(transaction.getReferenceId());
        MembershipPackage pkg = membershipPackageRepository.findById(membershipId)
                .orElseThrow(MembershipPackageNotFoundException::new);

        Long previousMembershipId = transaction.getPreviousMembershipId();
        UserMembership previous = previousMembershipId != null
                ? userMembershipRepository.findById(previousMembershipId).orElse(null)
                : null;

        // Chain from previous endDate if it hasn't expired yet; otherwise start from now
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = (previous != null && previous.getEndDate().isAfter(now))
                ? previous.getEndDate()
                : now;
        LocalDateTime endDate = startDate.plusMonths(pkg.getDurationMonths());
        int durationDays = pkg.getDurationMonths() * 30;

        UserMembership renewed = UserMembership.builder()
                .userId(transaction.getUserId())
                .membershipPackage(pkg)
                .startDate(startDate)
                .endDate(endDate)
                .durationDays(durationDays)
                .status(MembershipStatus.ACTIVE)
                .totalPaid(transaction.getAmount())
                .build();

        renewed = userMembershipRepository.save(renewed);

        List<UserMembershipBenefit> benefits = grantBenefits(renewed, pkg);
        userBenefitRepository.saveAll(benefits);

        log.info("Membership renewal completed for user: {}, new membership: {} (start={}, end={})",
                transaction.getUserId(), renewed.getUserMembershipId(), startDate, endDate);
        return mapToUserMembershipResponse(renewed);
    }

    // =====================================================
    // UPGRADE HELPER METHODS
    // =====================================================

    /**
     * Check if upgrade from current level to target level is valid
     * Only allows upgrading to higher tiers: BASIC -> STANDARD -> ADVANCED
     */
    private boolean isValidUpgrade(PackageLevel currentLevel, PackageLevel targetLevel) {
        return targetLevel.ordinal() > currentLevel.ordinal();
    }

    /**
     * Calculate discount amount based on remaining time in current membership.
     *
     * The discount is calculated as the pro-rated value of the remaining time:
     * discount = (days_remaining / total_days) * amount_paid
     *
     * This discount is then capped to ensure the user always pays at least
     * the price difference between the target and current package.
     *
     * @param currentMembership The user's current active membership
     * @param currentBenefits The user's current benefits (used for display, not calculation)
     * @param currentPackagePrice The price of the current membership package
     * @param targetPackagePrice The price of the target membership package
     * @return The discount amount (capped appropriately)
     */
    private BigDecimal calculateUpgradeDiscount(UserMembership currentMembership,
                                                 List<UserMembershipBenefit> currentBenefits,
                                                 BigDecimal currentPackagePrice,
                                                 BigDecimal targetPackagePrice) {
        // Calculate pro-rated time value (remaining days / total days * amount paid)
        long daysRemaining = currentMembership.getDaysRemaining();
        BigDecimal proRatedDiscount = BigDecimal.ZERO;

        if (daysRemaining > 0 && currentMembership.getDurationDays() > 0) {
            BigDecimal timeRatio = new BigDecimal(daysRemaining)
                    .divide(new BigDecimal(currentMembership.getDurationDays()), 4, RoundingMode.HALF_UP);
            proRatedDiscount = currentMembership.getTotalPaid().multiply(timeRatio);
        }

        // Calculate the minimum amount user must pay (price difference between packages)
        // User should always pay at least the difference to upgrade
        BigDecimal priceDifference = targetPackagePrice.subtract(currentPackagePrice).max(BigDecimal.ZERO);

        // Maximum discount = target price - price difference
        // This ensures: final_price = target_price - discount >= price_difference
        BigDecimal maxDiscount = targetPackagePrice.subtract(priceDifference);

        // Cap the discount
        BigDecimal finalDiscount = proRatedDiscount.min(maxDiscount);

        return finalDiscount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate the monetary value of a benefit based on type and quantity
     * Uses standard pricing from PricingConstants
     */
    private BigDecimal calculateBenefitValue(BenefitType benefitType, int quantity) {
        if (quantity <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal unitValue;
        switch (benefitType) {
            case POST_SILVER:
                // Value based on 30-day silver post price
                unitValue = PricingConstants.SILVER_POST_30_DAYS;
                break;
            case POST_GOLD:
                // Value based on 30-day gold post price
                unitValue = PricingConstants.GOLD_POST_30_DAYS;
                break;
            case POST_DIAMOND:
                // Value based on 30-day diamond post price
                unitValue = PricingConstants.DIAMOND_POST_30_DAYS;
                break;
            case PUSH:
                // Value based on push price per time
                unitValue = PricingConstants.PUSH_PER_TIME;
                break;
            default:
                return BigDecimal.ZERO;
        }

        return unitValue.multiply(new BigDecimal(quantity));
    }
}
