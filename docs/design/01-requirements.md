# 요구사항 명세서

## 1. 범위 및 목적

### 본 문서의 대상

이커머스 플랫폼 백엔드 **P0 요구사항**을 정의한다.

### 핵심 설계 원칙

- **DDD 기반 레이어드 아키텍처**: Interfaces → Domain → Infrastructure
- **Soft Delete 기본**: `BaseEntity.delete()` / `BaseEntity.restore()`를 통한 논리 삭제
- **FK 미사용**: 앱 레벨 정합성 관리 (데드락, 마이그레이션, 성능 이유)

### 포함하는 것

| 도메인 | 설명 |
|--------|------|
| **Users** | 회원가입, 내 정보 조회, 비밀번호 변경 |
| **Brands** | 브랜드 CRUD (Customer 조회 + Admin 관리) |
| **Products** | 상품 CRUD + 필터/정렬/페이징 (Customer 조회 + Admin 관리) |
| **Likes** | 상품 좋아요 등록/취소/목록 |
| **Orders** | 주문 생성, 기간별 조회, 상세 조회 (Customer + Admin) |

### 포함하지 않는 것

- 결제 (Payment) — 추후 확장
- 쿠폰 (Coupon) — 추후 확장
- 랭킹/추천 — 좋아요 데이터를 기반으로 추후 확장
- 배송 — 추후 확장

---

## 2. 액터 및 인증

### 액터 정의

| 액터 | 설명 | 인증 방식 |
|------|------|-----------|
| **Customer** | 일반 사용자. 상품 탐색, 좋아요, 주문을 수행한다 | `X-Loopers-LoginId` + `X-Loopers-LoginPw` 헤더 |
| **Admin** | 사내 어드민. 브랜드/상품/주문을 관리한다 | `X-Loopers-Ldap: loopers.admin` 헤더 (LDAP 인증) |

### Customer 인증

- HTTP 헤더 기반 인증 (세션/토큰 없음)
- 요청마다 `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더를 전송한다
- 인증 실패 시나리오:

| 시나리오 | HTTP 상태 | 설명 |
|----------|-----------|------|
| 인증 헤더 누락 | 401 UNAUTHORIZED | `X-Loopers-LoginId` 또는 `X-Loopers-LoginPw` 헤더가 없음 |
| 사용자 미존재 | 404 NOT_FOUND | 헤더에 담긴 사용자가 DB에 없음 ⚠️ 변경 가능 → [DEC-007](decisions/DEC-007-user-not-found-response-code.md) |
| 비밀번호 불일치 | 401 UNAUTHORIZED | 사용자는 존재하지만 비밀번호가 틀림 |

### Admin 인증

- `X-Loopers-Ldap: loopers.admin` 헤더로 인증한다
- LDAP(Lightweight Directory Access Protocol) 기반 사내 인증을 모사한다

---

## 3. 기능적 요구사항

### 3.1 유저 (Users)

#### 유저 스토리

> 사용자는 이커머스 서비스를 이용하기 위해 회원가입하고, 자신의 정보를 확인하고, 비밀번호를 변경할 수 있어야 한다.

- **사용자 관점**: 간편하게 가입하고, 내 정보를 확인하고, 비밀번호를 바꿀 수 있어야 한다
- **비즈니스 관점**: 사용자 식별/인증의 기반이 되는 도메인. 모든 행동 데이터의 주체다
- **시스템 관점**: 인증 헤더 기반으로 요청마다 사용자를 식별하므로, 빠른 조회가 필요하다

#### API 목록

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/users` | No | 회원가입 |
| GET | `/api/v1/users/me` | Yes | 내 정보 조회 |
| PUT | `/api/v1/users/password` | Yes | 비밀번호 변경 |

#### 기능 흐름

**UC-U01: 회원가입**

- Main Flow
  1. 사용자가 username, password, name, email, birthDate를 입력한다
  2. 비밀번호 유효성을 검증한다 (8~16자, 영문/숫자/특수문자 조합, 생년월일 미포함)
  3. username 중복을 확인한다
  4. 비밀번호를 암호화하여 저장한다
  5. 201 CREATED를 반환한다 (응답 본문 없음)
