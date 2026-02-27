# 유저 요구사항

## 개요

유저 도메인은 회원가입, 내 정보 조회, 비밀번호 변경 기능을 제공합니다.
Round2에서는 기존 Round1의 `member` 도메인을 내부적으로 유지하면서, API 경로를 `/api/v1/users`로 변경하고, 인증 방식을 JWT 토큰에서 HTTP 헤더 기반(`X-Loopers-LoginId`, `X-Loopers-LoginPw`)으로 전환합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 브랜드-상품 | 간접 | 유저가 상품을 조회하고 주문하기 위해 회원가입이 선행되어야 합니다 |
| 좋아요 | 의존 | 좋아요 등록/조회 시 유저 식별 정보(`X-Loopers-LoginId`)가 필요합니다 |
| 주문 | 의존 | 주문 생성 시 유저 식별 정보가 필요하며, 주문자 정보로 유저 데이터를 참조합니다 |

---

## Part 1: API 명세

### 1.1 회원가입

#### Endpoint
- **METHOD**: `POST`
- **URI**: `/api/v1/users`
- **인증 필요**: 불필요

#### Request Body
```json
{
  "loginId": "testuser01",
  "password": "password1!",
  "name": "홍길동",
  "birthDate": "1990-01-01",
  "email": "test@example.com"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| loginId | String | O | 로그인 ID (4~20자, 영문 소문자/숫자/언더스코어) |
| password | String | O | 비밀번호 (8~16자, Part 2 검증 규칙 참조) |
| name | String | O | 이름 (2~50자, 한글 또는 영문) |
| birthDate | LocalDate | O | 생년월일 (미래 날짜 불가) |
| email | String | O | 이메일 (이메일 형식 검증) |

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "id": 1,
    "loginId": "testuser01",
    "name": "홍길동",
    "email": "test@example.com"
  }
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 이미 존재하는 로그인 ID | 409 | Conflict | 이미 존재하는 리소스입니다. |
| 필수 필드 누락 또는 형식 오류 | 400 | Bad Request | 잘못된 요청입니다. |
| 비밀번호 검증 실패 | 400 | Bad Request | (검증 규칙별 상세 메시지) |

---

### 1.2 내 정보 조회

#### Endpoint
- **METHOD**: `GET`
- **URI**: `/api/v1/users/me`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Request Headers
| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-LoginId` | 로그인 ID | 유저를 식별하기 위한 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 | 유저의 비밀번호 (평문 전송, 서버에서 BCrypt 매칭으로 검증) |

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "loginId": "testuser01",
    "name": "홍*동",
    "birthDate": "1990-01-01",
    "email": "te***@example.com"
  }
}
```

마스킹 규칙:
- **이름**: 첫 글자와 마지막 글자만 표시하고 나머지는 `*`로 대체 (예: "홍길동" -> "홍*동")
- **이메일**: 로컬 파트의 처음 2글자만 표시하고 나머지를 `***`로 대체 (예: "test@example.com" -> "te***@example.com")

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 로그인 ID에 해당하는 유저가 없음 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 비밀번호 불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |

---

### 1.3 비밀번호 변경

#### Endpoint
- **METHOD**: `PUT`
- **URI**: `/api/v1/users/password`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)

#### Request Headers
| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-LoginId` | 로그인 ID | 유저를 식별하기 위한 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 | 유저의 현재 비밀번호 (평문 전송, 서버에서 BCrypt 매칭으로 검증) |

#### Request Body
```json
{
  "newPassword": "newPassword1!"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| newPassword | String | O | 새 비밀번호 (Part 2 검증 규칙 참조) |

> Round1에서는 `currentPassword`와 `newPassword`를 Request Body에 모두 포함했지만, Round2에서는 현재 비밀번호가 인증 헤더(`X-Loopers-LoginPw`)로 전달되므로 Request Body에는 `newPassword`만 포함합니다.

#### Response (성공) - `200 OK`
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": null
}
```

