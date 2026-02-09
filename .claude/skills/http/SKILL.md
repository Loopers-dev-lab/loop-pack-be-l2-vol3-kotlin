---
name: http
description:
  개발 완료된 API에 대한 IntelliJ HTTP Client 형식(.http) 파일을 작성한다.
  도메인별 파일 분류, 정상+에러 케이스 포함, 실제로 동작하는 값을 사용한다.
---

대상 API: $ARGUMENTS

## 절차

1. 대상 API의 Controller와 DTO 분석
2. `http/` 디렉토리 확인 (없으면 생성 여부 확인)
3. 도메인별로 파일 분류 (예: `http/user.http`, `http/product.http`)
4. 정상 + 에러 케이스 모두 포함
5. 결과 보고

## HTTP 파일 형식

```http
### 회원가입 — 정상
POST http://localhost:8080/api/v1/users/sign-up
Content-Type: application/json

{
  "loginId": "testuser1",
  "password": "Password1!",
  "name": "홍길동",
  "birthDate": "1990-01-15",
  "email": "test@example.com"
}

### 회원가입 — 실패 (빈 로그인 ID)
POST http://localhost:8080/api/v1/users/sign-up
Content-Type: application/json

{
  "loginId": "",
  "password": "Password1!",
  "name": "홍길동",
  "birthDate": "1990-01-15",
  "email": "test@example.com"
}
```

## 규칙

- IntelliJ HTTP Client 형식 (`.http` 확장자)
- 요청 본문은 실제로 동작하는 값 사용
