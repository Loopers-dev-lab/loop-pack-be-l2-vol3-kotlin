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

### Key Patterns

- **Hexagonal Architecture**: Domain defines Repository interfaces, Infrastructure provides JPA implementations
- **DDD 우선 설계**: Domain 계층의 순수성(Persistence Ignorance)을 유지하고, 애그리거트/VO/도메인 서비스로 비즈니스 규칙을 모델링한다
- **CQRS 도입 기준**: 읽기/쓰기 모델의 관심사가 명확히 분리되고 성능/정합성 요구가 있을 때 Command/Query 모델 분리를 도입한다
- **Domain Model**: private constructor + companion object factory method (`register()`, `retrieve()`)
- **Entity ↔ Domain 변환**: `Entity.toDomain()` / `Entity` companion object (or constructor)
- **Soft Delete**: `BaseEntity` provides `delete()`/`restore()` via `deletedAt` field
- **BaseEntity**: `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`, `deletedAt` + `guard()` template method

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

- **Info**: `{Domain}Info` — application 레이어 반환 DTO
- **Domain Model**: `{Domain}` — private constructor + companion object factory method (예: `User`)
- **Entity**: `{Domain}Entity extends BaseEntity` — infrastructure 레이어 (예: `UserEntity`)
- **Repository Interface**: `{Domain}Repository` — domain 레이어 (예: `UserRepository`)
- **Repository Impl**: `{Domain}RepositoryImpl` — infrastructure 레이어 (예: `UserRepositoryImpl`)
- **JPA Repository**: `{Domain}JpaRepository` — infrastructure 레이어 (예: `UserJpaRepository`)
> API 전용 네이밍(Controller, ApiSpec, DTO, 버전 관리)은 `apps/commerce-api/AGENTS.md` 참조

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
- DDD 완전 분리 구조를 기본 방향으로 삼고, 필요한 전술 패턴을 근거와 함께 선택한다
- CQRS/Event Sourcing 도입 여부는 요구사항, 복잡도, 운영 비용의 트레이드오프를 명시해 결정한다

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

> 상세 워크플로우: `.claude/guides/codex-collaboration.md` 참조
