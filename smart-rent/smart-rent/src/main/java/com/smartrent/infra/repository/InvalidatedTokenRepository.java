package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.InvalidatedToken;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
  @Query("SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END FROM invalidated_tokens t" +
      " WHERE t.accessId = :id OR t.refreshId = :id")
  boolean existsByAccessIdOrRefreshId(String id);

  void deleteAllByExpirationTimeBefore(LocalDateTime time);
}
