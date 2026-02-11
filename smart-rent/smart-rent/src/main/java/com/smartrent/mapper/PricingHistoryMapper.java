package com.smartrent.mapper;

import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.infra.repository.entity.PricingHistory;

public interface PricingHistoryMapper {
    PricingHistoryResponse toResponse(PricingHistory pricingHistory);
}
