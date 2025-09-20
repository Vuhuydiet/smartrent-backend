package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity(name = "users")
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_phone", columnNames = {"phone_code", "phone_number"})
    })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

  @Id
  @Column(name = "user_id")
  @GeneratedValue(strategy = GenerationType.UUID)
  String userId;

  @Column(name = "phone_code")
  String phoneCode;

  @Column(name = "phone_number")
  String phoneNumber;

  @Column(name = "email")
  String email;

  @Column(name = "password")
  String password;

  @Column(name = "first_name", nullable = false)
  String firstName;

  @Column(name = "last_name", nullable = false)
  String lastName;

  @Column(name = "id_document", unique = true)
  String idDocument;

  @Column(name = "tax_number", unique = true)
  String taxNumber;

  @Column(name = "is_verified")
  boolean isVerified;

}
