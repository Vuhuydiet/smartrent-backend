package com.smartrent.controller;

import com.smartrent.dto.request.PostingBlockRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.ListingReportResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.dto.response.ReportedAuthorResponse;
import com.smartrent.service.report.ReportedAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/admin/reported-authors")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Admin - Reported Authors",
        description = "Admin APIs for tracking listing authors by their reports and blocking them from posting")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminReportedAuthorController {

    ReportedAuthorService reportedAuthorService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_CM', 'ROLE_SPA')")
    @Operation(
            summary = "List reported listing authors (Admin)",
            description = "Paginated list of authors who have at least one report on their listings, " +
                    "with total / approved report counts and current posting-block state.")
    public ApiResponse<PageResponse<ReportedAuthorResponse>> getReportedAuthors(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<ReportedAuthorResponse> result = reportedAuthorService.getReportedAuthors(page, size);
        return ApiResponse.<PageResponse<ReportedAuthorResponse>>builder().data(result).build();
    }

    @GetMapping("/{userId}/reports")
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_CM', 'ROLE_SPA')")
    @Operation(
            summary = "Get an author's admin-approved reports (Admin)",
            description = "All reports across the author's listings that an admin has approved (RESOLVED).")
    public ApiResponse<List<ListingReportResponse>> getApprovedReports(
            @Parameter(description = "Author user ID", required = true) @PathVariable String userId) {

        List<ListingReportResponse> reports = reportedAuthorService.getApprovedReports(userId);
        return ApiResponse.<List<ListingReportResponse>>builder().data(reports).build();
    }

    @PatchMapping("/{userId}/posting-block")
    @PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_CM', 'ROLE_SPA')")
    @Operation(
            summary = "Block or unblock an author from posting (Admin)",
            description = "Blocks the author from creating new listings (or lifts an existing block). " +
                    "A blocked user is rejected at listing creation on the backend.")
    public ApiResponse<ReportedAuthorResponse> setPostingBlock(
            @Parameter(description = "Author user ID", required = true) @PathVariable String userId,
            @Valid @RequestBody PostingBlockRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String adminId = auth.getName();
        log.info("Admin {} setting posting-block={} for user {}", adminId, request.getBlocked(), userId);

        ReportedAuthorResponse response = reportedAuthorService.setPostingBlock(userId, request, adminId);
        return ApiResponse.<ReportedAuthorResponse>builder().data(response).build();
    }
}
