# 유저(Users) API 스펙

유저 도메인의 요구사항 문서(`docs/유저/01-requirements.md`) 작성 시 참고하는 API 명세입니다.

## API 엔드포인트 목록

| METHOD | URI | 인증 필요 | 설명 |
|--------|-----|-----------|------|
| POST | `/api/v1/users` | X | 회원가입 |
| GET | `/api/v1/users/me` | O | 내 정보 조회 |
| PUT | `/api/v1/users/password` | O | 비밀번호 변경 |

## 상세 스펙

### 1. 회원가입

- **METHOD**: `POST`
- **URI**: `/api/v1/users`
- **인증 필요**: X (비로그인 상태에서 호출 가능)
- **설명**: 새로운 사용자를 등록합니다.

### 2. 내 정보 조회

- **METHOD**: `GET`
- **URI**: `/api/v1/users/me`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)
- **설명**: 로그인한 사용자 본인의 정보를 조회합니다. 타 유저의 정보에는 접근할 수 없습니다.

### 3. 비밀번호 변경

- **METHOD**: `PUT`
- **URI**: `/api/v1/users/password`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)
- **설명**: 로그인한 사용자 본인의 비밀번호를 변경합니다.

## Round1과의 차이점

Round2에서는 다음과 같이 변경됩니다.

| 항목 | Round1 | Round2 |
|------|--------|--------|
| 리소스 경로 | `/api/v1/members` | `/api/v1/users` |
| 인증 방식 | JWT Bearer 토큰 | `X-Loopers-LoginId` / `X-Loopers-LoginPw` 헤더 |
| 비밀번호 변경 메서드 | `PATCH` | `PUT` |
| 비밀번호 변경 경로 | `/api/v1/members/me/password` | `/api/v1/users/password` |
