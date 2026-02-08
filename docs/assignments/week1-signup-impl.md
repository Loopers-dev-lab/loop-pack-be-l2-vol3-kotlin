# Week 1: 회원가입 API 구현 가이드

> 회원가입 API를 TDD 방식으로 구현하기 위한 상세 가이드입니다.

## 검증

- 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- 린트 통과: `./gradlew ktlintCheck`
- 패턴 참조: example 패키지 코드 패턴을 따른다

---

### 범위

| 포함                            | 미포함 (이번 범위 밖)      |
|-------------------------------|--------------------|
| 회원가입 API (POST /api/v1/users) | 이메일 중복 체크/인증       |
| 필드 유효성 검증 + 예외 처리             | 비밀번호 찾기/재설정        |
| TDD 구현 가이드 (Outside-In)       | 소셜 로그인, 회원 탈퇴      |
|                               | 회원 정보 수정 (비밀번호 제외) |
|                               | 로그인 시도 제한/계정 잠금    |

### 공통 가정

- 모든 도메인 엔티티는 `BaseEntity`(id, createdAt, updatedAt, deletedAt) 상속
- TLS 등 기본 보안은 인프라 가정, 이번 범위 밖

### 기술 스택

| 항목    | 기술                                      |
|-------|-----------------------------------------|
| 암호화   | BCrypt (spring-security-crypto)         |
| 검증    | Jakarta Validation (@Valid) + Domain 로직 |
| 응답 래퍼 | ApiResponse (프로젝트 표준)                   |

### 의존성 추가

```kotlin
// apps/commerce-api/build.gradle.kts
implementation("org.springframework.security:spring-security-crypto")
```

---

## 1. API Specification

### 엔드포인트

| 항목     | 값             |
|--------|---------------|
| Method | POST          |
| Path   | /api/v1/users |
| 인증     | 불필요           |

### Request Schema

```json
{
  "loginId": "string (4~20자, 영문+숫자만)",
  "password": "string (8~16자, 영문대/소문자+숫자+특수문자, 생년월일 불포함)",
  "name": "string (2~15자, 한글만)",
  "birthDate": "string (yyyy-MM-dd, 과거 날짜만)",
  "email": "string (이메일 형식)"
}
```

### Response Schema

**성공 (201 Created)**

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "loginId": "string"
  }
}
```

**실패 (4xx)**

```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "ERROR_CODE",
    "message": "에러 메시지"
  },
  "data": null
}
```

### HTTP Status Codes

| Status          | 상황                    |
|-----------------|-----------------------|
| 201 Created     | 회원가입 성공               |
| 400 Bad Request | 유효성 검증 실패 (형식, 규칙 위반) |
| 409 Conflict    | 중복 loginId            |

---

## 2. 예외 처리 명세

### 추가할 ErrorType

```kotlin
// ErrorType.kt에 추가

/** User 도메인 */
USER_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 ID입니다."),
USER_INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "USER_INVALID_LOGIN_ID", "로그인 ID는 영문 대소문자와 숫자만 사용할 수 있습니다."),
USER_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_INVALID_PASSWORD", "비밀번호는 영문, 숫자, 허용된 특수문자만 사용할 수 있습니다."),
USER_INVALID_NAME(HttpStatus.BAD_REQUEST, "USER_INVALID_NAME", "이름은 한글만 입력할 수 있습니다."),

