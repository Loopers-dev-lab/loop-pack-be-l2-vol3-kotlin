# CLAUDE.md — 프로젝트 규칙 & 코딩 컨벤션

## 1. 프로젝트 개요

커머스 API 서버 (Spring Boot 3.4 + Kotlin 2.0 + Java 21)

**멀티모듈 구조:**
```
apps/commerce-api          # 메인 API (BootJar)
apps/commerce-streamer     # 스트리머
apps/commerce-batch        # 배치
modules/jpa|redis|kafka    # 공통 인프라 모듈
supports/jackson|logging|monitoring
```

**빌드/테스트:** Gradle + KAPT, JUnit 5, MockK, ArchUnit, Testcontainers

---

## 2. 아키텍처 규칙

### 계층 구조 (DIP)

```
interfaces/api/ → application/ → domain/ ← infrastructure/
```

| 계층 | 패키지 | 책임 | Spring 어노테이션 |
|------|--------|------|------------------|
| Domain | `domain/{aggregate}/` | 비즈니스 규칙, 불변식 | 없음 (순수 POJO) |
| Application | `application/{aggregate}/` | 유스케이스, 트랜잭션 경계 | `@Component`/`@Service`, `@Transactional` |
| Infrastructure | `infrastructure/{aggregate}/` | 영속성, 외부 시스템 어댑터 | `@Repository`, `@Entity`, `@Component` |
| Interfaces | `interfaces/api/v1/{aggregate}/` | HTTP 요청/응답 | `@RestController` |

### 핵심 제약 (ArchUnit 강제 — `ArchitectureTest.kt`)

- **domain 패키지는 infrastructure, interfaces, application에 의존하지 않는다**
- **`reconstitute()`는 infrastructure 계층에서만 호출 가능** (Mapper 전용)

---

## 3. 도메인 객체 패턴

### Aggregate Root (참조: `domain/user/User.kt`)

```kotlin
class User private constructor(    // private constructor
    val persistenceId: Long?,      // 저장 전 null, 저장 후 Long
    val loginId: LoginId,          // 원시타입 대신 VO
    // ...
) {
    // 상태 변경 → 새 인스턴스 반환 (Immutable)
    fun changePassword(/*...*/): User = User(/*...*/)

    companion object {
        fun register(/*...*/): User    // 생성 팩토리 (persistenceId = null)
        fun reconstitute(/*...*/): User // DB 복원 (Infrastructure Mapper 전용)
    }
}
```

**규칙:**
- `create()` 또는 도메인 동사(`register`) 팩토리: `persistenceId = null`
- `reconstitute()`: `persistenceId = Long` (Mapper에서만 호출)
- 상태 변경 시 항상 새 인스턴스 반환 (불변성)
- Aggregate 간 참조는 ID로만 (직접 참조 X)

### Value Object (참조: `domain/user/Email.kt`)

```kotlin
data class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "이메일은 빈 문자열일 수 없습니다." }
        require(FORMAT_REGEX.matches(value)) { "올바른 이메일 형식이 아닙니다." }
    }
    companion object {
        private val FORMAT_REGEX = Regex("^[^@]+@[^@]+\\.[^@]+$")
    }
}
```

**규칙:**
- `data class` + `init` 블록 `require()` 검증
- 보안 민감 VO (Password 등)는 `class` + `private constructor` + `toString()` 마스킹
- 정규식은 `companion object`에 `private val`

### Enum

- `{Domain}Status.kt` (ACTIVE, INACTIVE 등)

---

## 4. 레이어별 코딩 컨벤션

### Repository Interface (domain 계층)

```kotlin
// domain/{aggregate}/{Aggregate}Repository.kt
interface UserRepository {
    fun save(user: User): Long
    fun findById(id: Long): User?
    fun findByLoginId(loginId: LoginId): User?
    fun existsByLoginId(loginId: LoginId): Boolean
}
```

- 반환 타입은 도메인 객체 (JPA Entity 아님)
- 인자로 VO 타입 사용
- 참조: `domain/user/UserRepository.kt`

### JPA Entity (infrastructure 계층)

```kotlin
// infrastructure/{aggregate}/{Aggregate}Entity.kt
@Entity
@Table(name = "users")
class UserEntity(
    id: Long?,
    @Column(name = "login_id", nullable = false, unique = true, length = 10)
    val loginId: String,
    // ...
) : BaseEntity() {
    init { this.id = id }
}
```

- `BaseEntity` 상속 (id, createdAt, updatedAt, deletedAt — `modules/jpa`)
- Entity 필드는 원시 타입 (String, Long, LocalDate 등)
- `init { this.id = id }` 패턴으로 ID 주입
- 참조: `infrastructure/user/UserEntity.kt`

### Mapper (infrastructure 계층)

```kotlin
// infrastructure/{aggregate}/{Aggregate}Mapper.kt
object UserMapper {
    fun toDomain(entity: UserEntity): User {
        val id = requireNotNull(entity.id) { "UserEntity.id가 null입니다." }
        return User.reconstitute(
            persistenceId = id,
            loginId = LoginId(entity.loginId),  // String → VO
            // ...
        )
    }
    fun toEntity(domain: User): UserEntity {
        return UserEntity(
            id = domain.persistenceId,
            loginId = domain.loginId.value,     // VO → String
            // ...
        )
    }
}
```

