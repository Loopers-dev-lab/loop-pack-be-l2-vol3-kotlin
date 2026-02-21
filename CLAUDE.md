# CLAUDE.md

> 이 문서는 Claude Code가 프로젝트를 이해하고 일관성 있게 개발을 지원하기 위한 가이드입니다.

## 프로젝트 개요

**loopers-kotlin-spring-template**는 Kotlin 기반의 멀티 모듈 Spring Boot 프로젝트입니다.

## 기술 스택 및 버전

| 분류 | 기술 | 버전 |
|------|------|------|
| **Language** | Kotlin | 2.0.20 |
| **JDK** | Java | 21 |
| **Framework** | Spring Boot | 3.4.4 |
| **Dependency Management** | Spring Cloud Dependencies | 2024.0.1 |
| **Build Tool** | Gradle (Kotlin DSL) | - |
| **Code Quality** | ktLint | 1.0.1 |
| **API Docs** | SpringDoc OpenAPI | 2.7.0 |
| **Test** | JUnit 5, MockK, Mockito, Instancio | - |
| **Test Infra** | Testcontainers | - |
| **Coverage** | JaCoCo | - |

## 모듈 구조

```
loopers-kotlin-spring-template/
├── apps/                          # 실행 가능한 애플리케이션
│   ├── commerce-api/              # REST API 서버 (메인)
│   ├── commerce-streamer/         # Kafka 스트리밍 처리
│   └── commerce-batch/            # 배치 작업 처리
├── modules/                       # 인프라 모듈
│   ├── jpa/                       # JPA + QueryDSL 설정
│   ├── redis/                     # Redis 마스터-레플리카 설정
│   └── kafka/                     # Kafka 클라이언트 설정
└── supports/                      # 공통 지원 모듈
    ├── jackson/                   # JSON 직렬화 설정
    ├── logging/                   # Logback 로깅 설정
    └── monitoring/                # Prometheus/Grafana 모니터링
```

## 레이어 아키텍처 (commerce-api 기준)

```
interfaces/api/     → Controller, DTO, ApiSpec (Swagger)
application/        → Facade (비즈니스 로직 조정)
domain/             → Service, Repository, Entity (핵심 도메인)
infrastructure/     → Repository 구현체, 외부 연동
support/            → 공통 유틸, 에러 처리
```

### 코드 패턴 예시

```kotlin
// Controller → Facade → Service → Repository
@RestController
class XxxV1Controller(private val xxxFacade: XxxFacade) : XxxV1ApiSpec

@Component
class XxxFacade(private val xxxService: XxxService)

@Component
class XxxService(private val xxxRepository: XxxRepository)
```

## 도메인 & 객체 설계 전략

- 도메인 객체는 비즈니스 규칙을 캡슐화해야 합니다.
- 애플리케이션 서비스는 서로 다른 도메인을 조립해, 도메인 로직을 조정하여 기능을 제공해야 합니다.
- 규칙이 여러 서비스에 나타나면 도메인 객체에 속할 가능성이 높습니다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행합니다.

## 아키텍처, 패키지 구성 전략

- 본 프로젝트는 레이어드 아키텍처를 따르며, DIP (의존성 역전 원칙) 을 준수합니다.
- API request, response DTO와 응용 레이어의 DTO는 분리해 작성하도록 합니다.
- 패키징 전략은 4개 레이어 패키지를 두고, 하위에 도메인 별로 패키징하는 형태로 작성합니다.
  - `/interfaces/api` (presentation 레이어 - API)
  - `/application/..` (application 레이어 - 도메인 레이어를 조합해 사용 가능한 기능을 제공)
  - `/domain/..` (domain 레이어 - 도메인 객체 및 엔티티, Repository 인터페이스가 위치)
  - `/infrastructure/..` (infrastructure 레이어 - JPA, Redis 등을 활용해 Repository 구현체를 제공)

## 개발 규칙

