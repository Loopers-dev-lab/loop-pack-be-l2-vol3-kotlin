---
name: http
description: 개발 완료된 API의 .http 테스트 파일을 생성합니다.
---

개발 완료된 API의 .http 테스트 파일을 생성합니다.

대상 Controller의 엔드포인트를 분석한 후, 아래 규칙에 맞게 .http 파일을 생성하세요.

## 파일 위치

```
http/commerce-api/{도메인}-v{버전}.http
```

예: `http/commerce-api/member-v1.http`

## 환경 변수

`http/http-client.env.json`에 정의된 변수를 사용합니다.

```json
{
  "local": {
    "commerce-api": "http://localhost:8080"
  }
}
```

## 작성 규칙

### 요청 구분
- `###` 로 각 요청을 구분
- 요청 앞에 `### 한글 설명` 작성

### URL
- `{{commerce-api}}/api/v1/...` 형태로 환경 변수 사용

### 헤더
- POST, PUT, PATCH 등 바디가 있는 요청: `Content-Type: application/json`
- 인증이 필요한 요청: `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 추가

### 바디
- JSON 형식, DTO의 `@Schema(example = ...)` 값을 참고하여 예시 데이터 작성

## 예시

```http
### 회원가입
POST {{commerce-api}}/api/v1/members/signup
Content-Type: application/json

{
  "loginId": "testuser1",
  "password": "Password1!",
  "name": "홍길동",
  "birthDate": "1990-01-15",
  "email": "test@example.com"
}

###

### 내 정보 조회
GET {{commerce-api}}/api/v1/members/me
X-Loopers-LoginId: testuser1
X-Loopers-LoginPw: Password1!

###
```
