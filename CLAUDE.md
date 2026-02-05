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
   - application/               # Application DTOs (Info)
   - domain/                    # Entities, domain services, repository interfaces
   - infrastructure/            # Repository implementations, JPA repositories
   - support/                   # CoreException, ErrorType, utilities

## Architecture Rules
 - Interfaces (Controller) layer
   - Thin layer: no business logic, no orchestration
   - DTO ↔ Entity conversion only
   - Acts as Anti-Corruption Layer between HTTP and Domain

 - Domain (Service + Entity) layer
   - Encapsulates domain-level operations
   - Must not depend on other services
   - Models business concepts and rules
   - Avoid anemic domain models
   - Must be independent of infrastructure concerns
   - Domain Service: coordinates entities/repositories, handles operations requiring collaborators
   - Domain Entity: owns validation (init block), state changes, business rules

 - Infrastructure layer
   - Handles persistence only (Repository implementations)
   - No domain rules or policy logic

## Naming Conventions
 - Controllers: `{Entity}V{Version}Controller` (e.g., ExampleV1Controller)
 - API Specs: `{Entity}V{Version}ApiSpec` (OpenAPI interface)
 - API DTOs: `{Entity}V{Version}Dto` (nested data classes with `from()` factory)
 - Application DTOs: `{Entity}Info` (e.g., ExampleInfo, with `from()` factory)
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
 - API DTOs (`V1Dto`) live in `interfaces/api/` layer - **Controller only**
 - Application DTOs (`Info`) live in `application/` layer
 - Command objects (`{Action}Command`) live in `domain/` layer
 - Controller converts Model → Info → API DTO using `from()` factory, then wraps in `ApiResponse`
 - **Request/Response DTOs must NOT leak beyond Controller**
 - Services accept: identifiers (id) for simple lookups, Command objects for all other operations (no domain entities)
 - Controllers act as Anti-Corruption Layer between HTTP and Domain

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
// Command object in domain layer
data class CompleteOrderCommand(
    val userId: Long,
    val productId: Long
)

// Service with injected dependencies, domain logic delegated to entities
class OrderService(
    private val userReader: UserReader,
    private val productReader: ProductReader,
    private val orderRepository: OrderRepository,
) {
    fun completeOrder(command: CompleteOrderCommand): OrderModel {
        val user = userReader.get(command.userId)
        val product = productReader.get(command.productId)

        product.decreaseStock()    // domain logic in entity
        user.pay(product.price)    // domain logic in entity

        return orderRepository.save(OrderModel(user, product))
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
