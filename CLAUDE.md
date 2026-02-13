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

## 개발 규칙

### 진행 Workflow - 증강 코딩

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 할 수 있으며, 최종 승인된 사항을 기반으로 작업을 수행
- **중간 결과 보고**: AI가 반복적인 동작을 하거나, 요청하지 않은 기능을 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입
- **설계 주도권 유지**: AI가 임의판단을 하지 않고, 방향성에 대한 제안을 진행할 수 있으나 개발자의 승인을 받은 후 수행

### 개발 Workflow - TDD (Red > Green > Refactor)

- 모든 테스트는 **3A 원칙**으로 작성: Arrange - Act - Assert

#### 1. Red Phase: 실패하는 테스트 먼저 작성
- 요구사항을 만족하는 기능 테스트 케이스 작성
- 컴파일 에러가 아닌 실제 실패하는 테스트 목표

#### 2. Green Phase: 테스트를 통과하는 코드 작성
- Red Phase의 테스트가 모두 통과할 수 있는 최소한의 코드 작성
- 오버엔지니어링 금지

#### 3. Refactor Phase: 불필요한 코드 제거 및 품질 개선
- 불필요한 private 함수 지양, 객체지향적 코드 작성
- unused import 제거
- 성능 최적화
- 모든 테스트 케이스가 통과해야 함
- 삭제: `delete*`, `remove*`

#### 코드 스타일
1. 삼항 연산자 사용을 지양한다.
2. 한 줄에는 하나의 책임만 가진다.
3. 메서드는 가능한 짧게 유지한다.
4. 중첩 if 문을 줄이고 early return을 사용한다.
5. else 사용을 최소화한다.
6. 매직 넘버를 직접 쓰지 말고 상수로 정의한다.
7. boolean 변수는 긍정형 이름을 사용한다.
8. 의미 없는 축약어를 사용하지 않는다.
9. 변수명과 메서드명은 역할이 드러나도록 작성한다.
10. 주석보다 이름으로 설명한다.
11. null 반환을 지양하고 Optional 또는 객체를 사용한다.
12. 하나의 메서드는 하나의 일만 하도록 만든다.
13. 조건문이 길어지면 의미 있는 변수로 분리한다.
14. 반복되는 코드는 반드시 메서드로 추출한다.
15. 코드 깊이(indent)는 2단계를 넘지 않도록 한다.
16. 한 메서드에서 여러 수준의 추상화를 섞지 않는다.
17. 예외는 숨기지 말고 명확하게 처리한다.
18. 컬렉션은 null 대신 빈 컬렉션을 반환한다.
19. getter/setter 남용을 지양한다.
20. 테스트 가능한 구조로 작성한다.
21. 로그는 의도를 설명하도록 작성한다.
22. 상수는 의미 있는 이름으로 선언한다.
23. switch 대신 다형성을 우선 고려한다.
24. 불필요한 public 노출을 줄인다.
25. 코드 스타일보다 가독성을 우선한다.

#### 추상화 원칙
1. 하나의 메서드에는 하나의 추상화 수준만 존재해야 한다.
2. 상위 추상화 코드에서 하위 구현 세부사항을 직접 다루지 않는다.
3. 구현보다 의도를 먼저 드러내는 이름을 사용한다.
4. 메서드 이름만 보고도 내부 구현을 추측할 수 있어야 한다.
5. 추상화 레벨이 다른 로직은 반드시 메서드로 분리한다.
6. 비즈니스 로직과 기술 구현 로직을 섞지 않는다.
7. 상위 로직은 "무엇을 하는가"를 표현하고, 하위 로직은 "어떻게 하는가"를 담당한다.
8. 구현 세부사항은 가능한 가장 낮은 레벨로 숨긴다.
9. 외부에 노출되는 인터페이스는 최소한의 개념만 포함한다.
10. 추상화는 재사용보다 이해를 쉽게 만드는 것을 우선한다.
11. 읽는 사람이 구현을 따라가지 않아도 흐름을 이해할 수 있어야 한다.
12. 한 메서드 안에서 서로 다른 관심사를 처리하지 않는다.
13. 메서드 이름이 길어지는 것은 추상화가 부족하다는 신호로 본다.
14. 조건 분기가 많아지면 추상화를 다시 설계한다.
15. 구현 설명이 필요한 코드는 추상화가 잘못된 것으로 본다.
16. 계층 간 의존성은 한 방향으로만 흐르게 한다.
17. 상위 계층은 하위 계층의 내부 구조를 몰라야 한다.
18. 추상화는 숨김이 아니라 의도 표현이다.
19. 공통 로직 추출보다 책임 분리가 우선이다.
20. 추상화는 코드 재사용보다 변경에 강한 구조를 목표로 한다.


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


