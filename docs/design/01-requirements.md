# 요구사항 정리 / 기능 명세 / 유비쿼터스 언어

## 1. 문제 상황 재해석

단순히 API를 나열하는 것이 아니라, **왜 이 기능이 필요한지** 관점에서 재해석한다.

### 사용자 관점

- 여러 브랜드의 상품을 탐색하고, 마음에 드는 상품에 좋아요를 남기고 싶다
- 여러 상품을 한 번에 주문하고 싶다 (장바구니 없이 즉시 주문)
- 내 주문 이력과 좋아요 목록을 확인하고 싶다

### 비즈니스 관점

- 브랜드-상품 카탈로그를 관리하고, 어드민이 CRUD를 수행할 수 있어야 한다
- 좋아요 데이터를 통해 인기 상품 랭킹, 추천 등으로 확장할 기반을 만들고 싶다
- 주문 시 재고 정합성이 보장되어야 한다 (과판매 방지)

### 시스템 관점

- 주문 시 **재고 확인과 차감이 원자적으로 보장**되어야 한다 (정합성)
- 좋아요 등록/취소가 **멱등하게 동작**해야 한다
- 주문 정보에는 **당시 상품 정보의 스냅샷**이 저장되어야 한다 (상품 변경에 영향받지 않음)
- 결제는 나중에 추가되므로, 주문 상태가 **확장 가능한 구조**여야 한다
- 동시성, 멱등성, 일관성 등의 문제는 기능 구현 후 별도 단계에서 해결한다

---

## 2. 유비쿼터스 언어

> 모든 문서, 코드, 커뮤니케이션에서 아래 용어를 통일하여 사용한다.

### 핵심 도메인 용어

| 한국어 | 영어 (코드) | 정의 |
|--------|------------|------|
| 유저 | User | 서비스에 가입한 사용자 |
| 브랜드 | Brand | 상품을 제공하는 브랜드 (1:N 상품) |
| 상품 | Product | 브랜드에 속한 판매 가능한 상품 |
| 좋아요 | Like | 유저가 상품에 남긴 관심 표시 |
| 주문 | Order | 유저가 상품을 구매하기 위한 요청 단위 |
| 주문 항목 | OrderItem | 주문에 포함된 개별 상품과 수량 |
| 재고 | stock (Product 필드) | 상품의 현재 판매 가능 수량 |

### 상태 용어

| 한국어 | 영어 (코드) | 정의 |
|--------|------------|------|
| 주문됨 | ORDERED | 주문이 생성되고 재고가 차감된 상태 |

> 결제(PAID), 취소(CANCELLED) 등의 상태는 결제 기능 추가 시 확장된다.

### 행위 용어

| 한국어 | 영어 (코드) | 정의 |
|--------|------------|------|
| 회원가입 | signUp | 새로운 유저 계정 생성 |
| 좋아요 등록 | like | 상품에 좋아요를 남김 (멱등) |
| 좋아요 취소 | unlike | 상품의 좋아요를 제거 (멱등) |
| 주문 요청 | placeOrder | 여러 상품을 한 번에 주문 |
| 재고 차감 | deductStock | 주문 시 상품 재고를 감소 |
| 스냅샷 저장 | snapshot | 주문 시점의 상품 정보를 주문 항목에 보존 |

### 역할 용어

| 한국어 | 영어 (코드) | 식별 방식 |
|--------|------------|----------|
| 고객 | User | X-Loopers-LoginId / X-Loopers-LoginPw 헤더 |
| 어드민 | Admin | X-Loopers-Ldap: loopers.admin 헤더 |

### 용어 사용 규칙

- "상품"과 "아이템"을 혼용하지 않는다. 상품은 `Product`, 주문 내 항목은 `OrderItem`
- "찜", "위시리스트" 대신 "좋아요(Like)"로 통일한다
- "재고"는 별도 엔티티가 아닌 `Product.stock` 필드로 관리한다
- "좋아요 수"는 `Product.likeCount` 필드로 비정규화하여 관리한다

---

## 3. 설계 결정 사항

