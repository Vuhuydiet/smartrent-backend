package com.smartrent.service.transaction;

import com.smartrent.dto.request.TransactionFilterRequest;
import com.smartrent.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionHistoryService {
    PageResponse<TransactionHistoryItemResponse> getCustomerTransactions(String customerId, TransactionFilterRequest filter, Pageable pageable);

    TransactionDetailResponse getCustomerTransactionDetail(String customerId, String transactionId);

    PageResponse<TransactionHistoryItemResponse> getAdminTransactions(TransactionFilterRequest filter, Pageable pageable);

    TransactionDetailResponse getAdminTransactionDetail(String transactionId);

    TransactionStatisticsResponse getStatistics(TransactionFilterRequest filter);

    List<RevenueSeriesResponse> getRevenueSeries(TransactionFilterRequest filter, String groupBy);

    byte[] exportCsv(TransactionFilterRequest filter);
}
