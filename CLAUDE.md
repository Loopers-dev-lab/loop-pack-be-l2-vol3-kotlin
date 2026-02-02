# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Loopers Kotlin Spring Template - Multi-module Spring Boot application written in Kotlin with JPA, Redis, and Kafka support.

## Essential Commands

### Initial Setup
```bash
# Install pre-commit hooks (runs ktlint before commits)
make init

# Start infrastructure services (Redis, Kafka, MySQL)
docker-compose -f ./docker/infra-compose.yml up -d

# Start monitoring stack (Prometheus & Grafana)
docker-compose -f ./docker/monitoring-compose.yml up -d
```

### Build & Test
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :apps:commerce-api:build
./gradlew :modules:jpa:build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :apps:commerce-api:test

# Run tests with coverage
./gradlew test jacocoTestReport

# Lint check (ktlint)
./gradlew ktlintCheck

# Lint auto-fix
./gradlew ktlintFormat
```

### Running Applications
```bash
# Run specific Spring Boot application
./gradlew :apps:commerce-api:bootRun
./gradlew :apps:commerce-batch:bootRun
./gradlew :apps:commerce-streamer:bootRun
```

## Multi-Module Architecture

This project follows a strict three-tier module architecture:

### 1. `apps/` - Executable Applications
- **Purpose**: Runnable Spring Boot applications (each has @SpringBootApplication)
- **Build**: BootJar enabled, regular Jar disabled
- **Modules**:
  - `commerce-api`: REST API server with web endpoints
  - `commerce-batch`: Batch processing application
  - `commerce-streamer`: Event streaming/processing application

### 2. `modules/` - Reusable Configurations
- **Purpose**: Domain-agnostic, reusable infrastructure configurations
- **Principle**: NOT tied to specific business logic or domain implementations
- **Build**: Regular Jar enabled, BootJar disabled
- **Modules**:
  - `jpa`: JPA/Hibernate configurations, BaseEntity, QueryDSL setup
  - `redis`: Redis configurations and utilities
  - `kafka`: Kafka producer/consumer configurations

### 3. `supports/` - Add-on Utilities
- **Purpose**: Cross-cutting concerns and supplementary features
- **Build**: Regular Jar enabled, BootJar disabled
- **Modules**:
  - `jackson`: JSON serialization customizations
  - `logging`: Logging configurations
  - `monitoring`: Actuator and Prometheus metrics

## Layered Architecture (within apps)

Each application in `apps/` follows this layered structure:

```
interfaces/    - API layer (Controllers, DTOs, API specs)
  └─ api/
application/   - Application service layer (Facades, orchestration)
domain/        - Domain layer (Models, Services, Repository interfaces)
infrastructure/- Infrastructure layer (Repository implementations, external integrations)
support/       - Application-specific utilities (error handling, etc.)
```

**Key Principles**:
- **Controller → Facade → Service → Repository**: Standard flow
- **Domain models** contain business logic and validation (see ExampleModel.kt)
- **Repository interfaces** in domain/, implementations in infrastructure/
- **Facades** orchestrate multiple services and handle DTO conversions
- **Controllers** implement API specs and return standardized ApiResponse

## Technology Stack

- **Language**: Kotlin 2.0.20, Java 21
- **Framework**: Spring Boot 3.4.4, Spring Cloud 2024.0.1
- **Database**: MySQL (with Testcontainers for tests), JPA/Hibernate, QueryDSL
- **Cache**: Redis
- **Messaging**: Kafka
- **Testing**: JUnit 5, SpringMockK, Mockito-Kotlin, Instancio
- **Linting**: ktlint 1.0.1
- **Monitoring**: Prometheus, Grafana (http://localhost:3000, admin/admin)
- **API Docs**: SpringDoc OpenAPI

## Development Workflow

### Adding New Features
1. Identify the appropriate `apps/` module for your feature
2. Follow the layered architecture:
   - Create domain model in `domain/` with business logic
   - Define repository interface in `domain/`
   - Implement repository in `infrastructure/`
   - Create service in `domain/`
   - Create facade in `application/` for orchestration
   - Create controller and DTOs in `interfaces/api/`
3. If reusable infrastructure is needed, add to appropriate `modules/` or `supports/`

### Module Dependencies
- `apps/` can depend on `modules/` and `supports/`
- `modules/` and `supports/` should NOT depend on `apps/`
- `modules/` should be domain-agnostic and reusable
- Test fixtures available: `modules:jpa` and `modules:redis` provide testFixtures for testing

### Test Configuration
- Tests run with `spring.profiles.active=test`
- Timezone: `Asia/Seoul`
- Max parallel forks: 1
- Testcontainers used for MySQL in integration tests

## Infrastructure Services

### Local Development Stack
- **MySQL**: localhost:3306
- **PostgreSQL**: localhost:5432 (commerce-main)
- **Redis (master)**: localhost:6379
- **Redis (readonly)**: localhost:6380
- **Kafka**: localhost:9092, localhost:19092
- **Kafka UI**: localhost:9099
- **Prometheus**: localhost:9090
- **Grafana**: localhost:3000 (admin/admin)

## Code Quality

### Pre-commit Hook
- Automatically runs `ktlintCheck` before every commit
- Set up via `make init`
- Located in `.githooks/pre-commit`

### Version Management
- Project version defaults to git commit hash (short SHA)
- Controlled in `build.gradle.kts` via `getGitHash()`

## Important Notes

- **Module containers** (`apps/`, `modules/`, `supports/`) have all tasks disabled - only run tasks on actual modules
- **QueryDSL** is configured with kapt for JPA entities
- **JaCoCo** coverage reports configured (XML output enabled, CSV/HTML disabled)
- All modules use consistent dependency versions from `gradle.properties`
