package com.smartrent.infra.repository.entity;

import com.smartrent.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity(name = "transaction_audits")
@Table(name = "transaction_audits",
        indexes = {
                @Index(name = "idx_transaction_audit_transaction", columnList = "transaction_id, created_at")
        })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "transaction_id", nullable = false, length = 36)
    String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    TransactionStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    TransactionStatus newStatus;

    @Column(name = "actor_type", nullable = false, length = 20)
    String actorType;

    @Column(name = "actor_id", length = 36)
    String actorId;

    @Column(name = "reason", length = 500)
    String reason;

    @Column(name = "provider_event_id", length = 120)
    String providerEventId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
