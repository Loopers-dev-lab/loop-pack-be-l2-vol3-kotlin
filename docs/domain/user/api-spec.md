# User API 스펙

## 공통

### 인증 헤더
인증이 필요한 API는 아래 헤더를 통해 사용자를 식별한다.

| Header | 설명 |
|--------|------|
| `X-Loopers-LoginId` | 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 |

### 응답 포맷
```json
{
  "meta": {
    "result": "SUCCESS | FAIL",
    "errorCode": "에러 코드 (실패 시)",
    "message": "사용자 메시지 (실패 시)"
  },
  "data": { 
    // ...
  }
}
```

### 공통 에러 코드
| HTTP Status | errorCode | 설명 |
|-------------|-----------|------|
| 400 | `BAD_REQUEST` | 잘못된 요청 (포맷 오류 등) |
| 401 | `UNAUTHORIZED` | 인증 실패 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 중복 리소스 |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

---

## 1. 회원가입

### Request
```
POST /api/v1/users
Content-Type: application/json
```

```json
{
  "loginId": "hyungki",
  "password": "Password1!",
  "name": "신형기",
  "birthDate": "1993-04-01",
  "email": "hyungki@loopers.com",
  "gender": "MALE"
}
```

### 필드 규칙
| 필드 | 규칙 |
|------|------|
| loginId | 영문 대소문자 + 숫자만, **10자 이내** |
| password | 8~16자, 영문 대소문자/숫자/특수문자(`!@#$%^&*()_+-=`), 생년월일 포함 불가 |
| name | 1자 이상 |
| birthDate | `yyyy-MM-dd` 형식, 미래 날짜 불가 |
| email | `xx@yy.zz` 형식 |
| gender | `MALE` 또는 `FEMALE`, **필수** |

### Response (201 Created)
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "id": 1,
    "loginId": "hyungki",
    "name": "신형기",
    "birthDate": "1993-04-01",
    "email": "hyungki@loopers.com",
    "gender": "MALE"
  }
}
```

### Error Cases
| 상황 | HTTP | errorCode | message 예시 |
|------|------|-----------|--------------|
| loginId 중복 | 409 | `CONFLICT` | "이미 사용 중인 로그인 ID입니다." |
| loginId 형식 오류 | 400 | `BAD_REQUEST` | "로그인 ID는 영문과 숫자 10자 이내만 허용됩니다." |
| 비밀번호 길이 오류 | 400 | `BAD_REQUEST` | "비밀번호는 8~16자여야 합니다." |
| 비밀번호에 생년월일 포함 | 400 | `BAD_REQUEST` | "비밀번호에 생년월일을 포함할 수 없습니다." |
| 이메일 형식 오류 | 400 | `BAD_REQUEST` | "올바른 이메일 형식이 아닙니다." |
| 생년월일 형식 오류 | 400 | `BAD_REQUEST` | "올바른 날짜 형식이 아닙니다." |
| **성별 누락** | 400 | `BAD_REQUEST` | "성별은 필수입니다." |

---

## 2. 내 정보 조회

### Request
```
GET /api/v1/users/me
X-Loopers-LoginId: hyungki
X-Loopers-LoginPw: Password1!
```

### Response (200 OK)
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "loginId": "hyungki",
    "name": "신형*",
    "birthDate": "1993-04-01",
    "email": "hyungki@loopers.com",
    "gender": "MALE"
  }
}
```

> **Note**: `name`은 마지막 글자가 `*`로 마스킹되어 반환된다.
> - "신형기" => "신형*"
> - "신" => "*"

### Error Cases
| 상황 | HTTP | errorCode | message 예시 |
|------|------|-----------|--------------|
| 헤더 누락 | 401 | `UNAUTHORIZED` | "인증 정보가 필요합니다." |
| 비밀번호 불일치 | 401 | `UNAUTHORIZED` | "인증에 실패했습니다." |
| **사용자 없음** | 404 | `NOT_FOUND` | "사용자를 찾을 수 없습니다." |

---

## 3. 비밀번호 변경

### Request
```
PUT /api/v1/users/password
Content-Type: application/json
X-Loopers-LoginId: hyungki
X-Loopers-LoginPw: Password1!
```

```json
{
  "oldPassword": "Password1!",
  "newPassword": "NewPass2@"
}
```

### Response (200 OK)
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

### Error Cases
| 상황 | HTTP | errorCode | message 예시 |
|------|------|-----------|--------------|
| 인증 실패 | 401 | `UNAUTHORIZED` | "인증에 실패했습니다." |
| 기존 비밀번호 불일치 | 400 | `BAD_REQUEST` | "기존 비밀번호가 일치하지 않습니다." |
| 새 비밀번호 = 기존 비밀번호 | 400 | `BAD_REQUEST` | "새 비밀번호는 기존과 달라야 합니다." |
| 새 비밀번호 포맷 오류 | 400 | `BAD_REQUEST` | "비밀번호는 8~16자여야 합니다." |
| 새 비밀번호에 생년월일 포함 | 400 | `BAD_REQUEST` | "비밀번호에 생년월일을 포함할 수 없습니다." |
