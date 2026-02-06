# Week 1: 비밀번호 수정 API 구현 가이드

> 비밀번호 수정 API를 TDD 방식으로 구현하기 위한 상세 가이드입니다.
> **전제 조건**: 회원가입 API + 내 정보 조회 API 구현 완료 (User 도메인, 헤더 인증, Repository 존재)

## 검증

- 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- 린트 통과: `./gradlew ktlintCheck`
- 패턴 참조: example 패키지 코드 패턴을 따른다

---

### 범위

| 포함                                          | 미포함 (이번 범위 밖)      |
|---------------------------------------------|---------------------|
| 비밀번호 수정 API (PATCH /api/v1/users/me/password) | 비밀번호 찾기/재설정         |
| 비밀번호 규칙 재검증 (회원가입과 동일)                       | 비밀번호 변경 이력 관리       |
| 부수효과 검증 (변경 후 인증)                            | 비밀번호 변경 횟수 제한       |
| TDD 구현 가이드 (Outside-In)                      | 이전 비밀번호 재사용 금지 정책   |

> **PRD 경로 차이**: PRD는 `/api/v1/members/me/password`로 정의하지만, 기존 코드의 `users` 컨벤션(회원가입: `/api/v1/users`, 내 정보 조회: `/api/v1/users/me`)을 따라 `/api/v1/users/me/password`를 사용한다.

### 공통 가정

- 회원가입 + 내 정보 조회가 완료되어 User 도메인, Repository(findByLoginId, save), 헤더 인증이 존재한다
- 인증은 `@RequestHeader`로 직접 추출한다 (Spring Security 필터 미사용)
- TLS 등 기본 보안은 인프라 가정, 이번 범위 밖

### 기술 스택

| 항목    | 기술                                         |
|-------|--------------------------------------------|
| 인증    | @RequestHeader + BCrypt (spring-security-crypto) |
| 검증    | Jakarta Validation (@Valid) + Domain 로직 |
| 응답 래퍼 | ApiResponse (프로젝트 표준)                       |

---

## 1. API Specification

### 엔드포인트

| 항목     | 값                            |
|--------|------------------------------|
| Method | PATCH                        |
| Path   | /api/v1/users/me/password    |
| 인증     | 필요 (헤더)                      |

### Request

**인증 헤더**

| 헤더 이름               | 타입     | 필수 | 설명       |
|----------------------|--------|----|----------|
| X-Loopers-LoginId    | String | O  | 로그인 ID   |
| X-Loopers-LoginPw    | String | O  | 비밀번호 (평문) |

**Request Body**

```json
{
  "currentPassword": "string (현재 비밀번호)",
  "newPassword": "string (8~16자, 영문대/소문자+숫자+특수문자, 생년월일 불포함)"
}
```

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
    "message": "비밀번호가 변경되었습니다."
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

| Status           | 상황                                         |
|------------------|--------------------------------------------|
| 200 OK           | 비밀번호 변경 성공                                  |
| 400 Bad Request  | currentPassword 불일치, 동일 비밀번호, 규칙 위반, 헤더 누락 |
| 401 Unauthorized | 인증 실패 (loginId 미존재 또는 헤더 비밀번호 불일치)          |

---

## 2. 예외 처리 명세

### 추가할 ErrorType

새 ErrorType 추가 없음. 기존 `USER_INVALID_PASSWORD`(회원가입)와 `UNAUTHORIZED`(내 정보 조회)를 customMessage로 재사용한다.

### 예외 발생 상황 매핑

