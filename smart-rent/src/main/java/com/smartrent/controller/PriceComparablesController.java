package com.smartrent.controller;

import com.smartrent.dto.request.PriceComparablesRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.PriceComparablesResponse;
import com.smartrent.service.predictor.PriceComparablesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/listings")
@Tag(name = "Listings - Price Comparables",
     description = "Aggregate price statistics over comparable listings, for AI price prediction.")
@RequiredArgsConstructor
public class PriceComparablesController {

    private final PriceComparablesService priceComparablesService;

    @PostMapping("/price-comparables")
    @Operation(
        summary = "[PUBLIC API] Aggregate price statistics of comparable listings near a point",
        description = """
            **PUBLIC API - Không cần authentication**

            Trả về thống kê giá (min / p25 / median / p75 / max / avg / giá trung vị theo m²)
            của các tin đăng tương tự quanh một toạ độ, đã lọc đúng theo loại BĐS, loại giao dịch,
            đơn vị giá, khoảng diện tích và bán kính. Chỉ tính trên tin đã verify, chưa hết hạn,
            không nháp/shadow.

            Khác `POST /search`: endpoint này KHÔNG phân trang và KHÔNG trả listing card — nó chạy
            một truy vấn aggregate (bounding-box + Haversine) và tính percentile server-side, để
            AI định giá dùng trực tiếp các con số ổn định thay vì tự cộng trừ trên tin thô.
            """
    )
    public ApiResponse<PriceComparablesResponse> getPriceComparables(
            @Valid @RequestBody PriceComparablesRequest request) {
        PriceComparablesResponse response = priceComparablesService.getComparables(request);
        return ApiResponse.<PriceComparablesResponse>builder().data(response).build();
    }
}