- Exception Flow
  - E1: 비밀번호가 유효성 조건을 만족하지 않는 경우 → 400 BAD_REQUEST
  - E2: username이 이미 존재하는 경우 → 409 CONFLICT

**UC-U02: 내 정보 조회**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. 사용자 정보를 반환한다
- Exception Flow
  - E1: 인증 헤더 누락 → 401 UNAUTHORIZED
  - E2: 사용자 미존재 → 404 NOT_FOUND

**UC-U03: 비밀번호 변경**

- Main Flow
  1. 인증 헤더로 사용자를 식별/인증한다
  2. 새 비밀번호 유효성을 검증한다
  3. 비밀번호를 변경하고 암호화하여 저장한다
  4. 204 NO_CONTENT를 반환한다 (응답 본문 없음)
- Exception Flow
  - E1: 인증 헤더 누락 → 401 UNAUTHORIZED
  - E2: 비밀번호 불일치 → 401 UNAUTHORIZED
  - E3: 사용자 미존재 → 404 NOT_FOUND
  - E4: 새 비밀번호가 유효성 조건을 만족하지 않는 경우 → 400 BAD_REQUEST

#### 비즈니스 규칙

- 비밀번호 규칙: 8~16자, 영문/숫자/특수문자 조합, 생년월일 미포함
- username은 고유해야 한다
- 비밀번호는 반드시 암호화하여 저장한다 (BCrypt)

---

### 3.2 브랜드 (Brands)

#### 유저 스토리

> Customer는 브랜드 정보를 조회할 수 있다. Admin은 브랜드를 등록/수정/삭제할 수 있으며, 브랜드 삭제 시 해당 브랜드의 모든 상품이 함께 삭제된다.

- **사용자 관점**: 특정 브랜드의 정보를 확인할 수 있어야 한다
- **비즈니스 관점**: 상품이 반드시 브랜드에 속해야 하므로, 브랜드는 상품의 상위 분류 역할을 한다
- **시스템 관점**: 브랜드 삭제 시 연관 상품의 연쇄 삭제(Soft Delete)가 필요하다

#### API 목록

**Customer API**

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/v1/brands/{brandId}` | No | 브랜드 정보 조회 |

**Admin API**

| METHOD | URI | LDAP | 설명 |
|--------|-----|------|------|
| GET | `/api-admin/v1/brands?page=0&size=20` | Yes | 브랜드 목록 조회 |
| GET | `/api-admin/v1/brands/{brandId}` | Yes | 브랜드 상세 조회 |
| POST | `/api-admin/v1/brands` | Yes | 브랜드 등록 |
| PUT | `/api-admin/v1/brands/{brandId}` | Yes | 브랜드 수정 |
| DELETE | `/api-admin/v1/brands/{brandId}` | Yes | 브랜드 삭제 (상품 연쇄 삭제) |

#### 기능 흐름

**UC-B01: 브랜드 정보 조회 (Customer)**

- Main Flow
  1. brandId로 브랜드를 조회한다
  2. 브랜드 정보(id, name)를 반환한다
- Exception Flow
  - E1: 브랜드가 존재하지 않는 경우 → 404 NOT_FOUND

**UC-B02: 브랜드 등록 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. 브랜드 이름을 입력받아 저장한다
  3. 생성된 브랜드 정보를 반환한다
- Exception Flow
  - E1: LDAP 인증 실패 → 401 UNAUTHORIZED

**UC-B03: 브랜드 수정 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. brandId로 브랜드를 조회한다
  3. 브랜드 정보를 수정한다
- Exception Flow
  - E1: 브랜드가 존재하지 않는 경우 → 404 NOT_FOUND

**UC-B04: 브랜드 삭제 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. brandId로 브랜드를 조회한다
  3. 해당 브랜드에 속한 **모든 상품을 Soft Delete** 한다
  4. 브랜드를 Soft Delete 한다
- Alternate Flow
  - A1: 삭제된 상품에 연결된 좋아요/주문 이력의 처리 → [DEC-008](decisions/DEC-008-brand-delete-cascading-scope.md)
- Exception Flow
  - E1: 브랜드가 존재하지 않는 경우 → 404 NOT_FOUND

#### 비즈니스 규칙

- 브랜드 삭제 시 해당 브랜드의 모든 상품이 연쇄 삭제(Soft Delete)된다
- Admin 응답에는 productCount, createdAt, updatedAt, deletedAt가 포함된다
- Customer 응답에는 id, name만 포함된다

---

### 3.3 상품 (Products)

#### 유저 스토리

> Customer는 상품 목록을 필터/정렬/페이징하여 탐색하고, 상세 정보를 확인할 수 있다. Admin은 상품을 등록/수정/삭제할 수 있으며, 등록 시 브랜드가 사전에 존재해야 하고, 수정 시 브랜드를 변경할 수 없다.

- **사용자 관점**: 브랜드별 필터, 가격순/좋아요순 정렬로 상품을 탐색할 수 있어야 한다
- **비즈니스 관점**: 상품은 반드시 하나의 브랜드에 속한다. 재고(quantity)를 관리하며, 주문 시 재고 차감이 발생한다
- **시스템 관점**: 상품 목록 조회는 정렬/필터/페이징을 지원해야 하며, 좋아요 수 기반 정렬이 필요할 수 있다

#### API 목록

**Customer API**

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/v1/products?brandId=1&sort=latest&page=0&size=20` | No | 상품 목록 조회 |
| GET | `/api/v1/products/{productId}` | No | 상품 상세 조회 |

