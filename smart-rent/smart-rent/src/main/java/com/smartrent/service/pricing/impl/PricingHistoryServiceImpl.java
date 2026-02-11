package com.smartrent.service.pricing.impl;

import com.smartrent.dto.request.PriceUpdateRequest;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.PricingHistoryResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.infra.repository.PricingHistoryRepository;
import com.smartrent.infra.repository.entity.Listing;
import com.smartrent.infra.repository.entity.PricingHistory;
import com.smartrent.mapper.PricingHistoryMapper;
import com.smartrent.service.pricing.PricingHistoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class PricingHistoryServiceImpl implements PricingHistoryService {

    PricingHistoryRepository pricingHistoryRepository;
    ListingRepository listingRepository;
    PricingHistoryMapper pricingHistoryMapper;

    @Override
    @Transactional
    public PricingHistoryResponse createInitialPricing(Long listingId, BigDecimal initialPrice, String priceUnit, String changedBy) {
        log.info("Creating initial pricing for listing: {} with price: {}", listingId, initialPrice);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + listingId));

        PricingHistory initialPricing = PricingHistory.builder()
                .listing(listing)
                .oldPrice(null)
                .newPrice(initialPrice)
                .oldPriceUnit(null)
                .newPriceUnit(Listing.PriceUnit.valueOf(priceUnit))
                .changeType(PricingHistory.PriceChangeType.INITIAL)
                .changePercentage(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .isCurrent(true)
                .changedBy(changedBy)
                .changeReason("Initial pricing")
                .build();

        PricingHistory savedPricing = pricingHistoryRepository.save(initialPricing);
        return pricingHistoryMapper.toResponse(savedPricing);
    }

    @Override
    public List<PricingHistoryResponse> getPricingHistoryByListingId(Long listingId) {
        log.info("Getting pricing history for listing: {}", listingId);
        return pricingHistoryRepository.findByListingListingIdOrderByChangedAtDesc(listingId)
                .stream()
                .map(pricingHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PricingHistoryResponse> getPricingHistoryByListingId(Long listingId, int page, int size) {
        log.info("Getting pricing history for listing: {} with pagination - page: {}, size: {}", listingId, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PricingHistory> pricingHistoryPage = pricingHistoryRepository.findAll(pageable);

        // Filter by listingId
        List<PricingHistoryResponse> pricingHistoryResponses = pricingHistoryPage.getContent().stream()
                .filter(ph -> ph.getListing().getListingId().equals(listingId))
                .map(pricingHistoryMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} pricing history records", pricingHistoryResponses.size());

        return PageResponse.<PricingHistoryResponse>builder()
                .page(page)
                .size(pricingHistoryPage.getSize())
                .totalPages(pricingHistoryPage.getTotalPages())
                .totalElements(pricingHistoryPage.getTotalElements())
                .data(pricingHistoryResponses)
                .build();
    }

    @Override
    public PricingHistoryResponse getCurrentPrice(Long listingId) {
        log.info("Getting current price for listing: {}", listingId);
        return pricingHistoryRepository.findByListingListingIdAndIsCurrentTrue(listingId)
                .map(pricingHistoryMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("No current pricing found for listing: " + listingId));
    }

    @Override
    public List<PricingHistoryResponse> getPricingHistoryByDateRange(Long listingId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Getting pricing history for listing: {} between {} and {}", listingId, startDate, endDate);
        return pricingHistoryRepository.findByListingListingIdAndChangedAtBetweenOrderByChangedAtDesc(listingId, startDate, endDate)
                .stream()
                .map(pricingHistoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PricingHistoryResponse> getPricingHistoryByDateRange(Long listingId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        log.info("Getting pricing history for listing: {} between {} and {} with pagination - page: {}, size: {}",
                listingId, startDate, endDate, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PricingHistory> pricingHistoryPage = pricingHistoryRepository.findAll(pageable);

        // Filter by listingId and date range
        List<PricingHistoryResponse> pricingHistoryResponses = pricingHistoryPage.getContent().stream()
                .filter(ph -> ph.getListing().getListingId().equals(listingId))
                .filter(ph -> !ph.getChangedAt().isBefore(startDate) && !ph.getChangedAt().isAfter(endDate))
                .map(pricingHistoryMapper::toResponse)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} pricing history records for date range", pricingHistoryResponses.size());

        return PageResponse.<PricingHistoryResponse>builder()
                .page(page)
                .size(pricingHistoryPage.getSize())
                .totalPages(pricingHistoryPage.getTotalPages())
                .totalElements(pricingHistoryPage.getTotalElements())
                .data(pricingHistoryResponses)
                .build();
    }

    @Override
    @Transactional
    public PricingHistoryResponse updatePrice(Long listingId, PriceUpdateRequest request, String changedBy) {
        log.info("Updating price for listing: {} to: {}", listingId, request.getNewPrice());

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found with id: " + listingId));

        // Get current price
        PricingHistory currentPricing = pricingHistoryRepository.findByListingListingIdAndIsCurrentTrue(listingId)
                .orElseThrow(() -> new RuntimeException("No current pricing found for listing: " + listingId));

        // Mark current pricing as not current
        pricingHistoryRepository.updateIsCurrentFalseByListingListingId(listingId);

        // Calculate change
        BigDecimal oldPrice = currentPricing.getNewPrice();
        BigDecimal newPrice = request.getNewPrice();
        Listing.PriceUnit oldPriceUnit = currentPricing.getNewPriceUnit();
        Listing.PriceUnit newPriceUnit = Listing.PriceUnit.valueOf(request.getPriceUnit());

        PricingHistory.PriceChangeType changeType = determineChangeType(oldPrice, newPrice, oldPriceUnit, newPriceUnit);
        BigDecimal changeAmount = newPrice.subtract(oldPrice);
        BigDecimal changePercentage = calculateChangePercentage(oldPrice, newPrice);

        // Create new pricing history
        PricingHistory newPricing = PricingHistory.builder()
                .listing(listing)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .oldPriceUnit(oldPriceUnit)
                .newPriceUnit(newPriceUnit)
                .changeType(changeType)
                .changePercentage(changePercentage)
                .changeAmount(changeAmount)
                .isCurrent(true)
                .changedBy(changedBy)
                .changeReason(request.getChangeReason())
                .build();

        PricingHistory savedPricing = pricingHistoryRepository.save(newPricing);

        // Update listing with new price
        listing.setPrice(newPrice);
        listing.setPriceUnit(newPriceUnit);
        listingRepository.save(listing);

        return pricingHistoryMapper.toResponse(savedPricing);
    }

    @Override
    public PriceStatistics getPriceStatistics(Long listingId) {
        log.info("Getting price statistics for listing: {}", listingId);

        List<PricingHistory> allPricing = pricingHistoryRepository.findByListingListingIdOrderByChangedAtDesc(listingId);

        if (allPricing.isEmpty()) {
            throw new RuntimeException("No pricing history found for listing: " + listingId);
        }

        BigDecimal minPrice = allPricing.stream()
                .map(PricingHistory::getNewPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = allPricing.stream()
                .map(PricingHistory::getNewPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal avgPrice = allPricing.stream()
                .map(PricingHistory::getNewPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allPricing.size()), RoundingMode.HALF_UP);

        int totalChanges = allPricing.size() - 1; // Exclude initial pricing
        int priceIncreases = (int) allPricing.stream()
                .filter(PricingHistory::isPriceIncrease)
                .count();
        int priceDecreases = (int) allPricing.stream()
                .filter(PricingHistory::isPriceDecrease)
                .count();

        return new PriceStatistics(minPrice, maxPrice, avgPrice, totalChanges, priceIncreases, priceDecreases);
    }

    @Override
    public List<Long> getListingsWithRecentPriceChanges(int daysBack) {
        log.info("Getting listings with price changes in last {} days", daysBack);
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(daysBack);
        return pricingHistoryRepository.findDistinctListingIdsByChangedAtAfterAndChangeTypeNot(sinceDate);
    }

    @Override
    public PageResponse<Long> getListingsWithRecentPriceChanges(int daysBack, int page, int size) {
        log.info("Getting listings with price changes in last {} days with pagination - page: {}, size: {}", daysBack, page, size);
        LocalDateTime sinceDate = LocalDateTime.now().minusDays(daysBack);
        List<Long> allListingIds = pricingHistoryRepository.findDistinctListingIdsByChangedAtAfterAndChangeTypeNot(sinceDate);

        // Manual pagination since repository returns List<Long>
        int start = (page - 1) * size;
        int end = Math.min(start + size, allListingIds.size());

        List<Long> paginatedListingIds = (start < allListingIds.size())
                ? allListingIds.subList(start, end)
                : List.of();

        int totalElements = allListingIds.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        log.info("Successfully retrieved {} listing IDs with recent price changes", paginatedListingIds.size());

        return PageResponse.<Long>builder()
                .page(page)
                .size(size)
                .totalPages(totalPages)
                .totalElements((long) totalElements)
                .data(paginatedListingIds)
                .build();
    }

    private PricingHistory.PriceChangeType determineChangeType(BigDecimal oldPrice, BigDecimal newPrice,
                                                              Listing.PriceUnit oldUnit, Listing.PriceUnit newUnit) {
        if (!oldUnit.equals(newUnit)) {
            return PricingHistory.PriceChangeType.UNIT_CHANGE;
        }

        int comparison = newPrice.compareTo(oldPrice);
        if (comparison > 0) {
            return PricingHistory.PriceChangeType.INCREASE;
        } else if (comparison < 0) {
            return PricingHistory.PriceChangeType.DECREASE;
        } else {
            return PricingHistory.PriceChangeType.CORRECTION;
        }
    }

    private BigDecimal calculateChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
