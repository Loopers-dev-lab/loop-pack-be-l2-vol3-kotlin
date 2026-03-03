# CLAUDE.md

LLM의 흔한 코딩 실수를 줄이기 위한 행동 가이드라인. 프로젝트별 지시사항과 함께 적용한다.

**트레이드오프:** 이 가이드라인은 속도보다 **신중함(caution)**에 무게를 둔다. 사소한 작업에는 스스로 판단하라.

## §0. 선호하는 언어

**한국어(Korean)**로 응답하라. 코드는 예외이다.
커밋 메시지 또한 되도록 한글로 명확하게 기능을 설명할 수 있어야 한다.

## §1. 코딩 전에 생각하라

**추측하지 마라. 혼란을 숨기지 마라. 트레이드오프를 드러내라.**

구현하기 전에:
**가정(assumptions)**을 명시적으로 밝혀라. 확실하지 않으면 질문하라.
여러 해석이 가능하면 모두 제시하라 — 조용히 하나를 고르지 마라.
더 **단순한 접근법(simpler approach)**이 있다면 말하라. 필요하면 반론을 제기하라.
뭔가 불명확하면 멈춰라. 무엇이 헷갈리는지 짚어라. 질문하라.
**읽기 우선(Read Before Write)**: 수정 대상 코드를 반드시 먼저 읽어라. 읽지 않고 수정하지 마라.
**환각 금지(No Hallucination)**: 존재하지 않는 API, 패키지, 파일 경로, 설정 옵션을 지어내지 마라. 확실하지 않으면 먼저 확인하라.

## §2. 단순함 우선 (Simplicity First)

**문제를 해결하는 최소한의 코드. 짐작으로 쓴 코드는 금지.**

요청받지 않은 기능을 추가하지 마라.
한 번만 쓰이는 코드에 **추상화(abstraction)**를 만들지 마라.
요청되지 않은 "유연성"이나 "설정 가능성"을 넣지 마라.
발생할 수 없는 시나리오에 대한 **에러 핸들링(error handling)**을 하지 마라.
200줄로 썼는데 50줄로 가능하면 다시 작성하라.

자문하라: "시니어 엔지니어가 이거 **오버엔지니어링(over-engineering)**이라고 할까?" 그렇다면 단순화하라.

## §3. 외과적 변경 (Surgical Changes)

**건드려야 할 것만 건드려라. 본인이 남긴 부산물만 정리하라.**

기존 코드를 수정할 때:
주변 코드, 주석, 포매팅을 "개선"하지 마라.
고장나지 않은 것을 **리팩토링(refactoring)**하지 마라.
다르게 할 수 있더라도 **기존 스타일(existing style)**에 맞춰라.
관련 없는 **데드코드(dead code)**를 발견하면 언급만 하라 — 삭제하지 마라.

본인의 변경으로 사용되지 않게 된 **import/변수/함수**는 제거하라.

검증 기준: **변경된 모든 라인은 사용자의 요청에 직접 연결**되어야 한다.

## §4. 목표 중심 실행 (Goal-Driven Execution)

**성공 기준을 정의하라. 검증될 때까지 **확장 사고(extended thinking)**로 자가점검하며 반복하라.**

작업을 **검증 가능한 목표(verifiable goals)**로 변환하라:
"유효성 검사 추가" → "잘못된 입력에 대한 테스트를 작성하고, 통과시켜라"
"버그 수정" → "재현하는 테스트를 작성하고, 통과시켜라"
"X 리팩토링" → "리팩토링 전후로 테스트가 통과하는지 확인하라"

다단계 작업에는 간략한 계획을 제시하라:

```
[단계] → 검증: [확인사항]
[단계] → 검증: [확인사항]
[단계] → 검증: [확인사항]
```

강한 **성공 기준(success criteria)**이 있으면 자율적으로 루프를 돌 수 있다. 약한 기준("되게 해줘")은 끊임없는 확인이 필요하다.

## §5. 점진적 실행 (Incremental Execution)

**대규모 변경을 한 번에 하지 마라. 작은 단위로 나눠서 각 단계마다 검증하라.**