**상품 목록 조회 파라미터**

| 파라미터 | 예시 | 설명 |
|----------|------|------|
| `brandId` | `1` | 특정 브랜드 필터 (선택) |
| `sort` | `latest` / `price_asc` / `likes_desc` | 정렬 기준 |
| `page` | `0` | 페이지 번호 (기본값 0) |
| `size` | `20` | 페이지 크기 (기본값 20) |

- `latest` 정렬은 필수 구현, `price_asc`와 `likes_desc`는 선택적 확장 → [DEC-006](decisions/DEC-006-product-sort-options.md)

**Admin API**

| METHOD | URI | LDAP | 설명 |
|--------|-----|------|------|
| GET | `/api-admin/v1/products?page=0&size=20&brandId={brandId}` | Yes | 상품 목록 조회 |
| GET | `/api-admin/v1/products/{productId}` | Yes | 상품 상세 조회 |
| POST | `/api-admin/v1/products` | Yes | 상품 등록 |
| PUT | `/api-admin/v1/products/{productId}` | Yes | 상품 수정 |
| DELETE | `/api-admin/v1/products/{productId}` | Yes | 상품 삭제 |

#### 기능 흐름

**UC-P01: 상품 목록 조회 (Customer)**

- Main Flow
  1. 필터(brandId), 정렬(sort), 페이징(page, size) 파라미터를 받는다
  2. 조건에 맞는 상품 목록을 조회하여 반환한다
- Alternate Flow
  - A1: brandId를 지정하지 않은 경우 → 전체 상품에서 조회한다
  - A2: sort를 지정하지 않은 경우 → 기본값(latest)으로 정렬한다
- Exception Flow
  - E1: 결과가 없는 경우 → 빈 리스트를 반환한다

**UC-P02: 상품 상세 조회 (Customer)**

- Main Flow
  1. productId로 상품을 조회한다
  2. 상품 정보(id, name, price, brandName)를 반환한다
- Exception Flow
  - E1: 상품이 존재하지 않는 경우 → 404 NOT_FOUND

**UC-P03: 상품 등록 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. brandId, name, quantity, price를 입력받는다
  3. brandId에 해당하는 브랜드가 존재하는지 확인한다
  4. 상품을 저장한다
- Exception Flow
  - E1: brandId에 해당하는 브랜드가 존재하지 않는 경우 → 404 NOT_FOUND ⚠️ 변경 가능

**UC-P04: 상품 수정 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. productId로 상품을 조회한다
  3. 상품 정보를 수정한다 (**브랜드는 변경 불가**)
- Exception Flow
  - E1: 상품이 존재하지 않는 경우 → 404 NOT_FOUND
  - E2: 브랜드를 변경하려고 시도한 경우 → 400 BAD_REQUEST

