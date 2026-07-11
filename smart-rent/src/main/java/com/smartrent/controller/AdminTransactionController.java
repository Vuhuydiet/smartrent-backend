package com.smartrent.controller;

import com.smartrent.dto.request.TransactionFilterRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.PaymentProvider;
import com.smartrent.enums.TransactionStatus;
import com.smartrent.enums.TransactionType;
import com.smartrent.service.transaction.TransactionHistoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_SPA', 'ROLE_FA')")
@Tag(name = "Admin Transaction Management", description = "Admin transaction search, detail, statistics, and export APIs")
public class AdminTransactionController {

    TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ApiResponse<PageResponse<TransactionHistoryItemResponse>> getTransactions(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentProvider paymentGateway,
            @RequestParam(required = false) TransactionType paymentType,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.<PageResponse<TransactionHistoryItemResponse>>builder()
                .code("200000")
                .message("Transactions retrieved successfully")
                .data(transactionHistoryService.getAdminTransactions(
                        filter(customerId, landlordId, transactionId, customer, status, paymentGateway, paymentType, createdAt, q),
                        newestFirst(page, size)))
                .build();
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionDetailResponse> getTransactionDetail(@PathVariable String transactionId) {
        return ApiResponse.<TransactionDetailResponse>builder()
                .code("200000")
                .message("Transaction retrieved successfully")
                .data(transactionHistoryService.getAdminTransactionDetail(transactionId))
                .build();
    }

    @GetMapping("/statistics")
    public ApiResponse<TransactionStatisticsResponse> getStatistics(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentProvider paymentGateway,
            @RequestParam(required = false) TransactionType paymentType,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String q) {
        return ApiResponse.<TransactionStatisticsResponse>builder()
                .code("200000")
                .message("Transaction statistics retrieved successfully")
                .data(transactionHistoryService.getStatistics(
                        filter(customerId, landlordId, transactionId, customer, status, paymentGateway, paymentType, createdAt, q)))
                .build();
    }

    @GetMapping("/revenue-series")
    public ApiResponse<List<RevenueSeriesResponse>> getRevenueSeries(
            @RequestParam(defaultValue = "DAY") String groupBy,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentProvider paymentGateway,
            @RequestParam(required = false) TransactionType paymentType,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String q) {
        return ApiResponse.<List<RevenueSeriesResponse>>builder()
                .code("200000")
                .message("Revenue series retrieved successfully")
                .data(transactionHistoryService.getRevenueSeries(
                        filter(customerId, landlordId, transactionId, customer, status, paymentGateway, paymentType, createdAt, q), groupBy))
                .build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String landlordId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) PaymentProvider paymentGateway,
            @RequestParam(required = false) TransactionType paymentType,
            @RequestParam(required = false) String createdAt,
            @RequestParam(required = false) String q) {
        byte[] csv = transactionHistoryService.exportCsv(
                filter(customerId, landlordId, transactionId, customer, status, paymentGateway, paymentType, createdAt, q));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    private TransactionFilterRequest filter(
            String customerId,
            String landlordId,
            String transactionId,
            String customer,
            String status,
            PaymentProvider paymentGateway,
            TransactionType paymentType,
            String createdAt,
            String q) {
        return TransactionFilterRequest.builder()
                .customerId(customerId)
                .landlordId(landlordId)
                .transactionId(transactionId)
                .customer(customer)
                .status(parseStatus(status))
                .paymentGateway(paymentGateway)
                .paymentType(paymentType)
                .createdAt(createdAt)
                .q(q)
                .build();
    }

    private Pageable newestFirst(int page, int size) {
        int normalizedPage = Math.max(page, 1) - 1;
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if ("SUCCESS".equalsIgnoreCase(status)) {
            return TransactionStatus.COMPLETED;
        }
        return TransactionStatus.valueOf(status.toUpperCase());
    }
}
