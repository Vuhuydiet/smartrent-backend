package com.smartrent.infra.repository;

import com.smartrent.infra.repository.entity.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {
    List<TransactionAudit> findByTransactionIdOrderByCreatedAtAsc(String transactionId);
}
