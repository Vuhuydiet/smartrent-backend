#!/bin/bash

# Database Migration Helper Script
# This script provides convenient commands for managing database migrations

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATION_DIR="$PROJECT_DIR/src/main/resources/db/migration"

# Functions
print_usage() {
    echo -e "${BLUE}Database Migration Helper${NC}"
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  create <description>    Create a new migration file"
    echo "  migrate                 Run all pending migrations"
    echo "  info                    Show migration status"
    echo "  validate               Validate migration files"
    echo "  clean                  Clean database (WARNING: Drops all objects)"
    echo "  baseline               Baseline existing database"
    echo "  help                   Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 create \"Add user roles table\""
    echo "  $0 migrate"
    echo "  $0 info"
}

get_next_version() {
    local max_version=0
    for file in "$MIGRATION_DIR"/V*.sql; do
        if [[ -f "$file" ]]; then
            local filename=$(basename "$file")
            local version=$(echo "$filename" | sed 's/V\([0-9]*\)__.*/\1/')
            if [[ $version -gt $max_version ]]; then
                max_version=$version
            fi
        fi
    done
    echo $((max_version + 1))
}

create_migration() {
    local description="$1"
    if [[ -z "$description" ]]; then
        echo -e "${RED}Error: Migration description is required${NC}"
        echo "Usage: $0 create \"<description>\""
        exit 1
    fi
    
    local version=$(get_next_version)
    local filename="V${version}__${description// /_}.sql"
    local filepath="$MIGRATION_DIR/$filename"
    
    # Create migration file with template
    cat > "$filepath" << EOF
-- $description
-- This migration: TODO - describe what this migration does

-- TODO: Add your SQL statements here

-- Example table creation:
-- CREATE TABLE example_table (
--     id VARCHAR(255) NOT NULL PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
-- );

-- Example index creation:
-- CREATE INDEX idx_example_table_name ON example_table (name);

-- Example constraint:
-- ALTER TABLE example_table ADD CONSTRAINT uk_example_table_name UNIQUE (name);
EOF
    
    echo -e "${GREEN}Created migration file: $filename${NC}"
    echo -e "${YELLOW}Please edit the file and add your SQL statements${NC}"
    echo -e "${BLUE}File location: $filepath${NC}"
}

run_gradle_flyway() {
    local command="$1"
    echo -e "${BLUE}Running: ./gradlew flyway${command}${NC}"
    cd "$PROJECT_DIR"
    ./gradlew "flyway${command}"
}

# Main script logic
case "${1:-help}" in
    create)
        create_migration "$2"
        ;;
    migrate)
        run_gradle_flyway "Migrate"
        ;;
    info)
        run_gradle_flyway "Info"
        ;;
    validate)
        run_gradle_flyway "Validate"
        ;;
    clean)
        echo -e "${RED}WARNING: This will drop all database objects!${NC}"
        read -p "Are you sure? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            run_gradle_flyway "Clean"
        else
            echo "Operation cancelled."
        fi
        ;;
    baseline)
        run_gradle_flyway "Baseline"
        ;;
    help|--help|-h)
        print_usage
        ;;
    *)
        echo -e "${RED}Unknown command: $1${NC}"
        print_usage
        exit 1
        ;;
esac