- `object` 싱글톤
- `toDomain()` 안에서 `reconstitute()` 호출
- `toEntity()` 안에서 VO → 원시타입 변환
- 참조: `infrastructure/user/UserMapper.kt`

### Repository 구현체 (infrastructure 계층)

```kotlin
// infrastructure/{aggregate}/{Aggregate}RepositoryImpl.kt
@Repository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): Long {
        val entity = UserMapper.toEntity(user)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) { "User 저장 실패: id가 생성되지 않았습니다." }
    }
    override fun findByLoginId(loginId: LoginId): User? {
        return jpaRepository.findByLoginId(loginId.value)
            ?.let { UserMapper.toDomain(it) }
    }
}
```

- `@Repository` + domain Repository interface 구현
- JpaRepository에 위임, Mapper로 변환
- 참조: `infrastructure/user/UserRepositoryImpl.kt`

### UseCase (application 계층)

```kotlin
// application/{aggregate}/{Action}UseCase.kt
@Component
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun register(command: RegisterUserCommand): Long {
        val loginId = LoginId(command.loginId)
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 사용 중인 로그인 ID입니다: ${loginId.value}")
        }
        val user = User.register(/*...*/)
        return userRepository.save(user)
    }
}
```

- 쓰기: `@Component` + `@Transactional`
- 읽기: `@Service` + `@Transactional(readOnly = true)`
- Command 객체로 입력 수신 (원시 타입, VO 변환은 UseCase 내부)
- Command의 `toString()`에서 비밀번호 마스킹
- `CoreException(ErrorType.XXX, "메시지")`로 비즈니스 예외
- 참조: `application/user/RegisterUserUseCase.kt`

### Controller (interfaces 계층)

```kotlin
// interfaces/api/v1/{aggregate}/{Aggregate}Controller.kt
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val registerUserUseCase: RegisterUserUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<CreateUserResponse> {
        val id = registerUserUseCase.register(request.toCommand())
        return ApiResponse.success(CreateUserResponse(id))
    }
}
```

- Request/Response DTO는 같은 패키지에 별도 파일
- Request: `data class` + `@field:` Bean Validation + `toCommand()` 메서드
- Response: `data class` + `companion object { fun from() }` 팩토리
- `ApiResponse.success(data)` 래핑
- `@AuthenticatedUser authUser: AuthUser`로 인증 사용자 주입
- 참조: `interfaces/api/v1/user/UserController.kt`

---

## 5. 에러 처리

```kotlin
// support/error/
enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),
}

class CoreException(
    val errorType: ErrorType,
    val customMessage: String? = null,
) : RuntimeException(customMessage ?: errorType.message)
```

**ApiControllerAdvice 에러 매핑:**
- `CoreException` → ErrorType의 HTTP 상태
- `IllegalArgumentException` → 400 (VO 검증 실패)
- `MethodArgumentNotValidException` → 400 (Bean Validation 실패)
- `DataIntegrityViolationException` → 409 (DB 제약 위반)
- `NoResourceFoundException` → 404
- `Throwable` → 500

---

## 6. 테스트 컨벤션

### 도메인 단위 테스트

```kotlin
// test/domain/{aggregate}/{Class}Test.kt
class UserTest {
    @Test
    fun `register로 생성한 User의 id는 null이어야 한다`() {  // 백틱 한글
        val user = createUser()
        assertThat(user.persistenceId).isNull()              // AssertJ
    }

    private fun createUser(): User = User.register(/*...*/)  // 헬퍼

    companion object {
        private const val LOGIN_ID = "testuser"              // 상수
    }
}
```

### 통합 테스트 (UseCase)

```kotlin
@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RegisterUserUseCaseIntegrationTest { /*...*/ }
```

### E2E 테스트 (API)

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserApiE2ETest {
    @Nested
    inner class Register { /*...*/ }  // @Nested inner class 그룹핑
}
```

**테스트 규칙:**
- 테스트명: 백틱 한글 메서드명
- 상수: `companion object`에 `private const val`
- 헬퍼: `private fun create{Object}()` 패턴
- assertion: AssertJ (`assertThat`, `assertThatThrownBy`)
- 도메인 테스트: Fake/Stub 사용 (`TestPasswordEncoder`)
- 통합/E2E: Testcontainers MySQL + `@Sql` 정리

---

## 7. 설계 문서

설계 문서는 `docs/design/` 하위:
- `01-requirements.md` — 요구사항 명세
- `02-sequence-diagrams.md` — 비즈니스 시퀀스
- `02-sequence-diagram-tech.md` — 동시성 전략 (SELECT FOR UPDATE, INSERT IGNORE 등)
- `03-class-diagram.md` — 도메인 모델 (Mermaid)
- `04-erd.md` — ERD 및 테이블 설계

**`03-class-diagram.md`를 충실히 따른다** — 도메인 구현의 근거 문서.

재고 차감/좋아요 카운트: 도메인 메서드가 아님, Repository 레벨 원자적 UPDATE로 처리.

---

## 8. Git & 코드 스타일

- ktlint pre-commit hook 적용
- 브랜치: `feat/` 접두어
- 커밋 메시지: `feat:`, `fix:`, `refactor:`, `docs:`, `test:` 접두어
