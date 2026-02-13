# 이커머스 요구사항 명세서

## 시나리오 및 요구사항 요약

| 도메인 | 대고객 API | 어드민 API | 핵심 기능 |
|--------|-----------|-----------|----------|
| 유저 | 3개 | - | 회원가입, 내 정보, 비밀번호 변경 |
| 브랜드 | 1개 | 5개 | 조회, CRUD |
| 상품 | 2개 | 5개 | 목록/상세 조회, CRUD |
| 좋아요 | 3개 | - | 등록, 취소, 목록 |
| 쿠폰 | 향후 개발 | 향후 개발 | 발급, 조회, 주문 시 적용 |
| 주문 | 3개 | 2개 | 생성(쿠폰 적용), 목록, 상세 |
| **합계** | **12개 + α** | **12개 + α** | **총 24개 + 쿠폰 API** |

---

## 서비스 흐름

```
회원가입 → 상품탐색 → 좋아요 → 쿠폰 발급 → 주문(쿠폰 적용)
```

---

## 도메인 관계

```
USER ─┬─ LIKE ─── PRODUCT ─── BRAND
      ├─ USER_COUPON ─── COUPON
      └─ ORDER(쿠폰 적용) ─── ORDER_ITEM ─┘
```

---

## API 인증

| 구분 | Base Path | 인증 헤더 |
|------|-----------|----------|
| 대고객 | `/api/v1` | `X-Loopers-LoginId` + `X-Loopers-LoginPw` |
| 어드민 | `/api-admin/v1` | `X-Loopers-Ldap: "loopers.admin"` |

---

## API 요약

### 대고객 API (`/api/v1`)

| # | 도메인 | Method | Endpoint | 인증 | 설명 |
|---|--------|--------|----------|------|------|
| C01 | 유저 | POST | `/users` | X | 회원가입 |
| C02 | 유저 | GET | `/users/me` | O | 내 정보 조회 |
| C03 | 유저 | PUT | `/users/password` | O | 비밀번호 변경 |
| C04 | 브랜드 | GET | `/brands/{brandId}` | X | 브랜드 조회 |
| C05 | 상품 | GET | `/products` | X | 상품 목록 |
| C06 | 상품 | GET | `/products/{productId}` | X | 상품 상세 |
| C07 | 좋아요 | POST | `/products/{productId}/likes` | O | 좋아요 등록 |
| C08 | 좋아요 | DELETE | `/products/{productId}/likes` | O | 좋아요 취소 |
| C09 | 좋아요 | GET | `/users/{userId}/likes` | O | 좋아요 목록 |
| C10 | 주문 | POST | `/orders` | O | 주문 생성 |
| C11 | 주문 | GET | `/orders` | O | 주문 목록 |
| C12 | 주문 | GET | `/orders/{orderId}` | O | 주문 상세 |

### 어드민 API (`/api-admin/v1`)

| # | 도메인 | Method | Endpoint | 설명 |
|---|--------|--------|----------|------|
| A01 | 브랜드 | GET | `/brands?page=0&size=20` | 목록 조회 |
| A02 | 브랜드 | GET | `/brands/{brandId}` | 상세 조회 |
| A03 | 브랜드 | POST | `/brands` | 등록 |
| A04 | 브랜드 | PUT | `/brands/{brandId}` | 수정 |
| A05 | 브랜드 | DELETE | `/brands/{brandId}` | 삭제 |
| A06 | 상품 | GET | `/products?page=0&size=20&brandId={brandId}` | 목록 조회 |
| A07 | 상품 | GET | `/products/{productId}` | 상세 조회 |
| A08 | 상품 | POST | `/products` | 등록 |
| A09 | 상품 | PUT | `/products/{productId}` | 수정 |
| A10 | 상품 | DELETE | `/products/{productId}` | 삭제 |
| A11 | 주문 | GET | `/orders?page=0&size=20` | 목록 조회 |
| A12 | 주문 | GET | `/orders/{orderId}` | 상세 조회 |

---

# 상세 요구사항

## 1. 유저 도메인 (Users)

### 1.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 서비스를 이용하려면 회원가입이 필요하고, 개인정보를 확인/관리할 수 있어야 함 |
| 비즈니스 | 회원 기반 서비스 운영, 유저 식별 및 행동 추적 |
| 시스템 | 인증 헤더 기반 유저 식별, 비밀번호 보안 관리 |

### 1.2 API 상세

#### C01: 회원가입

```
POST /api/v1/users
Content-Type: application/json

{
  "loginId": "string",      // 영문, 숫자만 허용
  "password": "string",     // 암호화 저장
  "name": "string",
  "birthDate": "yyyy-MM-dd",
  "email": "string"
}
```

