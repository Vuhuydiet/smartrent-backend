package com.smartrent.service.pushdetail.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartrent.dto.response.PushDetailResponse;
import com.smartrent.infra.repository.PushDetailRepository;
import com.smartrent.infra.repository.entity.PushDetail;
import com.smartrent.service.pushdetail.PushDetailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PushDetailServiceImpl implements PushDetailService {

    PushDetailRepository pushDetailRepository;
    ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PushDetailResponse> getAllActiveDetails() {
        log.info("Getting all active push details");
        return pushDetailRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PushDetailResponse> getAllDetails() {
        log.info("Getting all push details");
        return pushDetailRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PushDetailResponse getDetailByCode(String detailCode) {
        log.info("Getting push detail by code: {}", detailCode);
        PushDetail detail = pushDetailRepository.findByDetailCode(detailCode)
                .orElseThrow(() -> new RuntimeException("Push detail not found: " + detailCode));
        return mapToResponse(detail);
    }

    @Override
    @Transactional(readOnly = true)
    public PushDetailResponse getDetailById(Long pushDetailId) {
        log.info("Getting push detail by ID: {}", pushDetailId);
        PushDetail detail = pushDetailRepository.findById(pushDetailId)
                .orElseThrow(() -> new RuntimeException("Push detail not found: " + pushDetailId));
        return mapToResponse(detail);
    }

    private PushDetailResponse mapToResponse(PushDetail detail) {
        List<String> featuresList = null;
        if (detail.getFeatures() != null) {
            try {
                featuresList = objectMapper.readValue(detail.getFeatures(), new TypeReference<List<String>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse features JSON for detail {}: {}", detail.getDetailCode(), e.getMessage());
            }
        }

        return PushDetailResponse.builder()
                .pushDetailId(detail.getPushDetailId())
                .detailCode(detail.getDetailCode())
                .detailName(detail.getDetailName())
                .detailNameEn(detail.getDetailNameEn())
                .pricePerPush(detail.getPricePerPush())
                .quantity(detail.getQuantity())
                .totalPrice(detail.getTotalPrice())
                .discountPercentage(detail.getDiscountPercentage())
                .savings(detail.calculateSavings())
                .description(detail.getDescription())
                .features(featuresList)
                .isActive(detail.getIsActive())
                .displayOrder(detail.getDisplayOrder())
                .build();
    }
}

