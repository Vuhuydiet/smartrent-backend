#!/bin/bash

# Docker Helper Script for SmartRent Backend
# This script provides convenient commands for Docker operations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Function to show usage
show_usage() {
    echo "SmartRent Backend Docker Helper"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start-prod     Start production environment (app + db + cache)"
    echo "  start-dev      Start development environment (db + cache only)"
    echo "  stop           Stop all services"
    echo "  restart        Restart all services"
    echo "  rebuild        Rebuild and restart application"
    echo "  logs           Show logs for all services"
    echo "  logs-app       Show application logs"
    echo "  logs-db        Show database logs"
    echo "  status         Show status of all services"
    echo "  clean          Clean up containers and volumes"
    echo "  db-shell       Connect to database shell"
    echo "  redis-shell    Connect to Redis shell"
    echo "  app-shell      Connect to application container shell"
    echo "  health         Check application health"
    echo "  help           Show this help message"
}

# Main script logic
case "$1" in
    "start-prod")
        check_docker
        print_info "Starting production environment..."
        docker-compose up -d --build
        print_success "Production environment started!"
        print_info "Application: http://localhost:8080"
        print_info "Health check: http://localhost:8080/actuator/health"
        ;;
    
    "start-dev")
        check_docker
        print_info "Starting development environment..."
        docker-compose -f docker-compose.dev.yml up -d
        print_success "Development environment started!"
        print_info "MySQL: localhost:3307 (root/root)"
        print_info "Redis: localhost:6380"
        print_info "phpMyAdmin: http://localhost:8081"
        print_info "Redis Commander: http://localhost:8082"
        print_warning "Run your Spring Boot app locally with: cd smart-rent && ./gradlew bootRun --args='--spring.profiles.active=local'"
        ;;
    
    "stop")
        check_docker
        print_info "Stopping all services..."
        docker-compose down
        docker-compose -f docker-compose.dev.yml down 2>/dev/null || true
        print_success "All services stopped!"
        ;;
    
    "restart")
        check_docker
        print_info "Restarting services..."
        docker-compose restart
        print_success "Services restarted!"
        ;;
    
    "rebuild")
        check_docker
        print_info "Rebuilding and restarting application..."
        docker-compose up -d --build app
        print_success "Application rebuilt and restarted!"
        ;;
    
    "logs")
        check_docker
        docker-compose logs -f
        ;;
    
    "logs-app")
        check_docker
        docker-compose logs -f app
        ;;
    
    "logs-db")
        check_docker
        docker-compose logs -f mysql
        ;;
    
    "status")
        check_docker
        print_info "Service status:"
        docker-compose ps
        ;;
    
    "clean")
        check_docker
        print_warning "This will remove all containers, networks, and volumes. Are you sure? (y/N)"
        read -r response
        if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
            print_info "Cleaning up Docker resources..."
            docker-compose down -v --rmi local
            docker-compose -f docker-compose.dev.yml down -v 2>/dev/null || true
            docker system prune -f
            print_success "Cleanup completed!"
        else
            print_info "Cleanup cancelled."
        fi
        ;;
    
    "db-shell")
        check_docker
        print_info "Connecting to database shell..."
        docker-compose exec mysql mysql -u root -p smartrent
        ;;
    
    "redis-shell")
        check_docker
        print_info "Connecting to Redis shell..."
        docker-compose exec redis redis-cli
        ;;
    
    "app-shell")
        check_docker
        print_info "Connecting to application container shell..."
        docker-compose exec app bash
        ;;
    
    "health")
        check_docker
        print_info "Checking application health..."
        if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            print_success "Application is healthy!"
            curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/actuator/health
        else
            print_error "Application is not responding or unhealthy!"
            exit 1
        fi
        ;;
    
    "help"|"--help"|"-h"|"")
        show_usage
        ;;
    
    *)
        print_error "Unknown command: $1"
        echo ""
        show_usage
        exit 1
        ;;
esac