#### Response (실패)

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 인증 헤더 누락 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 로그인 ID에 해당하는 유저가 없음 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 비밀번호 불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 현재 비밀번호와 동일한 새 비밀번호 | 400 | Bad Request | 현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다. |
| 새 비밀번호 검증 실패 | 400 | Bad Request | (검증 규칙별 상세 메시지) |

---

## Part 2: 비즈니스 규칙

### 2.1 회원가입 규칙

1. **로그인 ID 유효성 검증**: 4~20자, 영문 소문자/숫자/언더스코어만 허용
2. **로그인 ID 중복 검사**: 이미 동일한 로그인 ID가 존재하면 회원가입을 거부합니다
3. **이름 유효성 검증**: 2~50자, 한글 또는 영문만 허용
4. **이메일 형식 검증**: 이메일 형식(`xxx@xxx.xxx`)을 만족해야 합니다
5. **생년월일 검증**: 미래 날짜는 허용하지 않습니다
6. **비밀번호 검증**: Part 2.2의 비밀번호 검증 규칙을 모두 통과해야 합니다
7. **비밀번호 암호화**: 비밀번호는 BCrypt로 해싱하여 저장합니다

### 2.2 비밀번호 검증 규칙

1. **길이 제한**: 8자 이상 16자 이하
2. **허용 문자**: 영문 대소문자, 숫자, 특수문자만 허용
3. **문자 종류 조합**: 영문, 숫자, 특수문자 중 2종류 이상 조합 필수
4. **연속 동일 문자 제한**: 동일 문자 3개 이상 연속 사용 불가 (예: "aaa")
5. **연속 순서 문자 제한**: 순차적 문자 3개 이상 연속 사용 불가 (예: "abc", "321")
6. **생년월일 포함 금지**: YYYYMMDD, YYMMDD, MMDD 형식 모두 검사
7. **로그인 ID 포함 금지**: 비밀번호에 로그인 ID 문자열이 포함되면 거부

### 2.3 헤더 기반 인증 규칙

1. 유저가 보호된 API를 호출할 때 `X-Loopers-LoginId`와 `X-Loopers-LoginPw` 헤더를 함께 전송합니다
2. 서버는 `X-Loopers-LoginId`로 유저를 조회하고, `X-Loopers-LoginPw`를 BCrypt로 인코딩된 비밀번호와 매칭하여 인증합니다
3. 인증 실패 시(헤더 누락, 유저 미존재, 비밀번호 불일치) 모두 동일한 `UNAUTHORIZED` 에러를 반환합니다 (보안을 위해 실패 원인을 구분하지 않음)
4. 인증이 필요한 API 경로: `/api/v1/users/me`, `/api/v1/users/password`

### 2.4 비밀번호 변경 규칙

1. 인증 헤더의 현재 비밀번호와 새 비밀번호가 동일하면 변경을 거부합니다
2. 새 비밀번호는 Part 2.2의 검증 규칙을 모두 통과해야 합니다
3. 새 비밀번호는 BCrypt로 해싱하여 저장합니다

---

## Part 3: 구현 컴포넌트

### 3.1 레이어별 구조

Round1의 `member` 패키지 구조를 유지하면서, API 경로와 인증 방식만 Round2 스펙에 맞게 변경합니다.

```
interfaces/api/member/
  ├── MemberV1ApiSpec.kt      - API 인터페이스 (경로를 /api/v1/users로 변경)
  ├── MemberV1Controller.kt   - 컨트롤러 (헤더 기반 인증으로 변경)
  └── MemberV1Dto.kt          - 요청/응답 DTO

application/member/
  ├── MemberFacade.kt         - 비즈니스 로직 조합 (비밀번호 변경 시그니처 변경)
  └── MemberInfo.kt           - 내부 전달 객체

domain/member/
  ├── MemberModel.kt          - 도메인 엔티티 (변경 없음)
  ├── MemberService.kt        - 도메인 서비스 (변경 없음)
  └── MemberRepository.kt     - 리포지토리 인터페이스 (변경 없음)

infrastructure/member/
  ├── MemberRepositoryImpl.kt - 리포지토리 구현체 (변경 없음)
  ├── MemberJpaRepository.kt  - JPA 리포지토리 (변경 없음)
  └── BCryptPasswordEncoder.kt - 비밀번호 인코더 (변경 없음)

infrastructure/auth/
  └── HeaderAuthenticationFilter.kt - 헤더 기반 인증 필터 (신규, JwtAuthenticationFilter 대체)

support/util/
  ├── PasswordValidator.kt    - 비밀번호 검증 유틸리티 (변경 없음)
  └── MaskingUtils.kt         - 마스킹 유틸리티 (변경 없음)
```