여러 파일을 동시에 변경하기보다, 한 단위씩 변경하고 **중간 검증(intermediate verification)**을 수행하라.
뭔가 깨졌을 때 원인을 특정할 수 있도록 **변경 범위(blast radius)**를 최소화하라.

## §6. 실패 대응 (Failure Response)

**에러가 나면 원인부터 파악하라. 같은 시도를 반복하지 마라.**

에러 발생 시 증상만 고치지 말고 **근본 원인(root cause)**을 분석하라.
같은 명령을 재시도하기 전에 왜 실패했는지 먼저 이해하라.
방향이 틀렸으면 고치려 하지 말고 과감히 버리고 다시 작성하라. **매몰 비용(sunk cost)**에 집착하지 마라.

## §7. 피드백 반영 (Self-Correction)

**사용자가 실수를 지적하면 기록하라.**

1회 지적 → **MEMORY.md**에 교훈을 기록하라.
2회 이상 반복 → **`~/.claude/rules/corrections.md`**로 승격하고, 사용자에게 알려라.

기록 포맷:

```
### [금지 행동 한 줄 요약]
상황: [어떤 맥락에서 발생했는지]
교훈: [앞으로 어떻게 해야 하는지]
```

---

**이 가이드라인이 작동하고 있다면:** diff에 불필요한 변경이 줄고, 과도한 복잡성으로 인한 재작성이 줄고, 실수 후가 아니라 구현 전에 확인 질문이 나온다.

## Project Overview
This is a E-Commerce platform backend (multi-module Gradle project).
 - Apps: commerce-api (REST API), commerce-batch (Batch), commerce-streamer (Kafka Stream)
 - Architecture: DDD-inspired layered architecture

## Tech Stack
 - Language: Kotlin 2.0.20 (JVM 21)
 - Framework: Spring Boot 3.4.4
 - Build: Gradle (Kotlin DSL)
 - Database: MySQL 8.0
 - ORM: Spring Data JPA + QueryDSL
 - Cache: Redis (Master-Replica, Lettuce)
 - Messaging: Apache Kafka
 - API Docs: SpringDoc OpenAPI (Swagger)
 - Testing: JUnit 5, SpringMockK, TestContainers, Instancio
 - Lint: KtLint
 - Monitoring: Prometheus + Grafana

## Project Structure Rules
 - apps/                        # Deployable Spring Boot applications
   - commerce-api/              # REST API application
   - commerce-batch/            # Batch processing application
   - commerce-streamer/         # Kafka stream processing application
 - modules/                     # Reusable infrastructure modules
   - jpa/                       # JPA, QueryDSL, MySQL config
   - redis/                     # Redis connection & template config
   - kafka/                     # Kafka producer/consumer config
 - supports/                    # Cross-cutting concern modules
   - jackson/                   # JSON serialization
   - logging/                   # Logback config
   - monitoring/                # Prometheus metrics & health checks

 Each app follows a layered package structure:
   - interfaces/api/            # Controllers, API specs, Request/Response DTOs
   - application/               # UseCases, Criteria (in), Result (out)
   - domain/                    # Entities, domain services, repository interfaces, Command (in), Info (out)
   - infrastructure/            # Repository implementations, JPA repositories
   - support/                   # CoreException, ErrorType, utilities

## Architecture Rules
 - Interfaces (Controller) layer
   - Thin layer: no business logic, no orchestration
   - DTO ↔ Entity conversion only
   - Acts as Anti-Corruption Layer between HTTP and Domain

 - Domain (Service + Entity) layer
   - Domain Service: single-domain operations only, depends on own domain's repositories
   - Must NOT depend on other domain's repositories or services
   - Models business concepts and rules
   - Avoid anemic domain models
   - Must be independent of infrastructure concerns
   - Domain Entity: owns validation (init block), state changes, business rules

 - Application (UseCase) layer
   - Single domain → Controller calls Domain Service directly (UseCase unnecessary)
   - Multiple domains → UseCase orchestrates Domain Services (not Repositories)

 - Infrastructure layer
   - Handles persistence only (Repository implementations)
   - No domain rules or policy logic

