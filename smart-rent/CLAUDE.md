# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Building and Running
- Build: `./gradlew build`
- Run locally: `./gradlew bootRun --args='--spring.profiles.active=local'`
- Clean build: `./gradlew clean build`

### Database Operations
- Run migrations: `./gradlew flywayMigrate`
- Migrate with custom DB: `./gradlew flywayMigrate -PdbUrl=jdbc:mysql://host:3306/db -PdbUser=user -PdbPassword=pass`

### Testing
- Run tests: `./gradlew test`
- Run tests with coverage: `./gradlew test jacocoTestReport`
- View coverage report: `build/reports/jacoco/test/html/index.html`

### Development Profile
- Create `src/main/resources/application-local.yml` for local configuration
- Load environment variables from `.env` file (auto-loaded by application)

## Architecture Overview

### Layered Architecture
- **Controllers** (`controller/`): REST endpoints with Swagger documentation
- **Services** (`service/`): Business logic organized by domain (authentication, user, admin, email, listing, etc.)
- **Repositories** (`infra/repository/`): Data access layer with JPA entities
- **DTOs** (`dto/`): Request/response objects separated by type
- **Configuration** (`config/`): Security, caching, circuit breaker, and external service configs

### Key Architectural Patterns
- **Domain-Driven Design**: Services organized by business domains
- **Circuit Breaker**: Resilience4j for external service calls (email service)
- **Retry Pattern**: Exponential backoff for transient failures
- **Caching**: Redis for OTP, tokens, and user sessions
- **JWT Authentication**: Separate access/refresh tokens with configurable expiration

### External Integrations
- **Brevo Email Service**: Circuit breaker protected email sending
- **Google OAuth**: External authentication via OpenFeign
- **Cloudflare R2/S3**: File storage service
- **Redis**: Caching and session management

## Database Schema

### Core Entities
- **User/Admin**: Separate inheritance hierarchy from `AbstractUser`
- **Roles**: RBAC system with many-to-many admin-role relationships
- **Listings**: Property listings with address, pricing history, amenities
- **Address Hierarchy**: Province → District → Ward → Street structure
- **Verification**: Email/phone verification via Redis-cached OTP

### Migration Strategy
- Flyway migrations in `src/main/resources/db/migration/`
- Recent migrations moved token management and OTP to Redis cache
- Follow `V{number}__{Description}.sql` naming convention

## Security Implementation

### JWT Token Management
- Three token types: access, refresh, reset-password
- Configurable expiration via environment variables
- Redis-based token invalidation for logout
- Custom JWT decoder with proper error handling

### Authentication Flow
1. Login → JWT access + refresh tokens
2. API calls → Bearer token validation
3. Token refresh → New access token from valid refresh token
4. Logout → Token invalidation in Redis cache

### Authorization
- Role-based access control with configurable endpoints
- Method-level security in controllers
- Ignored endpoints for public access (health, docs, auth)

## Service Patterns

### Email Service Architecture
```
ResilientEmailService (Circuit Breaker + Retry)
    ↓
BrevoEmailServiceImpl (Feign Client)
    ↓ 
VerificationEmailService (Business Logic)
```

### Circuit Breaker Configuration
- Failure rate threshold: 60%
- Open state duration: 120s
- Sliding window: 20 calls
- Custom failure predicate for email service

### Cache Strategy
- User details: 1 minute TTL
- OTP codes: 5 minutes TTL  
- Invalidated tokens: 24 hours TTL
- Access tokens cached for validation performance

## Error Handling

### Error Code System (see docs/ERROR_CODES.md)
- **1xxx**: Internal server errors
- **2xxx**: Client validation errors  
- **3xxx**: Resource conflicts
- **4xxx**: Not found errors
- **5xxx**: Authentication errors
- **6xxx**: Authorization errors

### Exception Handling
- Global exception handler in `infra/exception/`
- Custom domain exceptions with specific error codes
- Consistent API response format with `ApiResponse<T>`

## Development Practices

### Code Organization
- MapStruct for entity-DTO mapping (generated implementations)
- Lombok for boilerplate reduction
- Service interfaces with implementation classes
- Request/response DTOs separate from entities

### Configuration Management
- Environment-specific profiles (local, test, prod)
- External configuration via environment variables
- Sensitive data never committed (use `.env` or environment variables)

### Testing Strategy
- Integration tests use H2 in-memory database
- Circuit breaker and retry patterns tested
- Test configuration in `application-test.yml`
- Email service mocking for reliable tests

## Common Development Tasks

### Adding New Service
1. Create service interface in appropriate domain package
2. Implement service with `@Service` annotation
3. Add corresponding controller if needed
4. Create request/response DTOs
5. Add MapStruct mapper if entity conversion needed
6. Write tests for service logic

### Database Changes
1. Create new Flyway migration in `db/migration/`
2. Update JPA entities if needed
3. Run `./gradlew flywayMigrate` to apply changes
4. Update repositories and services as needed

### Adding External API Integration
1. Create Feign client interface with `@FeignClient`
2. Add configuration in `application.yml`
3. Implement circuit breaker if reliability needed
4. Add connector class in `infra/connector/`
5. Create models for request/response in connector package