| 상황                           | ErrorType              | HTTP Status | 발생 위치                | 메시지                                   |
|------------------------------|------------------------|-------------|----------------------|---------------------------------------|
| 헤더 loginId로 사용자 미존재           | UNAUTHORIZED           | 401         | Application(Service) | "인증에 실패했습니다."                         |
| 헤더 비밀번호 불일치                   | UNAUTHORIZED           | 401         | Application(Service) | "인증에 실패했습니다."                         |
| 인증 헤더 누락                      | BAD_REQUEST            | 400         | Spring Framework     | MissingRequestHeaderException         |
| currentPassword 불일치            | USER_INVALID_PASSWORD  | 400         | Application(Service) | customMessage: "현재 비밀번호가 일치하지 않습니다."  |
| currentPassword == newPassword | USER_INVALID_PASSWORD  | 400         | Application(Service) | customMessage: "새 비밀번호는 현재 비밀번호와 달라야 합니다." |
| newPassword 패턴 위반              | USER_INVALID_PASSWORD  | 400         | Domain(User)         | "비밀번호는 영문, 숫자, 허용된 특수문자만 사용할 수 있습니다." |
| newPassword에 생년월일 포함           | USER_INVALID_PASSWORD  | 400         | Domain(User)         | customMessage: "비밀번호에 생년월일을 포함할 수 없습니다." |
| currentPassword/newPassword 누락 | Bad Request            | 400         | DTO(@Valid)          | MethodArgumentNotValidException       |
| newPassword 길이 위반              | Bad Request            | 400         | DTO(@Valid)          | MethodArgumentNotValidException       |

> **헤더 인증(401) vs body currentPassword(400) 구분**: 헤더의 `X-Loopers-LoginPw`는 "이 사용자가 맞는가?"를 검증하는 **인증**(401). body의 `currentPassword`는 "현재 비밀번호를 알고 있는가?"를 검증하는 **비즈니스 검증**(400). 두 값이 같더라도 역할이 다르다.

> **PRD 차이점**: PRD는 인증 헤더 누락 시 401을 명시하지만, Spring의 `MissingRequestHeaderException` 기본 동작 + 내 정보 조회 API 컨벤션을 따라 400으로 처리한다.

---

## 3. 비밀번호 변경 처리 규칙

### 변경 플로우

```
 1. Controller: @RequestHeader로 loginId, password(헤더) 추출
 2. Controller: @RequestBody로 currentPassword, newPassword 추출
 3. Service: userRepository.findByLoginId(loginId) 조회
    → 미존재 시 CoreException(UNAUTHORIZED)
 4. Service: BCrypt.matches(headerPassword, user.password) 검증
    → 불일치 시 CoreException(UNAUTHORIZED)
 5. Service: BCrypt.matches(currentPassword, user.password) 검증
    → 불일치 시 CoreException(USER_INVALID_PASSWORD, "현재 비밀번호가 일치하지 않습니다.")
 6. Service: currentPassword == newPassword 비교 (raw string)
    → 동일 시 CoreException(USER_INVALID_PASSWORD, "새 비밀번호는 현재 비밀번호와 달라야 합니다.")
 7. Domain: newPassword 패턴 검증 (허용 문자)
 8. Domain: newPassword에 생년월일 포함 여부 검증 (DB에서 조회한 User의 birthDate 사용)
 9. Service: newPassword를 BCrypt로 암호화
10. Service: 암호화된 새 비밀번호로 User 저장
```

### 비밀번호 규칙 (회원가입과 동일)

- **DTO**: `@NotBlank` + `@Size(8, 16)` (newPassword에만 적용)
- **Domain (패턴)**: 허용문자 검증. 정규식: `^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{}|;:',.<>?/]+$`
- **Domain (규칙)**: 생년월일(yyyyMMdd, yyyy-MM-dd 형태 모두) 포함 불가

### 추가 제약

- `currentPassword == newPassword` (raw string 비교) → 동일하면 실패
- currentPassword에는 `@field:NotBlank`만 적용 (패턴/길이 검증 불필요 — 이미 저장된 비밀번호)

### birthDate 출처

newPassword의 생년월일 포함 여부는 **DB에서 조회한 User의 birthDate**를 사용한다. Request body에 birthDate를 받지 않는다. 이유: 공격자가 birthDate를 조작하여 검증을 우회하는 것을 방지한다.

### Command 객체

```kotlin
// user/application/model/UserChangePasswordCommand.kt
data class UserChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String,
)
```

### Service 반환 타입

비밀번호 변경은 반환할 데이터가 없으므로 `Unit`을 반환한다. Controller가 `ChangePasswordResponse.success()` 등으로 직접 응답을 생성한다.

---