### 진행 Workflow - 증강 코딩

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행
- **중간 결과 보고**: AI가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입
- **설계 주도권 유지**: AI가 임의판단을 하지 않고, 방향성에 대한 제안을 진행할 수 있으나 개발자의 승인을 받은 후 수행

### 개발 Workflow - TDD

- Red → Green → Refactor 순서로 개발한다.
- 테스트는 Arrange - Act - Assert (3A) 구조로 작성하고 주석으로 구분한다.
- 메서드명은 영어 camelCase `결과_when조건`, `@DisplayName`은 한글로 작성한다.
- 기능별 `@Nested`로 그룹핑한다.
- 정상 흐름, 예외 흐름, 경계값 케이스를 반드시 포함한다.
- 도메인 단위 테스트는 Mock 없이 순수 인스턴스화, 서비스 단위 테스트는 Mockito 사용.

### 코드 스타일 & 추상화 원칙

- early return을 사용하고, 중첩 if문/else 사용을 최소화한다.
- 매직 넘버는 상수로 정의하고, 의미 없는 축약어를 사용하지 않는다.
- 하나의 메서드는 하나의 일만 하고, 코드 깊이(indent)는 2단계를 넘지 않는다.
- 하나의 메서드에는 하나의 추상화 수준만 존재해야 한다.
- 비즈니스 로직과 기술 구현 로직을 섞지 않는다.
- null 반환을 지양하고, 컬렉션은 null 대신 빈 컬렉션을 반환한다.

## 주의사항

### 1. Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현을 하지 말 것
- null-safety 하지 않게 코드 작성하지 말 것 (Kotlin의 null-safety 활용)
- `println` 코드 남기지 말 것
- 테스트를 임의로 삭제하거나 `@Disabled` 처리하지 말 것
- 요청하지 않은 기능을 임의로 구현하지 말 것

### 2. Recommendation
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `.http/*.http`에 분류해 작성
- 기존 코드 패턴 분석 후 일관성 유지

### 3. Priority
- 실제 동작하는 해결책만 고려
- null-safety, thread-safety 고려
- 테스트 가능한 구조로 설계
- 기존 코드 패턴 분석 후 일관성 유지

### 4. 개발자 정보
- Kotlin 코드 작성 시 Java와 다른 핵심 기능이 있으면 상세히 설명할 것
- Kotlin 고유 기능을 사용할 경우, 왜 사용하는지 설명할 것

### 5. git
- commit, push 등 git에 관련된 명령어는 개발자의 확인을 받을 것
- 커밋 메시지 형식:
  ```
  <type>: <subject>

  - <변경 내용 1>
  - <변경 내용 2>
  ```
  - `type`: `feat`, `fix`, `docs`, `refactor`, `test`, `chore` 등
  - `subject`: 변경 대상 요약
  - 본문: 빈 줄 후 구체적인 변경 내용을 `-`로 나열


## 빌드 및 실행 명령어

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew test

# ktLint 검사
./gradlew ktlintCheck

# ktLint 자동 포맷팅
./gradlew ktlintFormat

# commerce-api 실행
./gradlew :apps:commerce-api:bootRun

# Docker 인프라 실행 (MySQL, Redis, Kafka)
docker-compose -f docker/infra-compose.yml up -d
```

## 테스트 환경

- 테스트 프로파일: `test`
- 타임존: `Asia/Seoul`
- Testcontainers를 통한 MySQL, Redis 통합 테스트 지원

## API 응답 형식

```kotlin
data class ApiResponse<T>(
    val meta: Metadata,  // result: SUCCESS/FAIL, errorCode, message
    val data: T?
)
```

## 에러 처리

```kotlin
// CoreException + ErrorType 사용
throw CoreException(ErrorType.NOT_FOUND, "커스텀 메시지")

// ErrorType 예시
enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    BAD_REQUEST, NOT_FOUND, CONFLICT, INTERNAL_ERROR
}
```
