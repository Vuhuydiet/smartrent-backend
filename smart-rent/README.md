# SmartRent Backend API

A comprehensive Spring Boot application providing backend services for the SmartRent platform, featuring user management, admin operations, email verification, and robust authentication with JWT tokens.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Database Schema](#database-schema)
- [Authentication & Security](#authentication--security)
- [Email Service](#email-service)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)

## Architecture Overview

SmartRent Backend follows a layered architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  Controllers (REST APIs) + Swagger Documentation           │
├─────────────────────────────────────────────────────────────┤
│                     Service Layer                          │
│  Business Logic + Authentication + Email Services          │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                      │
│  Repositories + External Connectors + Configuration        │
├─────────────────────────────────────────────────────────────┤
│                     Data Layer                             │
│  MySQL Database + Redis Cache + Flyway Migrations         │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **Controllers**: RESTful API endpoints with comprehensive Swagger documentation
- **Services**: Business logic implementation with circuit breaker patterns
- **Repositories**: JPA-based data access layer
- **Security**: JWT-based authentication with role-based access control
- **Email Service**: Resilient email service with retry and circuit breaker patterns
- **Database**: MySQL with Flyway migrations for schema management

## Features

### User Management
- User registration with email verification
- Profile management and updates
- Secure password handling with encryption
- Phone and email uniqueness validation

### Admin Management
- Admin account creation with role assignment
- Role-based access control (RBAC)
- Admin profile management
- Multiple role support per admin

### Authentication & Security
- JWT-based authentication for users and admins
- Access and refresh token management
- Token introspection and validation
- Password change and reset functionality
- Secure logout with Redis cache-based token invalidation

### Email Services
- Email verification for new accounts
- Password reset emails
- Circuit breaker pattern for resilience
- Retry mechanism with exponential backoff
- Integration with Brevo email service

### Resilience & Monitoring
- Circuit breaker for external services
- Health checks and monitoring endpoints
- Comprehensive logging and error handling
- Actuator endpoints for operational insights

## Technology Stack

### Core Framework
- **Spring Boot 3.4.0** - Main application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence layer
- **Spring Cloud OpenFeign** - HTTP client for external services

### Database & Caching
- **MySQL 8.0** - Primary database
- **Redis** - Caching and session storage
- **Flyway** - Database migration management

### Documentation & Testing
- **SpringDoc OpenAPI 3** - API documentation (Swagger)
- **JUnit 5** - Unit and integration testing
- **Jacoco** - Code coverage reporting

### External Services
- **Brevo** - Email service provider
- **Resilience4j** - Circuit breaker and retry patterns

### Build & Development
- **Gradle** - Build automation
- **Lombok** - Boilerplate code reduction
- **Java 17** - Programming language

## Quick Start

### Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Redis (optional, for caching)
- Gradle 7.0 or higher

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/smartrent-backend.git
cd smartrent-backend
```

### 2. Database Setup

Create a MySQL database:

```sql
CREATE DATABASE smartrent;
CREATE USER 'smartrent_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON smartrent.* TO 'smartrent_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Environment Configuration

Create `src/main/resources/application-local.yml`:

```yaml
# Database Configuration
IDENTITY_SERVICE_DB_URL: "jdbc:mysql://localhost:3306/smartrent"
DB_USERNAME: smartrent_user
DB_PASSWORD: your_password

# JWT Configuration
ACCESS_SIGNER_KEY: "your-access-signer-key-here"
REFRESH_SIGNER_KEY: "your-refresh-signer-key-here"
RESET_PASSWORD_SIGNER_KEY: "your-reset-password-signer-key-here"
VALID_DURATION: 3600 # 1 hour
REFRESHABLE_DURATION: 86400 # 24 hours
RESET_PASSWORD_DURATION: 900 # 15 minutes

# Email Configuration
BREVO_URL: "https://api.brevo.com"
BREVO_API_KEY: "your-brevo-api-key"
SENDER_EMAIL: "noreply@smartrent.com"
SENDER_NAME: "SmartRent"
```

### 4. Build and Run

```bash
# Build the application
./gradlew build

# Run database migrations
./gradlew flywayMigrate

# Start the application
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 5. Verify Installation

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## Database Schema

The application uses MySQL with Flyway for database migrations. Key entities include:

### Core Tables

- **users** - User account information
- **admins** - Administrator accounts
- **roles** - System roles (Super Admin, User Admin, etc.)
- **admins_roles** - Many-to-many relationship between admins and roles

### Migration Files

- `V1__Create_users_admins_roles_tables.sql` - Initial schema
- `V2__Create_invalidated_tokens_table.sql` - Token management (deprecated)
- `V3__Rename_admin_roles_to_admins_roles.sql` - Relationship table
- `V4__Add_missing_user_fields_and_constraints.sql` - User enhancements
- `V5__Fix_admins_table_constraints.sql` - Admin constraints
- `V6__Create_verify_codes_table.sql` - Email verification (deprecated)
- `V10__Drop_invalidated_tokens_table.sql` - Remove token table (moved to cache)
- `V11__Drop_verify_codes_table.sql` - Remove verify_codes table (moved to Redis cache)

### Entity Relationships

```
Users (1) ←→ (1) VerifyCode
Admins (M) ←→ (M) Roles
```

## Authentication & Security

### JWT Token Flow

1. **Authentication**: POST `/v1/auth` or `/v1/auth/admin`
2. **Token Usage**: Include `Authorization: Bearer <token>` header
3. **Token Refresh**: POST `/v1/auth/refresh` with refresh token
4. **Token Validation**: POST `/v1/auth/introspect`
5. **Logout**: POST `/v1/auth/logout` to invalidate tokens

### Security Features

- **Password Encryption**: BCrypt hashing
- **Token Expiration**: Configurable access and refresh token lifetimes
- **Role-Based Access**: Different permissions for users and admins
- **CORS Configuration**: Configurable cross-origin resource sharing
- **Request Validation**: Comprehensive input validation

### Available Roles

- **SA** - Super Admin (full system access)
- **UA** - User Admin (user management)
- **CM** - Content Moderator
- **SPA** - Support Admin
- **FA** - Finance Admin
- **MA** - Marketing Admin

## Email Service

The application includes a resilient email service with advanced fault tolerance:

### Features

- **Circuit Breaker**: Automatic failure detection and recovery
- **Retry Logic**: Exponential backoff with jitter
- **Health Monitoring**: Service health checks and metrics
- **Fallback Handling**: Graceful degradation during outages

### Configuration

```yaml
application:
  circuit-breaker:
    failure-rate-threshold: 60
    wait-duration-in-open-state: 120
    sliding-window-size: 20
  email-retry:
    max-attempts: 3
    base-wait-duration: 2000
    exponential-multiplier: 2.0
```

### Email Types

- **Verification Emails**: Account activation codes
- **Password Reset**: Secure password reset links
- **Administrative**: System notifications

## API Documentation

### Swagger UI

Access comprehensive API documentation at: http://localhost:8080/swagger-ui.html

### API Groups

- **Authentication & Verification** - Login, logout, token management
- **User Management** - User CRUD operations
- **Admin Management & Roles** - Admin operations and role management
- **Health & Monitoring** - System health and metrics

### Key Endpoints

#### Authentication
- `POST /v1/auth` - User authentication
- `POST /v1/auth/admin` - Admin authentication
- `POST /v1/auth/refresh` - Token refresh
- `POST /v1/auth/logout` - User logout
- `PATCH /v1/auth/change-password` - Password change

#### User Management
- `POST /v1/users` - Create user account
- `GET /v1/users` - Get user profile
- `POST /v1/verification` - Verify email
- `POST /v1/verification/code` - Send verification code

#### Admin Management
- `POST /v1/admins` - Create admin account
- `GET /v1/admins` - Get admin profile
- `GET /v1/roles` - List available roles

### Response Format

All API responses follow a consistent format:

```json
{
  "code": "999999",
  "message": "Operation completed successfully",
  "data": {
    // Response data here
  }
}
```

### Error Codes

- **1xxx**: Internal server errors
- **2xxx**: Client input validation errors
- **3xxx**: Resource conflict errors
- **4xxx**: Resource not found errors
- **5xxx**: Authentication errors
- **6xxx**: Authorization errors

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# View coverage report
open build/reports/jacoco/test/html/index.html
```

### Test Categories

- **Unit Tests**: Service layer and utility testing
- **Integration Tests**: Database and API endpoint testing
- **Security Tests**: Authentication and authorization testing
- **Email Service Tests**: Circuit breaker and retry testing

### Test Configuration

Tests use H2 in-memory database and mock external services:

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## Deployment

### Production Deployment

#### 1. Environment Variables

Set the following environment variables:

```bash
# Database
export IDENTITY_SERVICE_DB_URL="jdbc:mysql://prod-db:3306/smartrent"
export DB_USERNAME="smartrent_prod"
export DB_PASSWORD="secure_password"

# JWT Keys (generate secure keys)
export ACCESS_SIGNER_KEY="your-production-access-key"
export REFRESH_SIGNER_KEY="your-production-refresh-key"
export RESET_PASSWORD_SIGNER_KEY="your-production-reset-key"

# Email Service
export BREVO_API_KEY="your-production-brevo-key"
export SENDER_EMAIL="noreply@yourdomain.com"
```

#### 2. Build Production JAR

```bash
./gradlew clean build -Pprod
```

#### 3. Run Application

```bash
java -jar build/libs/smart-rent-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

### Docker Deployment

#### Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY build/libs/smart-rent-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose

```yaml
version: '3.8'
services:
  smartrent-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - IDENTITY_SERVICE_DB_URL=jdbc:mysql://db:3306/smartrent
    depends_on:
      - db
      - redis

  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: smartrent
      MYSQL_ROOT_PASSWORD: rootpassword
    volumes:
      - mysql_data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  mysql_data:
```

### Health Checks

Monitor application health using actuator endpoints:

- **Health**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`
- **Circuit Breakers**: `/actuator/circuitbreakers`

## Configuration

### Application Properties

Key configuration properties:

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /

# Database Configuration
spring:
  datasource:
    url: ${IDENTITY_SERVICE_DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# Security Configuration
application:
  authentication:
    jwt:
      access-signer-key: ${ACCESS_SIGNER_KEY}
      refresh-signer-key: ${REFRESH_SIGNER_KEY}
      valid-duration: ${VALID_DURATION:3600}
      refreshable-duration: ${REFRESHABLE_DURATION:86400}

# Email Configuration
  email:
    sender:
      email: ${SENDER_EMAIL}
      name: ${SENDER_NAME:SmartRent}

# Circuit Breaker Configuration
  circuit-breaker:
    failure-rate-threshold: 60
    wait-duration-in-open-state: 120
    sliding-window-size: 20
```

### Profiles

- **local**: Development environment
- **test**: Testing environment
- **prod**: Production environment

## Contributing

### Development Setup

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/amazing-feature`
3. **Make your changes**
4. **Run tests**: `./gradlew test`
5. **Commit changes**: `git commit -m 'Add amazing feature'`
6. **Push to branch**: `git push origin feature/amazing-feature`
7. **Open a Pull Request**

### Code Standards

- **Java Code Style**: Follow Google Java Style Guide
- **Commit Messages**: Use conventional commit format
- **Testing**: Maintain >80% code coverage
- **Documentation**: Update API documentation for new endpoints

## Additional Resources

### Documentation

- [Database Migration Guide](src/main/resources/db/migration/README.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)


