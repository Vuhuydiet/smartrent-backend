# Docker Setup for SmartRent Backend

This document provides instructions for setting up and running the SmartRent backend application using Docker and Docker Compose with environment variable support.

## Prerequisites

- Docker (version 20.10 or later)
- Docker Compose (version 2.0 or later)

## Quick Start

### 1. Environment Setup

Before running the application, set up your environment variables:

```bash
# Copy the example environment file
cp .env.example .env

# Edit the .env file with your preferred settings
nano .env
```

### 2. Production Setup

To run the complete application stack (app + database + cache):

```bash
# Build and start all services
docker-compose up --build

# Run in detached mode
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

The application will be available at:
- **Application**: http://localhost:8080 (or the port specified in `APP_PORT`)
- **Health Check**: http://localhost:8080/actuator/health

### 3. Development Setup

For development, you might want to run only the database and cache services while running the Spring Boot application locally:

```bash
# Start only database and cache services for development
docker-compose -f docker-compose.dev.yml up -d

# Stop development services
docker-compose -f docker-compose.dev.yml down
```

Development services will be available at:
- **MySQL**: localhost:3307 (or `MYSQL_DEV_PORT`) (root/root)
- **Redis**: localhost:6380 (or `REDIS_DEV_PORT`)
- **phpMyAdmin**: http://localhost:8081 (or `PHPMYADMIN_PORT`) (database management UI)
- **Redis Commander**: http://localhost:8082 (or `REDIS_COMMANDER_PORT`) (Redis management UI)

Then run your Spring Boot application locally with the `local` profile:

```bash
cd smart-rent
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Configuration

### Environment Variables with .env File

The application supports environment-based configuration using a `.env` file. Docker Compose automatically loads variables from this file, making it easy to customize settings without modifying the compose files directly.

#### Setting up Environment Variables

1. **Copy the example file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit the `.env` file** with your preferred settings:
   ```bash
   nano .env
   # or
   vim .env
   ```

3. **Start the services** (Docker Compose will automatically load the `.env` file):
   ```bash
   docker-compose up -d
   ```

#### How .env File Works

Docker Compose automatically reads the `.env` file from the same directory as your `docker-compose.yml` file. Variables are referenced in the compose file using the syntax `${VARIABLE_NAME:-default_value}`.

**Example:**
```yaml
environment:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
ports:
  - "${APP_PORT:-8080}:8080"
```

This means:
- If `MYSQL_ROOT_PASSWORD` is set in `.env`, use that value
- If not set, use the default value `root`
- If `APP_PORT` is set in `.env`, use that port
- If not set, use the default port `8080`

#### Key Environment Variables

**Application Configuration:**
- `SPRING_PROFILES_ACTIVE` - Spring Boot profile (docker, local, prod)
- `APP_PORT` - Application port (default: 8080)
- `JAVA_OPTS` - JVM options (default: -Xms512m -Xmx1024m)

**Database Configuration:**
- `MYSQL_ROOT_PASSWORD` - MySQL root password
- `MYSQL_DATABASE` - Database name
- `MYSQL_USER` - Additional MySQL user
- `MYSQL_PASSWORD` - Additional MySQL user password
- `DB_USERNAME` - Database username for Spring Boot
- `DB_PASSWORD` - Database password for Spring Boot
- `IDENTITY_SERVICE_DB_URL` - Full database connection URL
- `MYSQL_PORT` - MySQL external port (default: 3306)

**Redis Configuration:**
- `SPRING_REDIS_HOST` - Redis hostname (default: redis)
- `SPRING_REDIS_PORT` - Redis port (default: 6379)
- `REDIS_PORT` - Redis external port (default: 6379)

**Security Settings:**
- `JWT_SECRET` - JWT signing secret (change in production!)
- `ENCRYPTION_KEY` - Application encryption key (change in production!)

**Development Tools (for docker-compose.dev.yml):**
- `PHPMYADMIN_PORT` - phpMyAdmin port (default: 8081)
- `REDIS_COMMANDER_PORT` - Redis Commander port (default: 8082)
- `MYSQL_DEV_PORT` - Development MySQL port (default: 3307)
- `REDIS_DEV_PORT` - Development Redis port (default: 6380)

#### Environment-Specific Configurations

**Development (.env):**
```bash
SPRING_PROFILES_ACTIVE=docker
APP_PORT=8080
MYSQL_ROOT_PASSWORD=root
LOG_LEVEL=DEBUG
```

**Production (.env.prod):**
```bash
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080
MYSQL_ROOT_PASSWORD=super-secure-password
JWT_SECRET=your-super-secure-jwt-secret-key
ENCRYPTION_KEY=your-super-secure-encryption-key
LOG_LEVEL=WARN
JAVA_OPTS=-Xms1024m -Xmx2048m
```

To use a different environment file:
```bash
# Use production environment
docker-compose --env-file .env.prod up -d
```

### Database Configuration

- **Database**: MySQL 8.0
- **Default Database**: `smartrent` (configurable via `MYSQL_DATABASE`)
- **Root Password**: Configurable via `MYSQL_ROOT_PASSWORD`
- **Additional User**: Configurable via `MYSQL_USER` / `MYSQL_PASSWORD`

