# Week 1: 내 정보 조회 API 구현 가이드

> 내 정보 조회 API를 TDD 방식으로 구현하기 위한 상세 가이드입니다.
> **전제 조건**: 회원가입 API 구현 완료 (User 도메인 파일 존재)

## 검증

- 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- 린트 통과: `./gradlew ktlintCheck`
- 패턴 참조: example 패키지 코드 패턴을 따른다

---

### 범위

| 포함                                | 미포함 (이번 범위 밖)    |
|-----------------------------------|-------------------|
| 내 정보 조회 API (GET /api/v1/users/me) | JWT/세션 기반 인증      |
| 헤더 기반 인증 (loginId + password)      | 비밀번호 변경/찾기        |
| 이름 마스킹 처리                          | 회원 정보 수정          |
| TDD 구현 가이드 (Outside-In)            | 토큰 기반 인증, 인증 미들웨어 |

### 공통 가정

- 회원가입 구현이 완료되어 User 도메인 파일이 존재한다
- 인증은 `@RequestHeader`로 직접 추출한다 (Spring Security 필터 미사용)
- TLS 등 기본 보안은 인프라 가정, 이번 범위 밖

### 기술 스택

| 항목    | 기술                                         |
|-------|--------------------------------------------|
| 인증    | @RequestHeader + BCrypt (spring-security-crypto) |
| 마스킹   | Domain 레이어 (User.maskedName 프로퍼티)           |
| 응답 래퍼 | ApiResponse (프로젝트 표준)                       |

---

## 1. API Specification

### 엔드포인트

| 항목     | 값                |
|--------|------------------|
| Method | GET              |
| Path   | /api/v1/users/me |
| 인증     | 필요 (헤더)          |

### Request

Body 없음. 인증 정보를 HTTP 헤더로 전달한다.

| 헤더 이름               | 타입     | 필수 | 설명       |
|----------------------|--------|----|----------|
| X-Loopers-LoginId    | String | O  | 로그인 ID   |
| X-Loopers-LoginPw    | String | O  | 비밀번호 (평문) |

### Response Schema

**성공 (200 OK)**

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "loginId": "testuser1",
    "name": "홍길*",
    "birthDate": "1990-01-01",
    "email": "test@example.com"
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

| Status           | 상황                             |
|------------------|--------------------------------|
| 200 OK           | 조회 성공                          |
| 400 Bad Request  | 인증 헤더 누락 (MissingRequestHeaderException) |
| 401 Unauthorized | 인증 실패 (loginId 미존재 또는 비밀번호 불일치) |

---

## 2. 예외 처리 명세

### 추가할 ErrorType

```kotlin
// ErrorType.kt에 추가

/** 인증/인가 */
UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
```

### 예외 발생 상황 매핑

| 상황                | ErrorType    | HTTP Status | 발생 위치                | 메시지              |
|-------------------|--------------|-------------|----------------------|------------------|
| loginId로 사용자 미존재  | UNAUTHORIZED | 401         | Application(Service) | "인증에 실패했습니다."    |
| 비밀번호 불일치          | UNAUTHORIZED | 401         | Application(Service) | "인증에 실패했습니다."    |
| 인증 헤더 누락          | BAD_REQUEST  | 400         | Spring Framework     | MissingRequestHeaderException |

> **보안 원칙**: loginId 미존재와 비밀번호 불일치를 동일한 UNAUTHORIZED 메시지로 반환한다.
> 공격자가 특정 loginId의 존재 여부를 파악할 수 없도록 하기 위함이다.

### ApiControllerAdvice 수정

`MissingRequestHeaderException` 핸들러 추가 필요. 현재 이 예외를 처리하지 않으면 `Throwable` 핸들러에 걸려 500 Internal Server Error가 발생한다.

```kotlin
@ExceptionHandler
fun handleMissingRequestHeader(e: MissingRequestHeaderException): ResponseEntity<ApiResponse<*>> {
    val message = "필수 요청 헤더 '${e.headerName}'이(가) 누락되었습니다."
    return failureResponse(errorType = ErrorType.BAD_REQUEST, errorMessage = message)
}
```

