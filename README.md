# SmartRent Backend

A comprehensive rental property management system built with Spring Boot, providing APIs for property management, user authentication, billing, analytics, and communication features.

## Architecture Overview

SmartRent Backend is a modular Spring Boot application designed with Domain-Driven Design (DDD) principles, featuring:

- **User Authentication** - User registration, login, and profile management
- **Property Discovery** - Property search, listing, and management
- **Billing System** - Payment processing and invoice management
- **Analytics** - Usage tracking and reporting
- **Communication** - Messaging and notification system
- **Content Management** - Document and media handling

## Technology Stack

### Core Framework
- **Java 17** - Modern Java with latest features
- **Spring Boot 3.5.4** - Enterprise-grade application framework
- **Spring Data JPA** - Database abstraction and ORM
- **Spring Web** - RESTful API development
- **Spring Actuator** - Production monitoring and management

### Database & Caching
- **MySQL 8.0** - Primary relational database
- **Redis 8.2** - Caching and session management
- **Flyway** - Database migration and versioning
- **H2** - In-memory database for testing

### Development Tools
- **Lombok** - Reduces boilerplate code
- **Gradle** - Build automation and dependency management
- **Jakarta Validation** - Input validation and constraints

## Project Structure

```
smart-rent/
├── src/main/java/com/smartrent/
│   ├── SmartRentApplication.java         # Main application entry point
│   ├── config/                          # Configuration classes
│   │   └── security/                    # Security configuration
│   ├── controller/                      # REST API controllers
│   │   └── dto/                        # Data Transfer Objects
│   ├── infra/                          # Infrastructure layer
│   ├── mapper/                         # Object mappers
│   ├── service/                        # Business logic services
│   └── utility/                        # Utility classes
├── src/main/resources/
│   ├── application.yml                  # Main configuration
│   ├── application-local.yaml          # Local development config
│   ├── application-docker.yml          # Docker environment config
│   └── db/migration/                   # Flyway database migrations
└── src/test/                           # Unit and integration tests
```

## Quick Start

### Prerequisites

- **Java 17** or higher
- **MySQL 8.0**
- **Redis 8.2**

### Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd smartrent-backend
   ```

2. **Start required services**
   ```bash
   # Start MySQL and Redis using docker-compose
   docker-compose up -d mysql redis
   ```

3. **Run the application**
   ```bash
   cd smart-rent
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

4. **Verify the application**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Configuration

### Environment Profiles

- **`local`** - Local development with external database
- **`docker`** - Containerized environment
- **`test`** - Testing with H2 in-memory database

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `IDENTITY_SERVICE_DB_URL` | Database connection URL | `jdbc:mysql://localhost:3306/smartrent` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root` |
| `SPRING_REDIS_HOST` | Redis host | `localhost` |
| `SPRING_REDIS_PORT` | Redis port | `6379` |

## API Documentation

### Base URL
- **Local**: `http://localhost:8080`
- **Docker**: `http://localhost:8080`

### Core Endpoints

#### User Management
```http
POST /api/v1/users              # Create new user
GET  /api/v1/users              # Get user details
```

#### Health & Monitoring
```http
GET  /actuator/health           # Application health status
GET  /actuator/info             # Application information
GET  /actuator/metrics          # Application metrics
```

### Request/Response Examples

#### Create User
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "secure_password"
  }'
```

#### Get User Details
```bash
curl -X GET http://localhost:8080/api/v1/users \
  -H "user-id: user-uuid-here"
```

## Database Schema

### Current Tables

#### users
- **Purpose**: User authentication and profile management
- **Key Fields**: id, username, password, email, first_name, last_name, phone_number
- **Constraints**: Unique username and email

#### properties
- **Purpose**: Rental property management
- **Key Fields**: id, name, address, property_type, total_units, owner_id
- **Relationships**: Links to users table via owner_id foreign key

### Database Migrations

The application uses Flyway for database versioning:

- **V1**: Create users table with basic authentication fields
- **V2**: Add user profile fields (email, names, phone, etc.)
- **V3**: Insert default admin user for initial access
- **V4**: Create properties table for rental property management

## Docker Support

### Available Services

#### Production Stack (`docker-compose.yml`)
- **Application**: Spring Boot app on port 8080
- **MySQL**: Database on port 3306
- **Redis**: Cache on port 6379

#### Development Stack (`docker-compose.dev.yml`)
- **MySQL**: Database on port 3307
- **Redis**: Cache on port 6380
- **phpMyAdmin**: Database management UI on port 8081
- **Redis Commander**: Redis management UI on port 8082

### Docker Helper Commands

```bash
# Production environment
./docker-helper.sh start-prod    # Start all services
./docker-helper.sh stop          # Stop all services
./docker-helper.sh logs-app      # View application logs
./docker-helper.sh health        # Check application health

# Development environment
./docker-helper.sh start-dev     # Start DB and cache only
./docker-helper.sh db-shell      # Connect to database
./docker-helper.sh redis-shell   # Connect to Redis

# Maintenance
./docker-helper.sh clean         # Clean up containers and volumes
./docker-helper.sh rebuild       # Rebuild and restart application
```

## Testing

### Running Tests

```bash
cd smart-rent

# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run specific test class
./gradlew test --tests "UserServiceTest"
```

### Test Configuration

- **Test Database**: H2 in-memory database
- **Test Framework**: JUnit 5
- **Mocking**: Mockito (included with Spring Boot Test)

## Development

### Building the Application

```bash
cd smart-rent

# Build without tests
./gradlew build -x test

# Build with tests
./gradlew build

# Clean build
./gradlew clean build
```

### Code Style

- **Lombok**: Used for reducing boilerplate code
- **Package Structure**: Domain-driven design with clear module separation
- **Naming Conventions**: CamelCase for Java, snake_case for database