**UC-P05: 상품 삭제 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. productId로 상품을 조회한다
  3. 상품을 Soft Delete 한다
- Exception Flow
  - E1: 상품이 존재하지 않는 경우 → 404 NOT_FOUND

#### 비즈니스 규칙

- 상품은 반드시 하나의 브랜드에 속해야 한다 (브랜드가 사전 등록되어 있어야 함)
- 상품의 브랜드는 등록 후 변경할 수 없다
- Customer 응답에는 id, name, price, brandName이 포함된다
- Admin 응답에는 id, name, price, quantity, brandName, createdAt, updatedAt, deletedAt가 포함된다
- Customer에게는 재고(quantity) 정보를 노출하지 않는다

---

### 3.4 좋아요 (Likes)

#### 유저 스토리

> 로그인한 사용자는 상품에 좋아요를 누르거나 취소할 수 있고, 자신이 좋아요 한 상품 목록을 확인할 수 있다.

- **사용자 관점**: 마음에 드는 상품을 기록하고, 나중에 다시 찾아볼 수 있어야 한다
- **비즈니스 관점**: 좋아요 데이터는 인기 상품 랭킹, 추천 알고리즘의 기반이 된다
- **시스템 관점**: 좋아요는 토글 성격이며, Row Explosion이 발생할 수 있어 삭제 전략 결정이 필요하다 → [DEC-002](decisions/DEC-002-like-delete-strategy.md)

#### API 목록

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/products/{productId}/likes` | Yes | 좋아요 등록 |
| DELETE | `/api/v1/products/{productId}/likes` | Yes | 좋아요 취소 |
| GET | `/api/v1/users/{userId}/likes` | Yes | 내가 좋아요 한 상품 목록 |

#### 기능 흐름

**UC-L01: 좋아요 등록**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. productId로 상품 존재 여부를 확인한다
  3. 해당 사용자가 이미 좋아요를 눌렀는지 확인한다
  4. 좋아요를 저장한다
- Alternate Flow
  - A1: 이미 좋아요를 누른 상태의 처리 → [DEC-001](decisions/DEC-001-like-duplicate-request-policy.md)
- Exception Flow
  - E1: 상품이 존재하지 않는 경우 → 404 NOT_FOUND

**UC-L02: 좋아요 취소**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. 해당 사용자의 해당 상품에 대한 좋아요를 삭제한다
- Exception Flow
  - E1: 좋아요가 존재하지 않는 경우 → 404 NOT_FOUND ⚠️ 변경 가능

**UC-L03: 내가 좋아요 한 상품 목록**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. 해당 사용자가 좋아요 누른 상품 목록을 반환한다
- Alternate Flow
  - A1: 좋아요한 상품이 없는 경우 → 빈 리스트를 반환한다

#### 비즈니스 규칙

- 로그인한 사용자만 좋아요를 누를 수 있다
- 한 사용자가 같은 상품에 중복 좋아요를 누를 수 없다
- 삭제 전략(Soft Delete vs Hard Delete)은 미결정 → [DEC-002](decisions/DEC-002-like-delete-strategy.md)

---

### 3.5 주문 (Orders)

#### 유저 스토리

> Customer는 여러 상품을 선택하여 주문할 수 있고, 기간별로 자신의 주문 내역을 조회할 수 있다. 주문 시점의 상품 정보(가격, 이름 등)가 스냅샷으로 저장된다. Admin은 전체 주문 목록과 상세 정보를 조회할 수 있다.

- **사용자 관점**: 원하는 상품을 수량과 함께 한 번에 주문하고, 과거 주문 내역을 기간별로 확인할 수 있어야 한다
- **비즈니스 관점**: 주문은 상품의 시점 정보(스냅샷)를 보존해야 한다. 상품 가격이 나중에 변경되어도 주문 당시 금액이 유지되어야 한다
- **시스템 관점**: 재고 확인과 차감이 원자적으로 이루어져야 한다. 동시 주문 시 재고 정합성 문제가 발생할 수 있다

#### API 목록

**Customer API**

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/v1/orders` | Yes | 주문 요청 |
| GET | `/api/v1/orders?startAt=2026-01-31&endAt=2026-02-10` | Yes | 주문 목록 조회 (기간 필터) |
| GET | `/api/v1/orders/{orderId}` | Yes | 주문 상세 조회 |

