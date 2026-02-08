# E-Commerce Platform

## Project Overview

Multi-module e-commerce platform built with Kotlin + Spring Boot. Follows Hexagonal/Layered Architecture with 3 application modules, 3 infrastructure modules, and 3 cross-cutting support modules.

## Infrastructure

| Category  | Technology             | Version |
|-----------|------------------------|---------|
| Database  | MySQL                  | 8.0     |
| Cache     | Redis (Master-Replica) | 7.0     |
| Messaging | Kafka (KRaft mode)     | 3.5.1   |

> 애플리케이션 의존성(Kotlin, Spring Boot, 테스트 라이브러리 등)은 `build.gradle.kts` 참조

## Module Structure

```
e-commerce/
├── apps/                          # Application modules (bootJar enabled)
│   ├── commerce-api/              # REST API (Web + Swagger + Actuator)
│   ├── commerce-batch/            # Batch processing (Spring Batch)
│   └── commerce-streamer/         # Event streaming (Web + Kafka + Actuator)
├── modules/                       # Infrastructure modules (java-test-fixtures)
│   ├── jpa/                       # JPA + QueryDSL + MySQL
│   ├── redis/                     # Spring Data Redis
│   └── kafka/                     # Spring Kafka
├── supports/                      # Cross-cutting concerns
│   ├── jackson/                   # JSON serialization config
│   ├── logging/                   # Logback + Slack + Tracing
│   └── monitoring/                # Actuator + Prometheus metrics
└── docker/
    ├── infra-compose.yml          # MySQL, Redis, Kafka, Kafka-UI
    └── monitoring-compose.yml     # Prometheus, Grafana
```

### Module Dependency Graph

- **commerce-api**: jpa, redis, jackson, logging, monitoring
- **commerce-batch**: jpa, redis, jackson, logging, monitoring
- **commerce-streamer**: jpa, redis, kafka, jackson, logging, monitoring

## Architecture

### Layered Package Structure

```
com.loopers.
├── <domain>/                       # 도메인별 패키지 (user, order 등)
│   ├── interfaces/
│   │   └── api/                    # Controller, ApiSpec, Dto, ApiResponse, ControllerAdvice
│   ├── application/                # Service, Facade(필요 시), Info DTO
│   ├── domain/                     # Domain Model, Repository interface
│   └── infrastructure/             # Entity, RepositoryImpl, JpaRepository
└── support/                        # Cross-cutting (CoreException, ErrorType)
```

### Key Patterns

- **Hexagonal Architecture**: Domain defines Repository interfaces, Infrastructure provides JPA implementations
- **Service per Use Case**: `application/` layer implements use cases via Service classes. Facade is introduced only when multiple services need orchestration. 단일 Service만 필요한 경우 Controller → Service
  직접 호출
- **Domain Model**: private constructor + companion object factory method (`register()`, `retrieve()`)
- **Entity ↔ Domain 변환**: `Entity.toDomain()` / `Entity` companion object (or constructor)
- **Controller 변환 체인**: `.let { Dto.from(it) }.let { ApiResponse.success(it) }`
- **Soft Delete**: `BaseEntity` provides `delete()`/`restore()` via `deletedAt` field
- **BaseEntity**: `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`, `deletedAt` + `guard()` template method
- **ApiResponse**: `{ "meta": { "result": "SUCCESS", "errorCode": null, "message": null }, "data": {...} }` 형태의 표준 응답 래퍼
- **ApiControllerAdvice**: Global exception handling via `CoreException` + `ErrorType`

### Package Naming Convention

```
com.loopers.<domain>.<layer>
```

Example: `com.loopers.user.domain`, `com.loopers.user.infrastructure`

## Development Setup

### Prerequisites

```bash
# Initialize git hooks (ktlint pre-commit)
make init
```

### Infrastructure

```bash
# Start MySQL, Redis, Kafka
docker compose -f docker/infra-compose.yml up -d

# Start monitoring (Prometheus, Grafana)
docker compose -f docker/monitoring-compose.yml up -d
```

| Service          | Port  |
|------------------|-------|
| MySQL            | 3306  |
| Redis Master     | 6379  |
| Redis Replica    | 6380  |
| Kafka (internal) | 9092  |
| Kafka (external) | 19092 |
| Kafka-UI         | 9099  |
| Prometheus       | 9090  |
| Grafana          | 3000  |

### Profiles

- `local` - Local development (default)
- `test` - Test execution
- `dev` - Development server
- `qa` - QA environment
- `prd` - Production (Swagger disabled)

## Code Conventions

### Linting

- **ktlint** enforced via pre-commit hook (`make init` to set up)
- Check: `./gradlew ktlintCheck`
- Format: `./gradlew ktlintFormat`

### Naming Rules

