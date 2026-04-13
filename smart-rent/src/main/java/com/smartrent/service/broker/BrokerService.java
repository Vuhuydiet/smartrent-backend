package com.smartrent.service.broker;

import com.smartrent.dto.request.BrokerVerificationRequest;
import com.smartrent.dto.response.AdminBrokerUserResponse;
import com.smartrent.dto.response.BrokerStatusResponse;
import com.smartrent.dto.response.PageResponse;

public interface BrokerService {

    /**
     * Register broker intent for the authenticated user.
     * Idempotent: returns current status if already PENDING or APPROVED.
     * Notifies all admins when a new registration is submitted.
     *
     * @param userId the ID of the authenticated user
     * @return current broker status after the operation
     */
    BrokerStatusResponse registerBroker(String userId);

    /**
     * Get current broker registration status for the authenticated user.
     *
     * @param userId the ID of the authenticated user
     * @return current broker status
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
     * Admin: get paginated list of users with PENDING broker registration.
     * Ordered by brokerRegisteredAt ascending (oldest first) so admins
     * review in FIFO order.
     *
     * @param page 1-based page number
     * @param size page size
     * @return paginated list of pending broker users with full details
     */
    PageResponse<AdminBrokerUserResponse> getPendingBrokers(int page, int size);
}