/** 인증/인가 */
UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
```

### 예외 발생 상황 매핑

| 상황              | ErrorType               | HTTP Status | 발생 위치                | 메시지                                      |
|-----------------|-------------------------|-------------|----------------------|------------------------------------------|
| loginId 중복      | USER_DUPLICATE_LOGIN_ID | 409         | Application(Service) | "이미 사용 중인 로그인 ID입니다."                    |
| loginId 패턴 오류   | USER_INVALID_LOGIN_ID   | 400         | Domain(User)         | "로그인 ID는 영문 대소문자와 숫자만 사용할 수 있습니다."       |
| 비밀번호 패턴 오류      | USER_INVALID_PASSWORD   | 400         | Domain(User)         | "비밀번호는 영문, 숫자, 허용된 특수문자만 사용할 수 있습니다."    |
| 비밀번호에 생년월일 포함   | USER_INVALID_PASSWORD   | 400         | Domain(User)         | customMessage: "비밀번호에 생년월일을 포함할 수 없습니다." |
| name 패턴 오류      | USER_INVALID_NAME       | 400         | Domain(User)         | "이름은 한글만 입력할 수 있습니다."                    |
| loginId 길이 오류   | Bad Request             | 400         | DTO(@Valid)          | MethodArgumentNotValidException          |
| password 길이 오류  | Bad Request             | 400         | DTO(@Valid)          | MethodArgumentNotValidException          |
| name 길이 오류      | Bad Request             | 400         | DTO(@Valid)          | MethodArgumentNotValidException          |
| birthDate 미래 날짜 | Bad Request             | 400         | DTO(@Valid)          | MethodArgumentNotValidException          |
| email 형식 오류     | Bad Request             | 400         | DTO(@Valid)          | MethodArgumentNotValidException          |
| birthDate 형식 오류 | Bad Request             | 400         | JSON 파싱              | HttpMessageNotReadableException          |
| 필수 필드 누락        | Bad Request             | 400         | JSON 파싱              | HttpMessageNotReadableException          |
| 잘못된 JSON 형식     | Bad Request             | 400         | JSON 파싱              | HttpMessageNotReadableException          |

### ApiControllerAdvice 수정

`HttpMessageNotReadableException` 핸들러는 이미 존재함. `MethodArgumentNotValidException` 핸들러만 추가 필요:

```kotlin
@ExceptionHandler
fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<*>> {
    val errorMessage = e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage
        ?: "요청 값이 유효하지 않습니다."
    return failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = errorMessage)
}
```

---

## 3. 유효성 검증 규칙 상세

### loginId

- **DTO**: `@NotBlank` + `@Size(4, 20)`
- **Domain**: 영문(대소문자) + 숫자만 허용. 정규식: `^[a-zA-Z0-9]+$`

### password

- **DTO**: `@NotBlank` + `@Size(8, 16)`
- **Domain (패턴)**: 허용문자 검증. 정규식: `^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{}|;:',.<>?/]+$`
- **Domain (규칙)**: 생년월일(yyyyMMdd, yyyy-MM-dd 형태 모두) 포함 불가

### name

- **DTO**: `@NotBlank` + `@Size(2, 15)`
- **Domain**: 한글만 허용. 정규식: `^[가-힣]+$`

### birthDate

- **DTO**: `@NotNull` + `@Past` (LocalDate, Jackson 파싱)

### email

- **DTO**: `@NotBlank` + `@Email`

---

## 4. 인수조건 (테스트 케이스)

### 인수조건 목록

| #     | 인수조건 (구체화)                                          | 요구사항 근거                 | 유형   |
|-------|-----------------------------------------------------|-------------------------|------|
| AC-1  | 유효한 회원 정보로 가입 요청 시 201 Created + loginId 반환         | 201 반환 + 응답에 loginId 포함 | 정상   |
| AC-2  | 이미 존재하는 loginId로 가입 요청 시 409 Conflict 반환            | 중복 loginId 시 409 반환     | 예외   |
| AC-3  | loginId가 영문+숫자 4~20자 규칙을 위반하면 400 반환                | 유효성 규칙                  | 예외   |
| AC-4  | password가 8~16자, 허용 문자 규칙을 위반하면 400 반환              | 비밀번호 8~16자 위반 시 400 반환  | 예외   |
| AC-5  | password에 생년월일(yyyyMMdd 또는 yyyy-MM-dd)이 포함되면 400 반환 | 생년월일 포함 시 400 반환        | 예외   |
| AC-6  | name은 한글만 허용, 2~15자 규칙을 위반하면 400 반환                 | 유효성 규칙                  | 예외   |
| AC-7  | birthDate가 과거 날짜가 아니면(오늘 포함) 400 반환                 | 미래 날짜 시 400 반환          | 예외   |
| AC-8  | email이 이메일 형식이 아니면 400 반환                           | 이메일 형식 위반 시 400 반환      | 예외   |
| AC-9  | 필수 필드가 누락되면 400 반환                                  | 암묵적 요구사항                | 예외   |
| AC-10 | 회원가입 성공 시 비밀번호가 BCrypt로 암호화되어 저장됨                   | BCrypt 암호화 저장           | 부수효과 |

