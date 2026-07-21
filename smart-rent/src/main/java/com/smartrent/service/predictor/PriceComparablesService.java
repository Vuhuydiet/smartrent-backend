package com.smartrent.service.predictor;

import com.smartrent.dto.request.PriceComparablesRequest;
import com.smartrent.dto.response.PriceComparablesResponse;

/**
 * Computes deterministic price statistics over comparable listings near a point.
 * Backs the price-comparables endpoint used by the AI price-prediction agent so
 * the price range comes from real market data aggregated in code, not from an
 * LLM totalling up raw listings.
 */
public interface PriceComparablesService {

    PriceComparablesResponse getComparables(PriceComparablesRequest request);
}
