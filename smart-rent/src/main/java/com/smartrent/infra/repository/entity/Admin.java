package com.smartrent.infra.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity(name = "admins")
@Table(name = "admins",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_admins_phone", columnNames = {"phone_code", "phone_number"})
       })
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Admin {

  @Id
  @Column(name = "admin_id")
  @GeneratedValue(strategy = GenerationType.UUID)
  String adminId;

  @Column(name = "phone_code", nullable = false)
  String phoneCode;

  @Column(name = "phone_number", nullable = false)
  String phoneNumber;

  @Column(name = "email", nullable = false)
  String email;

  @Column(name = "password", nullable = false)
  String password;

  @Column(name = "first_name", nullable = false)
  String firstName;

  @Column(name = "last_name", nullable = false)
  String lastName;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "admins_roles",
      joinColumns = @JoinColumn(name = "admin_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  List<Role> roles;

}
