package com.smartrent.mapper.impl;

import com.smartrent.controller.dto.response.PricingHistoryResponse;
import com.smartrent.infra.repository.entity.PricingHistory;
import com.smartrent.mapper.PricingHistoryMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PricingHistoryMapperImpl implements PricingHistoryMapper {

    @Override
    public PricingHistoryResponse toResponse(PricingHistory pricingHistory) {
        if (pricingHistory == null) {
            return null;
        }

        return PricingHistoryResponse.builder()
                .id(pricingHistory.getId())
                .listingId(pricingHistory.getListing() != null ?
                          pricingHistory.getListing().getListingId() : null)
                .oldPrice(pricingHistory.getOldPrice())
                .newPrice(pricingHistory.getNewPrice())
                .oldPriceUnit(pricingHistory.getOldPriceUnit() != null ?
                             pricingHistory.getOldPriceUnit().name() : null)
                .newPriceUnit(pricingHistory.getNewPriceUnit() != null ?
                             pricingHistory.getNewPriceUnit().name() : null)
                .changeType(pricingHistory.getChangeType() != null ?
                           pricingHistory.getChangeType().name() : null)
                .changePercentage(pricingHistory.getChangePercentage())
                .changeAmount(pricingHistory.getChangeAmount())
                .isCurrent(pricingHistory.getIsCurrent())
                .changedBy(pricingHistory.getChangedBy())
                .changeReason(pricingHistory.getChangeReason())
                .changedAt(pricingHistory.getChangedAt())
                .build();
    }
}
