package com.smartrent.service.membership;

import com.smartrent.dto.request.MembershipPurchaseRequest;
import com.smartrent.dto.response.MembershipPackageResponse;
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
     * Get membership package by ID
     */
    MembershipPackageResponse getPackageById(Long membershipId);

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
     * Expire old memberships (called by scheduled job)
     */
    int expireOldMemberships();

    /**
     * Cancel user membership
     */
    void cancelMembership(String userId, Long userMembershipId);
}