**검증 규칙:**
- loginId: 필수, 영문+숫자만, 중복 불가
- password: 필수, 암호화하여 저장
- name: 필수
- birthDate: 필수, 유효한 날짜
- email: 필수, 이메일 형식

**응답:** 생성된 유저 ID

#### C02: 내 정보 조회

```
GET /api/v1/users/me
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**응답:** 유저 정보 (이름 마스킹 처리: "홍길동" → "홍길*")

#### C03: 비밀번호 변경

```
PUT /api/v1/users/password
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
Content-Type: application/json

{
  "currentPassword": "string",
  "newPassword": "string"
}
```

**검증 규칙:**
- currentPassword가 현재 비밀번호와 일치해야 함
- newPassword는 currentPassword와 달라야 함

---

## 2. 브랜드 도메인 (Brands)

### 2.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 브랜드별로 상품을 탐색하고 싶음 |
| 비즈니스 | 브랜드 단위로 상품을 관리하고 마케팅에 활용 |
| 시스템 | 브랜드-상품 1:N 관계, 브랜드 삭제 시 상품도 삭제 |

### 2.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| BR-01 | 브랜드 삭제 시 해당 브랜드의 상품들도 함께 삭제 |
| BR-02 | 브랜드명 중복 불가 (선택적 제약) |

### 2.3 API 상세

#### C04: 브랜드 조회 (대고객)

```
GET /api/v1/brands/{brandId}
```

**응답:** 브랜드 기본 정보 (id, name, description 등)

#### A01-A05: 브랜드 CRUD (어드민)

| API | 설명 | 요청 바디 |
|-----|------|----------|
| A01 | 목록 조회 | - (page, size 쿼리) |
| A02 | 상세 조회 | - |
| A03 | 등록 | { name, description, ... } |
| A04 | 수정 | { name, description, ... } |
| A05 | 삭제 | - (연관 상품도 삭제) |

---

## 3. 상품 도메인 (Products)

### 3.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 상품을 탐색하고, 정렬하고, 상세 정보를 확인하고 싶음 |
| 비즈니스 | 상품 재고 관리, 가격 정책, 판매 상태 관리 |
| 시스템 | 브랜드와의 관계, 재고 동시성, 주문 시 스냅샷 저장 |

### 3.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| PR-01 | 상품 등록 시 브랜드는 반드시 존재해야 함 |
| PR-02 | 상품 수정 시 브랜드 변경 불가 |
| PR-03 | 재고는 0 이상이어야 함 |

### 3.3 API 상세

#### C05: 상품 목록 조회

```
GET /api/v1/products?brandId={brandId}&sort={sort}&page={page}&size={size}
```

**쿼리 파라미터:**

| 파라미터 | 필수 | 기본값 | 설명 |
|----------|------|--------|------|
| brandId | X | - | 브랜드 필터 |
| sort | X | latest | 정렬 기준 |
| page | X | 0 | 페이지 번호 |
| size | X | 20 | 페이지 크기 |

**정렬 옵션:**

| 값 | 설명 | 필수 구현 |
|----|------|----------|
| latest | 최신순 (createdAt DESC) | O |
| price_asc | 가격 낮은순 | 선택 |
| likes_desc | 좋아요 많은순 | 선택 |

#### C06: 상품 상세 조회

```
GET /api/v1/products/{productId}
```

**응답:** 상품 전체 정보 (브랜드 정보 포함)

#### A06-A10: 상품 CRUD (어드민)

| API | 설명 | 특이사항 |
|-----|------|----------|
| A06 | 목록 조회 | brandId 필터 지원 |
| A07 | 상세 조회 | 어드민용 추가 정보 포함 가능 |
| A08 | 등록 | brandId 필수, 브랜드 존재 검증 |
| A09 | 수정 | brandId 변경 불가 |
| A10 | 삭제 | Soft Delete 권장 |

---

## 4. 좋아요 도메인 (Likes)

### 4.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 마음에 드는 상품을 저장하고 나중에 다시 보고 싶음 |
| 비즈니스 | 유저 관심사 파악, 추천/랭킹에 활용 |
| 시스템 | 유저-상품 M:N 관계, 중복 좋아요 방지 |

### 4.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| LK-01 | 한 유저가 같은 상품에 중복 좋아요 불가 (유니크 인덱스) |
| LK-02 | 이미 좋아요한 상품에 다시 좋아요 요청 시 200 OK (멱등성 보장) |
| LK-03 | 좋아요하지 않은 상품 취소 시 200 OK (멱등성 보장) |
| LK-04 | 본인의 좋아요 목록만 조회 가능 |
| LK-05 | 삭제된 상품에도 좋아요 등록 가능 |

### 4.3 API 상세

#### C07: 좋아요 등록

```
POST /api/v1/products/{productId}/likes
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**검증:**
- 상품 존재 여부 (Soft Delete 상품 포함)
- 이미 좋아요 했는지 확인 → 중복 시 200 OK (멱등성 보장)

