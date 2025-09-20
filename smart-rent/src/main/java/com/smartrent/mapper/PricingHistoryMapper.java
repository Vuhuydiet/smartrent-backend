package com.smartrent.mapper;

import com.smartrent.controller.dto.response.PricingHistoryResponse;
import com.smartrent.infra.repository.entity.PricingHistory;

public interface PricingHistoryMapper {
    PricingHistoryResponse toResponse(PricingHistory pricingHistory);
}