### 3.2 처리 흐름

#### 회원가입
```
유저 → POST /api/v1/users
  → MemberV1Controller.signUp()
    → MemberFacade.signUp()
      → PasswordValidator.validatePassword() (비밀번호 검증)
      → BCryptPasswordEncoder.encode() (비밀번호 해싱)
      → MemberService.signUp() (중복 검사 + 저장)
    → MemberV1Dto.SignUpResponse 반환
```

#### 내 정보 조회
```
유저 → GET /api/v1/users/me [X-Loopers-LoginId, X-Loopers-LoginPw 헤더]
  → HeaderAuthenticationFilter (헤더에서 loginId/password 추출, 유저 조회 및 비밀번호 검증)
    → MemberV1Controller.getMyInfo()
      → MemberFacade.getMyInfo()
        → MemberService.findById()
        → MemberInfo 생성
      → MemberV1Dto.MyInfoResponse (마스킹 적용) 반환
```

#### 비밀번호 변경
```
유저 → PUT /api/v1/users/password [X-Loopers-LoginId, X-Loopers-LoginPw 헤더]
  → HeaderAuthenticationFilter (헤더에서 loginId/password 추출, 유저 조회 및 비밀번호 검증)
    → MemberV1Controller.changePassword()
      → MemberFacade.changePassword()
        → 현재 비밀번호와 새 비밀번호 동일 여부 검사
        → PasswordValidator.validatePassword() (새 비밀번호 검증)
        → BCryptPasswordEncoder.encode() (새 비밀번호 해싱)
        → MemberService.changePassword() (저장)
      → 성공 응답 반환
```

---

## Part 4: 구현 체크리스트

### Phase 1: 헤더 기반 인증 필터 구현

기존 JWT 인증 필터(`JwtAuthenticationFilter`)를 대체하는 헤더 기반 인증 필터(`HeaderAuthenticationFilter`)를 구현합니다.