#### C08: 좋아요 취소

```
DELETE /api/v1/products/{productId}/likes
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**검증:**
- 좋아요 존재 여부 확인 → 없으면 200 OK (멱등성 보장)

#### C09: 좋아요 목록 조회

```
GET /api/v1/users/{userId}/likes
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**검증:**
- 본인의 목록만 조회 가능 (userId == 로그인 유저 ID)

---

## 5. 쿠폰 도메인 (Coupons) - 향후 개발

> 쿠폰 관련 API는 향후 개발 예정이나, 주문 흐름에 쿠폰 적용이 포함되므로 설계 단계에서 미리 고려

### 5.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 쿠폰을 발급받고 주문 시 할인을 적용하고 싶음 |
| 비즈니스 | 프로모션 수단으로 쿠폰 발급, 사용률 추적, 남용 방지 |
| 시스템 | 쿠폰 유효성 검증, 주문과의 연계, 동시 사용 방지 |

### 5.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| CP-01 | 쿠폰 타입: 정액 할인(FIXED) + 정률 할인(RATE) |
| CP-02 | 정률 할인 시 최대 할인 금액 상한 필요 |
| CP-03 | 주문 1건당 쿠폰 1장만 적용 가능 |
| CP-04 | 쿠폰 적용 실패 시(만료, 최소금액 미달 등) 쿠폰 무시하고 주문 진행 |
| CP-05 | 사용된 쿠폰은 재사용 불가 |
| CP-06 | 쿠폰 정의(템플릿)와 유저별 발급 이력을 분리 관리 |

### 5.3 쿠폰 구조

```
coupons (템플릿)          → 어드민이 생성하는 쿠폰 정의
user_coupons (발급 이력)   → 유저에게 발급된 개별 쿠폰
```

**쿠폰 템플릿 필드:**
- name: 쿠폰명
- type: FIXED / RATE
- discountValue: 할인 금액 또는 할인율
- minOrderAmount: 최소 주문 금액 (선택)
- maxDiscountAmount: 최대 할인 금액 (RATE 타입 시 필수)
- validFrom / validTo: 유효 기간

**유저 쿠폰 필드:**
- userId: 발급 대상 유저
- couponId: 쿠폰 템플릿 참조
- usedOrderId: 사용된 주문 ID (null이면 미사용)
- usedAt: 사용 시점
- expiredAt: 만료 시점

### 5.4 주문 연계

주문 생성(C10) 시 쿠폰 적용 흐름:
1. 유저 쿠폰 유효성 검증 (만료 여부, 사용 여부)
2. 최소 주문 금액 확인
3. 할인 금액 계산 (FIXED: 고정 금액, RATE: 주문금액 × 할인율, 상한 적용)
4. 검증 실패 시 → 쿠폰 무시, 할인 없이 주문 진행
5. 검증 성공 시 → 할인 적용, 쿠폰 사용 처리

---

## 6. 주문 도메인 (Orders)

### 6.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 여러 상품을 한 번에 주문하고, 쿠폰을 적용해 할인받고 싶음 |
| 비즈니스 | 주문 기록 관리, 매출 추적, 재고 관리, 쿠폰 사용 추적 |
| 시스템 | 재고 동시성 제어, 스냅샷 저장, 쿠폰 유효성 검증, 트랜잭션 일관성 |

### 6.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| OR-01 | 주문 시 상품 정보 스냅샷 저장 (가격, 이름 등) |
| OR-02 | 주문 시 재고 확인 및 차감 필수 |
| OR-03 | 일부 상품 재고 부족 시 부분 주문 진행 (재고 있는 상품만) |
| OR-04 | 전체 상품 재고 부족 시 주문 실패 (400) |
| OR-05 | 본인의 주문만 조회 가능 |
| OR-06 | 주문은 삭제 불가 (기록 보존) |
| OR-07 | 주문 시 쿠폰 적용 가능 (1건당 1장, 선택) |
| OR-08 | 쿠폰 적용 실패 시 쿠폰 무시하고 주문 진행 (할인만 빠짐) |
| OR-09 | 주문에 적용된 할인 금액을 기록 (쿠폰 스냅샷) |

### 6.3 API 상세

#### C10: 주문 생성

