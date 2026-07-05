package com.smartrent.controller;

import com.smartrent.dto.request.TransactionFilterRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.TransactionDetailResponse;
import com.smartrent.dto.response.TransactionHistoryItemResponse;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/me/transactions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Customer Transaction History", description = "Customer-facing transaction history APIs")
public class CustomerTransactionController {

    TransactionHistoryService transactionHistoryService;

    @GetMapping
    public ApiResponse<PageResponse<TransactionHistoryItemResponse>> getMyTransactions(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = newestFirst(page, size);
        TransactionFilterRequest filter = TransactionFilterRequest.builder()
                .status(parseStatus(status))
                .paymentType(type)
                .createdAt(toRangeParam(fromDate, toDate))
                .q(q)
                .build();

        return ApiResponse.<PageResponse<TransactionHistoryItemResponse>>builder()
                .code("200000")
                .message("Transactions retrieved successfully")
                .data(transactionHistoryService.getCustomerTransactions(authentication.getName(), filter, pageable))
                .build();
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionDetailResponse> getMyTransactionDetail(
            Authentication authentication,
            @PathVariable String transactionId) {
        return ApiResponse.<TransactionDetailResponse>builder()
                .code("200000")
                .message("Transaction retrieved successfully")
                .data(transactionHistoryService.getCustomerTransactionDetail(authentication.getName(), transactionId))
                .build();
    }

    private String toRangeParam(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return null;
        }
        return (fromDate != null ? fromDate : "") + ".." + (toDate != null ? toDate : "");
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