---

## 3. 인증 처리 규칙

### 인증 플로우

```
1. Controller: @RequestHeader로 loginId, password 추출
2. Service: userRepository.findByLoginId(loginId) 조회
   → 미존재 시 CoreException(UNAUTHORIZED)
3. Service: BCrypt.matches(rawPassword, user.password) 검증
   → 불일치 시 CoreException(UNAUTHORIZED)
4. Service: UserInfo 반환 (name, birthDate, email 포함)
5. Controller: MeResponse로 변환 (이름 마스킹 포함)
```

### BCrypt 검증 주의사항

```kotlin
// PasswordEncoder.matches(rawPassword, encodedPassword) 순서 주의
// rawPassword가 첫 번째, encodedPassword(DB 저장값)가 두 번째
passwordEncoder.matches(password, user.password)
```

### 이름 마스킹 규칙

마지막 글자를 `*`로 대체한다. name은 회원가입 시 `@Size(2, 15)`로 보장되므로 1글자 케이스는 발생하지 않는다.

| 입력 (name)                  | 출력 (maskedName) | 비고            |
|----------------------------|-----------------|---------------|
| "이순" (2글자)                 | "이*"            | 최소 경계값        |
| "홍길동" (3글자)                | "홍길*"           | 일반 케이스        |
| "가나다라마바사아자차카타파" (15글자) | "가나다라마바사아자차카타파*"에서 마지막 글자 대체 → "가나다라마바사아자차카타*" | 최대 경계값 (14글자+*) |

**구현 위치**: Domain 레이어 (`User` 모델의 `maskedName` 프로퍼티)

```kotlin
// User.kt에 프로퍼티 추가
val maskedName: String
    get() = name.dropLast(1) + "*"
```

---

## 4. 인수조건 (테스트 케이스)

### 인수조건 목록

| #    | 인수조건 (구체화)                                  | 요구사항 근거             | 유형     |
|------|---------------------------------------------|---------------------|--------|
| AC-1 | 유효한 인증으로 조회 시 200 OK + 마스킹된 이름 포함 응답 반환     | 200 반환 + 마스킹된 사용자 정보 | 정상     |
| AC-2 | 존재하지 않는 loginId로 조회 시 401 Unauthorized 반환    | 인증 실패 시 401 반환      | 예외     |
| AC-3 | 비밀번호 불일치 시 401 Unauthorized 반환               | 인증 실패 시 401 반환      | 예외     |
| AC-4 | 이름 마스킹: 마지막 글자를 "*"로 대체 (2글자/3글자/15글자)    | 이름 마스킹 규칙           | 정상+경계값 |
| AC-5 | 인증 헤더(X-Loopers-LoginId/LoginPw) 누락 시 400 반환 | 필수 헤더 누락 시 400 반환   | 예외     |

### 인수조건별 테스트 케이스 도출

| 인수조건 | 케이스 유형 | 구체적 시나리오                                          |
|------|--------|---------------------------------------------------|
| AC-1 | 정상     | 유효한 헤더 → 200 + loginId, maskedName, birthDate, email |
| AC-1 | 부수효과   | 응답의 name이 마스킹 처리됨 (원본과 다름)                        |
| AC-2 | 예외     | 존재하지 않는 loginId → 401 + UNAUTHORIZED               |
| AC-3 | 예외     | 존재하는 loginId + 틀린 비밀번호 → 401 + UNAUTHORIZED        |
| AC-4 | 경계값    | 이름 2글자 "이순" → "이*"                                 |
| AC-4 | 정상     | 이름 3글자 "홍길동" → "홍길*"                               |
| AC-4 | 경계값    | 이름 15글자 → 14글자+"*"                                 |
| AC-5 | 예외     | X-Loopers-LoginId 헤더 누락 → 400                      |
| AC-5 | 예외     | X-Loopers-LoginPw 헤더 누락 → 400                      |