요구사항 분석 과정에서 아래 사항들이 합의되었다.

| 항목 | 결정 | 근거 |
|------|------|------|
| 재고 관리 | Product에 stock 필드 | 현재 요구사항에 충분, 단순한 구조로 시작 |
| 좋아요 수 | Product에 likeCount 비정규화 | likes_desc 정렬 성능 확보, 동시성은 나중에 해결 |
| 재고 부족 시 | 전체 주문 실패 | 하나라도 부족하면 롤백. 트랜잭션 단순, 일관성 보장 |
| 주문 상태 | 최소 상태 (ORDERED) | 결제 추가 시 확장. YAGNI 원칙 |
| 좋아요 중복 | 멱등 처리 (200 반환) | 처음부터 멱등성 보장 |
| 좋아요 목록 URI | /users/{userId}/likes | 요구사항 그대로, 로그인 유저와 다르면 403 |
| 스냅샷 범위 | 핵심 정보 (상품명, 가격, 브랜드명) | 주문 조회 시 필요한 최소 정보 |
| 삭제 방식 | Soft delete | BaseEntity 패턴과 일관, 데이터 보존 |
| 노출 정보 차이 | 어드민만 관리 정보 추가 | 고객에겐 필요한 정보만, 어드민은 재고/생성일/수정일 등 포함 |
| 주문 항목 중복 | 동일 productId 중복 시 거부 | 클라이언트 책임으로 명확한 요청 강제, 서버 로직 단순화 |
| 어드민 삭제 데이터 | 삭제된 데이터 제외 | 고객과 동일하게 삭제된 데이터는 조회 불가, deletedAt 필드로 구분 |
| 상품 삭제 시 좋아요 | 좋아요는 유지, 조회 시 제외 | 브랜드 삭제 시와 동일 정책, 데이터 보존 |

---

## 4. 도메인별 기능 명세

### 4.1 브랜드 (Brand)

#### 유저 스토리

- 고객은 브랜드 정보를 조회할 수 있다
- 어드민은 브랜드를 등록/수정/삭제할 수 있다
- 브랜드 삭제 시 해당 브랜드의 상품들도 함께 삭제된다

#### 유스케이스: 브랜드 조회 (고객)

```
[Main Flow]
1. 고객이 GET /api/v1/brands/{brandId} 요청
2. 시스템이 브랜드 정보를 조회
3. 브랜드 기본 정보(이름, 설명 등)를 반환

[Exception Flow]
- brandId에 해당하는 브랜드가 없음 → 404 NOT_FOUND
- 삭제된 브랜드 → 404 NOT_FOUND (soft delete된 데이터는 조회 불가)
```

#### 유스케이스: 브랜드 등록 (어드민)

```
[Main Flow]
1. 어드민이 POST /api-admin/v1/brands 요청 (이름, 설명 등)
2. 시스템이 브랜드 정보를 검증
3. 브랜드를 저장하고 생성된 정보를 반환

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- 필수 정보 누락 → 400 BAD_REQUEST
```

#### 유스케이스: 브랜드 목록 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/brands?page=0&size=20 요청
2. 시스템이 브랜드 목록을 페이징하여 반환
3. 삭제된 브랜드는 제외

[기본값]
- page: 0
- size: 20

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
```

#### 유스케이스: 브랜드 상세 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/brands/{brandId} 요청
2. 시스템이 브랜드 상세 정보 반환 (생성일, 수정일 등 관리 정보 포함)

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- brandId에 해당하는 브랜드가 없음 → 404 NOT_FOUND
- 삭제된 브랜드 → 404 NOT_FOUND
```

#### 유스케이스: 브랜드 수정 (어드민)

```
[Main Flow]
1. 어드민이 PUT /api-admin/v1/brands/{brandId} 요청 (이름, 설명 등)
2. 시스템이 브랜드 정보를 검증 후 수정
3. 수정된 브랜드 정보를 반환

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- brandId에 해당하는 브랜드가 없음 → 404 NOT_FOUND
- 삭제된 브랜드 → 404 NOT_FOUND
- 필수 정보 누락 → 400 BAD_REQUEST
```

