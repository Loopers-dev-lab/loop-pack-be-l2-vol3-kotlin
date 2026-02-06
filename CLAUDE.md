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

### Core
- **Language**: Kotlin 2.0.20, Java 21
- **Framework**: Spring Boot 3.4.4, Spring Cloud 2024.0.1
- **Dependency Management**: Gradle with Kotlin DSL, Spring Dependency Management Plugin 1.1.7

### Data & Persistence
- **Database**: MySQL 8.0 (with Testcontainers for tests)
- **ORM**: JPA/Hibernate with Kotlin JPA Plugin
- **Query DSL**: QueryDSL with Kapt annotation processing
- **Cache**: Redis 7.0 (master + readonly replica)

### Messaging & Streaming
- **Message Broker**: Kafka 3.5.1 (Bitnami Legacy)
- **Kafka UI**: Provectus Kafka UI (localhost:9099)

### Testing
- **Framework**: JUnit 5 Platform
- **Mocking**: SpringMockK 4.0.2, Mockito 5.14.0, Mockito-Kotlin 5.4.0
- **Test Data**: Instancio JUnit 5.0.2
- **Containers**: Testcontainers (MySQL, Redis, Kafka)

### Code Quality & Linting
- **Linter**: ktlint 1.0.1 (Gradle Plugin 12.1.2)
- **Coverage**: JaCoCo (XML reports enabled)
- **Pre-commit**: Git hooks with ktlint check

### Monitoring & Observability
- **Metrics**: Micrometer with Prometheus Registry
- **Dashboard**: Grafana (localhost:3000, admin/admin)
- **Actuator**: Spring Boot Actuator
- **API Docs**: SpringDoc OpenAPI 2.7.0

### Libraries
- **JSON**: Jackson Module Kotlin, Jackson Datatype JSR310
- **Logging**: Slack Appender 1.6.1

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

## Development Rules

### Augmented Coding Workflow
**Core Principle**: Direction and major decisions are proposed to developers only, and work is performed based on final approved decisions.

- **Intermediate Results Reporting**: Developers intervene when AI performs repetitive actions, implements unrequested features, or arbitrarily deletes tests
- **Design Authority Maintenance**: AI does not make arbitrary judgments but can propose directions, performing only after developer approval

### TDD Workflow (Red → Green → Refactor)
All tests must follow the 3A principle: **Arrange - Act - Assert**

#### 1. Red Phase: Write Failing Tests First
- Write test cases that satisfy requirements
- Tests should fail initially

#### 2. Green Phase: Write Code to Pass Tests
- Write code that passes all Red Phase tests
- **NO over-engineering** - implement only what's needed to pass tests

#### 3. Refactor Phase: Remove Unnecessary Code & Improve Quality
- Avoid unnecessary private functions, write object-oriented code
- Remove unused imports
- Optimize performance
- **All test cases must pass**

### 요구사항 기반 TDD 워크플로우

요구사항 문서(`docs/*.md`)를 기준으로 구현할 때 다음 절차를 따릅니다:

#### 작업 단계
1. **Phase 분석**: 요구사항 문서의 Phase/단계를 확인하고 순서대로 진행
2. **단계별 검토**: 각 Phase 완료 후 반드시 사용자 검토 및 승인 대기
3. **다음 단계 진행**: 승인 후에만 다음 Phase로 이동

#### 각 Phase 진행 방식 (TDD)
1. **RED**: 해당 Phase의 테스트 코드 먼저 작성 → 실패 확인
2. **GREEN**: 테스트 통과하는 최소 구현
3. **REFACTOR**: 코드 정리 및 품질 개선
4. **검토 요청**: 작업 결과 보고 및 사용자 승인 대기

#### 검토 요청 형식
각 Phase 완료 시 다음 형식으로 보고:
- 완료된 작업 요약
- 생성/수정된 파일 목록
- 테스트 실행 결과
- 다음 단계 안내

#### 중요 규칙
- ❌ 사용자 승인 없이 다음 Phase 진행 금지
- ❌ 요구사항 문서에 없는 기능 임의 추가 금지
- ✅ 각 단계에서 발견한 문제나 제안사항은 보고
- ✅ 불명확한 요구사항은 구현 전 질문

## Guidelines & Best Practices

### Never Do
- ❌ Write non-functional code or implementations using unnecessary mock data
- ❌ Write code that is not null-safe (use Kotlin's null safety features)
- ❌ Leave `println` statements in code
- ❌ Delete or modify tests without explicit approval

### Recommendations
- ✅ Write E2E test code that calls actual APIs for verification
- ✅ Design reusable objects
- ✅ Provide alternatives and suggestions for performance optimization
- ✅ For completed APIs, create and organize `.http` files in `.http/**/*.http`

### Priority Order
1. **Functional Solutions Only**: Consider only solutions that actually work
2. **Safety First**: Ensure null-safety and thread-safety
3. **Testable Design**: Design structures that can be tested
4. **Consistency**: Analyze existing code patterns and maintain consistency

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
- **If commit fails due to ktlint**: Run `./gradlew ktlintFormat` to auto-fix

### Version Management
- Project version defaults to git commit hash (short SHA)
- Controlled in `build.gradle.kts` via `getGitHash()`

### API Documentation
- Completed APIs should be documented in `.http/**/*.http` files
- Use IntelliJ HTTP Client format for API testing

## Important Notes

- **Module containers** (`apps/`, `modules/`, `supports/`) have all tasks disabled - only run tasks on actual modules
- **QueryDSL** is configured with kapt for JPA entities
- **JaCoCo** coverage reports configured (XML output enabled, CSV/HTML disabled)
- All modules use consistent dependency versions from `gradle.properties`