## 4. 인수조건 (테스트 케이스)

### 인수조건 목록

| #    | 인수조건 (구체화)                                                   | 요구사항 근거                | 유형   |
|------|--------------------------------------------------------------|------------------------|------|
| AC-1 | 유효한 인증 + 유효한 비밀번호 변경 요청 시 200 OK + message 반환                | 200 반환 + message 포함     | 정상   |
| AC-2 | 응답 body에 `"비밀번호가 변경되었습니다."` 메시지 포함                           | 응답 body에 message 포함    | 정상   |
| AC-3 | 인증 헤더(X-Loopers-LoginId/LoginPw) 누락 시 400 반환                 | 인증 헤더 누락 시 에러          | 예외   |
| AC-4 | currentPassword가 저장된 비밀번호와 불일치 시 400 반환                       | currentPassword 불일치 시 400 | 예외   |
| AC-5 | currentPassword와 newPassword가 동일하면 400 반환                     | 동일 시 400 반환            | 예외   |
| AC-6 | newPassword가 비밀번호 규칙(8~16자, 허용 문자)을 위반하면 400 반환              | 비밀번호 규칙 위반 시 400 반환    | 예외   |
| AC-7 | newPassword에 생년월일(yyyyMMdd 또는 yyyy-MM-dd)이 포함되면 400 반환       | 생년월일 포함 시 400 반환       | 예외   |
| AC-8 | 비밀번호 변경 후 새 비밀번호로 인증(getMe 호출)이 성공한다                        | 변경 후 새 비밀번호로 인증 성공     | 부수효과 |
| AC-9 | 비밀번호 변경 후 기존 비밀번호로 인증(getMe 호출)이 실패한다                       | 변경 후 기존 비밀번호로 인증 실패    | 부수효과 |

### 인수조건별 테스트 케이스 도출

| 인수조건 | 케이스 유형 | 구체적 시나리오                                           |
|------|--------|------------------------------------------------------|
| AC-1 | 정상     | 유효한 헤더 + 유효한 body → 200 + message                    |
| AC-2 | 정상     | 응답 data.message == "비밀번호가 변경되었습니다."                   |
| AC-3 | 예외     | X-Loopers-LoginId 헤더 누락 → 400                        |
| AC-3 | 예외     | X-Loopers-LoginPw 헤더 누락 → 400                        |
| AC-4 | 예외     | currentPassword가 DB 비밀번호와 불일치 → 400                   |
| AC-5 | 예외     | currentPassword == newPassword → 400                   |
| AC-6 | 경계값    | newPassword 7자 → 400 (최소 미만)                          |
| AC-6 | 경계값    | newPassword 8자 → 200 (최소 경계)                          |
| AC-6 | 예외     | newPassword에 허용되지 않은 특수문자(한글, 공백 등) → 400            |
| AC-7 | 예외     | newPassword에 생년월일 19900101 포함 → 400                   |
| AC-7 | 예외     | newPassword에 생년월일 1990-01-01 포함 → 400                 |
| AC-8 | 부수효과   | 비밀번호 변경 → 새 비밀번호로 getMe → 200                        |
| AC-9 | 부수효과   | 비밀번호 변경 → 기존 비밀번호로 getMe → 401                       |

### E2E Test (`UserV1ControllerChangePasswordE2ETest`)

```kotlin
@DisplayName("PATCH /api/v1/users/me/password - 비밀번호 수정")
class UserV1ControllerChangePasswordE2ETest {

    @Test
    @DisplayName("유효한 인증과 유효한 요청으로 비밀번호 변경 시 200 OK와 성공 메시지를 반환한다")
    fun changePassword_success_returns200WithMessage()

    @Test
    @DisplayName("currentPassword가 저장된 비밀번호와 불일치하면 400 Bad Request를 반환한다")
    fun changePassword_wrongCurrentPassword_returns400()

    @Test
    @DisplayName("currentPassword와 newPassword가 동일하면 400 Bad Request를 반환한다")
    fun changePassword_samePassword_returns400()

    @Test
    @DisplayName("newPassword가 비밀번호 규칙을 위반하면 400 Bad Request를 반환한다")
    fun changePassword_invalidNewPassword_returns400()

    @Test
    @DisplayName("인증 헤더 누락 시 400 Bad Request를 반환한다")
    fun changePassword_missingHeader_returns400()

    @Test
    @DisplayName("인증 실패(잘못된 헤더 비밀번호) 시 401 Unauthorized를 반환한다")
    fun changePassword_invalidAuth_returns401()

    @Test
    @DisplayName("비밀번호 변경 후 새 비밀번호로 인증이 성공한다")
    fun changePassword_thenAuthWithNewPassword_returns200()

    @Test
    @DisplayName("비밀번호 변경 후 기존 비밀번호로 인증이 실패한다")
    fun changePassword_thenAuthWithOldPassword_returns401()
}
```