#### 유스케이스: 브랜드 삭제 (어드민)

```
[Main Flow]
1. 어드민이 DELETE /api-admin/v1/brands/{brandId} 요청
2. 시스템이 해당 브랜드를 soft delete
3. 해당 브랜드에 속한 모든 상품도 soft delete
4. 삭제 완료 응답

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- brandId에 해당하는 브랜드가 없음 → 404 NOT_FOUND
- 이미 삭제된 브랜드 → 404 NOT_FOUND

[주의]
- 이미 주문된 상품의 스냅샷에는 영향 없음 (스냅샷은 독립적)
- 삭제된 브랜드의 상품에 대한 좋아요는 유지, 조회 시 제외
```

---

### 4.2 상품 (Product)

#### 유저 스토리

- 고객은 상품 목록을 다양한 조건으로 조회할 수 있다
- 고객은 상품 상세 정보를 조회할 수 있다
- 어드민은 상품을 등록/수정/삭제할 수 있다
- 상품은 반드시 하나의 브랜드에 속한다

#### 유스케이스: 상품 목록 조회 (고객)

```
[Main Flow]
1. 고객이 GET /api/v1/products 요청 (쿼리 파라미터: brandId, sort, page, size)
2. 시스템이 조건에 맞는 상품 목록을 페이징하여 반환
3. 삭제된 상품은 제외

[정렬 기준]
- latest (기본값): 최신 등록순
- price_asc: 가격 낮은순
- likes_desc: 좋아요 많은순

[기본값]
- page: 0
- size: 20

[Alternate Flow]
- brandId 미지정 → 전체 브랜드의 상품 조회
- sort 미지정 → latest 적용
```

#### 유스케이스: 상품 상세 조회 (고객)

```
[Main Flow]
1. 고객이 GET /api/v1/products/{productId} 요청
2. 시스템이 상품 상세 정보를 반환 (브랜드명 포함)
3. 고객용 노출 정보만 반환 (재고, 생성일 등 제외)

[Exception Flow]
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 → 404 NOT_FOUND
```

#### 유스케이스: 상품 목록 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/products?page=0&size=20&brandId={brandId} 요청
2. 시스템이 조건에 맞는 상품 목록을 페이징하여 반환
3. 어드민용 정보 포함 (재고, 생성일, 수정일 등)
4. 삭제된 상품은 제외

[기본값]
- page: 0
- size: 20

[Alternate Flow]
- brandId 미지정 → 전체 브랜드의 상품 조회

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
```

#### 유스케이스: 상품 상세 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/products/{productId} 요청
2. 시스템이 상품 상세 정보를 반환 (재고, 생성일, 수정일 등 관리 정보 포함)

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 → 404 NOT_FOUND
```

#### 유스케이스: 상품 등록 (어드민)

```
[Main Flow]
1. 어드민이 POST /api-admin/v1/products 요청 (상품명, 가격, 설명, 브랜드ID, 재고 등)
2. 시스템이 브랜드 존재 여부 확인
3. 상품 정보 검증 후 저장
4. 생성된 상품 정보 반환

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- 필수 정보 누락 (상품명 등) → 400 BAD_REQUEST
- 브랜드ID에 해당하는 브랜드가 없음 → 404 NOT_FOUND ("등록되지 않은 브랜드입니다")
- 삭제된 브랜드에 상품 등록 시도 → 404 NOT_FOUND
- 가격이 0 이하 → 400 BAD_REQUEST
- 재고가 0 미만 → 400 BAD_REQUEST
```

#### 유스케이스: 상품 수정 (어드민)

```
[Main Flow]
1. 어드민이 PUT /api-admin/v1/products/{productId} 요청
2. 시스템이 상품 정보 검증 후 수정
3. 수정된 상품 정보 반환

[제약]
- 상품의 브랜드(brandId)는 수정할 수 없음

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- brandId 변경 시도 → 400 BAD_REQUEST ("상품의 브랜드는 수정할 수 없습니다")
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 수정 시도 → 404 NOT_FOUND
- 필수 정보 누락 → 400 BAD_REQUEST
- 가격이 0 이하 → 400 BAD_REQUEST
- 재고가 0 미만 → 400 BAD_REQUEST
```

