package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.ContactRevealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRevealLogRepository extends JpaRepository<ContactRevealLog, Long> {

    long countBySeller_UserId(String sellerUserId);

    long countByViewer_UserId(String viewerUserId);
}
