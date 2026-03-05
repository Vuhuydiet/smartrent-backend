package com.smartrent.infra.repository;

import com.smartrent.enums.RecipientType;
import com.smartrent.infra.repository.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(
            String recipientId, RecipientType recipientType, Pageable pageable);

    long countByRecipientIdAndRecipientTypeAndIsReadFalse(
            String recipientId, RecipientType recipientType);

    @Modifying
    @Query("UPDATE notifications n SET n.isRead = true " +
           "WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.isRead = false")
    int markAllAsRead(@Param("recipientId") String recipientId,
                      @Param("recipientType") RecipientType recipientType);
}
