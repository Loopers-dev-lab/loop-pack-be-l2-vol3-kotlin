# 이커머스 요구사항 명세서

## 시나리오 및 요구사항 요약

| 도메인 | 대고객 API | 어드민 API | 핵심 기능 |
|--------|-----------|-----------|----------|
| 유저 | 3개 | - | 회원가입, 내 정보, 비밀번호 변경 |
| 브랜드 | 1개 | 5개 | 조회, CRUD |
| 상품 | 2개 | 5개 | 목록/상세 조회, CRUD |
| 좋아요 | 3개 | - | 등록, 취소, 목록 |
| 주문 | 3개 | 2개 | 생성, 목록, 상세 |
| 쿠폰 | 2개 | 6개 | 발급, 내 쿠폰, 템플릿 CRUD, 발급 내역 |
| **합계** | **14개** | **18개** | **총 32개** |

---

## 서비스 흐름

```
회원가입 → 상품탐색 → 좋아요 → 쿠폰발급 → 주문(쿠폰적용)
```

---

## 도메인 관계

```
USER ─┬─ LIKE ─── PRODUCT ─── BRAND
      ├─ ISSUED_COUPON ─── COUPON
      └─ ORDER ─── ORDER_ITEM ─┘
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
| C10 | 주문 | POST | `/orders` | O | 주문 생성 (쿠폰 적용 가능) |
| C11 | 주문 | GET | `/orders` | O | 주문 목록 |
| C12 | 주문 | GET | `/orders/{orderId}` | O | 주문 상세 |
| C13 | 쿠폰 | POST | `/coupons/{couponId}/issue` | O | 쿠폰 발급 |
| C14 | 쿠폰 | GET | `/users/me/coupons` | O | 내 쿠폰 목록 |

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
| A13 | 쿠폰 | GET | `/coupons?page=0&size=20` | 쿠폰 템플릿 목록 |
| A14 | 쿠폰 | GET | `/coupons/{couponId}` | 쿠폰 템플릿 상세 |
| A15 | 쿠폰 | POST | `/coupons` | 쿠폰 템플릿 등록 |
| A16 | 쿠폰 | PUT | `/coupons/{couponId}` | 쿠폰 템플릿 수정 |
| A17 | 쿠폰 | DELETE | `/coupons/{couponId}` | 쿠폰 템플릿 삭제 |
| A18 | 쿠폰 | GET | `/coupons/{couponId}/issues?page=0&size=20` | 발급 내역 조회 |

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

## 5. 주문 도메인 (Orders)

### 5.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 여러 상품을 한 번에 주문하고 싶음 |
| 비즈니스 | 주문 기록 관리, 매출 추적, 재고 관리 |
| 시스템 | 재고 동시성 제어, 스냅샷 저장, 트랜잭션 일관성 |

### 5.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| OR-01 | 주문 시 상품 정보 스냅샷 저장 (가격, 이름 등) |
| OR-02 | 주문 시 재고 확인 및 차감 필수 |
| OR-03 | 일부 상품 재고 부족 시 부분 주문 진행 (재고 있는 상품만) |
| OR-04 | 전체 상품 재고 부족 시 주문 실패 (400) |
| OR-05 | 본인의 주문만 조회 가능 |
| OR-06 | 주문은 삭제 불가 (기록 보존) |

### 5.3 API 상세

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
  ]
}
```

**처리 순서:**
1. 상품 존재 확인
2. 재고 확인
3. 재고 차감 (동시성 제어 필요)
4. 주문 생성 (상품 스냅샷 포함)

**스냅샷 저장 필드:**
- 상품명
- 단가
- 브랜드명 (선택)
- 상품 이미지 URL (선택)

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

## 6. 쿠폰 도메인 (Coupons)

### 6.1 문제 정의

| 관점 | 문제 |
|------|------|
| 사용자 | 할인 혜택을 받아 저렴하게 구매하고 싶음 |
| 비즈니스 | 프로모션 운영, 구매 전환율 향상 |
| 시스템 | 쿠폰 발급/사용 동시성 제어, 주문과의 트랜잭션 일관성 |

### 6.2 비즈니스 규칙

| 규칙 | 설명 |
|------|------|
| CP-01 | 쿠폰 타입: 정액(FIXED), 정률(RATE) |
| CP-02 | 1인 1쿠폰 (동일 쿠폰 중복 발급 불가) |
| CP-03 | 만료된 쿠폰은 발급 불가 |
| CP-04 | 쿠폰 사용 시 비관적 락으로 동시성 제어 (SELECT FOR UPDATE) |
| CP-05 | 쿠폰 사용 주문은 부분 주문 불가 (전체 성공 or 전체 실패) |
| CP-06 | 쿠폰 미사용 주문은 기존대로 부분 주문 허용 |
| CP-07 | 최소 주문 금액 미달 시 쿠폰 사용 불가 |
| CP-08 | FIXED: 할인 금액이 주문 금액 초과 시 주문 금액만큼만 할인 |
| CP-09 | 타인의 쿠폰 사용 불가 (FORBIDDEN) |