#### 유스케이스: 상품 삭제 (어드민)

```
[Main Flow]
1. 어드민이 DELETE /api-admin/v1/products/{productId} 요청
2. 시스템이 해당 상품을 soft delete
3. 삭제 완료 응답

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 이미 삭제된 상품 → 404 NOT_FOUND

[주의]
- 이미 주문된 상품의 스냅샷에는 영향 없음 (스냅샷은 독립적)
- 삭제된 상품에 대한 좋아요는 유지, 조회 시 제외
```

#### 고객 vs 어드민 노출 정보

| 필드 | 고객 | 어드민 |
|------|------|--------|
| id | O | O |
| name (상품명) | O | O |
| price (가격) | O | O |
| description (설명) | O | O |
| brandId | O | O |
| brandName | O | O |
| likeCount | O | O |
| stock (재고) | X | O |
| createdAt | X | O |
| updatedAt | X | O |
| deletedAt | X | O |

---

### 4.3 좋아요 (Like)

#### 유저 스토리

- 로그인한 고객은 상품에 좋아요를 남길 수 있다
- 이미 좋아요한 상품에 다시 요청해도 정상 응답한다 (멱등)
- 좋아요를 취소할 수 있다
- 좋아요하지 않은 상품을 취소해도 정상 응답한다 (멱등)
- 내가 좋아요한 상품 목록을 조회할 수 있다

#### 유스케이스: 좋아요 등록

```
[Main Flow]
1. 로그인 유저가 POST /api/v1/products/{productId}/likes 요청
2. 시스템이 상품 존재 여부 확인
3. 이미 좋아요가 있는지 확인
4-a. 없으면 → 좋아요 저장, Product.likeCount 증가
4-b. 있으면 → 아무 동작 없이 200 반환 (멱등)
5. 성공 응답

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 → 404 NOT_FOUND
```

#### 유스케이스: 좋아요 취소

```
[Main Flow]
1. 로그인 유저가 DELETE /api/v1/products/{productId}/likes 요청
2. 시스템이 상품 존재 여부 확인
3. 좋아요가 있는지 확인
4-a. 있으면 → 좋아요 삭제, Product.likeCount 감소
4-b. 없으면 → 아무 동작 없이 200 반환 (멱등)
5. 성공 응답

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 → 404 NOT_FOUND
```

#### 유스케이스: 좋아요 목록 조회

```
[Main Flow]
1. 로그인 유저가 GET /api/v1/users/{userId}/likes 요청
2. 시스템이 요청 유저와 로그인 유저 일치 여부 확인
3. 해당 유저의 좋아요 상품 목록 반환

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- userId가 로그인 유저와 다름 → 403 FORBIDDEN ("타 유저의 정보에 접근할 수 없습니다")
- 삭제된 상품은 목록에서 제외
```

---

### 4.4 주문 (Order)

#### 유저 스토리

- 로그인한 고객은 여러 상품을 한 번에 주문할 수 있다
- 주문 시 상품의 재고가 확인되고 차감된다
- 주문 정보에는 당시의 상품 정보가 스냅샷으로 저장된다
- 기간별 주문 목록을 조회할 수 있다
- 주문 상세 정보를 조회할 수 있다
- 어드민은 전체 주문 목록과 상세를 조회할 수 있다

#### 유스케이스: 주문 요청