**주문 요청 본문**

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

**Admin API**

| METHOD | URI | LDAP | 설명 |
|--------|-----|------|------|
| GET | `/api-admin/v1/orders?page=0&size=20` | Yes | 전체 주문 목록 조회 |
| GET | `/api-admin/v1/orders/{orderId}` | Yes | 주문 상세 조회 |

#### 기능 흐름

**UC-O01: 주문 요청**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. 주문 항목(productId, quantity) 목록을 입력받는다
  3. 각 상품의 존재 여부를 확인한다
  4. 각 상품의 **재고를 확인**한다 (quantity >= 요청 수량)
  5. 각 상품의 **재고를 차감**한다 — 동시성 제어 전략은 [DEC-004](decisions/DEC-004-concurrent-order-stock-control.md) 참조
  6. 주문 시점의 상품 정보(가격, 이름)를 **스냅샷으로 저장**한다 (order_items)
  7. 총 주문 금액을 계산한다
  8. 주문을 저장한다
- Exception Flow
  - E1: 상품이 존재하지 않는 경우 → 404 NOT_FOUND
  - E2: 재고가 부족한 경우 → 400 BAD_REQUEST
  - E3: 주문 항목이 비어있는 경우 → 400 BAD_REQUEST

**UC-O02: 주문 목록 조회 (Customer)**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. startAt, endAt 기간 파라미터를 받는다
  3. 해당 기간 내 사용자의 주문 목록을 반환한다
- Alternate Flow
  - A1: 주문이 없는 경우 → 빈 리스트를 반환한다

**UC-O03: 주문 상세 조회 (Customer)**

- Main Flow
  1. 인증 헤더로 사용자를 식별한다
  2. orderId로 주문을 조회한다
  3. 주문 정보(id, totalPrice, orderItems, createdAt)를 반환한다
- Exception Flow
  - E1: 주문이 존재하지 않는 경우 → 404 NOT_FOUND
  - E2: 다른 사용자의 주문을 조회하려는 경우 → [DEC-003](decisions/DEC-003-order-other-user-response-code.md) 참조

**UC-O04: 전체 주문 목록 조회 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. 페이징 파라미터를 받는다
  3. 전체 주문 목록을 반환한다

**UC-O05: 주문 상세 조회 (Admin)**

- Main Flow
  1. LDAP 인증을 확인한다
  2. orderId로 주문을 조회한다
  3. 주문 상세 정보(user 정보 포함)를 반환한다
- Exception Flow
  - E1: 주문이 존재하지 않는 경우 → 404 NOT_FOUND

#### 비즈니스 규칙

- 주문 시 상품 정보(가격, 수량)를 스냅샷으로 order_items에 저장한다 (참조가 아닌 복사)
- 재고 확인 → 재고 차감은 원자적으로 이루어져야 한다
- Customer는 자신의 주문만 조회할 수 있다
- Admin 응답에는 user 정보, createdAt, updatedAt, deletedAt가 추가로 포함된다
- Customer 응답에는 id, totalPrice, orderItems, createdAt이 포함된다
- 주문 중복 생성 방지(멱등성) → [DEC-005](decisions/DEC-005-order-idempotency.md)

---

## 4. 비기능적 요구사항

### 성능

| 항목 | 요구사항 | 비고 |
|------|----------|------|
| **사용자 조회** | 인증 헤더 기반 매 요청 사용자 조회 → 빠른 lookup 필요 | username 인덱스 |
| **상품 목록** | 정렬/필터/페이징 성능 보장 | QueryDSL 동적 쿼리 |
| **좋아요 수 정렬** | `likes_desc` 정렬 시 집계 성능 | 비정규화 또는 캐시 고려 → [DEC-006](decisions/DEC-006-product-sort-options.md) |
| **느린 쿼리** | 기간별 주문 조회, 좋아요 수 기반 정렬에서 성능 저하 가능 | 인덱스 전략 필요 |

### 보안

