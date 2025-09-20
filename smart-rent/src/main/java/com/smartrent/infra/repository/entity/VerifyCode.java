package com.smartrent.infra.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity(name = "verify_codes")
@Table(name = "verify_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyCode {

  @Id
  @Column(name = "verify_code")
  @Size(min = 6, max = 6)
  String verifyCode;

  @Column(name = "expiration_time", nullable = false)
  LocalDateTime expirationTime;

  @OneToOne(fetch = FetchType.LAZY, cascade = {
      CascadeType.MERGE, CascadeType.PERSIST,
      CascadeType.DETACH, CascadeType.REFRESH
  })
  @JoinColumn(name = "user_id", nullable = false)
  User user;

}
