# 좋아요(Likes) API 스펙

좋아요 요구사항 문서(`docs/좋아요/01-requirements.md`) 작성 시 참고하는 API 명세입니다.
모든 좋아요 API는 로그인이 필요합니다(`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수).

## API 엔드포인트 목록

| METHOD | URI | 인증 필요 | 설명 |
|--------|-----|-----------|------|
| POST | `/api/v1/products/{productId}/likes` | O | 상품 좋아요 등록 |
| DELETE | `/api/v1/products/{productId}/likes` | O | 상품 좋아요 취소 |
| GET | `/api/v1/users/{userId}/likes` | O | 내가 좋아요 한 상품 목록 조회 |

## 상세 스펙

### 1. 상품 좋아요 등록

- **METHOD**: `POST`
- **URI**: `/api/v1/products/{productId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)
- **설명**: 특정 상품에 좋아요를 등록합니다. 이미 좋아요한 상품에 다시 요청할 경우의 처리 방식을 요구사항에서 정의해야 합니다.

### 2. 상품 좋아요 취소

- **METHOD**: `DELETE`
- **URI**: `/api/v1/products/{productId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)
- **설명**: 특정 상품에 등록한 좋아요를 취소합니다. 좋아요하지 않은 상품에 취소 요청할 경우의 처리 방식을 요구사항에서 정의해야 합니다.

### 3. 내가 좋아요 한 상품 목록 조회

- **METHOD**: `GET`
- **URI**: `/api/v1/users/{userId}/likes`
- **인증 필요**: O (`X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 필수)
- **설명**: 로그인한 사용자가 좋아요한 상품 목록을 조회합니다. 본인의 좋아요 목록만 조회 가능하며, 타 유저의 목록에는 접근할 수 없습니다.

## 요구사항 작성 시 고려 사항

| 항목 | 고려할 내용 |
|------|-------------|
| 중복 좋아요 처리 | 이미 좋아요한 상품에 다시 좋아요 요청 시 에러 반환 또는 멱등 처리 |
| 좋아요 취소 예외 | 좋아요하지 않은 상품에 취소 요청 시 에러 반환 또는 무시 |
| 본인 확인 | `userId`가 로그인한 사용자 본인인지 검증 필요 |
| 삭제된 상품 | 좋아요한 상품이 삭제된 경우 목록에서의 처리 방식 |
| 좋아요 수 반영 | 상품 조회 API(`/api/v1/products`)에서 좋아요 수를 함께 제공할지 여부 |
| 페이징 | 좋아요 목록 조회 시 페이징 지원 여부 및 파라미터 |