| 항목 | 요구사항 | 비고 |
|------|----------|------|
| **비밀번호 암호화** | BCrypt 암호화 저장 | 평문 저장 금지 |
| **인증 방식** | HTTP 헤더 기반 (세션/토큰 없음) | 매 요청 ID/PW 전송 |
| **사용자 열거 방지** | 인증 실패 시 응답으로 사용자 존재 여부 노출 가능성 | ⚠️ 변경 가능 → [DEC-007](decisions/DEC-007-user-not-found-response-code.md) |

### 데이터 무결성

| 항목 | 요구사항 | 비고 |
|------|----------|------|
| **FK 미사용** | 앱 레벨 정합성 관리 | 데드락, 마이그레이션, 성능 이유 |
| **Soft Delete** | 기본 삭제 정책 | `BaseEntity.delete()` / `restore()` |
| **재고 정합성** | 동시 주문 시 Race Condition 방지 | → [DEC-004](decisions/DEC-004-concurrent-order-stock-control.md) |
| **트랜잭션 정합성** | 재고 차감 후 주문 저장 실패 시 데이터 불일치 방지 | 트랜잭션 경계 관리 |

### 확장성

| 항목 | 고려 방향 |
|------|-----------|
| **결제/쿠폰** | 추후 확장 고려. 주문 도메인과의 연결점 설계 |
| **랭킹/추천** | 좋아요 데이터를 기반으로 추후 확장 |
| **멱등성** | 주문 중복 생성 방지 메커니즘 → [DEC-005](decisions/DEC-005-order-idempotency.md) |

---

## 5. 미결정 사항 요약

| ID | 제목 | 상태 | 관련 도메인 | 문서 |
|----|------|------|------------|------|
| DEC-001 | 좋아요 중복 요청 처리 정책 | OPEN | Likes | [링크](decisions/DEC-001-like-duplicate-request-policy.md) |
| DEC-002 | 좋아요 삭제 전략 (Soft vs Hard Delete) | OPEN | Likes | [링크](decisions/DEC-002-like-delete-strategy.md) |
| DEC-003 | 타인 주문 조회 시 응답 코드 | OPEN | Orders | [링크](decisions/DEC-003-order-other-user-response-code.md) |
| DEC-004 | 동시 주문 재고 제어 전략 | OPEN | Orders, Products | [링크](decisions/DEC-004-concurrent-order-stock-control.md) |
| DEC-005 | 주문 멱등성 보장 방식 | OPEN | Orders | [링크](decisions/DEC-005-order-idempotency.md) |
| DEC-006 | 상품 정렬 옵션 확장 범위 | OPEN | Products | [링크](decisions/DEC-006-product-sort-options.md) |
| DEC-007 | 사용자 미존재 시 응답 코드 | DECIDED | Users | [링크](decisions/DEC-007-user-not-found-response-code.md) |
| DEC-008 | 브랜드 삭제 시 연관 데이터 처리 범위 | OPEN | Brands, Products, Likes, Orders | [링크](decisions/DEC-008-brand-delete-cascading-scope.md) |

---

## 6. 검증 체크리스트

- [x] Users/Brands/Products/Likes/Orders 5개 도메인 모두 포함
- [x] 범위/목적 → 기능적 요구사항 → 비기능적 요구사항 구조를 따르는가
- [x] 기능적 요구사항이 유저 스토리 중심으로 정리
- [x] 예외/조건 분기(Main / Alternate / Exception Flow) 포함
- [x] Customer와 Admin의 API 경로 및 응답 차이 명시
- [x] 인증 방식(Customer 헤더, Admin LDAP) 명시
- [x] 비즈니스 규칙(브랜드 연쇄 삭제, 브랜드 수정 불가, 스냅샷 저장, 재고 관리) 포함
- [x] 비기능적 요구사항(성능, 보안, 데이터 무결성, 확장성) 분리
- [x] 모든 미결정 사항이 Decision 문서로 분리되고 링크되었는가
- [x] 각 Decision에 "진짜 필요한 것"이 한 단계 추상화되어 도출되었는가
- [x] 변경 가능성이 있는 확정 사항에 ⚠️ 변경 가능 태그가 붙었는가