```
[Main Flow]
1. 로그인 유저가 POST /api/v1/orders 요청
   { "items": [{ "productId": 1, "quantity": 2 }, { "productId": 3, "quantity": 1 }] }
2. 시스템이 모든 상품의 존재 여부 확인
3. 시스템이 모든 상품의 재고 확인
4. 모든 상품의 재고 차감 (원자적)
5. 주문 생성 (상태: ORDERED)
6. 각 주문 항목에 상품 스냅샷 저장 (상품명, 가격, 브랜드명)
7. 생성된 주문 정보 반환

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- items가 비어있음 → 400 BAD_REQUEST
- quantity가 0 이하 → 400 BAD_REQUEST
- 동일 productId가 items에 중복으로 포함 → 400 BAD_REQUEST ("중복된 상품이 포함되어 있습니다")
- productId에 해당하는 상품이 없음 → 404 NOT_FOUND
- 삭제된 상품 포함 → 404 NOT_FOUND
- 하나라도 재고 부족 → 400 BAD_REQUEST ("상품 '{상품명}'의 재고가 부족합니다"), 전체 주문 실패 (롤백)

[트랜잭션 경계]
- 재고 확인 ~ 차감 ~ 주문 생성 ~ 스냅샷 저장은 하나의 트랜잭션
- 하나라도 실패하면 전체 롤백
```

#### 유스케이스: 주문 목록 조회 (고객)

```
[Main Flow]
1. 로그인 유저가 GET /api/v1/orders?startAt=2026-01-31&endAt=2026-02-10 요청
2. 시스템이 해당 기간 내 유저의 주문 목록 반환

[기준]
- startAt/endAt은 주문 생성일(createdAt) 기준
- 해당 유저의 주문만 반환 (타 유저 주문 조회 불가)

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- startAt > endAt → 400 BAD_REQUEST
```

#### 유스케이스: 주문 상세 조회 (고객)

```
[Main Flow]
1. 로그인 유저가 GET /api/v1/orders/{orderId} 요청
2. 시스템이 주문 정보 + 주문 항목(스냅샷 포함) 반환

[Exception Flow]
- 미로그인 → 401 UNAUTHORIZED
- orderId에 해당하는 주문이 없음 → 404 NOT_FOUND
- 타 유저의 주문 → 403 FORBIDDEN
```

#### 유스케이스: 주문 목록 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/orders?page=0&size=20 요청
2. 모든 유저의 주문 목록을 페이징하여 반환

[기본값]
- page: 0
- size: 20

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
```

#### 유스케이스: 주문 상세 조회 (어드민)

```
[Main Flow]
1. 어드민이 GET /api-admin/v1/orders/{orderId} 요청
2. 시스템이 주문 정보 + 주문 항목(스냅샷 포함) 반환
3. 모든 유저의 주문 조회 가능

