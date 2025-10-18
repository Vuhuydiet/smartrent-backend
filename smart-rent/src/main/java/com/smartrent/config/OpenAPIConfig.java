package com.smartrent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for SmartRent API documentation.
 * This configuration provides comprehensive API documentation with proper grouping,
 * security schemes, and standardized error responses.
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${open.api.title}") String appTitle,
            @Value("${open.api.version}") String appVersion,
            @Value("${open.api.description}") String appDescription,
            @Value("${open.api.server.url}") String appServerUrl,
            @Value("${open.api.server.description}") String appServerDescription
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title(appTitle)
                        .version(appVersion)
                        .description(buildApiDescription(appDescription))
                        .termsOfService("https://smartrent.com/terms")
                        .contact(new Contact()
                                .name("SmartRent API Support")
                                .email("api-support@smartrent.com")
                                .url("https://smartrent.com/support"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(appServerUrl).description(appServerDescription),
                        new Server().url("https://api.smartrent.com").description("Production Server"),
                        new Server().url("https://staging-api.smartrent.com").description("Staging Server")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("SmartRent API Documentation")
                        .url("https://docs.smartrent.com"))
                .components(buildComponents())
                .security(List.of(new SecurityRequirement().addList("Bearer Authentication")));
    }

    /**
     * Builds comprehensive API description with authentication, error handling, and usage guidelines.
     */
    private String buildApiDescription(String baseDescription) {
        return baseDescription + "\n\n" +
                "## Authentication\n" +
                "This API uses JWT (JSON Web Token) for authentication. To access protected endpoints:\n" +
                "1. **User Authentication**: Use `/v1/auth` endpoint to authenticate users\n" +
                "2. **Admin Authentication**: Use `/v1/auth/admin` endpoint to authenticate administrators\n" +
                "3. **Include Token**: Add the access token in the Authorization header: `Bearer <access_token>`\n" +
                "4. **Token Refresh**: Use the refresh token to obtain new access tokens when they expire\n" +
                "5. **Token Validation**: Use `/v1/auth/introspect` to validate token status\n\n" +

                "## Email Verification\n" +
                "User accounts require email verification before full activation:\n" +
                "1. Create user account via `/v1/users`\n" +
                "2. Send verification code via `/v1/verification/code`\n" +
                "3. Verify email using `/v1/verification` with the received code\n\n" +

                "## VNPay Payment Integration\n" +
                "SmartRent uses VNPay as the exclusive payment gateway:\n" +
                "- **No Wallet System**: All payments go directly through VNPay\n" +
                "- **Dual Payment Model**: Use membership quota (free) or pay-per-action\n" +
                "- **Secure Transactions**: HMAC-SHA512 signature verification\n" +
                "- **Payment Flows**: Membership purchase, pay-per-post, pay-per-push\n" +
                "- **Transaction Tracking**: Complete history of all payments\n" +
                "- Initiate payments via `/v1/payments/*`\n" +
                "- View transaction history via `/v1/payments/history`\n\n" +

                "## Membership System\n" +
                "SmartRent offers premium membership packages with exclusive benefits:\n" +
                "- **Basic Package** (700,000 VND): 3 VIP posts, 1 Premium post, 5 pushes\n" +
                "- **Standard Package** (1,400,000 VND): 7 VIP posts, 3 Premium posts, 13 pushes\n" +
                "- **Advanced Package** (2,800,000 VND): 15 VIP posts, 7 Premium posts, 30 pushes\n" +
                "- **Auto-Verification**: VIP/Premium posts skip manual review\n" +
                "- Purchase memberships via `/v1/payments/membership`\n" +
                "- Check quota availability via `/v1/listings/quota-check`\n\n" +

                "## VIP Listing Creation\n" +
                "Create premium listings with enhanced visibility:\n" +
                "- **Normal Posts**: 90,000 VND/30 days\n" +
                "- **VIP Posts**: 600,000 VND/30 days (or use quota)\n" +
                "- **Premium Posts**: 1,800,000 VND/30 days (or use quota)\n" +
                "- **Premium Shadow**: Premium posts auto-create a NORMAL shadow listing\n" +
                "- Create VIP listings via `/v1/listings/vip`\n" +
                "- Check quota before posting via `/v1/listings/quota-check`\n\n" +

                "## Listing Push\n" +
                "Increase your listing visibility with push features:\n" +
                "- **Instant Push**: Push listing to top immediately via `/v1/pushes/push`\n" +
                "- **Scheduled Push**: Schedule automatic daily pushes via `/v1/pushes/schedule`\n" +
                "- **Payment Options**: Use membership quota (free) or pay 40,000 VND\n" +
                "- **Premium Auto-Push**: Pushing Premium also pushes shadow listing\n" +
                "- **History Tracking**: View push history for analytics\n\n" +

                "## Saved Listings\n" +
                "Users can save favorite listings for later viewing:\n" +
                "- Save listings via `/v1/saved-listings`\n" +
                "- View saved listings via `/v1/saved-listings/my-saved`\n" +
                "- Check if listing is saved via `/v1/saved-listings/check/{listingId}`\n\n" +

                "## Circuit Breaker & Resilience\n" +
                "The API implements circuit breaker patterns for email services to ensure reliability:\n" +
                "- Automatic retry on transient failures\n" +
                "- Circuit breaker protection for email service\n" +
                "- Graceful degradation during service outages\n\n" +

                "## Rate Limiting\n" +
                "API requests are rate-limited to prevent abuse. If you exceed the rate limit, you'll receive a 429 status code.\n\n" +

                "## Error Handling\n" +
                "All API responses follow a consistent format:\n" +
                "```json\n" +
                "  \"data\": { /* response data */ }\n" +
                "}\n" +
                "```\n" +
                "**Error Code Categories:**\n" +
                "- `1xxx`: Internal server errors\n" +
                "- `2xxx`: Client input validation errors\n" +
                "- `3xxx`: Resource conflict errors (already exists)\n" +
                "- `4xxx`: Resource not found errors\n" +
                "- `5xxx`: Authentication errors (unauthenticated)\n" +

                "## API Versioning\n" +
                "All endpoints are versioned with `/v1/` prefix. Future versions will use `/v2/`, etc.\n\n" +

                "## Response Format\n" +
                "- All timestamps are in UTC format\n" +
                "- Sensitive data (passwords, tokens) are masked in logs\n" +
                "- Null fields are excluded from JSON responses\n" +
                "- All string fields support UTF-8 encoding";
    }

    /**
     * Builds comprehensive components including security schemes and common response schemas.
     */
    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("Bearer Authentication",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")

                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT Authorization header using the Bearer scheme. " +
                                        "Enter 'Bearer' [space] and then your token in the text input below. " +
                                        "Example: 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'")
                )
                // Add common response schemas
                .addSchemas("ErrorResponse", new Schema<>()
                        .type("object")
                        .description("Standard error response format")
                        .addProperty("code", new Schema<>().type("string").example("400001").description("Error code"))
                        .addProperty("message", new Schema<>().type("string").example("INVALID_INPUT").description("Error message"))
                        .addProperty("data", new Schema<>().type("object").nullable(true).description("Additional error data"))
                )
                .addSchemas("SuccessResponse", new Schema<>()
                        .type("object")
                        .description("Standard success response format")
                        .addProperty("code", new Schema<>().type("string").example("999999").description("Success code"))
                        .addProperty("message", new Schema<>().type("string").nullable(true).description("Success message"))
                        .addProperty("data", new Schema<>().type("object").description("Response data"))
                )
                .addSchemas("PaymentResponse", new Schema<>()
                        .type("object")
                        .description("VNPay payment URL response")
                        .addProperty("paymentUrl", new Schema<>().type("string").description("VNPay payment URL to redirect user"))
                        .addProperty("transactionId", new Schema<>().type("string").description("Internal transaction ID"))
                        .addProperty("orderInfo", new Schema<>().type("string").description("Order description"))
                        .addProperty("amount", new Schema<>().type("number").description("Payment amount in VND"))
                )
                .addSchemas("TransactionResponse", new Schema<>()
                        .type("object")
                        .description("Transaction details")
                        .addProperty("transactionId", new Schema<>().type("string").description("Transaction ID"))
                        .addProperty("userId", new Schema<>().type("string").description("User ID"))
                        .addProperty("transactionType", new Schema<>().type("string").description("Type: MEMBERSHIP_PURCHASE, POST_FEE, PUSH_FEE"))
                        .addProperty("amount", new Schema<>().type("number").description("Amount in VND"))
                        .addProperty("status", new Schema<>().type("string").description("Status: PENDING, COMPLETED, FAILED"))
                        .addProperty("paymentProvider", new Schema<>().type("string").description("Payment provider: VNPAY"))
                        .addProperty("providerTransactionId", new Schema<>().type("string").description("VNPay transaction ID"))
                        .addProperty("createdAt", new Schema<>().type("string").format("date-time").description("Creation timestamp"))
                )
                .addSchemas("QuotaStatusResponse", new Schema<>()
                        .type("object")
                        .description("Quota availability status")
                        .addProperty("totalAvailable", new Schema<>().type("integer").description("Available quota"))
                        .addProperty("totalUsed", new Schema<>().type("integer").description("Used quota"))
                        .addProperty("totalGranted", new Schema<>().type("integer").description("Total granted quota"))
                )
                .addSchemas("InsufficientQuotaError", new Schema<>()
                        .type("object")
                        .description("Insufficient quota error response")
                        .addProperty("code", new Schema<>().type("string").example("QUOTA_001").description("Error code"))
                        .addProperty("message", new Schema<>().type("string").example("INSUFFICIENT_QUOTA").description("Error message"))
                        .addProperty("data", new Schema<>()
                                .type("object")
                                .addProperty("userId", new Schema<>().type("string"))
                                .addProperty("benefitType", new Schema<>().type("string"))
                                .addProperty("required", new Schema<>().type("integer"))
                                .addProperty("available", new Schema<>().type("integer"))
                        )
                )
                .addSchemas("PaymentFailedError", new Schema<>()
                        .type("object")
                        .description("Payment failed error response")
                        .addProperty("code", new Schema<>().type("string").example("PAYMENT_001").description("Error code"))
                        .addProperty("message", new Schema<>().type("string").example("PAYMENT_FAILED").description("Error message"))
                        .addProperty("data", new Schema<>()
                                .type("object")
                                .addProperty("transactionId", new Schema<>().type("string"))
                                .addProperty("responseCode", new Schema<>().type("string"))
                                .addProperty("reason", new Schema<>().type("string"))
                        )
                );
    }

    @Bean
    public GroupedOpenApi publicApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("smartrent-api")
                .displayName("SmartRent Complete API")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("Authentication & Verification")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/auth/**", "/v1/verification/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("user-management")
                .displayName("User Management")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("admin-management")
                .displayName("Admin Management & Roles")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/admins/**", "/v1/auth/admin/**", "/v1/roles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi listingApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("listings")
                            .displayName("Property Listings")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/listings/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi addressApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("addresses")
                            .displayName("Address Management")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/addresses/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi uploadApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("file-upload")
                            .displayName("File Upload")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/upload/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi pricingApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("pricing")
                            .displayName("Pricing & Price History")
                            .packagesToScan(packageToScan)
                            .pathsToMatch(
                                            "/v1/listings/*/price",
                                            "/v1/listings/*/pricing-history",
                                            "/v1/listings/*/pricing-history/date-range",
                                            "/v1/listings/*/current-price",
                                            "/v1/listings/*/price-statistics",
                                            "/v1/listings/recent-price-changes"
                            )
                            .build();
    }

    @Bean
    public GroupedOpenApi membershipApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("membership")
                            .displayName("Membership Management")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/memberships/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi pushApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("push")
                            .displayName("Push & Promotion")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/pushes/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi savedListingApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("saved-listings")
                            .displayName("Saved Listings")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/saved-listings/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi paymentApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("payments")
                            .displayName("VNPay Payments & Transactions")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/payments/**", "/api/v1/payments/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi quotaApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("quotas")
                            .displayName("Quota Management")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/quotas/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi vipTierApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("vip-tiers")
                            .displayName("VIP Tier Details")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/vip-tiers/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi pushDetailApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("push-details")
                            .displayName("Push Pricing Details")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/push-details/**")
                            .build();
    }
}