## Naming Conventions
 - Controllers: `{Entity}V{Version}Controller` (e.g., ExampleV1Controller)
 - API Specs: `{Entity}V{Version}ApiSpec` (OpenAPI interface)
 - API DTOs: `{Entity}V{Version}Dto` (nested data classes with `from()` factory)
 - UseCases: `{Admin|User}{Action}{Entity}UseCase` (e.g., AdminRegisterBrandUseCase)
 - Application Criteria: `{Action}{Entity}Criteria` (UseCase input, e.g., RegisterBrandCriteria)
 - Application Results: `{Action}{Entity}Result` (UseCase output, e.g., RegisterBrandResult, with `from()` factory)
 - Domain Info: `{Entity}Info` (Service output, e.g., BrandInfo, with `from()` factory)
 - Domain Commands: `{Action}{Entity}Command` (Service input, e.g., CompleteOrderCommand)
 - Domain Services: `{Entity}Service` (noun-based, e.g., ExampleService)
 - Domain Entities: `{Entity}Model` (e.g., ExampleModel, extends BaseEntity)
 - Domain Repositories: `{Entity}Repository` (interface in domain layer)
 - JPA Repositories: `{Entity}JpaRepository` (infrastructure layer)
 - Repository Impls: `{Entity}RepositoryImpl` (infrastructure layer)

## Coding Style
 - Kotlin idioms: data classes for DTOs, companion object `from()` for conversions, `let` chains
 - Protected setters on domain entity properties for immutability
 - Init blocks for domain entity validation
 - Max line length: 130 (off for test files)
 - No wildcard imports (enforced via .editorconfig)
 - KtLint with INTELLIJ_IDEA code style
 - Prefer explicit code over clever abstractions
 - Favor readability over DRY
 - Prefer object-oriented design over procedural style
 - Model business logic using Domain-Driven Design principles

## Domain & DTO Boundary