### Redis Configuration

- **Version**: Redis 7 (Alpine)
- **Port**: Configurable via `REDIS_PORT` (default: 6379)

## Useful Commands

### Application Management

```bash
# Rebuild and restart the application
docker-compose up --build app

# View application logs
docker-compose logs -f app

# Execute commands in the running container
docker-compose exec app bash

# Restart a specific service
docker-compose restart app

# Override environment variables for a single run
APP_PORT=9090 docker-compose up app
```

### Database Management

```bash
# Connect to MySQL database
docker-compose exec mysql mysql -u root -p smartrent

# Run database migrations manually
docker-compose exec app java -jar app.jar --spring.profiles.active=docker --flyway.migrate

# Backup database
docker-compose exec mysql mysqldump -u root -p smartrent > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u root -p smartrent < backup.sql
```

### Redis Management

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# Monitor Redis commands
docker-compose exec redis redis-cli monitor

# Flush all Redis data
docker-compose exec redis redis-cli flushall
```

### Environment Variable Management

```bash
# View current environment variables
docker-compose config

# Validate docker-compose file with current environment
docker-compose config --quiet

# Show resolved configuration (with environment variables substituted)
docker-compose config --resolve-image-digests

# Use a different environment file
docker-compose --env-file .env.staging up -d

# Override specific variables
MYSQL_ROOT_PASSWORD=newpassword docker-compose up -d mysql
```

### Cleanup

```bash
# Stop and remove containers, networks
docker-compose down

# Remove containers, networks, and volumes
docker-compose down -v

# Remove containers, networks, volumes, and images
docker-compose down -v --rmi all

# Clean up unused Docker resources
docker system prune -a
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Modify port mappings in `.env` file:
   ```bash
   APP_PORT=9090
   MYSQL_PORT=3307
   REDIS_PORT=6380
   ```

2. **Environment variables not loading**: Ensure `.env` file is in the same directory as `docker-compose.yml`

3. **Database connection issues**: Check database credentials in `.env` file match those used by the application

4. **Build failures**: Clear Docker cache and rebuild:
   ```bash
   docker-compose down
   docker system prune -f
   docker-compose up --build
   ```

5. **Permission issues**: On Linux/macOS, ensure Docker has proper permissions:
   ```bash
   sudo chown -R $USER:$USER .
   ```

### Environment Variable Debugging

```bash
# Check if .env file is being read
docker-compose config | grep -A 5 -B 5 "MYSQL_ROOT_PASSWORD"

# Verify environment variables inside container
docker-compose exec app env | grep -E "(MYSQL|REDIS|SPRING)"

# Test with specific environment file
docker-compose --env-file .env.test config
```

### Logs and Debugging

```bash
# View all service logs
docker-compose logs

# View specific service logs
docker-compose logs app
docker-compose logs mysql
docker-compose logs redis

# Follow logs in real-time
docker-compose logs -f app

# View last 100 lines
docker-compose logs --tail=100 app

# Filter logs by log level (if LOG_LEVEL is set)
docker-compose logs app | grep ERROR
```

## Production Considerations

For production deployment, consider:

1. **Security**: 
   - Change all default passwords in `.env.prod`
   - Use strong, unique values for `JWT_SECRET` and `ENCRYPTION_KEY`
   - Use secrets management instead of plain text files

2. **Environment Files**:
   - Never commit `.env` files with production secrets to version control
   - Use `.env.example` as a template
   - Consider using Docker secrets or external secret management

3. **Performance**:
   - Adjust `JAVA_OPTS` for production memory requirements
   - Configure database connection pool settings
   - Set appropriate health check intervals

4. **Monitoring**: Add monitoring and alerting services
5. **Load Balancing**: Use a reverse proxy like Nginx
6. **SSL/TLS**: Configure HTTPS termination
7. **Resource Limits**: Set appropriate CPU and memory limits
8. **Backup**: Ensure volumes are properly backed up

## File Structure

```
.
├── Dockerfile                          # Multi-stage build for Spring Boot app
├── docker-compose.yml                  # Production setup with env vars
├── docker-compose.dev.yml              # Development setup with env vars
├── .env                                # Environment variables (development)
├── .env.example                        # Environment variables template
├── .dockerignore                       # Docker build context exclusions
├── DOCKER_SETUP.md                     # This documentation
└── smart-rent/
    └── src/main/resources/
        ├── application.yml              # Default configuration
        ├── application-local.yml        # Local development
        └── application-docker.yml       # Docker-specific configuration
```

## Security Best Practices

1. **Never commit `.env` files** with real credentials to version control
2. **Use `.env.example`** as a template for required variables
3. **Generate strong secrets** for production:
   ```bash
   # Generate a strong JWT secret
   openssl rand -base64 64
   
   # Generate an encryption key
   openssl rand -hex 32
   ```
4. **Use different credentials** for each environment
5. **Regularly rotate secrets** in production
6. **Use Docker secrets** for production deployments when possible