### Unit Test (`UserServiceTest` - changePassword 추가)

```kotlin
@Nested
@DisplayName("비밀번호 수정")
inner class ChangePassword {

    @Nested
    @DisplayName("유효한 요청으로 비밀번호를 변경하면 성공한다")
    inner class WhenValidRequest {

        @Test
        @DisplayName("새 비밀번호가 BCrypt로 암호화되어 저장된다")
        fun changePassword_success_savesEncryptedPassword()
    }

    @Nested
    @DisplayName("currentPassword가 저장된 비밀번호와 불일치하면 실패한다")
    inner class WhenCurrentPasswordMismatch {

        @Test
        @DisplayName("CoreException(USER_INVALID_PASSWORD) 발생, '현재 비밀번호가 일치하지 않습니다.'")
        fun changePassword_wrongCurrentPassword_throwsException()
    }

    @Nested
    @DisplayName("currentPassword와 newPassword가 동일하면 실패한다")
    inner class WhenSamePassword {

        @Test
        @DisplayName("CoreException(USER_INVALID_PASSWORD) 발생, '새 비밀번호는 현재 비밀번호와 달라야 합니다.'")
        fun changePassword_samePassword_throwsException()
    }

    @Nested
    @DisplayName("newPassword가 비밀번호 규칙을 위반하면 실패한다")
    inner class WhenInvalidNewPassword {

        @Test
        @DisplayName("허용되지 않은 문자 포함 시 CoreException(USER_INVALID_PASSWORD)")
        fun changePassword_invalidPattern_throwsException()

        @Test
        @DisplayName("생년월일 포함 시 CoreException(USER_INVALID_PASSWORD)")
        fun changePassword_containsBirthDate_throwsException()
    }
}
```

### Unit Test (`UserTest` - changePassword 추가)

```kotlin
@Nested
@DisplayName("비밀번호 변경")
inner class ChangePassword {

    @Test
    @DisplayName("새 비밀번호로 변경하면 변경된 User를 반환한다")
    fun changePassword_success_returnsUpdatedUser()

    @Test
    @DisplayName("새 비밀번호 패턴이 유효하지 않으면 실패한다")
    fun changePassword_invalidPattern_throwsException()

    @Test
    @DisplayName("새 비밀번호에 생년월일(yyyyMMdd) 포함 시 실패한다")
    fun changePassword_containsBirthDateCompact_throwsException()

    @Test
    @DisplayName("새 비밀번호에 생년월일(yyyy-MM-dd) 포함 시 실패한다")
    fun changePassword_containsBirthDateWithDash_throwsException()
}
```

---

## 5. TDD 구현 순서 (Outside-In)

> 각 Step에서 Red → Green → Refactor 사이클을 완료한 후 다음 Step으로 이동.
> 상세 TDD 규칙: `docs/rules/tdd-workflow.md` 참조

### Macro to Micro 진행 원칙

| 순서 | 레이어       | 테스트 유형    | Mock 대상                     |
|----|-----------|-----------|----------------------------|
| 1  | Controller | E2E Test  | Service (stub)             |
| 2  | Service   | Unit Test | Repository, PasswordEncoder |
| 3  | Domain    | Unit Test | 없음 (순수 로직)                 |

> 기존 Repository(findByLoginId, save)와 Infrastructure(Entity, JpaRepository, RepositoryImpl)는 이전 기능에서 구현 완료. **Repository Step 없음.**