### E2E Test (`UserV1ControllerMeE2ETest`)

```kotlin
@DisplayName("GET /api/v1/users/me - 내 정보 조회")
class UserV1ControllerMeE2ETest {

    @Test
    @DisplayName("유효한 인증 헤더로 조회 시 200 OK와 마스킹된 사용자 정보를 반환한다")
    fun getMe_success_returns200WithMaskedInfo()

    @Test
    @DisplayName("존재하지 않는 loginId로 조회 시 401 Unauthorized를 반환한다")
    fun getMe_invalidLoginId_returns401()

    @Test
    @DisplayName("비밀번호 불일치 시 401 Unauthorized를 반환한다")
    fun getMe_wrongPassword_returns401()

    @Test
    @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
    fun getMe_missingHeader_returns400()
}
```

### Unit Test (`UserServiceTest` - getMe 추가)

```kotlin
@Nested
@DisplayName("내 정보 조회")
inner class GetMe {

    @Nested
    @DisplayName("유효한 인증으로 조회하면 사용자 정보를 반환한다")
    inner class WhenValidCredentials {

        @Test
        @DisplayName("loginId, name, birthDate, email을 포함한 UserInfo 반환")
        fun getMe_success_returnsUserInfo()
    }

    @Nested
    @DisplayName("존재하지 않는 loginId로 조회하면 인증 실패한다")
    inner class WhenLoginIdNotFound {

        @Test
        @DisplayName("CoreException(UNAUTHORIZED) 발생")
        fun getMe_loginIdNotFound_throwsUnauthorized()
    }

    @Nested
    @DisplayName("비밀번호가 불일치하면 인증 실패한다")
    inner class WhenPasswordMismatch {

        @Test
        @DisplayName("CoreException(UNAUTHORIZED) 발생")
        fun getMe_wrongPassword_throwsUnauthorized()
    }
}
```

### Unit Test (`UserTest` - maskedName 추가)

```kotlin
@Nested
@DisplayName("이름 마스킹")
inner class MaskedName {

    @Test
    @DisplayName("2글자 이름 '이순' → '이*' (최소 경계값)")
    fun maskedName_twoChars_masksLastChar()

    @Test
    @DisplayName("3글자 이름 '홍길동' → '홍길*' (일반 케이스)")
    fun maskedName_threeChars_masksLastChar()

    @Test
    @DisplayName("15글자 이름 → 14글자+'*' (최대 경계값)")
    fun maskedName_fifteenChars_masksLastChar()
}
```

### Integration Test (`UserRepositoryIntegrationTest` - findByLoginId 추가)

```kotlin
@Nested
@DisplayName("loginId로 조회")
inner class FindByLoginId {

    @Test
    @DisplayName("존재하는 loginId로 조회 시 User 반환")
    fun findByLoginId_existing_returnsUser()

    @Test
    @DisplayName("존재하지 않는 loginId로 조회 시 null 반환")
    fun findByLoginId_notExisting_returnsNull()
}
```

---

## 5. TDD 구현 순서 (Outside-In)

> 각 Step에서 Red → Green → Refactor 사이클을 완료한 후 다음 Step으로 이동.
> 상세 TDD 규칙: `docs/rules/tdd-workflow.md` 참조

### Macro to Micro 진행 원칙

| 순서 | 레이어       | 테스트 유형         | Mock 대상                        |
|----|-----------|-----------------|--------------------------------|
| 1  | Controller | E2E Test        | Service (stub)                 |
| 2  | Service   | Unit Test       | Repository, PasswordEncoder    |
| 3  | Domain    | Unit Test       | 없음 (순수 로직)                     |
| 4  | Repository | Integration Test | 없음 (실제 DB)                     |

> 각 Step을 완료할 때마다 하위 레이어의 Mock이 실제 구현으로 대체된다.

### Step 1: Controller Layer (E2E Test)

**테스트 유형**: E2E (`@SpringBootTest` + `TestRestTemplate`)
**Mock 대상**: Service — getMe 메서드가 아직 없으므로 stub 구현 필요

