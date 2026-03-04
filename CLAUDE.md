# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명: 영어

## Commands

### 초기 설정

```bash
make init                                          # git hooks 설정
docker compose -f docker/infra-compose.yml up -d   # 인프라 (MySQL, Redis, Kafka) 실행
```

### 빌드 및 실행

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
```

## 아키텍처

Kotlin + Spring Boot 3.4.4 + JDK 21 멀티모듈 프로젝트.

### 모듈 구조

- **apps/**: 실행 가능한 Spring Boot 애플리케이션 (commerce-api, commerce-batch, commerce-streamer)
- **modules/**: 인프라 설정 모듈 (jpa, redis, kafka) — `testFixtures` 제공
- **supports/**: 부가 기능 모듈 (jackson, logging, monitoring)

### 레이어드 아키텍처 (apps 내부)

```
interfaces/
  api/             → 고객 API: Controller, ApiSpec, Dto (/api/v1)
  api-admin/       → 어드민 API: Controller, ApiSpec, Dto (/api-admin/v1)
application/       → Facade(오케스트레이션), Info(레이어간 데이터 전달)
domain/            → Entity, Service(@Component), Repository(인터페이스)
infrastructure/    → RepositoryImpl(구현체), JpaRepository
support/error/     → CoreException, ErrorType
```

요청 흐름: `Controller → Facade → Service → Repository(interface) → RepositoryImpl → JpaRepository`

레이어드 아키텍처 + DIP(의존성 역전 원칙)를 적용하여 도메인 중심의 유연한 구조를 유지한다.

- **Interfaces 계층** (`interfaces/api/`, `interfaces/api-admin/`): Application Layer 호출만 담당. 요청 검증, 응답 매핑
- **Application 계층** (`application/`): 서로 다른 도메인을 조합해 유스케이스 기능 제공. 비즈니스 로직은 도메인에 위임
- **Domain 계층** (`domain/`): 비즈니스 핵심. 다른 계층에 의존하지 않음. 모든 의존 방향은 도메인을 향함
- **Infrastructure 계층** (`infrastructure/`): 외부 기술(JPA, Redis, Kafka) 의존. 도메인 인터페이스 구현 제공

패키징 전략:
- 4개 레이어 패키지를 두고, 하위에 **도메인 별로 패키징**한다
- API request/response DTO와 응용 레이어의 DTO는 **분리하여 작성**한다

DIP 강제: ArchUnit 아키텍처 테스트로 레이어 간 의존성 방향을 검증한다.
- `domain`은 `infrastructure`, `interfaces`에 의존하지 않는다
- `application`은 `infrastructure`에 의존하지 않는다
- 패키지 간 순환 의존이 없어야 한다
- 위반 시 `./gradlew test`에서 실패한다

### 고객 API / 어드민 API 분리

같은 모듈 내에서 패키지 레벨로 분리한다. 도메인/인프라는 공유한다.

- **고객 API** (`interfaces/api/`, `/api/v1`): 복잡한 비즈니스 흐름 (재고 차감, 멱등성, 스냅샷 등)
- **어드민 API** (`interfaces/api-admin/`, `/api-admin/v1`): 단순 CRUD 중심, 관리 정보 포함
- **인증 분리**: 고객은 LoginId/LoginPw 헤더, 어드민은 LDAP 헤더. URL prefix로 필터 분리
- **어드민 Facade는 단순 위임**: 도메인 서비스에 직접 위임하고, 복잡한 오케스트레이션을 넣지 않는다

### 핵심 패턴

**Entity**: `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원). 프로퍼티는 `protected set`. 도메인 검증은 `init` 블록에서 `CoreException` throw.

**에러 처리**: `CoreException(errorType: ErrorType, customMessage: String?)`. ErrorType enum은 `INTERNAL_ERROR(500)`, `BAD_REQUEST(400)`, `NOT_FOUND(404)`, `CONFLICT(409)`.

**API 응답**: 모든 응답은 `ApiResponse<T>` 래퍼 사용. `ApiResponse.success(data)` / `ApiResponse.fail(errorCode, message)`. `ApiControllerAdvice`에서 전역 예외 처리.

**Repository**: 도메인 레이어에 인터페이스 정의 → infrastructure에서 구현. JPA Repository는 `JpaRepository<Entity, Long>` 상속.

**DTO 변환**: Info/Dto에 `companion object { fun from(...) }` 팩토리 메서드 사용.

## 도메인 & 객체 설계 전략

- 도메인 객체는 **비즈니스 규칙을 캡슐화**해야 한다. 외부에서 getter로 꺼내 판단하지 않는다
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공한다
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높다
- 각 기능에 대한 **책임과 결합도**에 대해 개발자의 의도를 확인하고 개발을 진행한다
- **Entity**: 고유 식별자(id)를 가지며, 생명주기 동안 상태가 변한다
- **Value Object**: 식별자 없이 값 자체로 동등성을 판단한다. 불변으로 설계한다
- **Domain Service**: 특정 Entity에 속하지 않는 도메인 로직을 담당한다 (`@Component`)

## 테스트 패턴

- **단위 테스트**: `@Nested` + `@DisplayName`(한국어) 조합으로 BDD 스타일 구성
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()` 호출
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- 테스트 시 MySQL/Redis는 TestContainers로 자동 구동 (프로파일: `test`)
- 테스트 타임존: `Asia/Seoul`

## 설정 파일 위치

- DB 설정: `modules/jpa/src/main/resources/jpa.yml`
- Redis 설정: `modules/redis/src/main/resources/redis.yml`
- Kafka 설정: `modules/kafka/src/main/resources/kafka.yml`
- 앱 설정: `apps/commerce-api/src/main/resources/application.yml`
- JPA 엔티티 스캔: `com.loopers`, Repository 스캔: `com.loopers.infrastructure`

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

### 주의사항

**금지 (Never Do)**
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않은 코드 작성 금지
- `println` 코드 남기지 않는다

**권장 (Recommendation)**
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `http/*.http` 파일에 분류하여 작성

**우선순위 (Priority)**
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지
