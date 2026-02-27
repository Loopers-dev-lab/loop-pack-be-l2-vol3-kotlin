# 브랜드 & 상품 Admin 요구사항

## 개요
어드민이 브랜드와 상품을 등록, 조회, 수정, 삭제할 수 있는 관리 API를 제공합니다.
모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더를 통해 어드민 권한을 검증하며,
`/api-admin/v1` prefix를 사용하여 대고객 API와 명확히 구분합니다.

## 관련 도메인

| 도메인 | 관계 | 설명 |
|--------|------|------|
| 브랜드(Brand) | 핵심 도메인 | 어드민이 직접 등록/관리하는 대상 |
| 상품(Product) | 핵심 도메인 | 브랜드에 소속된 상품, 어드민이 직접 등록/관리 |
| 유저(User) | 간접 참조 | 대고객 API에서 상품을 조회하는 주체 |
| 좋아요(Like) | 간접 참조 | 유저가 상품에 좋아요를 누르면 좋아요 수가 증가 (상품 삭제 시 영향) |
| 주문(Order) | 간접 참조 | 유저가 상품을 주문할 때 상품 정보를 참조 (상품 삭제 시 스냅샷 필요) |

**브랜드-상품 관계**: 하나의 브랜드에 여러 상품이 소속됩니다 (1:N). 브랜드가 삭제되면 소속 상품도 함께 삭제됩니다.

---

## Part 1: API 명세

### 1.1 인증 방식

모든 어드민 API는 `X-Loopers-Ldap` 헤더를 통해 어드민 권한을 검증합니다.

| 헤더 이름 | 값 | 설명 |
|-----------|-----|------|
| `X-Loopers-Ldap` | `loopers.admin` | LDAP 기반 어드민 식별 값 |

헤더가 없거나 값이 `loopers.admin`이 아닌 경우 `401 UNAUTHORIZED`를 반환합니다.

### 1.2 엔드포인트 목록

#### 브랜드 관리

| METHOD | URI | 설명 |
|--------|-----|------|
| POST | `/api-admin/v1/brands` | 브랜드 등록 |
| GET | `/api-admin/v1/brands?page=0&size=20` | 브랜드 목록 조회 |
| GET | `/api-admin/v1/brands/{brandId}` | 브랜드 상세 조회 |
| PUT | `/api-admin/v1/brands/{brandId}` | 브랜드 정보 수정 |
| DELETE | `/api-admin/v1/brands/{brandId}` | 브랜드 삭제 |

#### 상품 관리

| METHOD | URI | 설명 |
|--------|-----|------|
| POST | `/api-admin/v1/products` | 상품 등록 |
| GET | `/api-admin/v1/products?page=0&size=20&brandId={brandId}` | 상품 목록 조회 |
| GET | `/api-admin/v1/products/{productId}` | 상품 상세 조회 |
| PUT | `/api-admin/v1/products/{productId}` | 상품 정보 수정 |
| DELETE | `/api-admin/v1/products/{productId}` | 상품 삭제 |

---

### 1.3 브랜드 API 상세

#### 1.3.1 브랜드 등록

어드민이 새로운 브랜드를 등록합니다. 등록 시 브랜드 상태는 `ACTIVE`로 설정됩니다.

