package com.smartrent.service.broker;

import com.smartrent.dto.request.BrokerRegisterRequest;
import com.smartrent.dto.request.BrokerVerificationRequest;
import com.smartrent.dto.response.AdminBrokerUserResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.dto.response.PageResponse;

public interface BrokerService {

    /**
     * Register broker intent for the authenticated user.
     * Validates that all four identity/certificate document images have been
     * uploaded and confirmed before setting status to PENDING.
     * Idempotent: returns current status if already PENDING or APPROVED.
     * Notifies all admins when a new registration is submitted.
     *
     * @param userId  ID of the authenticated user
     * @param request four confirmed media IDs for identity and certificate images
     * @return current broker status after the operation
     */
    BrokerStatusResponse registerBroker(String userId, BrokerRegisterRequest request);

    /**
     * Get current broker registration status for the authenticated user,
     * including presigned download URLs for submitted documents.
     *
     * @param userId ID of the authenticated user
     * @return current broker status with document URLs
     */
    BrokerStatusResponse getBrokerStatus(String userId);

    /**
     * Admin approves or rejects a user's broker registration.
     * Notifies the user of the decision via in-app notification.
     *
     * @param userId  ID of the user to review
     * @param adminId ID of the admin performing the action
     * @param request approval/rejection payload
     * @return updated broker status
     */
    BrokerStatusResponse reviewBroker(String userId, String adminId, BrokerVerificationRequest request);

    /**
     * Admin removes broker role from a user.
     * Sets isBroker=false and marks broker verification as REJECTED.
     *
     * @param userId  ID of the target user
     * @param adminId ID of the admin performing the action
     * @return updated broker status
     */
    BrokerStatusResponse removeBrokerRole(String userId, String adminId);

    /**
     * Admin: get paginated list of users with PENDING broker registration.
     * Ordered oldest-first (FIFO). Each entry includes presigned document URLs.
     *
     * @param page 1-based page number
     * @param size page size
     * @return paginated list of pending broker users
     */
    PageResponse<AdminBrokerUserResponse> getPendingBrokers(int page, int size);
}
