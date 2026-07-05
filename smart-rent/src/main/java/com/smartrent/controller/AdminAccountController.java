package com.smartrent.controller;

import com.smartrent.dto.request.AdminFilterRequest;
import com.smartrent.dto.response.AdminAccountSummaryResponse;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.GetAdminResponse;
import com.smartrent.dto.response.PageResponse;
import com.smartrent.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin/admins")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyAuthority('ROLE_SA', 'ROLE_UA', 'ROLE_SPA')")
@Tag(name = "Admin Account Management", description = "Admin-facing admin-account list (dedicated route, separate from the self-profile GET /v1/admins)")
public class AdminAccountController {

    AdminService adminService;

    @GetMapping
    @Operation(
        summary = "List admin accounts",
        description = """
            Paginated, filterable, sortable list of all admin accounts.

            - `role`: CSV of role IDs, e.g. `SA,UA` — matches admins having any of the given roles.
            - `createdAt` accepts a single date (`2026-02-09`) or a range (`2026-02-09..2026-03-10`);
              either side of a range may be omitted for an open-ended bound.
            - `sort` format: `field,direction`, e.g. `firstName,asc`. Supported fields: `firstName`, `lastName`, `createdAt` (default `createdAt,desc`).
            """
    )
    public ApiResponse<PageResponse<AdminAccountSummaryResponse>> getAdmins(
            @Parameter(description = "Contains-match on first name") @RequestParam(required = false) String firstName,
            @Parameter(description = "Contains-match on last name") @RequestParam(required = false) String lastName,
            @Parameter(description = "Contains-match on email") @RequestParam(required = false) String email,
            @Parameter(description = "Contains-match on phone number") @RequestParam(required = false) String phoneNumber,
            @Parameter(description = "CSV of role IDs, e.g. SA,UA") @RequestParam(required = false) String role,
            @Parameter(description = "Single date or range (see description)", example = "2026-02-09..2026-03-10")
            @RequestParam(required = false) String createdAt,
            @Parameter(description = "field,direction — e.g. firstName,asc", example = "createdAt,desc")
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        AdminFilterRequest filterRequest = buildFilter(page, size, sort);
        putIfPresent(filterRequest, "firstName", firstName);
        putIfPresent(filterRequest, "lastName", lastName);
        putIfPresent(filterRequest, "email", email);
        putIfPresent(filterRequest, "phoneNumber", phoneNumber);
        putIfPresent(filterRequest, "role", role);
        putIfPresent(filterRequest, "createdAt", createdAt);

        PageResponse<GetAdminResponse> pageResponse = adminService.getAllAdmins(filterRequest);
        return ApiResponse.<PageResponse<AdminAccountSummaryResponse>>builder()
                .data(toSummaryPage(pageResponse))
                .build();
    }

    private AdminFilterRequest buildFilter(Integer page, Integer size, String sort) {
        AdminFilterRequest filterRequest = AdminFilterRequest.builder()
                .page(page != null ? page : 1)
                .size(size != null ? size : 20)
                .build();
        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",", 2);
            filterRequest.setSortBy(parts[0].trim());
            filterRequest.setSortDirection(parts[1].trim());
        } else if (sort != null && !sort.isBlank()) {
            filterRequest.setSortBy(sort.trim());
        }
        return filterRequest;
    }

    private void putIfPresent(AdminFilterRequest filterRequest, String key, String value) {
        if (value != null && !value.isBlank()) {
            filterRequest.getFilters().put(key, value);
        }
    }

    private PageResponse<AdminAccountSummaryResponse> toSummaryPage(PageResponse<GetAdminResponse> source) {
        return PageResponse.<AdminAccountSummaryResponse>builder()
                .page(source.getPage())
                .size(source.getSize())
                .totalElements(source.getTotalElements())
                .totalPages(source.getTotalPages())
                .data(source.getData().stream().map(this::toSummary).toList())
                .build();
    }

    private AdminAccountSummaryResponse toSummary(GetAdminResponse admin) {
        return AdminAccountSummaryResponse.builder()
                .adminId(admin.getAdminId())
                .phoneCode(admin.getPhoneCode())
                .phoneNumber(admin.getPhoneNumber())
                .email(admin.getEmail())
                .firstName(admin.getFirstName())
                .lastName(admin.getLastName())
                .createdAt(admin.getCreatedAt())
                .roles(admin.getRoles())
                .build();
    }
}