### 인수조건별 테스트 케이스 도출

| 인수조건  | 케이스 유형 | 구체적 시나리오                      |
|-------|--------|-------------------------------|
| AC-1  | 정상     | 모든 필드 유효 → 201 + loginId      |
| AC-1  | 부수효과   | 저장 후 Repository에 존재 확인        |
| AC-2  | 예외     | 동일 loginId 2회 요청 → 409        |
| AC-3  | 경계값    | loginId 4자(최소) → 성공           |
| AC-3  | 경계값    | loginId 3자 → 실패               |
| AC-3  | 경계값    | loginId 20자(최대) → 성공          |
| AC-3  | 경계값    | loginId 21자 → 실패              |
| AC-3  | 예외     | 특수문자 포함 → 실패                  |
| AC-4  | 경계값    | password 8자(최소) → 성공          |
| AC-4  | 경계값    | password 7자 → 실패              |
| AC-4  | 예외     | 허용되지 않은 특수문자 → 실패             |
| AC-5  | 예외     | 생년월일 19900101 포함 → 실패         |
| AC-5  | 예외     | 생년월일 1990-01-01 포함 → 실패       |
| AC-6  | 정상     | 한글 이름 "홍길동" → 성공              |
| AC-6  | 예외     | 영문 포함 "Hong길동" → 실패           |
| AC-6  | 예외     | 숫자 포함 → 실패                    |
| AC-6  | 예외     | 공백 포함 → 실패                    |
| AC-6  | 경계값    | 2자(최소) → 성공                   |
| AC-6  | 경계값    | 1자 → 실패                       |
| AC-6  | 경계값    | 15자(최대) → 성공                  |
| AC-6  | 경계값    | 16자 → 실패                      |
| AC-7  | 경계값    | 오늘 날짜 → 실패                    |
| AC-7  | 예외     | 미래 날짜 → 실패                    |
| AC-8  | 예외     | @ 없음 → 실패                     |
| AC-8  | 예외     | 도메인 없음 → 실패                   |
| AC-9  | 예외     | 필수 필드 누락 → 실패                 |
| AC-10 | 부수효과   | 저장된 비밀번호 ≠ 원문, BCrypt matches |

### E2E Test (`UserV1ControllerSignUpE2ETest`)

```kotlin
@DisplayName("POST /api/v1/users - 회원가입 시나리오")
class UserV1ControllerSignUpE2ETest {

    @Test
    @DisplayName("회원가입 성공 - 201 Created 반환, 응답에 loginId 포함")
    fun signUp_success_returns201WithLoginId()

    @Test
    @DisplayName("잘못된 요청 시 400 Bad Request 반환")
    fun signUp_invalidRequest_returns400()

    @Test
    @DisplayName("중복 loginId 시 409 Conflict 반환")
    fun signUp_duplicateLoginId_returns409()
}
```

### Unit Test (`UserV1DtoTest` - Request DTO 유효성)

```kotlin
@DisplayName("UserV1Dto 유효성 검증")
class UserV1DtoTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Nested
    @DisplayName("loginId 검증")
    inner class LoginIdValidation { ... }

    @Nested
    @DisplayName("password 검증")
    inner class PasswordValidation { ... }

    @Nested
    @DisplayName("name 검증")
    inner class NameValidation { ... }

    @Nested
    @DisplayName("birthDate 검증")
    inner class BirthDateValidation { ... }

    @Nested
    @DisplayName("email 검증")
    inner class EmailValidation { ... }

    @Nested
    @DisplayName("필수 필드 검증")
    inner class RequiredFieldValidation { ... }
}
```

### Unit Test (`UserServiceTest`)