### Layer-specific DTO rules
 - **interfaces/api/** — API DTOs (`V1Dto`): Controller only, **must NOT leak beyond Controller**
 - **application/** — Criteria (UseCase input), Result (UseCase output)
 - **domain/** — Command (Service input), Info (Service output)
 - Controllers act as Anti-Corruption Layer between HTTP and Domain

### Data flow
 - Controller → UseCase: `V1Dto.Request` → `Criteria`
 - UseCase → Controller: `Result` → `V1Dto.Response` (wraps in `ApiResponse`)
 - UseCase → Service: `Command`
 - Service → UseCase: `Info`
 - Info conversion: `Model` → `Info` via `Info.from(model)`
 - Result conversion: `Info` → `Result` via `Result.from(info)`

### id-only cases
 - UseCase/Service that only need a single id: pass `Long` directly, do not wrap in Criteria/Command
 - e.g., `UseCase<Long, GetBrandResult>`, `fun findById(id: Long): BrandInfo`

### Bad Example (DO NOT DO THIS)
```kotlin
// Service accepting Request DTO - architectural violation
fun createOrder(dto: OrderV1Dto.CreateRequest): OrderInfo

// Service accepting Domain Entity - use Command instead
fun register(user: UserModel): UserModel

// All logic in service, anemic domain model
class OrderService {
    fun completeOrder(userId: Long, productId: Long) {
        val user = userRepository.findById(userId)
        val product = productRepository.findById(productId)

        if (product.stock <= 0) throw CoreException(...)  // domain logic leaked
        product.stock--                                    // direct field mutation

        if (user.point < product.price) throw CoreException(...)
        user.point -= product.price

        orderRepository.save(Order(user, product))
    }
}
```

### Good Example
```kotlin
// ── Application layer ──
data class CompleteOrderCriteria(
    val userId: Long,
    val productId: Long
)

data class CompleteOrderResult(
    val id: Long,
) {
    companion object {
        fun from(info: OrderInfo): CompleteOrderResult =
            CompleteOrderResult(id = info.id)
    }
}

class CompleteOrderUseCase(
    private val orderService: OrderService,
) : UseCase<CompleteOrderCriteria, CompleteOrderResult> {
    override fun execute(criteria: CompleteOrderCriteria): CompleteOrderResult {
        val command = CompleteOrderCommand(userId = criteria.userId, productId = criteria.productId)
        val info = orderService.completeOrder(command)
        return CompleteOrderResult.from(info)
    }
}

// ── Domain layer ──
data class CompleteOrderCommand(
    val userId: Long,
    val productId: Long
)

class OrderService(
    private val userReader: UserReader,
    private val productReader: ProductReader,
    private val orderRepository: OrderRepository,
) {
    fun completeOrder(command: CompleteOrderCommand): OrderInfo {
        val user = userReader.get(command.userId)
        val product = productReader.get(command.productId)

        product.decreaseStock()    // domain logic in entity
        user.pay(product.price)    // domain logic in entity

        val order = OrderModel(user, product)
        val saved = orderRepository.save(order)
        return OrderInfo.from(saved)
    }
}

// Domain entity with business logic
class ProductModel(...) : BaseEntity() {
    fun decreaseStock() {
        if (stock <= 0) throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        stock--
    }
}
```

## Domain Layer Rules
 - Domain entities extend `BaseEntity` (id, createdAt, updatedAt, deletedAt)
 - Validation logic in entity `init` block using `CoreException`
 - Business methods on entity (e.g., `update()`, `delete()`, `restore()`)
 - Soft delete via `BaseEntity.delete()` / `BaseEntity.restore()`
 - Avoid anemic domain models

## Error Handling
 - `CoreException(errorType: ErrorType, customMessage: String?)` for all business errors
 - `ErrorType` enum: BAD_REQUEST(400), UNAUTHORIZED(401), NOT_FOUND(404), CONFLICT(409), INTERNAL_ERROR(500)
 - `ApiControllerAdvice` catches exceptions and converts to `ApiResponse.fail()`
 - Response format: `ApiResponse<T>` with `meta(result, errorCode, errorMessage)` and `data`

## Testing
 - Unit tests: `{Entity}ModelTest` - no Spring context, JUnit 5 + @Nested + @DisplayName
 - Integration tests: `{Entity}ServiceIntegrationTest` - @SpringBootTest, constructor injection
 - E2E tests: `{Entity}V{Version}ApiE2ETest` - @SpringBootTest(RANDOM_PORT), TestRestTemplate
 - Batch tests: `{Job}E2ETest` - @SpringBootTest + @SpringBatchTest
 - Database cleanup: `DatabaseCleanUp.truncateAllTables()` in `@AfterEach`
 - Test containers: MySQL 8.0 (utf8mb4) via TestContainers
 - Test data: Instancio for fixture generation
 - Max parallel forks: 1 (sequential execution)
 - Test profile: `test` (ddl-auto: create)
 - All tests follow 3A pattern (Arrange - Act - Assert)
 - Arrange variables use `expected` prefix for expected values (e.g., `expectedUsername`, `expectedName`)
 - Method parameters must use variables, not inline string literals
 - Common test values extracted to `companion object` constants (e.g., `DEFAULT_USERNAME`, `DEFAULT_PASSWORD`)

## Development Rules

### Augmented Coding Workflow
 - **Principle**: Direction and key decisions are up to the developer. AI may only propose; developer has final approval.
 - **Intervention**: Developer intervenes when AI repeats actions, implements unrequested features, or deletes tests without approval.
 - **Design Ownership**: AI must not make arbitrary decisions. Proposals require developer approval before execution.

### TDD Workflow (Red > Green > Refactor)
 - **Red Phase**: Write failing tests first that express the requirement.
 - **Green Phase**: Write the minimum code to make all Red Phase tests pass. No over-engineering.
 - **Refactor Phase**: Remove unnecessary code, improve quality, ensure all tests still pass.

## Cautions

### Never Do
 - Write non-functional code or implement with unnecessary mock data
 - Write null-unsafe code
 - Leave `println` statements in code
 - Delete or modify existing tests without developer approval
 - Guess business rules when requirements are ambiguous

### Recommendation
 - Write E2E tests that call actual APIs
 - Design reusable objects
 - Propose performance optimization alternatives
 - Document completed APIs in `.http/**.http` files

### Priority
 1. Only consider solutions that actually work
 2. Ensure null-safety and thread-safety
 3. Design for testability
 4. Analyze existing code patterns and maintain consistency

## Working Style
 - Small, reviewable commits
 - One logical change per PR
 - Explain "why" more than "what"