### Step 1: Controller Layer (E2E Test)

**테스트 유형**: E2E (`@SpringBootTest` + `TestRestTemplate`)
**Mock 대상**: Service — changePassword 메서드가 아직 없으므로 stub 구현 필요

- **Red**: E2E 테스트 작성 → 엔드포인트가 없으므로 실패
- **Green**: Controller에 PATCH 엔드포인트 추가. ApiSpec에 changePassword 시그니처 추가. Dto에 ChangePasswordRequest, ChangePasswordResponse 추가. Service에 changePassword **stub 메서드** 추가 (아무 동작 없이 반환)
  - 사전 데이터: 회원가입 API 호출로 테스트 사용자 생성 (E2E는 전체 스택 통과)
- **Refactor**

**이 시점의 구현 상태**:

| 컴포넌트       | 상태         | 비고                                       |
|------------|------------|------------------------------------------|
| Controller | ✅ 실제 구현    | PATCH /api/v1/users/me/password 엔드포인트     |
| ApiSpec    | ✅ 실제 구현    | changePassword 메서드 시그니처                   |
| Dto        | ✅ 실제 구현    | ChangePasswordRequest, ChangePasswordResponse |
| Service    | ⚠️ stub    | changePassword가 아무 동작 없이 반환              |
| Command    | ✅ 실제 구현    | UserChangePasswordCommand                 |
| Domain     | —          | 아직 변경 없음                                 |

### Step 2: Service Layer (Unit Test)

**테스트 유형**: Unit (Spring Context 없이, `@SpringBootTest` 사용 금지)
**Mock 대상**: UserRepository (`mock()`), PasswordEncoder (`mock()`)

- **Red**: Service 테스트 작성 → stub 구현이 실제 로직이 아니므로 테스트 실패
- **Green**: Service의 changePassword를 **실제 로직으로 구현**
  - 헤더 인증은 이미 getMe에서 구현된 로직을 재사용 (또는 별도 메서드 추출)
  - currentPassword BCrypt 검증
  - 동일 비밀번호 체크 (raw string 비교)
  - newPassword 규칙 검증 — **이 시점에서는 Domain의 changePassword()가 아직 없으므로 Service에서 직접 처리** (validatePassword 호출 또는 인라인)
  - BCrypt 암호화 → 저장
- **Refactor**

**이 시점의 구현 상태**: Step 1의 stub Service가 **실제 구현으로 대체됨**

| 컴포넌트       | 상태         | 비고                                       |
|------------|------------|------------------------------------------|
| Controller | ✅ 실제 구현    | 변경 없음                                    |
| Service    | ✅ 실제 구현    | stub → 실제 로직 (인증 + 검증 + 암호화 + 저장)        |
| Command    | ✅ 실제 구현    | 변경 없음                                    |
| Domain     | —          | 아직 변경 없음 (비밀번호 검증은 Service에서 직접 처리 중)    |

### Step 3: Domain Layer (Unit Test)

**테스트 유형**: Unit (Mock 없음, 순수 로직)
**Mock 대상**: 없음

- **Red**: User.changePassword 테스트 작성 → 메서드가 없으므로 실패
- **Green**: User에 `changePassword(newPassword, birthDate)` 메서드 추가
  - 비밀번호 검증 로직을 `validatePassword()`로 공통화 (register에서 호출하던 것과 동일)
  - 변경된 password를 가진 새 User 인스턴스 반환 (불변 객체 패턴)
- **Refactor**: Service의 비밀번호 검증 로직을 Domain의 changePassword()로 위임하도록 리팩토링

**이 시점의 구현 상태**: 모든 레이어 완성

| 컴포넌트       | 상태       | 비고                                   |
|------------|----------|--------------------------------------|
| Controller | ✅ 실제 구현  | PATCH /api/v1/users/me/password      |
| Service    | ✅ 실제 구현  | 인증 + 검증 + Domain 위임 + 저장             |
| Command    | ✅ 실제 구현  | UserChangePasswordCommand            |
| Domain     | ✅ 실제 구현  | changePassword() + validatePassword 공통화 |

