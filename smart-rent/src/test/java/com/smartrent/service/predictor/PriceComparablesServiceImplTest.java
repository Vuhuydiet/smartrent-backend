package com.smartrent.service.predictor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.smartrent.dto.request.PriceComparablesRequest;
import com.smartrent.dto.response.PriceComparablesResponse;
import com.smartrent.infra.repository.ListingRepository;
import com.smartrent.service.predictor.impl.PriceComparablesServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceComparablesServiceImplTest {

  @Mock ListingRepository listingRepository;

  @InjectMocks PriceComparablesServiceImpl service;

  private static Object[] row(long price, float area) {
    return new Object[] {BigDecimal.valueOf(price), area};
  }

  private PriceComparablesRequest request() {
    return PriceComparablesRequest.builder()
        .latitude(21.0)
        .longitude(105.8)
        .radiusKm(2.0)
        .productType("ROOM")
        .build();
  }

  @Test
  void computesPercentilesFromComparables() {
    // Prices 1..5 million; median = 3M, p25 = 2M, p75 = 4M.
    when(listingRepository.findPriceComparables(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), eq("RENT"), eq("ROOM"), eq("MONTH"), any(), any(), anyInt()))
        .thenReturn(List.of(
            row(1_000_000, 20f),
            row(2_000_000, 20f),
            row(3_000_000, 20f),
            row(4_000_000, 20f),
            row(5_000_000, 20f)));

    PriceComparablesResponse res = service.getComparables(request());

    assertThat(res.getSampleSize()).isEqualTo(5);
    assertThat(res.getMin()).isEqualTo(1_000_000);
    assertThat(res.getP25()).isEqualTo(2_000_000);
    assertThat(res.getMedian()).isEqualTo(3_000_000);
    assertThat(res.getP75()).isEqualTo(4_000_000);
    assertThat(res.getMax()).isEqualTo(5_000_000);
    assertThat(res.getAvg()).isEqualTo(3_000_000);
    // 3M / 20m² = 150k per m²
    assertThat(res.getPricePerSqmMedian()).isEqualTo(150_000);
  }

  @Test
  void returnsEmptyStatsWhenNoComparables() {
    when(listingRepository.findPriceComparables(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(List.of());

    PriceComparablesResponse res = service.getComparables(request());

    assertThat(res.getSampleSize()).isZero();
    assertThat(res.getMin()).isNull();
    assertThat(res.getP75()).isNull();
    assertThat(res.getCurrency()).isEqualTo("VND");
  }

  @Test
  void skipsRowsWithNullOrZeroAreaForPerSqmButKeepsThemForPrice() {
    when(listingRepository.findPriceComparables(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            anyDouble(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(java.util.Arrays.asList(
            new Object[] {BigDecimal.valueOf(2_000_000), null},
            row(4_000_000, 0f),
            row(6_000_000, 30f)));

    PriceComparablesResponse res = service.getComparables(request());

    assertThat(res.getSampleSize()).isEqualTo(3);
    assertThat(res.getMedian()).isEqualTo(4_000_000);
    // Only the 30m² row yields a per-m² figure: 6M / 30 = 200k
    assertThat(res.getPricePerSqmMedian()).isEqualTo(200_000);
  }
}
