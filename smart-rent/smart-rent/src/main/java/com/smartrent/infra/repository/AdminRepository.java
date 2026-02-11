package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.Admin;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

  Optional<Admin> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByPhoneCodeAndPhoneNumber(String phoneCode, String phoneNumber);
  
}