- **Red**: E2E 테스트 작성 → 엔드포인트가 없으므로 실패
- **Green**: Controller + ApiSpec + Dto(MeResponse) 생성. Service에 getMe **stub 메서드** 추가 (하드코딩된 UserInfo 반환)
  - 사전 데이터: 회원가입 API 호출로 테스트 사용자 생성 (E2E는 전체 스택 통과)
- **Refactor**

**이 시점의 구현 상태**:

| 컴포넌트       | 상태         | 비고                           |
|------------|------------|------------------------------|
| Controller | ✅ 실제 구현    | GET /api/v1/users/me 엔드포인트   |
| ApiSpec    | ✅ 실제 구현    | getMe 메서드 시그니처               |
| Dto        | ✅ 실제 구현    | MeResponse inner class       |
| Service    | ⚠️ stub    | getMe가 하드코딩된 UserInfo 반환     |
| Domain     | —          | 아직 변경 없음                     |
| Repository | —          | 아직 변경 없음                     |

### Step 2: Service Layer (Unit Test)

**테스트 유형**: Unit (Spring Context 없이, `@SpringBootTest` 사용 금지)
**Mock 대상**: UserRepository (`mock()`), PasswordEncoder (`mock()`)

- **Red**: Service 테스트 작성 → stub 구현이 실제 로직이 아니므로 테스트 실패
- **Green**: Service의 getMe를 **실제 로직으로 구현** (findByLoginId → BCrypt 검증 → UserInfo 반환)
  - UserRepository에 `findByLoginId` 인터페이스 메서드 추가
  - UserInfo에 name, birthDate, email 필드 추가 (필요 시)
  - ErrorType에 UNAUTHORIZED 추가
- **Refactor**

**이 시점의 구현 상태**: Step 1의 stub Service가 **실제 구현으로 대체됨**

| 컴포넌트       | 상태         | 비고                             |
|------------|------------|--------------------------------|
| Controller | ✅ 실제 구현    | 변경 없음                          |
| Service    | ✅ 실제 구현    | stub → 실제 로직 (findByLoginId + BCrypt) |
| Domain     | —          | 아직 변경 없음                       |
| Repository | ⚠️ 인터페이스만  | findByLoginId 시그니처 추가, 구현체 없음   |

### Step 3: Domain Layer (Unit Test)

**테스트 유형**: Unit (Mock 없음, 순수 로직)
**Mock 대상**: 없음

- **Red**: User.maskedName 테스트 작성 → 프로퍼티가 없으므로 실패
- **Green**: User에 maskedName 프로퍼티 추가
- **Refactor**

**이 시점의 구현 상태**:

| 컴포넌트       | 상태         | 비고                        |
|------------|------------|---------------------------|
| Controller | ✅ 실제 구현    | 변경 없음                     |
| Service    | ✅ 실제 구현    | 변경 없음                     |
| Domain     | ✅ 실제 구현    | maskedName 프로퍼티 추가         |
| Repository | ⚠️ 인터페이스만  | 구현체 없음                    |

### Step 4: Repository Layer (Integration Test)

**테스트 유형**: Integration (`@SpringBootTest` + Testcontainers, 실제 DB)
**Mock 대상**: 없음

- **Red**: findByLoginId 테스트 작성 → 구현체가 없으므로 실패
- **Green**: JpaRepository + RepositoryImpl에 findByLoginId 구현
- **Refactor**

**이 시점의 구현 상태**: 모든 레이어 완성

| 컴포넌트       | 상태       | 비고                        |
|------------|----------|---------------------------|
| Controller | ✅ 실제 구현  | GET /api/v1/users/me      |
| Service    | ✅ 실제 구현  | 인증 + UserInfo 반환          |
| Domain     | ✅ 실제 구현  | maskedName                 |
| Repository | ✅ 실제 구현  | findByLoginId (JPA + Impl) |

### 최종 통합 확인