## Swagger (API 문서화) 규칙

> 참고 구현: `MemberV1Controller.kt`, `MemberV1Dto.kt`

### 1. Controller에 직접 어노테이션

`@Tag`, `@Operation`, `@ApiResponses`를 Controller 클래스에 직접 작성한다. (별도 ApiSpec 인터페이스 분리 없이)

```kotlin
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
```

### 2. @Tag (Controller 클래스 레벨)

```kotlin
@Tag(name = "도메인 V1 API", description = "도메인 관련 API")
@RestController
@RequestMapping("/api/v1/도메인")
class XxxV1Controller(...)
```

### 3. @Operation (각 엔드포인트)

`summary` (한줄 요약) + `description` (상세 설명)을 작성한다.

```kotlin
@Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.")
```

### 4. @ApiResponses (각 엔드포인트)

가능한 HTTP 응답 코드별로 `responseCode` + `description`을 작성한다.

- **200**: 성공 케이스
- **400**: 잘못된 요청 (구체적 사유를 괄호로 명시)
- **401**: 인증 실패
- **404**: 리소스 없음
- **409**: 충돌 (중복 등)

```kotlin
@ApiResponses(
    value = [
        SwaggerApiResponse(responseCode = "200", description = "회원가입 성공"),
        SwaggerApiResponse(responseCode = "400", description = "잘못된 요청 (비밀번호 8자 미만, 로그인 ID 10자 초과 등)"),
        SwaggerApiResponse(responseCode = "409", description = "이미 존재하는 로그인 ID"),
    ],
)
```

> `io.swagger.v3.oas.annotations.responses.ApiResponse`는 프로젝트의 `ApiResponse`와 이름이 충돌하므로 `SwaggerApiResponse`로 alias하여 사용한다.

### 5. @Schema (DTO 클래스 및 필드)

Request/Response DTO 클래스와 각 필드에 `description` + `example`을 작성한다.

```kotlin
@Schema(description = "회원가입 요청")
data class SignUpRequest(
    @Schema(description = "로그인 ID (최대 10자)", example = "testuser1")
    val loginId: String,
    @Schema(description = "비밀번호 (최소 8자, 영문+숫자+특수문자 포함)", example = "Password1!")
    val password: String,
)
```

### 6. @Parameter (요청 헤더 및 경로 변수)

`@RequestHeader`, `@PathVariable` 파라미터에 `description` + `required`를 작성한다.

```kotlin
@Parameter(description = "로그인 ID", required = true)
@RequestHeader("X-Loopers-LoginId") loginId: String,
```


## 성능 최적화 가이드

### JPA/Hibernate
- N+1 문제가 발생할 수 있는 양방향 @OneToOne을 지양한다.
- N+1 문제는 `fetch join`, `@BatchSize`로 해결한다.
- 전체 조회를 지양하고, `Pageable`을 적용하거나 필요한 컬럼만 Projection한다.
- 
### QueryDSL
- 전체 Entity를 조회하지 말고, 필요한 필드만 Projection하여 DTO로 반환한다.
- 
### Stream API
- 동일한 조건으로 스트림을 여러 번 순회하지 말고, 한 번 필터링한 결과를 재사용한다.
- 
### 성능 주의사항
- 조회 메서드에는 `@Transactional(readOnly = true)`를 적용한다.
- Lazy Loading은 트랜잭션 범위 안에서만 접근하여 `LazyInitializationException`을 방지한다.
- 자주 조회하는 컬럼에 인덱스가 존재하는지 확인한다.

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
