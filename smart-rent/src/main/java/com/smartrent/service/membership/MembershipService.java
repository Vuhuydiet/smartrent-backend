package com.smartrent.service.membership;

import com.smartrent.dto.request.MembershipPackageCreateRequest;
import com.smartrent.dto.request.MembershipPackageUpdateRequest;
import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.response.MembershipPackageResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PaymentResponse;
import com.smartrent.dto.response.QuotaStatusResponse;
import com.smartrent.dto.response.UserMembershipResponse;
import com.smartrent.enums.BenefitType;

import java.util.List;

public interface MembershipService {

    /**
     * Get all active membership packages
     */
    List<MembershipPackageResponse> getAllActivePackages();

    /**
     * Get all active membership packages with pagination
     */
    PageResponse<MembershipPackageResponse> getAllActivePackages(int page, int size);

    /**
     * Get membership package by ID
     */
    MembershipPackageResponse getPackageById(Long membershipId);

    /**
     * Create a new membership package
     */
    MembershipPackageResponse createPackage(MembershipPackageCreateRequest request);

    /**
     * Update an existing membership package
     */
    MembershipPackageResponse updatePackage(Long membershipId, MembershipPackageUpdateRequest request);

    /**
     * Delete a membership package
     */
    void deletePackage(Long membershipId);

    /**
     * Initiate membership purchase (creates transaction and returns VNPay payment URL)
     */
    PaymentResponse initiateMembershipPurchase(String userId, MembershipPurchaseRequest request);

    /**
     * Complete membership purchase after successful payment
     * Called from payment callback handler
     */
    UserMembershipResponse completeMembershipPurchase(String transactionId);

    /**
     * Purchase a membership package (legacy - for backward compatibility)
     * @deprecated Use initiateMembershipPurchase instead
     */
    @Deprecated
    UserMembershipResponse purchaseMembership(String userId, MembershipPurchaseRequest request);

    /**
     * Get user's active membership
     */
    UserMembershipResponse getActiveMembership(String userId);

    /**
     * Get user's membership history
     */
    List<UserMembershipResponse> getMembershipHistory(String userId);

    /**
     * Get user's membership history with pagination
     */
    PageResponse<UserMembershipResponse> getMembershipHistory(String userId, int page, int size);

    /**
     * Expire old memberships (called by scheduled job)
     */
    int expireOldMemberships();

    /**
     * Cancel user membership
     */
    void cancelMembership(String userId, Long userMembershipId);
}

