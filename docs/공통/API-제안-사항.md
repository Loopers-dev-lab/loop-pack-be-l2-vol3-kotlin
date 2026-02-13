# API 제안 사항

Round2에서 구현하는 모든 API는 아래 규칙을 따릅니다.

## 대고객 API (Customer-facing)

### URL 규칙

대고객 기능은 `/api/v1` prefix를 통해 제공합니다.

```
POST /api/v1/members/sign-up
GET  /api/v1/products
POST /api/v1/orders
```

### 유저 식별 방식

유저 로그인이 필요한 기능은 아래 HTTP 헤더를 통해 유저를 식별합니다.
인증/인가는 Round2의 주요 스코프가 아니므로 별도의 인증 체계(JWT, 세션 등)를 구현하지 않습니다.

| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-LoginId` | 로그인 ID | 유저를 식별하기 위한 로그인 ID |
| `X-Loopers-LoginPw` | 비밀번호 | 유저의 비밀번호 (평문 전송, 서버에서 검증) |

### 유저 접근 제한

유저는 타 유저의 정보에 직접 접근할 수 없습니다.
헤더에 포함된 로그인 정보를 기준으로 본인의 데이터에만 접근 가능합니다.

### 요청 예시

```http
GET /api/v1/members/me
X-Loopers-LoginId: testuser01
X-Loopers-LoginPw: password123!
```

## 어드민 API (Admin)

### URL 규칙

어드민 기능은 `/api-admin/v1` prefix를 통해 제공합니다.
대고객 API와 어드민 API를 URL prefix로 명확히 구분하여 라우팅과 권한 관리를 분리합니다.

```
GET    /api-admin/v1/products
POST   /api-admin/v1/brands
PATCH  /api-admin/v1/orders/{orderId}/status
```

### 어드민 식별 방식

어드민 기능은 아래 HTTP 헤더를 통해 어드민을 식별합니다.

| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-Ldap` | `loopers.admin` | LDAP 기반 사내 어드민 식별 값 |

### LDAP(Lightweight Directory Access Protocol)이란

LDAP은 중앙 집중형 사용자 인증, 정보 검색, 액세스 제어를 위한 프로토콜입니다.
회사 사내 어드민 시스템에서는 LDAP을 통해 관리자 권한을 중앙에서 관리합니다.
이 프로젝트에서는 LDAP 인증을 직접 구현하지 않고, 헤더 값(`loopers.admin`)으로 어드민 여부를 간이 식별합니다.

### 요청 예시

```http
GET /api-admin/v1/products
X-Loopers-Ldap: loopers.admin
```

## 공통 응답 형식

모든 API는 `ApiResponse<T>` 형식으로 응답합니다.

### 성공 응답

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": { ... }
}
```

### 실패 응답

```json
{
  "meta": {
    "result": "FAIL",
    "errorCode": "Bad Request",
    "message": "잘못된 요청입니다."
  },
  "data": null
}
```

## 에러 코드 목록

| 에러 타입 | HTTP 상태 | 코드 | 메시지 |
|-----------|-----------|------|--------|
| `INTERNAL_ERROR` | 500 | Internal Server Error | 일시적인 오류가 발생했습니다. |
| `BAD_REQUEST` | 400 | Bad Request | 잘못된 요청입니다. |
| `NOT_FOUND` | 404 | Not Found | 존재하지 않는 요청입니다. |
| `CONFLICT` | 409 | Conflict | 이미 존재하는 리소스입니다. |
| `UNAUTHORIZED` | 401 | UNAUTHORIZED | 인증이 필요합니다. |