[Exception Flow]
- LDAP 헤더 누락/불일치 → 401 UNAUTHORIZED
- orderId에 해당하는 주문이 없음 → 404 NOT_FOUND
```

## 5. 잠재 리스크

### 5.1 트랜잭션 비대화 (주문)

주문 요청 시 `재고 확인 → 차감 → 주문 생성 → 스냅샷 저장`이 하나의 트랜잭션으로 묶인다. 상품 수가 많아지면 트랜잭션이 길어지고, DB 락 점유 시간이 증가한다.

| 선택지 | 설명 |
|--------|------|
| 현재 방식 유지 | 기능 구현 단계에서는 충분. 동시성 단계에서 개선 |
| 재고 차감을 비동기로 분리 | 이벤트 기반으로 분리 가능하나, 보상 트랜잭션 필요 |

> 현재 단계에서는 단일 트랜잭션으로 구현하고, "동시 주문" 문제를 해결하는 단계에서 개선한다.

### 5.2 비정규화 필드 정합성 (likeCount, stock)

Product의 `likeCount`와 `stock`은 비정규화된 필드다. 동시 요청 시 lost update가 발생할 수 있다.

| 선택지 | 설명 |
|--------|------|
| 현재 방식 유지 | 기능 구현 단계에서는 정합성 이슈 무시 |
| 비관적 락 | SELECT FOR UPDATE로 정합성 보장, 성능 저하 |
| 낙관적 락 | @Version 활용, 충돌 시 재시도 |

> 요구사항에 "동시성 문제는 나중에 해결"이라고 명시되어 있으므로, 현재는 단순 구현 후 별도 단계에서 해결한다.

### 5.3 Soft Delete와 조회 성능

모든 엔티티가 soft delete를 사용하므로, 모든 조회 쿼리에 `WHERE deleted_at IS NULL` 조건이 붙는다.

| 선택지 | 설명 |
|--------|------|
| @Where 어노테이션 | 엔티티 레벨에서 자동 적용, 삭제 데이터 조회 어려움 |
| 쿼리마다 명시적 조건 | 유연하지만 누락 가능성 |

> 현재 프로젝트 패턴에 맞춰 결정한다. 기존 User 엔티티에 @Where가 없으므로 쿼리마다 명시적으로 처리하는 방향이 일관적이다.

### 5.4 브랜드 삭제 시 연쇄 영향

브랜드 삭제 시 해당 상품도 삭제해야 하는데, 그 상품에 대한 좋아요는 어떻게 할 것인가?

| 선택지 | 설명 |
|--------|------|
| 좋아요도 함께 삭제 | 깔끔하지만 유저의 좋아요 이력 손실 |
| 좋아요는 유지, 조회 시 삭제된 상품 필터링 | 데이터 보존, 조회 로직에서 처리 |

> 좋아요 목록 조회 시 삭제된 상품은 제외하는 방식이 데이터 보존 관점에서 적절하다.

### 5.5 상품 삭제 시 연쇄 영향

브랜드 삭제와 유사하게, 상품이 개별 삭제될 때도 해당 상품에 대한 좋아요가 존재할 수 있다.

| 선택지 | 설명 |
|--------|------|
| 좋아요도 함께 삭제 | 깔끔하지만 유저의 좋아요 이력 손실 |
| 좋아요는 유지, 조회 시 삭제된 상품 필터링 | 데이터 보존, 브랜드 삭제와 동일 정책 |

> 브랜드 삭제와 동일하게, 좋아요는 유지하고 조회 시 삭제된 상품을 제외하는 방식으로 처리한다.

---

## 6. API 전체 요약

### 고객 API (`/api/v1`)

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | /api/v1/brands/{brandId} | X | 브랜드 정보 조회 |
| GET | /api/v1/products | X | 상품 목록 조회 (필터/정렬/페이징) |
| GET | /api/v1/products/{productId} | X | 상품 상세 조회 |
| POST | /api/v1/products/{productId}/likes | O | 좋아요 등록 (멱등) |
| DELETE | /api/v1/products/{productId}/likes | O | 좋아요 취소 (멱등) |
| GET | /api/v1/users/{userId}/likes | O | 내 좋아요 목록 조회 |
| POST | /api/v1/orders | O | 주문 요청 |
| GET | /api/v1/orders | O | 주문 목록 조회 (기간 필터) |
| GET | /api/v1/orders/{orderId} | O | 주문 상세 조회 |

### 어드민 API (`/api-admin/v1`)

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | /api-admin/v1/brands | O (LDAP) | 브랜드 목록 조회 |
| GET | /api-admin/v1/brands/{brandId} | O (LDAP) | 브랜드 상세 조회 |
| POST | /api-admin/v1/brands | O (LDAP) | 브랜드 등록 |
| PUT | /api-admin/v1/brands/{brandId} | O (LDAP) | 브랜드 수정 |
| DELETE | /api-admin/v1/brands/{brandId} | O (LDAP) | 브랜드 삭제 (상품 연쇄 삭제) |
| GET | /api-admin/v1/products | O (LDAP) | 상품 목록 조회 |
| GET | /api-admin/v1/products/{productId} | O (LDAP) | 상품 상세 조회 |
| POST | /api-admin/v1/products | O (LDAP) | 상품 등록 |
| PUT | /api-admin/v1/products/{productId} | O (LDAP) | 상품 수정 (브랜드 변경 불가) |
| DELETE | /api-admin/v1/products/{productId} | O (LDAP) | 상품 삭제 |
| GET | /api-admin/v1/orders | O (LDAP) | 주문 목록 조회 |
| GET | /api-admin/v1/orders/{orderId} | O (LDAP) | 주문 상세 조회 |
