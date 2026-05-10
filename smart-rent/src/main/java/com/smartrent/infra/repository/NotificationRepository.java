package com.smartrent.infra.repository;

import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import com.smartrent.infra.repository.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(
            String recipientId, RecipientType recipientType, Pageable pageable);

    long countByRecipientIdAndRecipientTypeAndIsReadFalse(
            String recipientId, RecipientType recipientType);

    boolean existsByIdAndRecipientIdAndRecipientType(
            Long id, String recipientId, RecipientType recipientType);

    @Modifying
    @Query("UPDATE notifications n SET n.isRead = true " +
           "WHERE n.id = :notificationId " +
           "AND n.recipientId = :recipientId " +
           "AND n.recipientType = :recipientType " +
           "AND n.isRead = false")
    int markAsRead(@Param("notificationId") Long notificationId,
                   @Param("recipientId") String recipientId,
                   @Param("recipientType") RecipientType recipientType);

    @Modifying
    @Query("UPDATE notifications n SET n.isRead = true " +
           "WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.isRead = false")
    int markAllAsRead(@Param("recipientId") String recipientId,
                      @Param("recipientType") RecipientType recipientType);

    /**
     * Dedup probe used by recurring jobs (e.g. listing-expiring scheduler) to avoid
     * sending the same milestone notification twice within a short window.
     */
    boolean existsByRecipientIdAndRecipientTypeAndTypeAndReferenceIdAndReferenceTypeAndCreatedAtAfter(
            String recipientId,
            RecipientType recipientType,
            NotificationType type,
            Long referenceId,
            String referenceType,
            LocalDateTime createdAtAfter);

    /**
     * Dedup probe for daily-summary notifications that have no specific listing
     * reference (referenceId is null). Used by the expiring-listing scheduler
     * to skip a recipient who already received today's summary.
     */
    boolean existsByRecipientIdAndRecipientTypeAndTypeAndReferenceTypeAndCreatedAtAfter(
            String recipientId,
            RecipientType recipientType,
            NotificationType type,
            String referenceType,
            LocalDateTime createdAtAfter);
}