### 최종 통합 확인

Step 3 완료 후 **모든 테스트를 재실행**하여 전체 스택이 정상 동작하는지 검증한다.

```bash
./gradlew :apps:commerce-api:test
```

특히 **AC-8, AC-9 (부수효과)** 검증: E2E 테스트에서 비밀번호 변경 후 getMe 호출로 새 비밀번호 인증 성공 / 기존 비밀번호 인증 실패를 확인한다.

---

## 6. 수정할 파일 목록

### Main (src/main/kotlin/com/loopers/)

**수정하는 파일**

| 레이어        | 파일 경로                                      | 수정 내용                              |
|------------|--------------------------------------------|------------------------------------|
| ApiSpec    | `user/interfaces/api/UserV1ApiSpec.kt`     | changePassword 시그니처 추가             |
| Controller | `user/interfaces/api/UserV1Controller.kt`  | PATCH /api/v1/users/me/password 추가 |
| DTO        | `user/interfaces/api/UserV1Dto.kt`         | ChangePasswordRequest, ChangePasswordResponse 추가 |
| Service    | `user/application/UserService.kt`          | changePassword 메서드 추가              |
| Domain     | `user/domain/User.kt`                      | changePassword 메서드 추가              |

**신규 생성**

| 레이어     | 파일 경로                                               | 설명          |
|---------|-----------------------------------------------------|-------------|
| Command | `user/application/model/UserChangePasswordCommand.kt` | Service 입력 DTO |

### Test (src/test/kotlin/com/loopers/)

**신규 생성**

| 레벨  | 파일 경로                                                               | 설명                  |
|-----|---------------------------------------------------------------------|---------------------|
| E2E | `user/interfaces/api/UserV1ControllerChangePasswordE2ETest.kt`      | 비밀번호 수정 E2E 테스트     |

**수정하는 파일**

| 레벨   | 파일 경로                               | 수정 내용                     |
|------|-------------------------------------|---------------------------|
| Unit | `user/application/UserServiceTest.kt` | changePassword @Nested 추가 |
| Unit | `user/domain/UserTest.kt`            | changePassword @Nested 추가 |

---

## 7. 검증 체크리스트

### 구현 완료 후 확인

- [ ] 모든 테스트 통과: `./gradlew :apps:commerce-api:test`
- [ ] 린트 통과: `./gradlew ktlintCheck`
- [ ] E2E: 핵심 시나리오(성공 200/currentPassword 불일치 400/인증 실패 401/헤더 누락 400) 검증
- [ ] E2E: 부수효과(AC-8 새 비밀번호 인증 성공, AC-9 기존 비밀번호 인증 실패) 검증
- [ ] Unit: Service 비밀번호 검증 로직(currentPassword 불일치, 동일 비밀번호, 규칙 위반) 검증
- [ ] Unit: Domain changePassword 로직(패턴 검증, 생년월일 포함 검증) 검증
- [ ] BCrypt.matches 파라미터 순서: (rawPassword, encodedPassword)
- [ ] 헤더 인증(401)과 body currentPassword(400) 구분이 올바른지 확인
- [ ] 비밀번호 검증 로직이 회원가입(register)과 공통화되었는지 확인
- [ ] birthDate는 DB에서 조회한 User의 값을 사용하는지 확인
- [ ] ErrorType 매핑이 예외 처리 명세(섹션 2)와 일치함

### 코드 품질 확인

- [ ] 생성자 주입 사용 (필드 주입 금지)
- [ ] `@DisplayName` 한국어 설명 포함
- [ ] `@Nested` inner class로 논리적 그룹핑
- [ ] 테스트 메서드명: `{action}_{condition}()` 형식

---

## 8. 참고 자료

- 회원가입 구현 가이드: `docs/assignments/week1-signup-impl.md`
- 내 정보 조회 구현 가이드: `docs/assignments/week1-getme-impl.md`
- TDD 워크플로우: `docs/rules/tdd-workflow.md`
- 테스트 레벨 가이드: `docs/rules/testing-levels.md`
- 프로젝트 규칙: `CLAUDE.md`