- [ ] **RED**: `HeaderAuthenticationFilter` 단위 테스트 작성
  - [ ] `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더가 모두 있으면 인증 성공하는지 검증
  - [ ] 헤더가 누락되면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 존재하지 않는 로그인 ID로 요청하면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 비밀번호가 일치하지 않으면 `UNAUTHORIZED` 에러를 반환하는지 검증
  - [ ] 보호 대상 경로(`/api/v1/users/me`, `/api/v1/users/password`)에만 필터가 적용되는지 검증
  - [ ] 보호 대상이 아닌 경로에는 필터가 적용되지 않는지 검증
- [ ] **GREEN**: `HeaderAuthenticationFilter` 구현
  - [ ] `OncePerRequestFilter`를 상속하여 헤더 기반 인증 필터 작성
  - [ ] `shouldNotFilter()`에서 보호 대상 경로를 판별
  - [ ] `doFilterInternal()`에서 헤더 추출, 유저 조회, 비밀번호 매칭 수행
  - [ ] 인증 성공 시 유저 정보를 요청 속성에 설정
- [ ] **REFACTOR**: 기존 `JwtAuthenticationFilter` 비활성화 또는 제거

### Phase 2: API 경로 변경 및 컨트롤러 수정

기존 컨트롤러의 API 경로를 Round2 스펙에 맞게 변경하고, 인증 방식을 헤더 기반으로 전환합니다.

- [ ] **RED**: 컨트롤러 통합 테스트 작성
  - [ ] `POST /api/v1/users` 회원가입 요청이 정상 동작하는지 검증
  - [ ] `GET /api/v1/users/me` 내 정보 조회가 인증 헤더와 함께 정상 동작하는지 검증
  - [ ] `PUT /api/v1/users/password` 비밀번호 변경이 인증 헤더와 함께 정상 동작하는지 검증
- [ ] **GREEN**: 컨트롤러 수정
  - [ ] `@RequestMapping` 경로를 `/api/v1/members`에서 `/api/v1/users`로 변경
  - [ ] 회원가입: `@PostMapping("/sign-up")`을 `@PostMapping`(루트)으로 변경
  - [ ] 내 정보 조회: `@GetMapping("/me")` 유지, 인증 객체를 요청 속성에서 가져오도록 수정
  - [ ] 비밀번호 변경: `@PatchMapping("/me/password")`을 `@PutMapping("/password")`으로 변경
  - [ ] 비밀번호 변경 Request Body에서 `currentPassword` 필드 제거 (인증 헤더로 대체)
- [ ] **REFACTOR**: ApiSpec 인터페이스도 함께 수정하고 불필요한 JWT 관련 import 정리

### Phase 3: Facade 및 비즈니스 로직 수정

비밀번호 변경 로직에서 현재 비밀번호를 인증 헤더에서 받은 값과 새 비밀번호를 비교하도록 수정합니다.

- [ ] **RED**: `MemberFacade` 단위 테스트 수정
  - [ ] `changePassword()`에서 인증 헤더의 비밀번호(평문)와 새 비밀번호가 동일하면 거부하는지 검증
  - [ ] 새 비밀번호가 검증 규칙을 통과하지 못하면 거부하는지 검증
  - [ ] 정상적인 비밀번호 변경이 성공하는지 검증
- [ ] **GREEN**: `MemberFacade.changePassword()` 시그니처 및 로직 수정
  - [ ] 기존: `changePassword(memberId, currentPassword, newPassword)`
  - [ ] 변경: `changePassword(memberId, currentPlainPassword, newPassword)` - 인증 필터에서 이미 비밀번호 검증이 완료되었으므로 BCrypt 매칭 로직 제거
  - [ ] 현재 비밀번호(평문)와 새 비밀번호 동일 여부 검사 유지
- [ ] **REFACTOR**: 불필요한 코드 제거 및 정리

### Phase 4: E2E 테스트 및 .http 파일 작성

모든 API 엔드포인트에 대한 E2E 테스트와 .http 테스트 파일을 작성합니다.

- [ ] **RED**: E2E 테스트 작성
  - [ ] 회원가입 -> 내 정보 조회 -> 비밀번호 변경 전체 흐름 테스트
  - [ ] 인증 실패 케이스 (헤더 누락, 잘못된 비밀번호) 테스트
  - [ ] 비밀번호 변경 후 변경된 비밀번호로 인증 성공하는지 테스트
- [ ] **GREEN**: 모든 E2E 테스트 통과 확인
- [ ] `.http` 파일 작성
  - [ ] `.http/user/sign-up.http`: 회원가입 API 테스트
  - [ ] `.http/user/me.http`: 내 정보 조회 API 테스트
  - [ ] `.http/user/change-password.http`: 비밀번호 변경 API 테스트

---

## Part 5: 테스트 시나리오

### 5.1 단위 테스트

#### HeaderAuthenticationFilter
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 인증 헤더가 모두 있고 유효한 경우 | loginId: "testuser01", loginPw: "password1!" | 요청 속성에 인증 정보 설정, 필터 체인 진행 |
| `X-Loopers-LoginId` 헤더 누락 | loginPw만 존재 | 401 UNAUTHORIZED 응답 |
| `X-Loopers-LoginPw` 헤더 누락 | loginId만 존재 | 401 UNAUTHORIZED 응답 |
| 존재하지 않는 로그인 ID | loginId: "nonexistent" | 401 UNAUTHORIZED 응답 |
| 비밀번호 불일치 | loginId: "testuser01", loginPw: "wrongpw1!" | 401 UNAUTHORIZED 응답 |
| 보호 대상이 아닌 경로 | POST /api/v1/users | 필터 스킵, 필터 체인 진행 |

#### MemberFacade.changePassword()
| 시나리오 | 입력 | 기대 결과 |
|----------|------|-----------|
| 현재 비밀번호와 새 비밀번호가 동일 | currentPw: "password1!", newPw: "password1!" | BAD_REQUEST 에러 |
| 새 비밀번호가 검증 규칙 위반 (짧은 길이) | newPw: "abc1!" | BAD_REQUEST 에러 |
| 새 비밀번호가 검증 규칙 위반 (연속 동일 문자) | newPw: "aaab1234!" | BAD_REQUEST 에러 |
| 정상적인 비밀번호 변경 | newPw: "newValid1!" | 성공, BCrypt 해싱 후 저장 |

### 5.2 통합 테스트

| 시나리오 | 요청 | 기대 결과 |
|----------|------|-----------|
| 회원가입 성공 | POST /api/v1/users (유효한 데이터) | 200 OK, 가입 정보 반환 |
| 회원가입 중복 로그인 ID | POST /api/v1/users (중복 loginId) | 409 Conflict |
| 내 정보 조회 성공 | GET /api/v1/users/me (유효한 인증 헤더) | 200 OK, 마스킹된 정보 반환 |
| 내 정보 조회 인증 실패 | GET /api/v1/users/me (잘못된 비밀번호) | 401 UNAUTHORIZED |
| 비밀번호 변경 성공 | PUT /api/v1/users/password (유효한 데이터) | 200 OK |
| 비밀번호 변경 동일 비밀번호 | PUT /api/v1/users/password (현재=새 비밀번호) | 400 BAD_REQUEST |

### 5.3 E2E 테스트

| 시나리오 | 흐름 | 기대 결과 |
|----------|------|-----------|
| 전체 유저 플로우 | 회원가입 -> 내 정보 조회 -> 비밀번호 변경 -> 변경된 비밀번호로 내 정보 조회 | 모든 단계 성공 |
| 인증 실패 후 재시도 | 잘못된 비밀번호로 내 정보 조회 실패 -> 올바른 비밀번호로 재시도 성공 | 첫 시도 401, 재시도 200 |
| 비밀번호 변경 후 이전 비밀번호 거부 | 비밀번호 변경 -> 이전 비밀번호로 내 정보 조회 | 401 UNAUTHORIZED |

---

## Part 6: 보안 고려사항

1. **비밀번호 평문 전송**: `X-Loopers-LoginPw` 헤더로 비밀번호가 평문으로 전송됩니다. 프로덕션 환경에서는 반드시 HTTPS를 사용해야 하지만, Round2에서는 인증/인가가 주요 스코프가 아니므로 현행 방식을 유지합니다.
2. **인증 실패 메시지 통일**: 로그인 ID 미존재, 비밀번호 불일치 등 인증 실패의 구체적인 원인을 응답에 노출하지 않고, 동일한 `UNAUTHORIZED` 메시지를 반환하여 정보 유출을 방지합니다.
3. **비밀번호 해싱**: 모든 비밀번호는 BCrypt로 해싱하여 데이터베이스에 저장합니다.
4. **접근 제한**: 유저는 헤더에 포함된 본인의 로그인 정보를 기준으로 자신의 데이터에만 접근할 수 있으며, 타 유저의 정보에는 접근할 수 없습니다.

---

## Part 7: 검증 명령어

```bash
# 전체 빌드
./gradlew build

# 유저 관련 테스트만 실행
./gradlew :apps:commerce-api:test --tests "*Member*"
./gradlew :apps:commerce-api:test --tests "*HeaderAuthentication*"

# 린트 체크
./gradlew ktlintCheck

# 린트 자동 수정
./gradlew ktlintFormat

# 테스트 커버리지 확인
./gradlew test jacocoTestReport
```

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [x] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
- [x] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
