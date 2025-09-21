package com.smartrent.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
                "## 🔐 Authentication\n" +
                "This API uses JWT (JSON Web Token) for authentication. To access protected endpoints:\n" +
                "1. **User Authentication**: Use `/v1/auth` endpoint to authenticate users\n" +
                "2. **Admin Authentication**: Use `/v1/auth/admin` endpoint to authenticate administrators\n" +
                "3. **Include Token**: Add the access token in the Authorization header: `Bearer <access_token>`\n" +
                "4. **Token Refresh**: Use the refresh token to obtain new access tokens when they expire\n" +
                "5. **Token Validation**: Use `/v1/auth/introspect` to validate token status\n\n" +

                "## 📧 Email Verification\n" +
                "User accounts require email verification before full activation:\n" +
                "1. Create user account via `/v1/users`\n" +
                "2. Send verification code via `/v1/verification/code`\n" +
                "3. Verify email using `/v1/verification` with the received code\n\n" +

                "## 🔄 Circuit Breaker & Resilience\n" +
                "The API implements circuit breaker patterns for email services to ensure reliability:\n" +
                "- Automatic retry on transient failures\n" +
                "- Circuit breaker protection for email service\n" +
                "- Graceful degradation during service outages\n\n" +

                "## 📊 Rate Limiting\n" +
                "API requests are rate-limited to prevent abuse. If you exceed the rate limit, you'll receive a 429 status code.\n\n" +

                "## ⚠️ Error Handling\n" +
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

                "## 🏗️ API Versioning\n" +
                "All endpoints are versioned with `/v1/` prefix. Future versions will use `/v2/`, etc.\n\n" +

                "## 📱 Response Format\n" +
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
                );
    }

    @Bean
    public GroupedOpenApi publicApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("smartrent-api")
                .displayName("🏠 SmartRent Complete API")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi authApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("🔐 Authentication & Verification")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/auth/**", "/v1/verification/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("user-management")
                .displayName("👤 User Management")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
        return GroupedOpenApi.builder()
                .group("admin-management")
                .displayName("👨‍💼 Admin Management & Roles")
                .packagesToScan(packageToScan)
                .pathsToMatch("/v1/admins/**", "/v1/auth/admin/**", "/v1/roles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi listingApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("listings")
                            .displayName("Listing APIs")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/listings/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi addressApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("addresses")
                            .displayName("Address APIs")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/v1/addresses/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi uploadApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("file-upload")
                            .displayName("File Upload APIs")
                            .packagesToScan(packageToScan)
                            .pathsToMatch("/upload/**")
                            .build();
    }

    @Bean
    public GroupedOpenApi pricingApi(@Value("${open.api.group.package-to-scan}") String packageToScan) {
            return GroupedOpenApi.builder()
                            .group("pricing")
                            .displayName("Pricing & Price History APIs")
                            .packagesToScan(packageToScan)
                            .pathsToMatch(
                                            "/v1/listings/*/price",
                                            "/v1/listings/*/pricing-history",
                                            "/v1/listings/*/pricing-history/date-range",
                                            "/v1/listings/*/current-price",
                                            "/v1/listings/recent-price-changes"
                            )
                            .build();
    }
}
