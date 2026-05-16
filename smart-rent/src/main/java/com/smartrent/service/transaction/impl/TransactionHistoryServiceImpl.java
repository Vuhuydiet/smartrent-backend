package com.smartrent.service.transaction.impl;

import com.smartrent.dto.request.TransactionFilterRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.ReferenceType;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.TransactionAuditRepository;
import com.smartrent.infra.repository.TransactionRepository;
import com.smartrent.infra.repository.UserRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.Transaction;
import com.smartrent.infra.repository.entity.TransactionAudit;
import com.smartrent.infra.repository.entity.User;
import com.smartrent.service.transaction.TransactionHistoryService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    TransactionRepository transactionRepository;
    TransactionAuditRepository transactionAuditRepository;
    UserRepository userRepository;
    ListingRepository listingRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryItemResponse> getCustomerTransactions(
            String customerId, TransactionFilterRequest filter, Pageable pageable) {
        filter.setCustomerId(customerId);
        Page<Transaction> page = transactionRepository.findAll(buildSpecification(filter), pageable);
        return toPageResponse(page.map(transaction -> toListItem(transaction, false)));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getCustomerTransactionDetail(String customerId, String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(item -> customerId.equals(item.getUserId()))
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
        return toDetail(transaction, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionHistoryItemResponse> getAdminTransactions(TransactionFilterRequest filter, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findAll(buildSpecification(filter), pageable);
        return toPageResponse(page.map(transaction -> toListItem(transaction, true)));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getAdminTransactionDetail(String transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found"));
        return toDetail(transaction, true);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionStatisticsResponse getStatistics(TransactionFilterRequest filter) {
        List<Transaction> transactions = transactionRepository.findAll(buildSpecification(filter));
        long total = transactions.size();
        long success = countStatus(transactions, TransactionStatus.COMPLETED);
        long failed = countStatus(transactions, TransactionStatus.FAILED);
        long pending = countStatus(transactions, TransactionStatus.PENDING);
        long cancelled = countStatus(transactions, TransactionStatus.CANCELLED);
        long refunded = countStatus(transactions, TransactionStatus.REFUNDED);

        BigDecimal revenue = transactions.stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.COMPLETED)
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = success == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(success), 0, RoundingMode.HALF_UP);

        return TransactionStatisticsResponse.builder()
                .totalRevenue(revenue)
                .totalTransactions(total)
                .successfulPayments(success)
                .failedPayments(failed)
                .pendingPayments(pending)
                .cancelledPayments(cancelled)
                .refundedPayments(refunded)
                .successRate(total == 0 ? 0 : BigDecimal.valueOf(success * 100.0 / total)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue())
                .averageSuccessfulAmount(average)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueSeriesResponse> getRevenueSeries(TransactionFilterRequest filter, String groupBy) {
        List<Transaction> transactions = transactionRepository.findAll(buildSpecification(filter)).stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.COMPLETED)
                .filter(transaction -> transaction.getCreatedAt() != null)
                .toList();

        DateTimeFormatter formatter = "MONTH".equalsIgnoreCase(groupBy)
                ? DateTimeFormatter.ofPattern("yyyy-MM")
                : DateTimeFormatter.ISO_LOCAL_DATE;

        Map<String, List<Transaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(transaction -> transaction.getCreatedAt().format(formatter), TreeMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> RevenueSeriesResponse.builder()
                        .period(entry.getKey())
                        .revenue(entry.getValue().stream()
                                .map(Transaction::getAmount)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add))
                        .successfulCount(entry.getValue().size())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(TransactionFilterRequest filter) {
        List<Transaction> transactions = transactionRepository.findAll(buildSpecification(filter));
        StringBuilder csv = new StringBuilder();
        csv.append("Transaction Code,Invoice Code,Customer Name,Customer Phone,Landlord Name,Room,Type,Gateway,Gateway Transaction Code,Status,Amount,Created At,Completed At,Failure Reason\n");
        for (Transaction transaction : transactions) {
            TransactionHistoryItemResponse row = toListItem(transaction, true);
            csv.append(csv(row.getTransactionCode())).append(',')
                    .append(csv(row.getInvoice() != null ? row.getInvoice().getInvoiceCode() : null)).append(',')
                    .append(csv(row.getCustomer() != null ? row.getCustomer().getName() : null)).append(',')
                    .append(csv(row.getCustomer() != null ? row.getCustomer().getPhone() : null)).append(',')
                    .append(csv(row.getLandlord() != null ? row.getLandlord().getName() : null)).append(',')
                    .append(csv(row.getRoom() != null ? row.getRoom().getRoomName() : null)).append(',')
                    .append(csv(row.getPaymentType())).append(',')
                    .append(csv(row.getPaymentGateway())).append(',')
                    .append(csv(row.getGatewayTransactionCode())).append(',')
                    .append(csv(row.getStatus())).append(',')
                    .append(row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO).append(',')
                    .append(csv(row.getCreatedAt() != null ? row.getCreatedAt().toString() : null)).append(',')
                    .append(csv(row.getCompletedAt() != null ? row.getCompletedAt().toString() : null)).append(',')
                    .append(csv(row.getFailureReason()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Specification<Transaction> buildSpecification(TransactionFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return criteriaBuilder.conjunction();
            }
            if (filter.getCustomerId() != null && !filter.getCustomerId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), filter.getCustomerId()));
            }
            if (filter.getLandlordId() != null && !filter.getLandlordId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("landlordId"), filter.getLandlordId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), filter.getType()));
            }
            if (filter.getGateway() != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentProvider"), filter.getGateway()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getFromDate().atStartOfDay()));
            }
            if (filter.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), filter.getToDate().plusDays(1).atStartOfDay()));
            }
            if (filter.getQ() != null && !filter.getQ().isBlank()) {
                String keyword = "%" + filter.getQ().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("transactionId")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("providerTransactionId")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("referenceId")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceCode")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("orderInfo")), keyword)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private PageResponse<TransactionHistoryItemResponse> toPageResponse(Page<TransactionHistoryItemResponse> page) {
        return PageResponse.<TransactionHistoryItemResponse>builder()
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .data(page.getContent())
                .build();
    }

    private TransactionHistoryItemResponse toListItem(Transaction transaction, boolean includeCustomer) {
        Listing listing = resolveListing(transaction).orElse(null);
        User customer = userRepository.findById(transaction.getUserId()).orElse(null);
        User landlord = resolveLandlord(transaction, listing).orElse(null);

        return TransactionHistoryItemResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transactionCode(transaction.getTransactionId())
                .amount(transaction.getAmount())
                .currency("VND")
                .paymentGateway(transaction.getPaymentProvider() != null ? transaction.getPaymentProvider().name() : null)
                .paymentMethod(transaction.getPaymentMethod())
                .gatewayTransactionCode(transaction.getProviderTransactionId())
                .status(toApiStatus(transaction.getStatus()))
                .paymentType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .createdAt(transaction.getCreatedAt())
                .completedAt(resolveCompletedAt(transaction))
                .invoice(toInvoice(transaction))
                .room(toRoom(transaction, listing))
                .customer(includeCustomer ? toCustomer(transaction, customer) : null)
                .landlord(toLandlord(transaction, landlord))
                .failureReason(resolveFailureReason(transaction))
                .build();
    }

    private TransactionDetailResponse toDetail(Transaction transaction, boolean includeAdminFields) {
        TransactionHistoryItemResponse item = toListItem(transaction, true);
        return TransactionDetailResponse.builder()
                .transactionId(transaction.getTransactionId())
                .transactionCode(transaction.getTransactionId())
                .idempotencyKey(includeAdminFields ? transaction.getIdempotencyKey() : null)
                .amount(transaction.getAmount())
                .currency("VND")
                .paymentGateway(transaction.getPaymentProvider() != null ? transaction.getPaymentProvider().name() : null)
                .paymentMethod(transaction.getPaymentMethod())
                .gatewayTransactionCode(transaction.getProviderTransactionId())
                .gatewayResponseCode(includeAdminFields ? transaction.getGatewayResponseCode() : null)
                .status(toApiStatus(transaction.getStatus()))
                .paymentType(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : null)
                .createdAt(transaction.getCreatedAt())
                .completedAt(item.getCompletedAt())
                .expiredAt(transaction.getExpiredAt())
                .invoice(item.getInvoice())
                .room(item.getRoom())
                .customer(item.getCustomer())
                .landlord(item.getLandlord())
                .failureReason(resolveFailureReason(transaction))
                .orderInfo(transaction.getOrderInfo())
                .providerPayload(includeAdminFields ? transaction.getProviderPayload() : null)
                .timeline(toTimeline(transaction))
                .build();
    }

    private List<TransactionTimelineResponse> toTimeline(Transaction transaction) {
        List<TransactionAudit> audits = transactionAuditRepository.findByTransactionIdOrderByCreatedAtAsc(transaction.getTransactionId());
        if (audits.isEmpty()) {
            List<TransactionTimelineResponse> timeline = new ArrayList<>();
            timeline.add(TransactionTimelineResponse.builder()
                    .status("PENDING")
                    .at(transaction.getCreatedAt())
                    .actorType("SYSTEM")
                    .note("Payment created")
                    .build());
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                timeline.add(TransactionTimelineResponse.builder()
                        .status(toApiStatus(transaction.getStatus()))
                        .at(resolveCompletedAt(transaction) != null ? resolveCompletedAt(transaction) : transaction.getUpdatedAt())
                        .actorType("GATEWAY")
                        .note(resolveFailureReason(transaction))
                        .build());
            }
            return timeline;
        }
        return audits.stream()
                .map(audit -> TransactionTimelineResponse.builder()
                        .status(toApiStatus(audit.getNewStatus()))
                        .at(audit.getCreatedAt())
                        .actorType(audit.getActorType())
                        .actorId(audit.getActorId())
                        .note(audit.getReason())
                        .build())
                .toList();
    }

    private Optional<Listing> resolveListing(Transaction transaction) {
        Long listingId = transaction.getRoomId();
        if (listingId == null && transaction.getReferenceId() != null && isListingReference(transaction)) {
            try {
                listingId = Long.valueOf(transaction.getReferenceId());
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return listingId == null ? Optional.empty() : listingRepository.findById(listingId);
    }

    private boolean isListingReference(Transaction transaction) {
        return transaction.getReferenceType() == ReferenceType.LISTING
                || transaction.getReferenceType() == ReferenceType.PUSH
                || transaction.getReferenceType() == ReferenceType.REPOST;
    }

    private Optional<User> resolveLandlord(Transaction transaction, Listing listing) {
        String landlordId = transaction.getLandlordId();
        if ((landlordId == null || landlordId.isBlank()) && listing != null) {
            landlordId = listing.getUserId();
        }
        return landlordId == null || landlordId.isBlank() ? Optional.empty() : userRepository.findById(landlordId);
    }

    private TransactionInvoiceResponse toInvoice(Transaction transaction) {
        if (isBlank(transaction.getInvoiceId()) && isBlank(transaction.getInvoiceCode())) {
            return null;
        }
        return TransactionInvoiceResponse.builder()
                .invoiceId(transaction.getInvoiceId())
                .invoiceCode(transaction.getInvoiceCode())
                .description(transaction.getOrderInfo())
                .build();
    }

    private TransactionRoomResponse toRoom(Transaction transaction, Listing listing) {
        Long roomId = transaction.getRoomId() != null ? transaction.getRoomId() : listing != null ? listing.getListingId() : null;
        String roomName = firstNonBlank(transaction.getRoomName(), listing != null ? listing.getTitle() : null);
        String address = firstNonBlank(transaction.getRoomAddress(),
                listing != null && listing.getAddress() != null ? listing.getAddress().getDisplayAddress() : null);
        if (roomId == null && roomName == null && address == null) {
            return null;
        }
        return TransactionRoomResponse.builder()
                .roomId(roomId)
                .roomCode(transaction.getRoomCode())
                .roomName(roomName)
                .address(address)
                .build();
    }

    private TransactionPartyResponse toCustomer(Transaction transaction, User user) {
        return TransactionPartyResponse.builder()
                .customerId(transaction.getUserId())
                .name(firstNonBlank(transaction.getCustomerNameSnapshot(), fullName(user)))
                .email(user != null ? user.getEmail() : null)
                .phone(firstNonBlank(transaction.getCustomerPhoneSnapshot(), phone(user)))
                .build();
    }

    private TransactionPartyResponse toLandlord(Transaction transaction, User user) {
        String landlordId = firstNonBlank(transaction.getLandlordId(), user != null ? user.getUserId() : null);
        String name = firstNonBlank(transaction.getLandlordNameSnapshot(), fullName(user));
        String phone = firstNonBlank(transaction.getLandlordPhoneSnapshot(), phone(user));
        if (landlordId == null && name == null && phone == null) {
            return null;
        }
        return TransactionPartyResponse.builder()
                .landlordId(landlordId)
                .name(name)
                .email(user != null ? user.getEmail() : null)
                .phone(phone)
                .build();
    }

    private LocalDateTime resolveCompletedAt(Transaction transaction) {
        if (transaction.getCompletedAt() != null) {
            return transaction.getCompletedAt();
        }
        return transaction.getStatus() == TransactionStatus.COMPLETED ? transaction.getUpdatedAt() : null;
    }

    private String resolveFailureReason(Transaction transaction) {
        if (!isBlank(transaction.getFailureReason())) {
            return transaction.getFailureReason();
        }
        if (transaction.getStatus() == TransactionStatus.FAILED || transaction.getStatus() == TransactionStatus.CANCELLED) {
            return transaction.getAdditionalInfo();
        }
        return null;
    }

    private long countStatus(List<Transaction> transactions, TransactionStatus status) {
        return transactions.stream().filter(transaction -> transaction.getStatus() == status).count();
    }

    private String toApiStatus(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return status == TransactionStatus.COMPLETED ? "SUCCESS" : status.name();
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        return (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
    }

    private String phone(User user) {
        if (user == null) {
            return null;
        }
        return firstNonBlank(user.getContactPhoneNumber(), (safe(user.getPhoneCode()) + safe(user.getPhoneNumber())).trim());
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : !isBlank(second) ? second : null;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
