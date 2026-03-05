package com.smartrent.infra.repository.entity;

import com.smartrent.enums.NotificationType;
import com.smartrent.enums.RecipientType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity(name = "notifications")
@Table(name = "notifications")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "recipient_id", nullable = false, length = 36)
    String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 10)
    RecipientType recipientType;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    NotificationType type;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    String message;

    @Column(name = "reference_id")
    Long referenceId;

    @Column(name = "reference_type", length = 30)
    String referenceType;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
