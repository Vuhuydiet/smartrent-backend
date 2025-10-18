package com.smartrent.service.membership.impl;

import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.request.PaymentRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    public MembershipPackageResponse getPackageById(Long membershipId) {
        log.info("Getting membership package by ID: {}", membershipId);
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership package not found: " + membershipId));
        return mapToPackageResponse(membershipPackage);
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
                .orElseThrow(() -> new RuntimeException("Membership package not found: " + request.getMembershipId()));

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
                .orElseThrow(() -> new RuntimeException("Membership package not found: " + request.getMembershipId()));

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

        // Generate payment URL
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .provider(com.smartrent.enums.PaymentProvider.valueOf(
                        request.getPaymentProvider() != null ? request.getPaymentProvider() : "VNPAY"))
                .amount(membershipPackage.getSalePrice())
                .orderInfo("Membership: " + membershipPackage.getPackageName())
                .returnUrl(request.getReturnUrl())
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

        // Get membership package
        Long membershipId = Long.parseLong(transaction.getReferenceId());
        MembershipPackage membershipPackage = membershipPackageRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership package not found: " + membershipId));

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

