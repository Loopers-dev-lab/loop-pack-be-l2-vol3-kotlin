# 주문 Admin API 스펙

주문 Admin 요구사항 문서(`docs/주문-Admin/01-requirements.md`) 작성 시 참고하는 API 명세입니다.
모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 필수입니다.

## API 엔드포인트 목록

| METHOD | URI | LDAP 필수 | 설명 |
|--------|-----|-----------|------|
| GET | `/api-admin/v1/orders?page=0&size=20` | O | 주문 목록 조회 |
| GET | `/api-admin/v1/orders/{orderId}` | O | 단일 주문 상세 조회 |

## 상세 스펙

### 1. 주문 목록 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/orders`
- **LDAP 필수**: O (`X-Loopers-Ldap: loopers.admin` 헤더 필수)
- **설명**: 전체 사용자의 주문 목록을 페이징하여 조회합니다. 고객 API와 달리 모든 유저의 주문을 조회할 수 있습니다.

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 여부 | 기본값 | 예시 | 설명 |
|----------|------|-----------|--------|------|------|
| `page` | Int | X | `0` | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | `20` | 페이지당 주문 수 |

#### 요청 예시

```http
GET /api-admin/v1/orders?page=0&size=20
X-Loopers-Ldap: loopers.admin
```

### 2. 단일 주문 상세 조회

- **METHOD**: `GET`
- **URI**: `/api-admin/v1/orders/{orderId}`
- **LDAP 필수**: O (`X-Loopers-Ldap: loopers.admin` 헤더 필수)
- **설명**: 특정 주문의 상세 내역을 조회합니다. 주문자 정보, 주문 항목, 상품 스냅샷 등 전체 정보를 확인할 수 있습니다.

#### 요청 예시

```http
GET /api-admin/v1/orders/1
X-Loopers-Ldap: loopers.admin
```

## 고객 API vs 어드민 API 비교

| 항목 | 고객 API | 어드민 API |
|------|----------|------------|
| 조회 범위 | 본인 주문만 | 전체 유저의 주문 |
| 인증 방식 | `X-Loopers-LoginId/LoginPw` | `X-Loopers-Ldap` |
| 목록 필터 | 기간 필터(`startAt`, `endAt`) | 페이징(`page`, `size`) |
| 주문자 정보 | 표시 불필요 (본인이므로) | 주문자 정보 포함 필요 |

## 요구사항 작성 시 고려 사항

| 항목 | 고려할 내용 |
|------|-------------|
| 주문 상태 필터 | 주문 목록 조회 시 상태별 필터링 지원 여부 |
| 정렬 기준 | 최신순, 금액순 등 정렬 옵션 필요 여부 |
| 주문자 검색 | 특정 유저의 주문만 필터링하는 파라미터 필요 여부 |
| 상세 조회 정보 범위 | 주문자 정보, 주문 항목, 상품 스냅샷 외 추가 필요 정보 |