```
POST /api/v1/orders
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
Content-Type: application/json

{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ],
  "userCouponId": 15
}
```

> `userCouponId`는 선택 필드. 미전송 시 쿠폰 없이 주문 진행

**처리 순서:**
1. 상품 존재 확인
2. 재고 확인
3. 재고 차감 (동시성 제어 필요)
4. 쿠폰 검증 및 할인 계산 (전송 시)
   - 유효성 검증 실패 → 쿠폰 무시, 할인 없이 진행
   - 유효성 검증 성공 → 할인 금액 계산
5. 주문 생성 (상품 스냅샷 + 할인 정보 포함)
6. 쿠폰 사용 처리 (usedAt, usedOrderId 업데이트)

**스냅샷 저장 필드:**
- 상품명
- 단가
- 브랜드명 (선택)
- 상품 이미지 URL (선택)
- 할인 금액 (쿠폰 적용 시)

#### C11: 주문 목록 조회

```
GET /api/v1/orders?startAt={startAt}&endAt={endAt}
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**쿼리 파라미터:**

| 파라미터 | 필수 | 형식 | 설명 |
|----------|------|------|------|
| startAt | O | yyyy-MM-dd | 조회 시작일 |
| endAt | O | yyyy-MM-dd | 조회 종료일 |

#### C12: 주문 상세 조회

```
GET /api/v1/orders/{orderId}
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**검증:**
- 본인의 주문만 조회 가능

#### A11-A12: 주문 조회 (어드민)

| API | 설명 |
|-----|------|
| A11 | 전체 주문 목록 (페이징) |
| A12 | 주문 상세 (모든 주문 조회 가능) |

---

## 7. 정책 결정 사항

### 7.1 삭제 정책

| 도메인 | 방식 | 비고 |
|--------|------|------|
| 좋아요 | Soft Delete | deletedAt 필드 사용 |
| 브랜드 | Soft Delete | 삭제 시 연관 상품도 Soft Delete |
| 상품 | Soft Delete | 주문 스냅샷에서 참조 가능 |
| 주문 | 삭제 불가 | 기록 보존 |

### 7.2 좋아요 정책

| 상황 | 응답 | 설명 |
|------|------|------|
| 중복 등록 | 200 OK | 멱등성 보장, 기존 좋아요 유지 |
| 없는 좋아요 취소 | 200 OK | 멱등성 보장, 이미 없는 상태 |
| 삭제된 상품에 등록 | 허용 | Soft Delete 상품도 좋아요 가능 |

### 7.3 쿠폰 정책

| 상황 | 처리 |
|------|------|
| 쿠폰 타입 | FIXED(정액) / RATE(정률) |
| 주문당 적용 수 | 1장 |
| 쿠폰 만료/미달 시 | 쿠폰 무시, 주문은 진행 (할인만 빠짐) |
| 사용된 쿠폰 | 재사용 불가 (usedAt 기록) |
| RATE 타입 상한 | maxDiscountAmount 초과 시 상한 금액 적용 |

### 7.4 주문 정책

| 상황 | 처리 |
|------|------|
| 일부 상품 재고 부족 | 부분 주문 진행 |
| 부분 주문 응답 | orderedItems + excludedItems 포함 |
| 전체 재고 부족 | 주문 실패 (400) |

**부분 주문 응답 예시:**
```json
{
  "orderedItems": [
    { "productId": 1, "quantity": 2 }
  ],
  "excludedItems": [
    { "productId": 3, "reason": "INSUFFICIENT_STOCK" }
  ]
}
```

### 7.5 동시성 처리

| 상황 | 방안 |
|------|------|
| 재고 차감 | 비관적 락 (SELECT FOR UPDATE) |
| 좋아요 중복 | 유니크 인덱스 (user_id, product_id) + 멱등성 |

---

## 8. 확장 고려사항 (향후)

> ⚙️ 결제, 쿠폰 API는 추후 개발 예정

| 기능 | 설명 |
|------|------|
| 쿠폰 API | 쿠폰 발급/조회/관리 API (설계는 반영 완료, API 구현 향후) |
| 결제 | 주문 후 결제 프로세스 |
| 동시성 | 대량 동시 주문 처리 |
| 멱등성 | 중복 주문 방지 |
| 일관성 | 분산 트랜잭션 |

---

## 9. 고객/어드민 정보 차별화

| 도메인 | 고객용 | 어드민용 |
|--------|--------|----------|
| 브랜드 | 기본 정보 | + 생성일, 수정일, 통계 |
| 상품 | 공개 정보 | + 재고, 판매 상태, 관리 정보 |
| 주문 | 본인 주문만 | 전체 주문 + 유저 정보 |
