package com.smartrent.infra.repository;

import com.smartrent.enums.BrokerVerificationStatus;
import com.smartrent.infra.repository.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

        Optional<User> findByEmail(String email);

        boolean existsByEmail(String email);

        boolean existsByPhoneCodeAndPhoneNumber(String phoneCode, String phoneNumber);

        boolean existsByIdDocument(String document);

        boolean existsByTaxNumber(String taxNumber);

        @Query(value = "SELECT DATE(u.created_at) AS label, COUNT(*) AS cnt " +
                        "FROM users u WHERE u.created_at BETWEEN :start AND :end " +
                        "GROUP BY DATE(u.created_at) ORDER BY label ASC", nativeQuery = true)
        List<Object[]> countNewUsersByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        @Query(value = "SELECT DATE_FORMAT(u.created_at, '%Y-%m') AS label, COUNT(*) AS cnt " +
                        "FROM users u WHERE u.created_at BETWEEN :start AND :end " +
                        "GROUP BY DATE_FORMAT(u.created_at, '%Y-%m') ORDER BY label ASC", nativeQuery = true)
        List<Object[]> countNewUsersByMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        long countByCreatedAtBefore(LocalDateTime dateTime);

        // Classifies by whether the user ever submitted a broker application (verification
        // status <> NONE), not by the current is_broker flag — is_broker only flips to true
        // once an admin approves, so keying off it made almost every signup cohort show up
        // as REGULAR even when a chunk of them had a pending/rejected broker application.
        @Query(value = "SELECT CASE WHEN u.broker_verification_status <> 'NONE' THEN 'BROKER' ELSE 'REGULAR' END AS label, COUNT(*) AS cnt " +
                        "FROM users u WHERE u.created_at BETWEEN :start AND :end " +
                        "GROUP BY 1", nativeQuery = true)
        List<Object[]> countNewUsersByRole(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        // Filtering on is_broker = true here excluded PENDING and REJECTED entirely, since
        // is_broker is only ever true for APPROVED users — the breakdown could never show
        // anything but 100% APPROVED. Filter on having applied at all instead.
        @Query(value = "SELECT u.broker_verification_status AS label, COUNT(*) AS cnt " +
                        "FROM users u WHERE u.created_at BETWEEN :start AND :end AND u.broker_verification_status <> 'NONE' " +
                        "GROUP BY u.broker_verification_status", nativeQuery = true)
        List<Object[]> countNewBrokersByVerificationStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        /**
         * System-wide count of brokers in a given verification state, independent of any date range.
         * Used for the "brokers pending approval" KPI, which reflects the current backlog rather
         * than only brokers who registered within the selected analytics window.
         */
        long countByBrokerVerificationStatus(BrokerVerificationStatus status);
}
