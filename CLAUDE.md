# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 언어 규칙

- 응답, 코드 주석, 커밋 메시지, 문서화: 한국어
- 변수명/함수명/클래스명: 영어

## Commands

```bash
./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:build     # 특정 모듈 빌드
./gradlew :apps:commerce-api:bootRun   # 애플리케이션 실행
./gradlew test                         # 전체 테스트
./gradlew ktlintCheck                  # 린트 체크
./gradlew ktlintFormat                 # 린트 자동 수정
./gradlew ktlintCheck test             # 커밋 전 최종 검증
```

단일 테스트 실행:

```bash
./gradlew :apps:commerce-api:test --tests "패키지.클래스명"
./gradlew :apps:commerce-api:test --tests "패키지.클래스명.메서드명"
```

## 아키텍처

Kotlin + Spring Boot 3.4.4 + JDK 21 멀티모듈 프로젝트.

### 모듈 구조

- **apps/**: 실행 가능한 Spring Boot 애플리케이션 (commerce-api, commerce-batch, commerce-streamer)
- **modules/**: 인프라 설정 모듈 (jpa, redis, kafka) — `testFixtures` 제공
- **supports/**: 부가 기능 모듈 (jackson, logging, monitoring)

### 레이어드 아키텍처 (apps 내부)

```
interfaces/api/    → Controller, ApiSpec(OpenAPI 인터페이스), Dto
application/       → Facade(여러 Domain Service를 조합하는 유스케이스 오케스트레이션), Application Service(단일 도메인 유스케이스, 예: AuthService)
domain/            → Entity, Domain Service(@Component), Repository(인터페이스), Value Object
infrastructure/    → RepositoryImpl(구현체), JpaRepository
support/error/     → CoreException, ErrorType
```

**의존 방향:** `Application → Domain ← Infrastructure` (DIP). Domain 계층은 다른 계층에 의존하지 않는다.

**패키지 전략:** 계층 패키지 하위에 도메인별로 패키징한다 (예: `domain/product/`, `domain/order/`, `infrastructure/product/`).

**요청 흐름:** `Controller → Facade 또는 Domain Service → Repository(interface) → RepositoryImpl → JpaRepository`
- 여러 Domain Service를 조합해야 하는 경우: Controller → **Facade** → Domain Services
- 단일 Domain Service로 충분한 경우: Controller → **Domain Service** 직접 호출 (Facade 생략)
- 애플리케이션 관심사(인증 등): Controller → **Application Service** → Domain Service

### 핵심 패턴

**Entity**: `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt 자동관리, soft delete 지원). 프로퍼티는 `protected set`. 도메인 검증은
`guard()` 메서드를 override하여 VO로 수행한다. `guard()`는 `@PrePersist`/`@PreUpdate` 시 자동 호출된다.

**Entity 생성 패턴 선택 기준:**
- **생성 후 상태가 변하는 엔티티** (Product, UserPoint): `public constructor` + `init { guard() }`. `guard()`가 `@PrePersist`/`@PreUpdate`마다 재검증.
- **생성 시 복합 조립이 필요하고 생성 후 불변인 엔티티** (Order, OrderItem): `private constructor` + `companion object { create() }`. 팩토리 메서드가 유일한 생성 경로이며, 생성 후 재검증할 필드가 없으므로 `guard()` 불필요. 엔티티가 자신의 구성 요소 조립과 파생 값 계산을 내부에서 책임진다 (예: `Order.create()`가 OrderItem 생성 + totalPrice 계산을 수행하며, Facade는 그 결과를 사용할 뿐이다). cross-domain 타입 의존을 방지하기 위해 다른 도메인의 엔티티를 직접 받지 않고, 자기 도메인의 데이터 클래스(예: `OrderProductInfo`)를 통해 필요한 정보만 수신한다.

**Value Object**: 생성 시점에 자가 검증. `init` 블록에서 규칙 위반 시 `CoreException` throw. Entity 필드는 기본 타입(String, Int, Long, BigDecimal)으로 유지하되, 생성/변경 시 VO를 통해 검증한다.

**Command**: 서비스 호출 시 요청 파라미터를 `XxxCommand` sealed interface로 묶는다 (예: `UserCommand.SignUp`). Controller에서 Dto → Command 변환 후 서비스에 전달.

**에러 처리**: `CoreException(errorType: ErrorType, customMessage: String?)`. ErrorType enum: `INTERNAL_ERROR(500)`,
`BAD_REQUEST(400)`, `NOT_FOUND(404)`, `CONFLICT(409)`, `UNAUTHORIZED(401)`.

**API 응답**: 모든 응답은 `ApiResponse<T>` 래퍼 사용. `ApiResponse.success(data)` / `ApiResponse.fail(errorCode, message)`.
`ApiControllerAdvice`에서 전역 예외 처리.

**Repository**: 도메인 레이어에 인터페이스 정의 → infrastructure에서 구현. JPA Repository는 `JpaRepository<Entity, Long>` 상속.
도메인 Repository 인터페이스는 **도메인 언어와 기본 타입만** 사용한다. Spring Data 타입(`Pageable`, `Page` 등)을 도메인 계약에 노출하지 않는다.
페이지네이션은 `page: Int, size: Int` 파라미터와 `PageResult<T>`(도메인 고유 타입)로 표현하고, Spring `Page<T>` 변환은 Controller에서 `toSpringPage()` 확장함수로 수행한다.

**Domain Service**: 도메인 레이어의 서비스. `@Component`로 등록하며, Repository를 통해 도메인 객체를 조회/저장하고 비즈니스 로직을 수행한다.
단일 도메인 CRUD(예: ProductService)부터 복수 도메인 객체 협력(예: PointChargingService)까지 도메인 레이어에서 처리한다.

**DTO 변환 규칙**:

- **Facade 없이 Controller → Domain Service 직접 호출**: Domain Service가 Entity 또는 domain 레이어 데이터 클래스를 반환 → Controller에서 Dto로 변환
- **같은 바운디드 컨텍스트 내 조합** (예: Product + Brand): 조합 결과물은 **domain 레이어**에 데이터 클래스로 둔다 (예: `domain/catalog/ProductDetail`). Application 레이어에 두면 Domain → Application 의존이 생겨 DIP 위반
- **다른 바운디드 컨텍스트 간 조합** (Facade 경유): Facade가 **application 레이어**의 Info 객체를 반환 → Controller에서 Dto로 변환
- Dto/Info에 `companion object { fun from(...) }` 팩토리 메서드 사용

## 도메인 & 객체 설계 전략

- 도메인 객체(Entity)는 비즈니스 규칙을 캡슐화한다. 검증과 상태 변경은 Entity 내부에서 수행한다
- Value Object는 자가 검증하며 불변이다. 도메인 규칙이 있는 값(금액, 수량, 재고 등)은 VO로 표현한다
- 규칙이 여러 Service에 나타나면 도메인 객체(Entity 또는 Domain Service)에 속할 가능성이 높다
- Domain Service는 상태 없이, 도메인 객체의 협력을 중심으로 설계한다
- Application Layer(Facade)는 경량으로 유지하고, 실질적인 비즈니스 로직은 도메인으로 위임한다
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행한다

## 테스트 패턴

- `@Nested` + `@DisplayName`(한국어) BDD 스타일, **3A 원칙** (Arrange → Act → Assert)
- **통합 테스트**: `@SpringBootTest`, `@AfterEach`에서 `databaseCleanUp.truncateAllTables()`
- **E2E 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- **단위 테스트**: 도메인 로직은 Fake/Stub Repository를 주입하여 외부 의존 없이 검증. `@SpringBootTest` 없이 순수 도메인 테스트
- MySQL/Redis는 TestContainers 자동 구동 (프로파일: `test`), 타임존: `Asia/Seoul`
- 상세 테스트 작성 절차는 `/red`, `/e2e` 스킬 참고

## 개발 방법론: TDD (Kent Beck) + Tidy First

### 증강 코딩 원칙

- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만 하며, 최종 승인된 사항을 기반으로 작업 수행
- **임의 작업 금지**: 반복적 동작, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행하지 않는다
- **설계 주도권**: AI는 임의판단하지 않고 방향성을 제안할 수 있으나, 개발자 승인 후 수행
- **가정 명시**: 불확실한 부분은 가정을 명시적으로 나열하고 질문한다. 조용히 하나를 선택하지 않는다
- **모호함 표면화**: 해석이 여러 개 가능하면 선택지와 영향도를 함께 제시한다. 혼란스러우면 멈추고 무엇이 혼란스러운지 명명한다

### 수술적 변경 원칙

요청받은 것만 변경한다. 변경된 모든 줄은 사용자의 요청으로 추적 가능해야 한다.

- 인접한 코드, 주석, 포맷을 "개선"하지 않는다
- 깨지지 않은 것을 리팩토링하지 않는다
- 본인이 다르게 했을지라도 기존 스타일을 따른다
- 관련 없는 dead code를 발견하면 언급만 하고 삭제하지 않는다
- 내 변경으로 인해 사용되지 않게 된 import/변수/함수만 정리한다

### TDD 사이클: Red → Green → Refactor

- **Red** → **Green** → **Refactor** 순서를 반드시 따른다
- 구조적 변경과 행위적 변경을 절대 같은 커밋에 섞지 않는다 (Tidy First)
- 둘 다 필요하면 구조적 변경을 먼저 수행한다
- 각 단계의 상세 절차는 `/red`, `/green`, `/refactor` 스킬 참고
- **plan 작성 시에도 TDD 형식을 따른다**: 각 구현 항목을 `[RED] 테스트 → [GREEN] 구현` 쌍으로 구성한다. Fake Repository 생성 항목도 포함한다

### 코드 품질 기준

- 중복을 철저히 제거한다
- 이름과 구조로 의도를 명확히 표현한다
- 의존성을 명시적으로 드러낸다
- 메서드는 작게, 단일 책임으로 유지한다
- 가능한 가장 단순한 해결책을 사용한다

### 병렬 작업 워크플로우

생산성 = 속도 × 인지 안정성 × 병렬 처리량. 인지 부하를 통제한 상태에서 유지 가능한 처리량을 높인다.

**Phase 1 — 수렴 (Converge)**: 요구사항 정제 → 설계 확정. 이 구간에서 Q&A를 집중하여 모호함을 전부 해소한다.
**Phase 2 — 발산 (Diverge)**: plan.md 기반 위임 + 체크포인트 검수. 에이전트는 자가 검증(lint+test) 후 보고한다.

**Self-Validation 원칙**: 보고 전에 할 수 있는 검증은 전부 수행한다.
1. `ktlintFormat` → `ktlintCheck` (포맷 + 린트)
2. `./gradlew test` (전체 테스트)
3. 통과 상태에서만 보고. 실패 시 자가 수정 시도.

**보고 포맷**: 모든 작업 보고에 아래 구조를 따른다.
- **Change**: 무엇을 변경했는지 (3줄 요약)
- **Validation**: 어떤 검증을 통과했는지
- **Risk/Ambiguity**: 개발자가 판단해야 할 모호한 부분. 임의 결정한 네이밍, 가정한 비즈니스 로직, 사용하지 않은 에러 처리 등을 반드시 명시. 정말 없으면 "Perfectly aligned with spec".

## 작업 환경

- **OS**: Windows 또는 WSL (Ubuntu)
- Windows 환경에서는 Linux/Unix 전용 명령어(`chmod`, `ln -s` 등) 사용 금지
- WSL 환경에서는 Unix 명령어 사용 가능하나, 파일 조작은 Claude Code 전용 도구(Read, Edit, Write, Grep, Glob 등)를 우선 사용할 것

## 주의사항

### Never Do

- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않게 코드 작성 금지
- println 코드 남기지 않는다

### Recommendation

- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API는 `http/*.http` 파일에 분류하여 작성

### Priority

1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지

## 브랜치 및 PR 규칙

- 브랜치: `main`에서 분기 (예: `feature/round2-design`)
- 커밋 접두사: `feat:` | `refactor:` | `fix:` | `test:` | `docs:` | `chore:`
- 커밋 상세 절차는 `/commit` 스킬 참고
- PR 제목: `[N주차] 제출 내용`, 리뷰 포인트 필수 작성
