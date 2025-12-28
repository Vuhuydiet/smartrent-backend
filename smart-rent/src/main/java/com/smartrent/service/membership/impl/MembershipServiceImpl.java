package com.smartrent.service.membership.impl;

import com.smartrent.constants.PricingConstants;
import com.smartrent.dto.request.MembershipPackageCreateRequest;
import com.smartrent.dto.request.MembershipPackageUpdateRequest;
import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.*;
import com.smartrent.infra.exception.MembershipPackageExistingException;
import com.smartrent.infra.exception.MembershipPackageNotFoundException;
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
import java.time.LocalDateTime;
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

        if (request.getSalePrice() != null) {
            membershipPackage.setSalePrice(request.getSalePrice());
        }

        if (request.getDiscountPercentage() != null) {
            membershipPackage.setDiscountPercentage(request.getDiscountPercentage());
        }

        if (request.getIsActive() != null) {
            membershipPackage.setIsActive(request.getIsActive());
        }

        if (request.getDescription() != null) {
            membershipPackage.setDescription(request.getDescription());
        }

        membershipPackage = membershipPackageRepository.save(membershipPackage);
        log.info("Successfully updated membership package with ID: {}", membershipId);

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
                request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"
        );

        // Generate payment URL - pass transactionId to reuse existing transaction
        // Use simple English orderInfo to avoid VNPay encoding issues
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .transactionId(transactionId) // Reuse the transaction created above
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
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
    private Transaction createMembershipTransaction(String userId, MembershipPackage membershipPackage, String paymentProvider) {
        return Transaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .userId(userId)
                .transactionType(TransactionType.MEMBERSHIP_PURCHASE)
                .amount(membershipPackage.getSalePrice())
                .referenceType(ReferenceType.MEMBERSHIP)
                .referenceId(membershipPackage.getMembershipId().toString())
                .status(TransactionStatus.PENDING)
                .paymentProvider(paymentProvider != null ? PaymentProvider.valueOf(paymentProvider) : PaymentProvider.VNPAY)
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
        return MembershipPackageBenefitResponse.builder()
                .benefitId(benefit.getBenefitId())
                .benefitType(benefit.getBenefitType().name())
                .benefitNameDisplay(benefit.getBenefitNameDisplay())
                .quantityPerMonth(benefit.getQuantityPerMonth())
                .createdAt(benefit.getCreatedAt())
                .build();
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
                .createdAt(benefit.getCreatedAt())
                .updatedAt(benefit.getUpdatedAt())
                .build();
    }
}

