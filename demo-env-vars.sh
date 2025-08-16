#!/bin/bash

# Demo script to show how environment variables work with Docker Compose
# This script demonstrates different ways to use environment variables

set -e

echo "🚀 SmartRent Backend - Environment Variables Demo"
echo "=================================================="
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() {
    echo -e "${BLUE}📋 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Step 1: Show current .env file
print_step "Step 1: Current .env file contents"
if [ -f .env ]; then
    echo "Current .env file:"
    echo "---"
    cat .env | head -20
    echo "..."
    echo "---"
else
    print_warning ".env file not found. Creating from .env.example..."
    cp .env.example .env
fi
echo

# Step 2: Show how Docker Compose resolves variables
print_step "Step 2: How Docker Compose resolves environment variables"
echo "Running: docker-compose config | grep -A 5 -B 5 'APP_PORT\\|MYSQL_ROOT_PASSWORD'"
docker-compose config | grep -A 5 -B 5 'APP_PORT\|MYSQL_ROOT_PASSWORD' || true
echo

# Step 3: Test with custom port
print_step "Step 3: Testing with custom APP_PORT"
echo "Setting APP_PORT=9090 in .env file..."
echo "APP_PORT=9090" >> .env

echo "Checking resolved configuration:"
docker-compose config | grep -A 2 -B 2 "published.*9090" || echo "Port configuration not found"
echo

# Step 4: Test with environment variable override
print_step "Step 4: Testing environment variable override"
echo "Overriding APP_PORT to 7070 using environment variable..."
echo "Running: APP_PORT=7070 docker-compose config | grep published"
APP_PORT=7070 docker-compose config | grep -A 2 -B 2 "published.*7070" || echo "Override not found"
echo

# Step 5: Show environment variables inside container
print_step "Step 5: Environment variables inside the application container"
if docker-compose ps | grep -q "smartrent-app.*Up"; then
    echo "Application container is running. Showing environment variables:"
    echo "Running: docker-compose exec app env | grep -E '(MYSQL|REDIS|SPRING|JWT)'"
    docker-compose exec app env | grep -E "(MYSQL|REDIS|SPRING|JWT)" | sort
else
    print_warning "Application container is not running. Starting services..."
    docker-compose up -d
    echo "Waiting for services to be ready..."
    sleep 10
    docker-compose exec app env | grep -E "(MYSQL|REDIS|SPRING|JWT)" | sort
fi
echo

# Step 6: Test different environment files
print_step "Step 6: Testing with different environment files"
echo "Creating .env.test with different values..."
cat > .env.test << EOF
# Test environment
SPRING_PROFILES_ACTIVE=test
APP_PORT=8888
MYSQL_ROOT_PASSWORD=test-password
LOG_LEVEL=DEBUG
EOF

echo "Testing with .env.test file:"
echo "Running: docker-compose --env-file .env.test config | grep -E '(APP_PORT|MYSQL_ROOT_PASSWORD)'"
docker-compose --env-file .env.test config | grep -A 2 -B 2 "published.*8888\|MYSQL_ROOT_PASSWORD.*test-password" || echo "Test config not found"
echo

# Step 7: Cleanup and reset
print_step "Step 7: Cleanup and reset"
echo "Removing test configurations..."
rm -f .env.test
sed -i '' '/APP_PORT=9090/d' .env 2>/dev/null || true
sed -i '' '/APP_PORT=7070/d' .env 2>/dev/null || true

print_success "Demo completed!"
echo
echo "📚 Key Takeaways:"
echo "   • Docker Compose automatically reads .env files"
echo "   • Variables use syntax: \${VARIABLE_NAME:-default_value}"
echo "   • Environment variables override .env file values"
echo "   • Use --env-file to specify different environment files"
echo "   • Variables are passed to containers as environment variables"
echo
echo "🔧 Useful Commands:"
echo "   • docker-compose config                    # Show resolved configuration"
echo "   • docker-compose --env-file .env.prod up  # Use specific env file"
echo "   • APP_PORT=9090 docker-compose up         # Override single variable"
echo "   • docker-compose exec app env             # Show container environment"
echo
