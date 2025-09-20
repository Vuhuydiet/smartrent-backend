package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.VerifyCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface VerifyCodeRepository extends JpaRepository<VerifyCode, String> {

  @Query("SELECT v FROM verify_codes v WHERE v.verifyCode = :verifyCode AND v.user.email = :email")
  Optional<VerifyCode> findByVerifyCodeAndUserEmail(String verifyCode, String email);

  @Modifying
  @Transactional
  @Query("DELETE FROM verify_codes v WHERE v.user.userId = :userId")
  void deleteByUserId(String userId);
}
