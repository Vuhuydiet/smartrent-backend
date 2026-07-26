package com.smartrent.controller;

import com.smartrent.dto.request.RevealContactRequest;
import com.smartrent.dto.response.ApiResponse;
import com.smartrent.dto.response.RevealContactResponse;
import com.smartrent.service.contactreveal.ContactRevealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Tag(
        name = "Contact Reveal",
        description = "Authenticated reveal + audit of a seller's contact details (phone / email / zalo)."
)
public class ContactRevealController {

    ContactRevealService contactRevealService;

    @PostMapping("/{sellerUserId}/reveal-contact")
    @Operation(
            summary = "Reveal a seller's contact and log the access",
            description = """
                    Requires authentication. Records who revealed which seller's contact
                    (phone / email / zalo), optionally from which listing, and returns the
                    seller's contact details. Anonymous callers receive 401.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ApiResponse<RevealContactResponse> revealContact(
            @PathVariable String sellerUserId,
            @Valid @RequestBody RevealContactRequest request,
            HttpServletRequest httpRequest
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String viewerUserId = authentication == null ? null : authentication.getName();
        if (viewerUserId == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        String ipAddress = extractIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        RevealContactResponse response = contactRevealService.revealContact(
                sellerUserId, request, viewerUserId, ipAddress, userAgent);

        return ApiResponse.<RevealContactResponse>builder()
                .code("999999")
                .data(response)
                .build();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        // X-Forwarded-For can contain multiple IPs, take the first one
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}
