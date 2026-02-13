# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Loopers Kotlin Spring Template - Multi-module Spring Boot application written in Kotlin with JPA, Redis, and Kafka support.

## 커뮤니케이션 원칙

이 프로젝트의 모든 산출물(코드, 문서, 커밋 메시지, PR 설명, 요구사항 문서)은 **저맥락 커뮤니케이션** 원칙을 따릅니다.
축약어와 키워드 나열을 지양하고, 동일 레벨의 동료 개발자가 추가 설명 없이 이해할 수 있도록 의도를 명확히 서술합니다.

**좋은 예:**
- 커밋 메시지: `feat: 비밀번호 변경 시 기존 비밀번호 일치 여부를 검증하는 로직 추가`
- 변수명: `isCurrentPasswordMatched`, `passwordChangeRequest`

**나쁜 예:**
- 커밋 메시지: `feat: pw chg validation`
- 변수명: `flag`, `dto`, `tmp`

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
- **Domain models** contain business logic and validation
- **Repository interfaces** in domain/, implementations in infrastructure/
- **Facades** orchestrate multiple services and handle DTO conversions
- **Controllers** implement API specs and return standardized ApiResponse

**실제 구현 참고 파일** (commerce-api 기준):
- Domain Model: `domain/member/MemberModel.kt`
- Domain Service: `domain/member/MemberService.kt`
- Facade: `application/member/MemberFacade.kt`
- Controller: `interfaces/api/member/MemberV1Controller.kt`

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

### Security & Authentication
- **Password Hashing**: BCrypt (`org.mindrot:jbcrypt:0.4`)
- **JWT Token**: jjwt 0.12.5 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- **JWT Algorithm**: HS256, 만료 시간 1시간 (3600초)

## 구현 완료된 기능

### API 엔드포인트
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/v1/members/sign-up` | 회원가입 | 불필요 |
| POST | `/api/v1/auth/login` | 로그인 (JWT 토큰 발급) | 불필요 |
| GET | `/api/v1/members/me` | 내정보 조회 (이름/이메일 마스킹 적용) | JWT 필요 |
| PATCH | `/api/v1/members/me/password` | 비밀번호 변경 | JWT 필요 |

### JWT 인증 필터
- 보호 대상 경로: `/api/v1/members/me` 로 시작하는 모든 요청
- `Authorization: Bearer <token>` 헤더에서 토큰을 추출하여 검증
- 검증 성공 시 `AuthenticatedMember` 객체를 요청 속성에 설정

### 유틸리티 클래스

**PasswordValidator** (`support/util/PasswordValidator.kt`):
1. 길이 제한: 8자 이상 16자 이하
2. 허용 문자: 영문 대소문자, 숫자, 특수문자만 허용
3. 문자 종류 조합: 영문, 숫자, 특수문자 중 2종류 이상 조합 필수
4. 연속 동일 문자 제한: 동일 문자 3개 이상 연속 사용 불가 (예: "aaa")
5. 연속 순서 문자 제한: 순차적 문자 3개 이상 연속 사용 불가 (예: "abc", "321")
6. 생년월일 포함 금지: YYYYMMDD, YYMMDD, MMDD 형식 모두 검사
7. 로그인 ID 포함 금지: 비밀번호에 로그인 ID 문자열이 포함되면 거부

**MaskingUtils** (`support/util/MaskingUtils.kt`):
- 이름 마스킹: 첫 글자와 마지막 글자만 표시하고 나머지는 `*`로 대체 (예: "홍길동" → "홍*동")
- 이메일 마스킹: 로컬 파트의 처음 2글자만 표시하고 나머지를 `***`로 대체 (예: "test@example.com" → "te***@example.com")

### 에러 타입

| 타입 | HTTP 상태 | 코드 | 설명 |
|------|-----------|------|------|
| `INTERNAL_ERROR` | 500 | Internal Server Error | 일시적인 오류가 발생했습니다. |
| `BAD_REQUEST` | 400 | Bad Request | 잘못된 요청입니다. |
| `NOT_FOUND` | 404 | Not Found | 존재하지 않는 요청입니다. |
| `CONFLICT` | 409 | Conflict | 이미 존재하는 리소스입니다. |
| `UNAUTHORIZED` | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| `TOKEN_EXPIRED` | 401 | TOKEN_EXPIRED | 토큰이 만료되었습니다. |
| `INVALID_TOKEN` | 401 | INVALID_TOKEN | 유효하지 않은 토큰입니다. |

## API 응답 형식

모든 API는 `ApiResponse<T>` 형식으로 응답합니다.

**성공 응답 예시:**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": { ... }
}
```

**실패 응답 예시:**
```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "잘못된 요청입니다."
  },
  "data": null
}
```

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
- 사용자 승인 없이 다음 Phase 진행 금지
- 요구사항 문서에 없는 기능 임의 추가 금지
- 각 단계에서 발견한 문제나 제안사항은 보고
- 불명확한 요구사항은 구현 전 질문

## Guidelines & Best Practices

### Never Do
- Write non-functional code or implementations using unnecessary mock data
- Write code that is not null-safe (use Kotlin's null safety features)
- Leave `println` statements in code
- Delete or modify tests without explicit approval

### Recommendations
- Write E2E test code that calls actual APIs for verification
- Design reusable objects
- Provide alternatives and suggestions for performance optimization
- For completed APIs, create and organize `.http` files in `.http/<도메인명>/<API명>.http` (예: `.http/member/sign-up.http`)

### Priority Order
1. **Functional Solutions Only**: Consider only solutions that actually work
2. **Safety First**: Ensure null-safety and thread-safety
3. **Testable Design**: Design structures that can be tested
4. **Consistency**: Analyze existing code patterns and maintain consistency

## 프로젝트 문서

### 요구사항 문서 (`docs/`)
- `내정보조회-요구사항.md`: 내정보 조회 API의 기능 요구사항 및 마스킹 규칙 정의
- `비밀번호-수정-요구사항.md`: 비밀번호 변경 API의 기능 요구사항 및 검증 규칙 정의
- `PROJECT_COMPREHENSIVE_GUIDE.md`: 프로젝트 전체 구조 및 기술 스택 종합 가이드
- `프로젝트_완전_이해_가이드.md`: 프로젝트 이해를 위한 한국어 종합 가이드

### API 테스트 파일 (`.http/`)
- `.http/member/sign-up.http`: 회원가입 API 테스트
- `.http/member/me.http`: 내정보 조회 API 테스트
- `.http/member/change-password.http`: 비밀번호 변경 API 테스트

### GitHub 설정 (`.github/`)
- `pull_request_template.md`: PR 생성 시 사용되는 템플릿
- `auto_assign.yml`: PR 생성 시 리뷰어 자동 할당 설정

## Infrastructure Services

### Local Development Stack
- **MySQL**: localhost:3306
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
- Completed APIs should be documented in `.http/<도메인명>/<API명>.http` files (예: `.http/member/sign-up.http`)
- Use IntelliJ HTTP Client format for API testing

## Important Notes

- **Module containers** (`apps/`, `modules/`, `supports/`) have all tasks disabled - only run tasks on actual modules
- **QueryDSL** is configured with kapt for JPA entities
- **JaCoCo** coverage reports configured (XML output enabled, CSV/HTML disabled)
- All modules use consistent dependency versions from `gradle.properties`
