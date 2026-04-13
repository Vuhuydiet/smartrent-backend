package com.smartrent.infra.repository.entity;

import com.smartrent.enums.BrokerVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity(name = "users")
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_phone", columnNames = {"phone_code", "phone_number"})
    })
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends AbstractUser {

  @Id
  @Column(name = "user_id")
  @GeneratedValue(strategy = GenerationType.UUID)
  String userId;

  @Column(name = "id_document", unique = true)
  String idDocument;

  @Column(name = "tax_number", unique = true)
  String taxNumber;

  @Column(name = "is_verified")
  boolean isVerified;

  @Column(name = "contact_phone_number")
  String contactPhoneNumber;

  @Column(name = "contact_phone_verified")
  Boolean contactPhoneVerified;

  @Column(name = "avatar_url")
  String avatarUrl;

  @Column(name = "avatar_media_id")
  Long avatarMediaId;

  // ============ BROKER FIELDS ============

  /** True when the user is an approved broker. */
  @Builder.Default
  @Column(name = "is_broker", nullable = false)
  boolean isBroker = false;

  /** Current state in the broker verification lifecycle. */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "broker_verification_status", nullable = false, length = 20)
  BrokerVerificationStatus brokerVerificationStatus = BrokerVerificationStatus.NONE;

  /** Timestamp when the user first submitted a broker registration. */
  @Column(name = "broker_registered_at")
  LocalDateTime brokerRegisteredAt;

  /** Timestamp when an admin approved or rejected the registration. */
  @Column(name = "broker_verified_at")
  LocalDateTime brokerVerifiedAt;

  /** Admin ID who performed the final review (stored as VARCHAR(36) to support UUID-style admin IDs). */
  @Column(name = "broker_verified_by_admin_id", length = 36)
  String brokerVerifiedByAdminId;

  /** Admin-provided reason for rejection. Null when approved. */
  @Column(name = "broker_rejection_reason", length = 500)
  String brokerRejectionReason;

  /**
   * URL of the external registry used to manually verify the broker.
   * Default: https://www.nangluchdxd.gov.vn/Canhan?page=2&pagesize=20
   */
  @Column(name = "broker_verification_source", length = 255)
  String brokerVerificationSource;
}