- **Controller**: `{Domain}V{Version}Controller` implements `{Domain}V{Version}ApiSpec` (예: `ExampleV1Controller`)
- **ApiSpec**: `{Domain}V{Version}ApiSpec` — Swagger 어노테이션 분리용 인터페이스
- **DTO**: `{Domain}V{Version}Dto` container + inner class (예: `ExampleV1Dto.ExampleResponse`)
- **Info**: `{Domain}Info` — application 레이어 반환 DTO
- **Domain Model**: `{Domain}` — private constructor + companion object factory method (예: `Example`)
- **Entity**: `{Domain}Entity extends BaseEntity` — infrastructure 레이어 (예: `ExampleEntity`)
- **Repository Interface**: `{Domain}Repository` — domain 레이어 (예: `ExampleRepository`)
- **Repository Impl**: `{Domain}RepositoryImpl` — infrastructure 레이어 (예: `ExampleRepositoryImpl`)
- **JPA Repository**: `{Domain}JpaRepository` — infrastructure 레이어 (예: `ExampleJpaRepository`)
- **Application Service**: `{Domain}Service` — use case 단위로 분리 (예: `ExampleService`)
- **Domain Service**: `{Domain}DomainService` — 여러 엔티티 간 순수 비즈니스 규칙 (필요 시)
- **Facade**: `{Domain}Facade` — 여러 Service 조합이 필요할 때 도입 (필요 시)
- **버전 관리**: 클래스명에 V{Version} 포함, 패키지 경로에는 버전 없음

### 패턴 레퍼런스 (Example 코드)

새로운 도메인 구현 시 아래 example 파일들의 패턴을 따른다:

| 패턴                   | 참조 파일                                                                  |
|----------------------|------------------------------------------------------------------------|
| Controller + ApiSpec | `example/interfaces/api/ExampleV1Controller.kt`, `ExampleV1ApiSpec.kt` |
| DTO (Response)       | `example/interfaces/api/ExampleV1Dto.kt`                               |
| Service              | `example/application/ExampleService.kt`                                |
| Info DTO             | `example/application/ExampleInfo.kt`                                   |
| Domain Model         | `example/domain/Example.kt`                                            |
| Repository Interface | `example/domain/ExampleRepository.kt`                                  |
| Entity               | `example/infrastructure/ExampleEntity.kt`                              |
| Repository Impl      | `example/infrastructure/ExampleRepositoryImpl.kt`                      |
| Unit Test            | `example/domain/ExampleModelTest.kt`                                   |
| Integration Test     | `example/domain/ExampleServiceIntegrationTest.kt`                      |
| E2E Test             | `example/interfaces/api/ExampleV1ApiE2ETest.kt`                        |

### Layer Rules

- `interfaces` depends on `application`
- `application` depends on `domain`
- `infrastructure` depends on `domain`
- `domain` has **no outward dependencies** (defines interfaces only)

## Build & Test

```bash
# Full build
./gradlew build

# Build specific app
./gradlew :apps:commerce-api:build

# Run tests (profile=test, timezone=Asia/Seoul)
./gradlew test

# Run specific module tests
./gradlew :modules:jpa:test

# Lint check
./gradlew ktlintCheck

# Coverage report (XML)
./gradlew jacocoTestReport
```

### Test Configuration

- JUnit 5 with `maxParallelForks = 1`
- Timezone: `Asia/Seoul`
- Profile: `test`
- Testcontainers for MySQL, Redis, Kafka integration tests
- Test fixtures available via `testFixtures(project(":modules:jpa"))` and `testFixtures(project(":modules:redis"))`

## 개발 규칙

### 진행 Workflow - 증강 코딩

#### 대원칙

- AI는 **설계 주도권을 유지**하되, 사용자에게 중간 결과를 투명하게 보고한다
- 구현 전 설계 방향을 먼저 제시하고, 승인 후 코드를 작성한다
- 반복적인 동작(동일 패턴 3회 이상 반복)은 자동화 스크립트 또는 공통 유틸로 추출한다

#### 사전 보고 기준

다음 조건에 해당하면 구현 전 사용자에게 방향을 보고한다:

- 3개 이상 파일 수정이 필요한 경우
- 새로운 패턴이나 구조를 도입하는 경우
- 기존 인터페이스(public API, DB 스키마)를 변경하는 경우
- 모듈 간 의존성에 영향을 주는 경우

#### 중간 결과 보고

- 각 Phase(설계 → 구현 → 검증) 완료 시 결과를 요약 보고한다
- 예상과 다른 결과가 나오면 즉시 보고하고 방향을 재조율한다
- 테스트 실패 시 원인 분석 결과와 함께 보고한다

### AI 협업 - Codex CLI

다음 조건에서 Codex 검증을 수행한다:

- 사용자 명시적 요청 ("Codex로 리뷰해줘", "코드 검토해줘")
- 중요 코드 구현 완료 후 (보안, 성능, 핵심 비즈니스 로직)
- TDD 사이클 완료 후 (Red → Green → Refactor 완료 시점)
- 복잡한 계획 수립 시 (아키텍처 변경, 대규모 리팩토링)

> 상세 워크플로우: `.claude/rules/codex-collaboration.md` 참조

### 개발 Workflow - 인수조건 기반 TDD

**핵심 원칙: 인수조건 먼저, 코드는 나중**

- 기능 구현 전 반드시 인수조건을 정의한다
- 인수조건 → 테스트 스켈레톤 → TDD 사이클 순서로 진행한다

@.claude/rules/acceptance-criteria.md

### 개발 Workflow - TDD (Red → Green → Refactor)

**핵심 원칙: Macro to Micro (Outside-In TDD)**

- 기능 구현 시 상위 레이어(Controller)에서 하위 레이어(Repository)로 단계별 진행
- 각 레이어에서 Red → Green → Refactor 사이클 완료 후 다음 레이어로 이동

@.claude/rules/tdd-workflow.md

### 테스트 레벨별 가이드라인

@.claude/rules/testing-levels.md

### Git Workflow - PR 기반 과제 제출

@.claude/rules/pr-guide.md

### 학습자 컨텍스트

@.claude/learning/learner-context.md
