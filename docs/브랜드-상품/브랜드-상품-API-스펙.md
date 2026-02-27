# 브랜드 & 상품(Brands / Products) API 스펙

브랜드 & 상품 도메인의 요구사항 문서(`docs/브랜드-상품/01-requirements.md`) 작성 시 참고하는 API 명세입니다.

## API 엔드포인트 목록

| METHOD | URI | 인증 필요 | 설명 |
|--------|-----|-----------|------|
| GET | `/api/v1/brands/{brandId}` | X | 브랜드 정보 조회 |
| GET | `/api/v1/products` | X | 상품 목록 조회 |
| GET | `/api/v1/products/{productId}` | X | 상품 정보 조회 |

## 상세 스펙

### 1. 브랜드 정보 조회

- **METHOD**: `GET`
- **URI**: `/api/v1/brands/{brandId}`
- **인증 필요**: X (비로그인 상태에서 호출 가능)
- **설명**: 특정 브랜드의 상세 정보를 조회합니다. 브랜드 ID를 경로 변수로 전달받습니다.

### 2. 상품 목록 조회

- **METHOD**: `GET`
- **URI**: `/api/v1/products`
- **인증 필요**: X (비로그인 상태에서 호출 가능)
- **설명**: 상품 목록을 조회합니다. 쿼리 파라미터를 통한 필터링, 정렬, 페이징을 지원합니다.

#### 쿼리 파라미터

| 파라미터 | 타입 | 필수 여부 | 기본값 | 예시 | 설명 |
|----------|------|-----------|--------|------|------|
| `brandId` | Long | X | - | `1` | 특정 브랜드의 상품만 필터링 |
| `sort` | String | X | `latest` | `latest`, `price_asc`, `likes_desc` | 정렬 기준 |
| `page` | Int | X | `0` | `0` | 페이지 번호 (0부터 시작) |
| `size` | Int | X | `20` | `20` | 페이지당 상품 수 |

#### 정렬 기준

| 값 | 설명 |
|----|------|
| `latest` | 최신 등록순 (필수 구현) |
| `price_asc` | 가격 낮은 순 |
| `likes_desc` | 좋아요 많은 순 |

`latest` 정렬 기준은 필수로 구현해야 합니다.

#### 요청 예시

```http
GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
```

```http
GET /api/v1/products?sort=price_asc&page=1&size=10
```

### 3. 상품 정보 조회

- **METHOD**: `GET`
- **URI**: `/api/v1/products/{productId}`
- **인증 필요**: X (비로그인 상태에서 호출 가능)
- **설명**: 특정 상품의 상세 정보를 조회합니다. 상품 ID를 경로 변수로 전달받습니다.

## 참고 사항

- 브랜드와 상품 조회 API는 모두 인증이 불필요합니다. 비로그인 사용자도 자유롭게 상품을 탐색할 수 있습니다.
- 브랜드와 상품의 등록/수정은 어드민 API(`/api-admin/v1`)를 통해 관리되며, `docs/브랜드-상품-Admin/01-requirements.md`에서 정의합니다.