```kotlin
@DisplayName("UserService 회원가입")
class UserServiceTest {

    @Nested
    @DisplayName("성공")
    inner class Success {
        @Test
        @DisplayName("회원가입 성공 - UserInfo 반환")
        fun signUp_success_returnsUserInfo()

        @Test
        @DisplayName("회원가입 성공 - 비밀번호가 BCrypt로 암호화됨")
        fun signUp_success_passwordIsEncrypted()

        @Test
        @DisplayName("회원가입 성공 - Repository.save() 호출됨")
        fun signUp_success_callsRepositorySave()
    }

    @Nested
    @DisplayName("실패")
    inner class Failure {
        @Test
        @DisplayName("회원가입 실패 - 중복 loginId - CoreException(USER_DUPLICATE_LOGIN_ID)")
        fun signUp_duplicateLoginId_throwsException()

        @Test
        @DisplayName("회원가입 실패 - 비밀번호에 생년월일 포함 - CoreException(USER_INVALID_PASSWORD)")
        fun signUp_passwordContainsBirthDate_throwsException()

        @Test
        @DisplayName("회원가입 실패 - Repository 예외 발생 시 전파")
        fun signUp_repositoryFails_propagatesException()
    }
}
```

### Unit Test (`UserTest` - Domain)

```kotlin
@DisplayName("User 도메인")
class UserTest {

    @Nested
    @DisplayName("생성")
    inner class Create {
        @Test
        @DisplayName("User 생성 성공")
        fun create_success()

        @Test
        @DisplayName("생성 실패 - 비밀번호에 생년월일 포함(yyyyMMdd)")
        fun create_passwordContainsBirthDateCompact_throwsException()

        @Test
        @DisplayName("생성 실패 - 비밀번호에 생년월일 포함(yyyy-MM-dd)")
        fun create_passwordContainsBirthDateWithDash_throwsException()
    }

    @Nested
    @DisplayName("loginId 패턴 검증")
    inner class LoginIdPatternValidation {
        @Test
        @DisplayName("특수문자 포함 - 실패")
        fun create_loginIdWithSpecialChar_throwsException()

        @Test
        @DisplayName("한글 포함 - 실패")
        fun create_loginIdWithKorean_throwsException()
    }

    @Nested
    @DisplayName("password 패턴 검증")
    inner class PasswordPatternValidation {
        @Test
        @DisplayName("허용되지 않은 특수문자 포함 - 실패")
        fun create_passwordWithInvalidSpecialChar_throwsException()
    }

    @Nested
    @DisplayName("name 패턴 검증")
    inner class NamePatternValidation {
        @Test
        @DisplayName("숫자 포함 - 실패")
        fun create_nameWithNumber_throwsException()

        @Test
        @DisplayName("영문 포함 - 실패")
        fun create_nameWithEnglish_throwsException()

        @Test
        @DisplayName("공백 포함 - 실패")
        fun create_nameWithSpace_throwsException()
    }
}
```

### Integration Test (`UserRepositoryIntegrationTest`)

```kotlin
@DisplayName("회원 저장 및 조회")
@Nested
inner class SaveAndFind {
    @Test
    @DisplayName("회원 저장 및 조회 성공")
    fun save_andFindById_success()

    @Test
    @DisplayName("existsByLoginId - 존재하는 loginId - true 반환")
    fun existsByLoginId_existing_returnsTrue()

    @Test
    @DisplayName("existsByLoginId - 존재하지 않는 loginId - false 반환")
    fun existsByLoginId_notExisting_returnsFalse()
}
```

---

## 5. TDD 구현 순서 (Outside-In)

> 각 Step에서 Red → Green → Refactor 사이클을 완료한 후 다음 Step으로 이동.
> 상세 TDD 규칙: `docs/rules/tdd-workflow.md` 참조

### Step 1: Controller Layer (E2E Test)

- E2E 테스트 작성 (Red)
- Controller, ApiSpec, Dto, Service 스텁 생성 (Green)
- Refactor

### Step 2: Service Layer (Unit Test)

- Service 테스트 작성 (Red)
- Service, Repository interface 생성 (Green)
- Refactor

### Step 3: Domain Layer (Unit Test)

