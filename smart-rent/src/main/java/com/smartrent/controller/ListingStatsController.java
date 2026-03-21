package com.smartrent.controller;

import com.smartrent.dto.request.CategoryStatsRequest;
import com.smartrent.dto.request.ProvinceStatsRequest;
import com.smartrent.dto.response.*;
import com.smartrent.enums.BenefitType;
import com.smartrent.service.listing.ListingService;
import com.smartrent.service.quota.QuotaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/listings")
@Tag(
    name = "Listings - Statistics & Quota",
    description = """
        Listing statistics and posting quota management.

        **Public endpoints (no auth required):**
        - `POST /stats/provinces` - Listing count by province (for Home screen)
        - `POST /stats/categories` - Listing count by category (for Home screen)

        **Authenticated endpoints:**
        - `GET /quota-check` - Check VIP posting quota availability (SILVER/GOLD/DIAMOND)
        """
)
@RequiredArgsConstructor
public class ListingStatsController {

    private final ListingService listingService;
    private final QuotaService quotaService;

    @PostMapping("/stats/provinces")
    @Operation(
        summary = "Lấy thống kê số lượng bài đăng theo tỉnh/thành phố",
        description = """
            ## MÔ TẢ
            API này được thiết kế cho **màn hình Home** - Frontend truyền danh sách tỉnh/thành phố
            và nhận về thống kê số lượng bài đăng của từng tỉnh.

            **Use Case**: Hiển thị 5 địa điểm (tỉnh/thành phố) với số lượng bài đăng trên màn Home.

            **API CÔNG KHAI** - KHÔNG CẦN authentication token
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProvinceStatsRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Top 5 tỉnh lớn",
                        summary = "Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ",
                        value = """
                            {
                              "provinceIds": [1, 79, 48, 31, 92],
                              "verifiedOnly": false,
                              "addressType": "OLD"
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Thống kê thành công",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ApiResponse<List<ProvinceListingStatsResponse>> getProvinceStats(
            @Valid @RequestBody ProvinceStatsRequest request) {
        List<ProvinceListingStatsResponse> stats = listingService.getProvinceStats(request);
        return ApiResponse.<List<ProvinceListingStatsResponse>>builder()
                .data(stats)
                .build();
    }

    @PostMapping("/stats/categories")
    @Operation(
        summary = "Lấy thống kê bài đăng theo categories (API công khai)",
        description = """
            API công khai để lấy thống kê số lượng bài đăng theo danh sách categories.
            Trả về số lượng tổng, đã verify, và VIP cho mỗi category.

            **Public API** - Không cần authentication
            """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Category statistics request với danh sách category IDs",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CategoryStatsRequest.class),
                examples = {
                    @ExampleObject(
                        name = "All categories",
                        summary = "Lấy thống kê cho tất cả loại BĐS",
                        value = """
                            {
                              "categoryIds": [1, 2, 3, 4, 5],
                              "verifiedOnly": false
                            }
                            """
                    )
                }
            )
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Thống kê thành công",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ApiResponse<List<CategoryListingStatsResponse>> getCategoryStats(
            @Valid @RequestBody CategoryStatsRequest request) {
        List<CategoryListingStatsResponse> stats = listingService.getCategoryStats(request);
        return ApiResponse.<List<CategoryListingStatsResponse>>builder()
                .data(stats)
                .build();
    }

    @GetMapping("/quota-check")
    @Operation(
        summary = "Check posting quota",
        description = "Check available VIP and Premium posting quota for current user. Returns all quotas if vipType not specified.",
        parameters = {
            @Parameter(
                name = "vipType",
                description = "Specific VIP type to check (SILVER, GOLD, DIAMOND). If not provided, returns all quotas.",
                required = false
            )
        },
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Quota information retrieved",
                content = @Content(mediaType = "application/json")
            )
        }
    )
    public ApiResponse<Object> checkPostingQuota(@RequestParam(required = false) String vipType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        if (vipType != null) {
            BenefitType benefitType = switch (vipType.toUpperCase()) {
                case "SILVER" -> BenefitType.POST_SILVER;
                case "GOLD" -> BenefitType.POST_GOLD;
                case "DIAMOND" -> BenefitType.POST_DIAMOND;
                default -> BenefitType.POST_SILVER;
            };
            QuotaStatusResponse quota = quotaService.checkQuotaAvailability(userId, benefitType);
            return ApiResponse.builder().data(quota).build();
        }

        QuotaStatusResponse silverQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_SILVER);
        QuotaStatusResponse goldQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_GOLD);
        QuotaStatusResponse diamondQuota = quotaService.checkQuotaAvailability(userId, BenefitType.POST_DIAMOND);
        QuotaStatusResponse pushQuota = quotaService.checkQuotaAvailability(userId, BenefitType.PUSH);

        return ApiResponse.builder().data(java.util.Map.of(
                "silverPosts", silverQuota,
                "goldPosts", goldQuota,
                "diamondPosts", diamondQuota,
                "pushes", pushQuota
        )).build();
    }
}