### 6.3 API 상세

#### C13: 쿠폰 발급

```
POST /api/v1/coupons/{couponId}/issue
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**검증:**
- 쿠폰 존재 여부 (삭제된 쿠폰 → NOT_FOUND)
- 쿠폰 만료 여부 (만료 → BAD_REQUEST)
- 중복 발급 여부 (이미 발급 → CONFLICT)

#### C14: 내 쿠폰 목록 조회

```
GET /api/v1/users/me/coupons
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
```

**응답:** 발급된 쿠폰 목록 (상태별 포함: AVAILABLE, USED, EXPIRED)

#### A13-A18: 쿠폰 CRUD + 발급 내역 (어드민)

| API | 설명 | 요청 바디 |
|-----|------|----------|
| A13 | 템플릿 목록 조회 | - (page, size 쿼리) |
| A14 | 템플릿 상세 조회 | - |
| A15 | 템플릿 등록 | { name, type, value, minOrderAmount, expiredAt } |
| A16 | 템플릿 수정 | { name, value, minOrderAmount, expiredAt } |
| A17 | 템플릿 삭제 | - (Soft Delete) |
| A18 | 발급 내역 조회 | - (couponId 경로, page/size 쿼리) |

#### C10 변경: 쿠폰 적용 주문

```
POST /api/v1/orders
X-Loopers-LoginId: {loginId}
X-Loopers-LoginPw: {password}
Content-Type: application/json

{
  "items": [
    { "productId": 1, "quantity": 2 }
  ],
  "couponId": 1  // optional, 발급된 쿠폰 ID (issued_coupons.id)
}
```

**쿠폰 적용 시 처리 순서:**
1. 비관적 락으로 상품 조회
2. 전체 재고 확인 (부분 주문 불가)
3. 비관적 락으로 발급 쿠폰 조회
4. 소유자 검증, 사용 가능 상태 검증
5. 재고 차감
6. 할인 계산 (최소 주문 금액 검증)
7. 쿠폰 사용 처리 (USED)
8. 주문 생성 (originalAmount, discountAmount, totalAmount)

---

## 7. 정책 결정 사항

### 7.1 삭제 정책

| 도메인 | 방식 | 비고 |
|--------|------|------|
| 좋아요 | Soft Delete | deletedAt 필드 사용 |
| 브랜드 | Soft Delete | 삭제 시 연관 상품도 Soft Delete |
| 상품 | Soft Delete | 주문 스냅샷에서 참조 가능 |
| 주문 | 삭제 불가 | 기록 보존 |
| 쿠폰 | Soft Delete | deletedAt 필드 사용 |
| 발급 쿠폰 | 삭제 불가 | status로 상태 관리 (AVAILABLE/USED/EXPIRED) |

### 7.2 좋아요 정책

| 상황 | 응답 | 설명 |
|------|------|------|
| 중복 등록 | 200 OK | 멱등성 보장, 기존 좋아요 유지 |
| 없는 좋아요 취소 | 200 OK | 멱등성 보장, 이미 없는 상태 |
| 삭제된 상품에 등록 | 허용 | Soft Delete 상품도 좋아요 가능 |

### 7.3 주문 정책

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

### 7.4 쿠폰 정책

| 상황 | 처리 |
|------|------|
| 쿠폰 적용 주문 | 부분 주문 불가, 전체 성공 or 전체 실패 |
| 쿠폰 미적용 주문 | 기존 부분 주문 허용 |
| 동일 쿠폰 동시 사용 | 비관적 락으로 1건만 성공 |
| FIXED 할인 > 주문 금액 | 주문 금액만큼만 할인 |
| RATE 할인 | 주문 금액 × (value / 100), 소수점 반올림 |

### 7.5 동시성 처리

| 상황 | 방안 |
|------|------|
| 재고 차감 | 비관적 락 (SELECT FOR UPDATE) |
| 좋아요 중복 | 유니크 인덱스 (user_id, product_id) + 멱등성 |
| 쿠폰 사용 | 비관적 락 (SELECT FOR UPDATE) — 재고와 동일 패턴 |

---

## 8. 확장 고려사항 (향후)

> ⚙️ 결제 등은 추후 개발 예정

| 기능 | 설명 |
|------|------|
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
| 쿠폰 | 내 발급 쿠폰 목록 | 템플릿 CRUD + 발급 내역 조회 |