Step 4 완료 후 **모든 테스트를 재실행**하여 전체 스택이 정상 동작하는지 검증한다.

```bash
./gradlew :apps:commerce-api:test
```

E2E 테스트가 stub이 아닌 실제 전체 스택(Controller → Service → Repository → DB)을 통과하는지 확인한다.

---

## 6. 수정할 파일 목록

### Main (src/main/kotlin/com/loopers/)

**수정하는 파일**

| 레이어             | 파일 경로                                           | 수정 내용                                |
|-----------------|-------------------------------------------------|--------------------------------------|
| ApiSpec         | `user/interfaces/api/UserV1ApiSpec.kt`          | getMe 메서드 시그니처 추가                    |
| Controller      | `user/interfaces/api/UserV1Controller.kt`       | GET /api/v1/users/me 엔드포인트 추가        |
| DTO             | `user/interfaces/api/UserV1Dto.kt`              | MeResponse inner class 추가            |
| Service         | `user/application/UserService.kt`               | getMe(loginId, password) 메서드 추가      |
| Info            | `user/application/UserInfo.kt`                  | name, birthDate, email 필드 추가 (필요 시)  |
| Domain          | `user/domain/User.kt`                           | maskedName 프로퍼티 추가                   |
| Repository      | `user/domain/UserRepository.kt`                 | findByLoginId(loginId): User? 메서드 추가 |
| Repo Impl       | `user/infrastructure/UserRepositoryImpl.kt`     | findByLoginId 구현                     |
| JPA             | `user/infrastructure/UserJpaRepository.kt`      | findByLoginId 메서드 추가                 |
| ErrorType       | `support/error/ErrorType.kt`                    | UNAUTHORIZED 추가                      |
| ControllerAdvice | `example/interfaces/api/ApiControllerAdvice.kt` | MissingRequestHeaderException 핸들러 추가 |

### Test (src/test/kotlin/com/loopers/)

**신규 생성**

| 레벨  | 파일 경로                                              | 설명                 |
|-----|----------------------------------------------------|--------------------|
| E2E | `user/interfaces/api/UserV1ControllerMeE2ETest.kt` | 내 정보 조회 E2E 테스트    |

**수정하는 파일**

| 레벨          | 파일 경로                                                  | 수정 내용                         |
|-------------|--------------------------------------------------------|-------------------------------|
| Unit        | `user/application/UserServiceTest.kt`                  | getMe @Nested 추가              |
| Unit        | `user/domain/UserTest.kt`                              | maskedName @Nested 추가          |
| Integration | `user/infrastructure/UserRepositoryIntegrationTest.kt` | findByLoginId @Nested 추가       |

---

## 7. 검증 체크리스트

### 구현 완료 후 확인

- [ ] 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- [ ] 린트 통과: `./gradlew ktlintCheck`
- [ ] E2E: 핵심 시나리오(성공 200/인증 실패 401/헤더 누락 400) 검증
- [ ] Unit: Service 인증 로직, Domain 마스킹 로직 검증
- [ ] Integration: findByLoginId Repository 동작 검증
- [ ] 보안: loginId 미존재/비밀번호 불일치 → 동일한 UNAUTHORIZED 메시지 반환
- [ ] BCrypt.matches 파라미터 순서: (rawPassword, encodedPassword)
- [ ] ErrorType 매핑이 예외 처리 명세(섹션 2)와 일치함

### 코드 품질 확인

- [ ] 생성자 주입 사용 (필드 주입 금지)
- [ ] `@DisplayName` 한국어 설명 포함
- [ ] `@Nested` inner class로 논리적 그룹핑
- [ ] 테스트 메서드명: `{action}_{condition}()` 형식

---

## 8. 참고 자료

- 회원가입 구현 가이드: `docs/assignments/week1-signup-impl.md`
- TDD 워크플로우: `docs/rules/tdd-workflow.md`
- 테스트 레벨 가이드: `docs/rules/testing-levels.md`
- 프로젝트 규칙: `CLAUDE.md`