- Domain 테스트 작성 (Red)
- Domain Model 생성 (Green)
- Refactor

### Step 4: Repository Layer (Integration Test)

- Repository 테스트 작성 (Red)
- Entity, JpaRepository, RepositoryImpl 생성 (Green)
- Refactor

---

## 6. 생성할 파일 목록

### Main (src/main/kotlin/com/loopers/)

| 레이어        | 파일 경로                                         | 설명                           |
|------------|-----------------------------------------------|------------------------------|
| ApiSpec    | `user/interfaces/api/UserV1ApiSpec.kt`        | API 스펙 인터페이스                 |
| Controller | `user/interfaces/api/UserV1Controller.kt`     | 컨트롤러 구현체                     |
| DTO        | `user/interfaces/api/UserV1Dto.kt`            | Request/Response inner class |
| Service    | `user/application/UserService.kt`             | 애플리케이션 서비스                   |
| Info       | `user/application/UserInfo.kt`                | Service 출력                   |
| Command    | `user/application/model/UserSignUpCommand.kt` | Service 입력 (비즈니스 입력)         |
| Domain     | `user/domain/User.kt`                         | 도메인 (패턴 검증 + 비밀번호 규칙)        |
| Repository | `user/domain/UserRepository.kt`               | 리포지토리 인터페이스                  |
| Entity     | `user/infrastructure/UserEntity.kt`           | JPA 엔티티                      |
| Repo Impl  | `user/infrastructure/UserRepositoryImpl.kt`   | Repository 구현체               |
| JPA        | `user/infrastructure/UserJpaRepository.kt`    | JPA 인터페이스                    |

### Test (src/test/kotlin/com/loopers/)

| 레벨          | 파일 경로                                                  | 설명             |
|-------------|--------------------------------------------------------|----------------|
| E2E         | `user/interfaces/api/UserV1ControllerSignUpE2ETest.kt` | E2E 테스트        |
| Unit        | `user/interfaces/api/UserV1DtoTest.kt`                 | DTO 유효성 테스트    |
| Unit        | `user/application/UserServiceTest.kt`                  | Service 단위 테스트 |
| Unit        | `user/domain/UserTest.kt`                              | 도메인 단위 테스트     |
| Integration | `user/infrastructure/UserRepositoryIntegrationTest.kt` | 리포지토리 통합 테스트   |

### 수정할 파일

| 파일                                              | 수정 내용                                                                                                                         |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `support/error/ErrorType.kt`                    | User 도메인 섹션 (USER_DUPLICATE_LOGIN_ID, USER_INVALID_LOGIN_ID, USER_INVALID_PASSWORD, USER_INVALID_NAME) + 인증 섹션 (UNAUTHORIZED) |
| `example/interfaces/api/ApiControllerAdvice.kt` | MethodArgumentNotValidException 핸들러 추가                                                                                        |
| `apps/commerce-api/build.gradle.kts`            | `spring-security-crypto` 의존성 추가                                                                                               |

---

## 7. 검증 체크리스트

### 구현 완료 후 확인

- [ ] 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- [ ] 린트 통과: `./gradlew ktlintCheck`
- [ ] E2E: 핵심 시나리오(성공/실패) 검증
- [ ] Unit: DTO 형식 검증, 비즈니스 로직, 도메인 규칙 검증
- [ ] Integration: Repository 동작 검증
- [ ] 비밀번호 BCrypt 암호화 검증 (원문과 다름)
- [ ] ErrorType 매핑이 예외 처리 명세(섹션 2)와 일치함

### 코드 품질 확인

- [ ] 생성자 주입 사용 (필드 주입 금지)
- [ ] `@DisplayName` 한국어 설명 포함
- [ ] `@Nested` inner class로 논리적 그룹핑
- [ ] 테스트 메서드명: `{action}_{condition}()` 형식

---

## 8. 참고 자료

- TDD 워크플로우: `docs/rules/tdd-workflow.md`
- 테스트 레벨 가이드: `docs/rules/testing-levels.md`
- 프로젝트 규칙: `CLAUDE.md`