**Endpoint**
```
POST /api-admin/v1/brands
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "나이키",
  "description": "글로벌 스포츠 브랜드",
  "logoUrl": "https://example.com/nike-logo.png"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~100자, 중복 불가 | 브랜드명 |
| description | String | X | 최대 500자 | 브랜드 설명 |
| logoUrl | String | X | 최대 500자, URL 형식 | 브랜드 로고 URL |

**Response (성공 - 201 Created)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "brandId": 1,
    "name": "나이키",
    "description": "글로벌 스포츠 브랜드",
    "logoUrl": "https://example.com/nike-logo.png",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| LDAP 헤더 없음/불일치 | 401 | UNAUTHORIZED | 인증이 필요합니다. |
| 브랜드명 누락 | 400 | Bad Request | 브랜드명은 필수입니다. |
| 브랜드명 100자 초과 | 400 | Bad Request | 브랜드명은 100자 이하여야 합니다. |
| 브랜드명 중복 | 409 | Conflict | 이미 존재하는 브랜드명입니다. |
| 브랜드 설명 500자 초과 | 400 | Bad Request | 브랜드 설명은 500자 이하여야 합니다. |
| 로고 URL 형식 오류 | 400 | Bad Request | 유효하지 않은 URL 형식입니다. |

#### 1.3.2 브랜드 목록 조회

어드민이 등록된 브랜드 목록을 페이징하여 조회합니다. 각 브랜드의 소속 상품 수를 함께 표시합니다.

**Endpoint**
```
GET /api-admin/v1/brands?page=0&size=20
X-Loopers-Ldap: loopers.admin
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Int | X | 0 | 페이지 번호 (0부터 시작) |
| size | Int | X | 20 | 페이지당 항목 수 |

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "content": [
      {
        "brandId": 1,
        "name": "나이키",
        "description": "글로벌 스포츠 브랜드",
        "logoUrl": "https://example.com/nike-logo.png",
        "status": "ACTIVE",
        "productCount": 15,
        "createdAt": "2024-01-01T00:00:00+09:00",
        "updatedAt": "2024-01-01T00:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

#### 1.3.3 브랜드 상세 조회

어드민이 특정 브랜드의 상세 정보를 조회합니다. 해당 브랜드의 소속 상품 수를 함께 표시합니다.

**Endpoint**
```
GET /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "brandId": 1,
    "name": "나이키",
    "description": "글로벌 스포츠 브랜드",
    "logoUrl": "https://example.com/nike-logo.png",
    "status": "ACTIVE",
    "productCount": 15,
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |

#### 1.3.4 브랜드 정보 수정

어드민이 기존 브랜드의 정보를 수정합니다. 브랜드 상태를 `ACTIVE`/`INACTIVE`로 변경할 수 있습니다.

**Endpoint**
```
PUT /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "나이키 코리아",
  "description": "글로벌 스포츠 브랜드 (한국 공식)",
  "logoUrl": "https://example.com/nike-korea-logo.png",
  "status": "ACTIVE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~100자, 중복 불가 (본인 제외) | 브랜드명 |
| description | String | X | 최대 500자 | 브랜드 설명 |
| logoUrl | String | X | 최대 500자, URL 형식 | 브랜드 로고 URL |
| status | String | O | ACTIVE 또는 INACTIVE | 브랜드 상태 |

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "brandId": 1,
    "name": "나이키 코리아",
    "description": "글로벌 스포츠 브랜드 (한국 공식)",
    "logoUrl": "https://example.com/nike-korea-logo.png",
    "status": "ACTIVE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-15T10:30:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |
| 브랜드명 중복 (다른 브랜드) | 409 | Conflict | 이미 존재하는 브랜드명입니다. |
| 유효하지 않은 상태 값 | 400 | Bad Request | 유효하지 않은 브랜드 상태입니다. |

#### 1.3.5 브랜드 삭제

어드민이 브랜드를 삭제합니다. 브랜드 삭제 시 해당 브랜드에 소속된 모든 상품도 함께 소프트 삭제됩니다.

**Endpoint**
```
DELETE /api-admin/v1/brands/{brandId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
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

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |

---

### 1.4 상품 API 상세

#### 1.4.1 상품 등록

어드민이 특정 브랜드에 소속된 새로운 상품을 등록합니다. 지정한 브랜드가 실제로 존재하고 활성 상태여야 합니다.

**Endpoint**
```
POST /api-admin/v1/products
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "에어맥스 90",
  "description": "클래식 러닝화",
  "price": 179000,
  "brandId": 1,
  "saleStatus": "SELLING",
  "stockQuantity": 100,
  "displayStatus": "VISIBLE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~200자 | 상품명 |
| description | String | X | 최대 1000자 | 상품 설명 |
| price | Long | O | 0 이상, 최대 100,000,000 | 상품 가격 (원 단위) |
| brandId | Long | O | 존재하는 활성 브랜드 | 소속 브랜드 ID |
| saleStatus | String | O | SELLING 또는 STOP_SELLING | 판매 상태 |
| stockQuantity | Int | O | 0 이상, 최대 999,999 | 재고 수량 |
| displayStatus | String | O | VISIBLE 또는 HIDDEN | 노출 상태 |

**Response (성공 - 201 Created)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "productId": 1,
    "name": "에어맥스 90",
    "description": "클래식 러닝화",
    "price": 179000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 100,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 상품명 누락 | 400 | Bad Request | 상품명은 필수입니다. |
| 상품명 200자 초과 | 400 | Bad Request | 상품명은 200자 이하여야 합니다. |
| 가격 음수 | 400 | Bad Request | 가격은 0 이상이어야 합니다. |
| 가격 상한 초과 | 400 | Bad Request | 가격은 100,000,000원 이하여야 합니다. |
| 존재하지 않는 브랜드 | 404 | Not Found | 존재하지 않는 브랜드입니다. |
| 비활성 브랜드 | 400 | Bad Request | 비활성 브랜드에는 상품을 등록할 수 없습니다. |
| 재고 수량 음수 | 400 | Bad Request | 재고 수량은 0 이상이어야 합니다. |
| 유효하지 않은 판매 상태 | 400 | Bad Request | 유효하지 않은 판매 상태입니다. |
| 유효하지 않은 노출 상태 | 400 | Bad Request | 유효하지 않은 노출 상태입니다. |

#### 1.4.2 상품 목록 조회

어드민이 등록된 상품 목록을 페이징하여 조회합니다. `brandId` 파라미터로 특정 브랜드의 상품만 필터링할 수 있습니다.

**Endpoint**
```
GET /api-admin/v1/products?page=0&size=20&brandId=1
X-Loopers-Ldap: loopers.admin
```

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | Int | X | 0 | 페이지 번호 (0부터 시작) |
| size | Int | X | 20 | 페이지당 항목 수 |
| brandId | Long | X | - | 특정 브랜드의 상품만 필터링 |

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "에어맥스 90",
        "description": "클래식 러닝화",
        "price": 179000,
        "brand": {
          "brandId": 1,
          "name": "나이키"
        },
        "saleStatus": "SELLING",
        "stockQuantity": 100,
        "displayStatus": "VISIBLE",
        "createdAt": "2024-01-01T00:00:00+09:00",
        "updatedAt": "2024-01-01T00:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

#### 1.4.3 상품 상세 조회

어드민이 특정 상품의 상세 정보를 조회합니다.

**Endpoint**
```
GET /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "productId": 1,
    "name": "에어맥스 90",
    "description": "클래식 러닝화",
    "price": 179000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 100,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-01T00:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

#### 1.4.4 상품 정보 수정

어드민이 기존 상품의 정보를 수정합니다. 상품의 소속 브랜드는 변경할 수 없습니다.

**Endpoint**
```
PUT /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Request Body**
```json
{
  "name": "에어맥스 90 리뉴얼",
  "description": "클래식 러닝화 (2024 리뉴얼 에디션)",
  "price": 189000,
  "saleStatus": "SELLING",
  "stockQuantity": 50,
  "displayStatus": "VISIBLE"
}
```

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|------|------|------|----------|------|
| name | String | O | 1~200자 | 상품명 |
| description | String | X | 최대 1000자 | 상품 설명 |
| price | Long | O | 0 이상, 최대 100,000,000 | 상품 가격 (원 단위) |
| saleStatus | String | O | SELLING 또는 STOP_SELLING | 판매 상태 |
| stockQuantity | Int | O | 0 이상, 최대 999,999 | 재고 수량 |
| displayStatus | String | O | VISIBLE 또는 HIDDEN | 노출 상태 |

**주의**: `brandId`는 요청 본문에 포함하지 않습니다. 상품의 브랜드는 변경할 수 없습니다.

**Response (성공 - 200 OK)**
```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "productId": 1,
    "name": "에어맥스 90 리뉴얼",
    "description": "클래식 러닝화 (2024 리뉴얼 에디션)",
    "price": 189000,
    "brand": {
      "brandId": 1,
      "name": "나이키"
    },
    "saleStatus": "SELLING",
    "stockQuantity": 50,
    "displayStatus": "VISIBLE",
    "createdAt": "2024-01-01T00:00:00+09:00",
    "updatedAt": "2024-01-15T14:00:00+09:00"
  }
}
```

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

#### 1.4.5 상품 삭제

어드민이 상품을 삭제합니다. 소프트 삭제 방식으로 처리됩니다.

**Endpoint**
```
DELETE /api-admin/v1/products/{productId}
X-Loopers-Ldap: loopers.admin
```

**Response (성공 - 200 OK)**
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

**Response (실패)**

| 상황 | HTTP 상태 | errorCode | message |
|------|-----------|-----------|---------|
| 존재하지 않는 상품 | 404 | Not Found | 존재하지 않는 상품입니다. |

---

## Part 2: 비즈니스 규칙

### 2.1 필수 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 어드민 인증 필수 | 모든 어드민 API는 `X-Loopers-Ldap: loopers.admin` 헤더가 필요합니다 |
| 2 | 브랜드 삭제 시 연쇄 소프트 삭제 | 브랜드를 삭제하면 해당 브랜드에 소속된 모든 상품도 함께 소프트 삭제됩니다 |
| 3 | 상품 등록 시 브랜드 존재 및 활성 검증 | 상품 등록 시 지정한 브랜드가 존재하고 `ACTIVE` 상태여야 합니다 |
| 4 | 상품 브랜드 변경 불가 | 상품 수정 시 소속 브랜드를 다른 브랜드로 변경할 수 없습니다 |
| 5 | 브랜드명 중복 불가 | 동일한 이름의 브랜드를 중복 등록할 수 없습니다 (대소문자 구분) |
| 6 | 소프트 삭제 적용 | 브랜드와 상품 삭제 시 `deletedAt` 필드를 설정하여 논리적으로 삭제합니다 |
| 7 | 삭제된 항목 조회 제외 | 목록/상세 조회 시 소프트 삭제된 항목은 조회 대상에서 제외됩니다 |

### 2.2 입력 검증 규칙

#### 브랜드

| # | 필드 | 규칙 | 검증 시점 |
|---|------|------|-----------|
| 1 | name | 필수, 1~100자, 공백만으로 구성 불가 | 등록/수정 |
| 2 | name | 중복 불가 (삭제되지 않은 브랜드 기준, 수정 시 자기 자신 제외) | 등록/수정 |
| 3 | description | 선택, 최대 500자 | 등록/수정 |
| 4 | logoUrl | 선택, 최대 500자, 입력 시 URL 형식 검증 | 등록/수정 |
| 5 | status | `ACTIVE` 또는 `INACTIVE`만 허용 | 수정 |

#### 상품

| # | 필드 | 규칙 | 검증 시점 |
|---|------|------|-----------|
| 1 | name | 필수, 1~200자, 공백만으로 구성 불가 | 등록/수정 |
| 2 | price | 필수, 0 이상 100,000,000 이하 (원 단위) | 등록/수정 |
| 3 | brandId | 필수, 존재하는 활성 브랜드 ID | 등록 |
| 4 | description | 선택, 최대 1000자 | 등록/수정 |
| 5 | saleStatus | 필수, `SELLING` 또는 `STOP_SELLING` | 등록/수정 |
| 6 | stockQuantity | 필수, 0 이상 999,999 이하 | 등록/수정 |
| 7 | displayStatus | 필수, `VISIBLE` 또는 `HIDDEN` | 등록/수정 |

### 2.3 삭제 및 데이터 보전 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 소프트 삭제 | `BaseEntity.delete()` 메서드를 사용하여 `deletedAt`을 현재 시각으로 설정 |
| 2 | 브랜드 연쇄 삭제 | 브랜드 소프트 삭제 시, 해당 브랜드에 소속된 모든 상품도 동시에 소프트 삭제 |
| 3 | 스냅샷 고려 | 주문 도메인 구현 시 상품 정보 스냅샷을 주문 시점에 저장하여, 이후 상품이 삭제/수정되더라도 주문 이력에서 원본 정보를 보존 |

### 2.4 대고객 API와의 연동 규칙

| # | 규칙 | 설명 |
|---|------|------|
| 1 | 브랜드 비활성 시 상품 비노출 | `INACTIVE` 상태의 브랜드에 소속된 상품은 대고객 API에서 조회되지 않아야 합니다 |
| 2 | 상품 숨김 시 비노출 | `HIDDEN` 상태의 상품은 대고객 API에서 조회되지 않아야 합니다 |
| 3 | 판매 중지 상품 | `STOP_SELLING` 상태의 상품은 대고객 API에서 조회는 가능하지만 주문할 수 없습니다 |

---

## Part 3: 도메인 모델

### 3.1 Enum 정의

#### BrandStatus
```kotlin
enum class BrandStatus {
    ACTIVE,     // 활성: 정상 운영 중인 브랜드
    INACTIVE    // 비활성: 운영 중단된 브랜드 (소속 상품 대고객 비노출)
}
```

#### SaleStatus
```kotlin
enum class SaleStatus {
    SELLING,       // 판매 중: 주문 가능
    STOP_SELLING   // 판매 중지: 조회 가능하지만 주문 불가
}
```

#### DisplayStatus
```kotlin
enum class DisplayStatus {
    VISIBLE,   // 노출: 대고객 API에서 조회 가능
    HIDDEN     // 숨김: 대고객 API에서 조회 불가, 어드민에서만 관리
}
```

### 3.2 Brand 모델

| 필드 | 타입 | DB 컬럼 | 제약조건 | 설명 |
|------|------|---------|----------|------|
| id | Long | id | PK, auto-increment | 브랜드 ID (BaseEntity) |
| name | String | name | NOT NULL, UNIQUE, max 100 | 브랜드명 |
| description | String? | description | max 500 | 브랜드 설명 |
| logoUrl | String? | logo_url | max 500 | 브랜드 로고 URL |
| status | BrandStatus | status | NOT NULL, default ACTIVE | 브랜드 상태 |
| createdAt | ZonedDateTime | created_at | NOT NULL | 등록일시 (BaseEntity) |
| updatedAt | ZonedDateTime | updated_at | NOT NULL | 수정일시 (BaseEntity) |
| deletedAt | ZonedDateTime? | deleted_at | nullable | 삭제일시 (BaseEntity, 소프트 삭제) |

### 3.3 Product 모델

| 필드 | 타입 | DB 컬럼 | 제약조건 | 설명 |
|------|------|---------|----------|------|
| id | Long | id | PK, auto-increment | 상품 ID (BaseEntity) |
| name | String | name | NOT NULL, max 200 | 상품명 |
| description | String? | description | max 1000 | 상품 설명 |
| price | Long | price | NOT NULL, >= 0 | 상품 가격 (원 단위) |
| brandId | Long | brand_id | NOT NULL, FK | 소속 브랜드 ID |
| saleStatus | SaleStatus | sale_status | NOT NULL | 판매 상태 |
| stockQuantity | Int | stock_quantity | NOT NULL, >= 0 | 재고 수량 |
| displayStatus | DisplayStatus | display_status | NOT NULL | 노출 상태 |
| createdAt | ZonedDateTime | created_at | NOT NULL | 등록일시 (BaseEntity) |
| updatedAt | ZonedDateTime | updated_at | NOT NULL | 수정일시 (BaseEntity) |
| deletedAt | ZonedDateTime? | deleted_at | nullable | 삭제일시 (BaseEntity, 소프트 삭제) |

---

## Part 4: 구현 컴포넌트

### 4.1 레이어별 구조

```
support/
  └── filter/
      └── AdminLdapAuthenticationFilter.kt   # 어드민 LDAP 인증 필터

domain/brand/
  ├── BrandModel.kt          # 브랜드 도메인 모델 (엔티티)
  ├── BrandStatus.kt         # 브랜드 상태 enum
  ├── BrandService.kt        # 브랜드 도메인 서비스
  └── BrandRepository.kt     # 브랜드 레포지토리 인터페이스

domain/product/
  ├── ProductModel.kt        # 상품 도메인 모델 (엔티티)
  ├── SaleStatus.kt          # 판매 상태 enum
  ├── DisplayStatus.kt       # 노출 상태 enum
  ├── ProductService.kt      # 상품 도메인 서비스
  └── ProductRepository.kt   # 상품 레포지토리 인터페이스

infrastructure/brand/
  ├── BrandJpaRepository.kt      # Spring Data JPA 레포지토리
  └── BrandRepositoryImpl.kt     # 도메인 레포지토리 구현체

infrastructure/product/
  ├── ProductJpaRepository.kt    # Spring Data JPA 레포지토리
  └── ProductRepositoryImpl.kt   # 도메인 레포지토리 구현체

application/brand/
  ├── BrandFacade.kt         # 브랜드 애플리케이션 퍼사드
  └── BrandInfo.kt           # 브랜드 정보 전달 객체

application/product/
  ├── ProductFacade.kt       # 상품 애플리케이션 퍼사드
  └── ProductInfo.kt         # 상품 정보 전달 객체

interfaces/api/admin/brand/
  ├── AdminBrandV1Controller.kt  # 브랜드 어드민 컨트롤러
  ├── AdminBrandV1ApiSpec.kt     # OpenAPI 스펙 인터페이스
  └── AdminBrandV1Dto.kt        # Request/Response DTO

interfaces/api/admin/product/
  ├── AdminProductV1Controller.kt  # 상품 어드민 컨트롤러
  ├── AdminProductV1ApiSpec.kt     # OpenAPI 스펙 인터페이스
  └── AdminProductV1Dto.kt        # Request/Response DTO
```

### 4.2 처리 흐름

#### 브랜드 등록 흐름
```
AdminBrandV1Controller (POST /api-admin/v1/brands)
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → AdminBrandV1Dto.CreateBrandRequest 입력 검증 (init 블록)
  → BrandFacade.createBrand(name, description, logoUrl)
    → BrandService.create(name, description, logoUrl)
      → BrandRepository.existsByName(name) : 중복 검증
      → BrandModel(name, description, logoUrl) : 모델 생성 (init 블록에서 필드 검증)
      → BrandRepository.save(brand) : 저장
    → BrandInfo.from(brand) : Info 변환
  → AdminBrandV1Dto.BrandResponse.from(info) : Response 변환
  → ApiResponse.success(response) : 응답 반환
```

#### 브랜드 삭제 흐름 (연쇄 삭제 포함)
```
AdminBrandV1Controller (DELETE /api-admin/v1/brands/{brandId})
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → BrandFacade.deleteBrand(brandId)
    → BrandService.findById(brandId) : 브랜드 조회
    → ProductService.softDeleteAllByBrandId(brandId) : 소속 상품 일괄 소프트 삭제
    → BrandService.delete(brandId) : 브랜드 소프트 삭제
  → ApiResponse.success() : 응답 반환
```

#### 상품 등록 흐름
```
AdminProductV1Controller (POST /api-admin/v1/products)
  → AdminLdapAuthenticationFilter에서 LDAP 헤더 검증
  → AdminProductV1Dto.CreateProductRequest 입력 검증 (init 블록)
  → ProductFacade.createProduct(name, description, price, brandId, saleStatus, stockQuantity, displayStatus)
    → BrandService.findById(brandId) : 브랜드 존재 및 활성 상태 검증
    → ProductService.create(...) : 상품 생성
      → ProductModel(...) : 모델 생성 (init 블록에서 필드 검증)
      → ProductRepository.save(product) : 저장
    → ProductInfo.from(product, brand) : Info 변환
  → AdminProductV1Dto.ProductResponse.from(info) : Response 변환
  → ApiResponse.success(response) : 응답 반환
```

---

## Part 5: 구현 체크리스트

### Phase 1: 어드민 인증 필터 + Enum 정의

**목표**: 어드민 API의 LDAP 인증 인프라를 구축하고, 브랜드/상품에 사용할 Enum을 정의합니다.

- [ ] `AdminLdapAuthenticationFilter` 구현 (`/api-admin/v1/**` 경로에 적용)
- [ ] LDAP 헤더 누락/불일치 시 `UNAUTHORIZED` 에러 반환
- [ ] `BrandStatus` enum 정의 (ACTIVE, INACTIVE)
- [ ] `SaleStatus` enum 정의 (SELLING, STOP_SELLING)
- [ ] `DisplayStatus` enum 정의 (VISIBLE, HIDDEN)
- [ ] `AdminLdapAuthenticationFilter` 단위 테스트

### Phase 2: Brand 도메인 모델 + Repository

**목표**: 브랜드 도메인 모델, 레포지토리 인터페이스 및 구현체를 작성합니다.

- [ ] `BrandModel` 엔티티 생성 (BaseEntity 상속, init 블록에서 필드 검증)
- [ ] `BrandRepository` 인터페이스 정의 (save, findById, existsByName, findAll 등)
- [ ] `BrandJpaRepository` + `BrandRepositoryImpl` 구현
- [ ] `BrandModelTest` 단위 테스트 (필드 검증, 상태 변경, 소프트 삭제)

### Phase 3: Brand CRUD 서비스 + Facade + Controller

**목표**: 브랜드 CRUD API를 완성합니다.

- [ ] `BrandService` 구현 (create, findById, findAll, update, delete)
- [ ] `BrandInfo` 정보 전달 객체 생성
- [ ] `BrandFacade` 구현 (CRUD 오케스트레이션)
- [ ] `AdminBrandV1Dto` Request/Response DTO 생성
- [ ] `AdminBrandV1ApiSpec` OpenAPI 스펙 인터페이스
- [ ] `AdminBrandV1Controller` 구현
- [ ] `BrandServiceTest` 단위 테스트
- [ ] `BrandFacadeTest` 단위 테스트
- [ ] `AdminBrandV1Controller` 통합 테스트

### Phase 4: Product 도메인 모델 + Repository

**목표**: 상품 도메인 모델, 레포지토리 인터페이스 및 구현체를 작성합니다.

- [ ] `ProductModel` 엔티티 생성 (BaseEntity 상속, brandId FK, init 블록에서 필드 검증)
- [ ] `ProductRepository` 인터페이스 정의 (save, findById, findAllByBrandId, softDeleteAllByBrandId 등)
- [ ] `ProductJpaRepository` + `ProductRepositoryImpl` 구현
- [ ] `ProductModelTest` 단위 테스트 (필드 검증, 상태 변경, 소프트 삭제)

### Phase 5: Product CRUD 서비스 + Facade + Controller

**목표**: 상품 CRUD API를 완성합니다.

- [ ] `ProductService` 구현 (create, findById, findAll, update, delete, softDeleteAllByBrandId)
- [ ] `ProductInfo` 정보 전달 객체 생성
- [ ] `ProductFacade` 구현 (CRUD 오케스트레이션, 브랜드 존재/활성 검증 포함)
- [ ] `AdminProductV1Dto` Request/Response DTO 생성
- [ ] `AdminProductV1ApiSpec` OpenAPI 스펙 인터페이스
- [ ] `AdminProductV1Controller` 구현
- [ ] `ProductServiceTest` 단위 테스트
- [ ] `ProductFacadeTest` 단위 테스트
- [ ] `AdminProductV1Controller` 통합 테스트

### Phase 6: 연쇄 삭제 + E2E 테스트

**목표**: 브랜드 삭제 시 연쇄 삭제 로직을 검증하고, 전체 시나리오 E2E 테스트를 작성합니다.

- [ ] 브랜드 삭제 시 소속 상품 연쇄 소프트 삭제 통합 테스트
- [ ] 삭제된 브랜드/상품이 목록 조회에서 제외되는지 검증
- [ ] 브랜드/상품 CRUD 전체 시나리오 E2E 테스트

### Phase 7: HTTP 테스트 파일

**목표**: IntelliJ HTTP Client 형식의 API 테스트 파일을 작성합니다.

- [ ] `.http/admin/brand/create-brand.http` 생성
- [ ] `.http/admin/brand/get-brands.http` 생성
- [ ] `.http/admin/brand/get-brand.http` 생성
- [ ] `.http/admin/brand/update-brand.http` 생성
- [ ] `.http/admin/brand/delete-brand.http` 생성
- [ ] `.http/admin/product/create-product.http` 생성
- [ ] `.http/admin/product/get-products.http` 생성
- [ ] `.http/admin/product/get-product.http` 생성
- [ ] `.http/admin/product/update-product.http` 생성
- [ ] `.http/admin/product/delete-product.http` 생성

---

## Part 6: 테스트 시나리오

### 6.1 어드민 인증 필터 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 유효한 LDAP 헤더 | `X-Loopers-Ldap: loopers.admin` | 요청 통과 |
| LDAP 헤더 없음 | 헤더 누락 | 401 UNAUTHORIZED |
| 잘못된 LDAP 값 | `X-Loopers-Ldap: invalid` | 401 UNAUTHORIZED |
| 대고객 API 경로 | `/api/v1/products` (LDAP 헤더 없음) | 필터 미적용, 요청 통과 |

### 6.2 브랜드 등록 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 등록 (모든 필드) | name, description, logoUrl | 201 Created + 브랜드 정보 |
| 정상 등록 (필수 필드만) | name만 | 201 Created + description/logoUrl null |
| 브랜드명 누락 | name 없음 | 400 Bad Request |
| 브랜드명 빈 문자열 | name = "" | 400 Bad Request |
| 브랜드명 공백만 | name = "   " | 400 Bad Request |
| 브랜드명 100자 초과 | 101자 name | 400 Bad Request |
| 브랜드명 중복 | 이미 존재하는 name | 409 Conflict |
| 로고 URL 형식 오류 | logoUrl = "not-a-url" | 400 Bad Request |
| 설명 500자 초과 | 501자 description | 400 Bad Request |
| LDAP 인증 실패 | 헤더 없음 | 401 UNAUTHORIZED |

### 6.3 브랜드 목록 조회 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 기본 조회 | page=0, size=20 | 200 OK + 페이징 정보 |
| 빈 목록 | 브랜드 없음 | 200 OK + 빈 content 배열 |
| 페이징 동작 확인 | 21개 브랜드, size=20 | totalPages=2, 첫 페이지 20개 |
| 삭제된 브랜드 제외 | 소프트 삭제된 브랜드 존재 | 삭제된 브랜드 미포함 |
| 상품 수 정확성 | 상품이 등록된 브랜드 | productCount 정확히 반영 |

### 6.4 브랜드 수정 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 수정 | 유효한 필드 값 | 200 OK + 수정된 정보 |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 다른 브랜드와 이름 중복 | 다른 브랜드의 name | 409 Conflict |
| 자기 자신과 이름 동일 | 기존 name 유지 | 200 OK (중복 아님) |
| 상태 INACTIVE 변경 | status = "INACTIVE" | 200 OK + 상태 변경됨 |

### 6.5 브랜드 삭제 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 삭제 (상품 없음) | 유효한 brandId | 200 OK |
| 정상 삭제 (상품 있음) | 상품이 소속된 brandId | 200 OK + 소속 상품도 소프트 삭제 |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 삭제 후 목록 조회 | 삭제된 brandId | 목록에서 제외됨 |
| 삭제 후 상세 조회 | 삭제된 brandId | 404 Not Found |

### 6.6 상품 등록 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 등록 | 모든 필수 필드 | 201 Created + 상품 정보 |
| 상품명 누락 | name 없음 | 400 Bad Request |
| 가격 음수 | price = -1 | 400 Bad Request |
| 가격 상한 초과 | price = 100,000,001 | 400 Bad Request |
| 존재하지 않는 브랜드 | 없는 brandId | 404 Not Found |
| 비활성 브랜드 | INACTIVE 브랜드의 brandId | 400 Bad Request |
| 재고 음수 | stockQuantity = -1 | 400 Bad Request |
| 유효하지 않은 판매 상태 | saleStatus = "INVALID" | 400 Bad Request |
| 유효하지 않은 노출 상태 | displayStatus = "INVALID" | 400 Bad Request |

### 6.7 상품 목록 조회 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 전체 조회 | page=0, size=20 | 200 OK + 전체 상품 페이징 |
| 브랜드별 필터링 | brandId=1 | 해당 브랜드 상품만 반환 |
| 삭제된 상품 제외 | 소프트 삭제된 상품 존재 | 삭제된 상품 미포함 |

### 6.8 상품 수정 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 수정 | 유효한 필드 값 | 200 OK + 수정된 정보 |
| 존재하지 않는 상품 | 없는 productId | 404 Not Found |
| 브랜드 불변 확인 | 응답에 기존 브랜드 정보 유지 | brandId 변경되지 않음 |
| 판매 중지로 변경 | saleStatus = "STOP_SELLING" | 200 OK + 상태 변경됨 |
| 숨김으로 변경 | displayStatus = "HIDDEN" | 200 OK + 상태 변경됨 |

### 6.9 상품 삭제 테스트

| 시나리오 | 입력 | 기대 결과 |
|---------|------|----------|
| 정상 삭제 | 유효한 productId | 200 OK |
| 존재하지 않는 상품 | 없는 productId | 404 Not Found |
| 삭제 후 목록 조회 | 삭제된 productId | 목록에서 제외됨 |

### 6.10 연쇄 삭제 E2E 테스트

| 시나리오 | 조건 | 기대 결과 |
|---------|------|----------|
| 브랜드 삭제 시 상품 연쇄 삭제 | 브랜드에 상품 3개 소속 | 브랜드 + 상품 3개 모두 소프트 삭제 |
| 연쇄 삭제 후 상품 목록 조회 | 위 시나리오 이후 | 해당 상품 3개 모두 목록에서 제외 |

---

## Part 7: 보안 고려사항

### 7.1 인증/인가
- 모든 어드민 API에 `AdminLdapAuthenticationFilter`를 적용하여 LDAP 헤더를 검증합니다
- 대고객 API(`/api/v1/**`)와 어드민 API(`/api-admin/v1/**`)의 경로를 분리하여 권한 체계를 명확히 구분합니다
- 필터는 `/api-admin/v1/**` 경로에만 적용되며, 다른 경로의 요청에는 영향을 주지 않습니다

### 7.2 입력 검증
- 모든 입력 값은 서버 사이드에서 검증합니다 (DTO init 블록 + 도메인 모델 init 블록)
- SQL Injection 방지: JPA 파라미터 바인딩을 통해 안전하게 쿼리를 실행합니다
- XSS 방지: 입력 문자열의 HTML 태그를 이스케이프 처리합니다 (필요 시)

### 7.3 데이터 보전
- 소프트 삭제를 사용하여 데이터 복구 가능성을 유지합니다
- 주문/좋아요 등 다른 도메인에서 참조하는 상품 데이터의 정합성을 보전합니다
- 브랜드 삭제 시 연쇄 삭제는 단일 트랜잭션 내에서 처리하여 데이터 일관성을 보장합니다

### 7.4 에러 메시지
- 에러 메시지에 내부 구현 세부사항이나 스택 트레이스를 노출하지 않습니다
- 사용자 친화적인 메시지를 반환합니다

---

## Part 8: 검증 명령어

```bash
# 전체 테스트
./gradlew :apps:commerce-api:test

# ktlint 검사
./gradlew :apps:commerce-api:ktlintCheck

# ktlint 자동 수정
./gradlew :apps:commerce-api:ktlintFormat

# 빌드
./gradlew :apps:commerce-api:build

# 테스트 커버리지 리포트
./gradlew :apps:commerce-api:test jacocoTestReport
```

`.http/admin/brand/*.http` 및 `.http/admin/product/*.http` 파일로 수동 API 테스트가 가능합니다.

---

## 품질 체크리스트

- [x] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [x] 기능 요구사항이 유저 중심("어드민이 ~한다")으로 서술되어 있는가?
- [x] 인증 방식(LDAP 헤더 기반)이 정확히 명시되어 있는가?
- [x] 에러 케이스와 예외 상황이 포함되어 있는가?
- [x] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
- [x] 소프트 삭제 및 스냅샷 고려사항이 반영되어 있는가?
- [x] 입력 검증 규칙(중복 브랜드명, 가격 범위, URL 형식 등)이 상세히 정의되어 있는가?
- [x] 대고객 API와의 연동 규칙(비활성 브랜드, 숨김 상품 비노출 등)이 명시되어 있는가?
