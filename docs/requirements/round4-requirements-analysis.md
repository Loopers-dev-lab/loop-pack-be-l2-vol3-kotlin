# Round 4 — 트랜잭션 & 동시성 제어, 쿠폰 도메인 요구사항

## 배경

Round 3에서 확립한 DDD 기반 아키텍처(Domain Model/JPA Entity 분리, UseCase 직접 호출, 비관적 락)를 유지하면서,
**쿠폰 도메인을 신규 도입**하고 **주문 흐름에 쿠폰 할인을 통합**한다.
동시성 문제(Lost Update)를 락 전략으로 해결하고, 이를 동시성 테스트로 검증한다.

> 기능 요구사항(API 명세, 아키텍처)은 [Round 3 요구사항](round3-requirements-analysis.md)을 기준으로 한다.
> 본 문서는 Round 3 대비 **변경/추가된 사항**만 다룬다.

---

## 0. 변경 요약 (Delta from Round 3)

### 신규

- **Coupon 바운디드 컨텍스트**: Coupon(템플릿) + IssuedCoupon(발급) 2모델 분리
- **쿠폰 대고객 API 2개**: 발급 요청, 내 쿠폰 목록 조회
- **쿠폰 어드민 API 5개**: 템플릿 CRUD + 발급 내역 조회
- **주문 할인 스냅샷**: Order에 originalPrice, discountAmount, refCouponId 필드 추가
- **동시성 테스트**: 좋아요/쿠폰/재고에 대한 동시성 검증 테스트

### 변경

- **주문 흐름 확장**: 쿠폰 유효성 검증 → 할인 적용 → 사용 처리가 기존 흐름에 추가
- **Order Domain Model**: 할인 관련 필드 추가 (originalPrice, discountAmount, refCouponId)
- **PlaceOrderUseCase**: 쿠폰 검증/적용 로직 추가
- **PlaceOrderCommand**: couponId(nullable) 필드 추가

### 삭제

- **Point 도메인 전체 제거**: UserPoint, PointHistory, Point(VO), PointDeductor, PointCharger, 관련 UseCase/API/테스트 삭제
- **주문 흐름에서 포인트 차감 단계 제거**: 결제(금액 차감) 개념이 제거됨. 향후 PG 연동 시 결제 모듈 추가 예정
- **RegisterUserUseCase에서 UserPoint 생성 제거**: 회원가입 시 UserPoint를 더 이상 생성하지 않음

### 유지

- 기존 아키텍처 (레이어, DIP, UseCase 패턴) 변경 없음
- 기존 도메인 (User, Catalog, Like, Order) 구조 유지
- 기존 비관적 락 전략 (Product, Like) 유지
- 기존 API 명세 변경 없음 (주문 API에 optional couponId 추가만)

---

## 1. 문제 정의

### Round 4 핵심 문제

| 관점     | 문제                                                                                         |
|--------|--------------------------------------------------------------------------------------------|
| 동시성    | 동시에 여러 사용자가 주문할 때 재고가 꼬이지 않도록 트랜잭션과 락을 적용해야 한다. `@Transactional`만으로는 Lost Update를 방지할 수 없다 |
| 정합성    | 주문 시 재고 차감 + 쿠폰 사용이 all-or-nothing으로 처리되어야 한다                                               |
| 쿠폰 도메인 | 쿠폰 템플릿 관리, 사용자 발급, 주문 시 할인 적용까지의 전체 생명주기를 설계해야 한다                                          |
| 검증     | 동시성 문제는 로컬 환경에서 드러나지 않으므로 멀티스레드 테스트로 검증해야 한다                                               |

---

## 2. 유비쿼터스 언어 (추가)

Round 3 유비쿼터스 언어에 다음 용어를 추가한다.

| 한글       | 영문             | 정의                                                                  |
|----------|----------------|---------------------------------------------------------------------|
| 쿠폰 템플릿   | Coupon         | 어드민이 생성/관리하는 쿠폰 정의. 할인 유형(FIXED/RATE), 값, 최소 주문 금액, 만료일, 발급 수량을 포함  |
| 발급 쿠폰    | IssuedCoupon   | 사용자에게 발급된 개별 쿠폰. AVAILABLE/USED/EXPIRED 상태를 가진다                     |
| 쿠폰 유형    | CouponType     | 할인 방식을 구분하는 enum. FIXED(정액 할인) / RATE(정률 할인)                        |
| 쿠폰 상태    | CouponStatus   | 발급 쿠폰의 생명주기. AVAILABLE(사용 가능) → USED(사용 완료) / EXPIRED(만료)           |
| 정액 할인    | FIXED          | 고정 금액을 할인. value=5000이면 5,000원 할인                                   |
| 정률 할인    | RATE           | 주문 금액의 비율을 할인. value=10이면 10% 할인. maxDiscount로 최대 할인 한도 설정 가능       |
| 최대 할인 한도 | maxDiscount    | 정률 쿠폰에서 할인 금액의 상한선 (nullable). null이면 한도 없음                         |
| 최소 주문 금액 | minOrderAmount | 쿠폰 적용을 위한 최소 주문 금액 조건 (nullable). 쿠폰 적용 전 금액(originalPrice) 기준으로 확인 |
| 할인 전 금액  | originalPrice  | 쿠폰 적용 전 주문 총액. 각 상품의 (가격 x 수량) 합계                                   |
| 할인 금액    | discountAmount | 쿠폰에 의해 할인된 금액                                                       |
| 최종 금액    | totalPrice     | 쿠폰 할인 적용 후 최종 주문 금액. originalPrice - discountAmount                   |
| 발급 수량    | totalQuantity  | 쿠폰 템플릿에서 발급 가능한 최대 수량 (nullable). null이면 무제한 발급                     |
| 발급된 수량   | issuedCount    | 현재까지 발급된 쿠폰 수. totalQuantity와 비교하여 발급 가능 여부 판단                      |

---

## 3. 신규 도메인: Coupon

### 3.1 Coupon (쿠폰 템플릿)

어드민이 생성/관리하는 쿠폰의 정의(틀)이다. 할인 규칙과 발급 조건을 포함한다.

**Domain Model 구조:**

| 필드             | 타입             | 설명                                        |
|----------------|----------------|-------------------------------------------|
| id             | Long           | PK                                        |
| name           | String         | 쿠폰명 (예: "신규가입 10% 할인")                    |
| type           | CouponType     | FIXED / RATE                              |
| value          | Long           | FIXED: 할인 금액(원), RATE: 할인 비율(%)           |
| maxDiscount    | Money?         | RATE 타입 최대 할인 한도 (nullable, FIXED 시 null) |
| minOrderAmount | Money?         | 최소 주문 금액 조건 (nullable)                    |
| totalQuantity  | Int?           | 발급 가능 수량 (nullable = 무제한)                 |
| issuedCount    | Int            | 현재 발급된 수량 (기본 0)                          |
| expiredAt      | ZonedDateTime  | 만료 일시                                     |
| deletedAt      | ZonedDateTime? | soft delete                               |

**행위:**

| 메서드                                            | 설명                                                                                   |
|------------------------------------------------|--------------------------------------------------------------------------------------|
| `canIssue(): Boolean`                          | 발급 가능 여부 (수량 잔여 + 미만료 + 미삭제)                                                         |
| `issue()`                                      | 발급 처리 (issuedCount 증가). canIssue() 실패 시 예외                                           |
| `calculateDiscount(orderAmount: Money): Money` | 주문 금액에 대한 할인 금액 계산. FIXED: value 그대로, RATE: orderAmount * value/100 (maxDiscount 적용) |
| `isExpired(): Boolean`                         | 만료 여부 확인                                                                             |

**불변식:**

- `name`은 비어있을 수 없다
- `value > 0`
- RATE 타입: `1 <= value <= 100`
- `totalQuantity`가 not null이면 `totalQuantity > 0`
- `issuedCount >= 0`, `issuedCount <= totalQuantity` (totalQuantity가 not null일 때)

### 3.2 IssuedCoupon (발급 쿠폰)

사용자에게 발급된 개별 쿠폰 인스턴스. 사용자의 소유이며 1회만 사용 가능하다.

**Domain Model 구조:**

| 필드          | 타입             | 설명                         |
|-------------|----------------|----------------------------|
| id          | Long           | PK                         |
| refCouponId | Long           | Coupon 템플릿 참조 (논리FK)       |
| refUserId   | UserId         | 소유 사용자 참조 (논리FK)           |
| status      | CouponStatus   | AVAILABLE / USED / EXPIRED |
| usedAt      | ZonedDateTime? | 사용 일시 (USED 상태 전환 시 기록)    |
| createdAt   | ZonedDateTime  | 발급 일시                      |

**행위:**

| 메서드                          | 설명                                           |
|------------------------------|----------------------------------------------|
| `use()`                      | 쿠폰 사용 처리. AVAILABLE → USED 전환. 이미 사용/만료 시 예외 |
| `isAvailable(): Boolean`     | 사용 가능 여부 (status == AVAILABLE)               |
| `isOwnedBy(userId): Boolean` | 소유자 확인                                       |

**불변식:**

- `status == USED`이면 `usedAt != null`

### 3.3 CouponType (enum)

| 값     | 설명                                   |
|-------|--------------------------------------|
| FIXED | 정액 할인 (value = 할인 금액)                |
| RATE  | 정률 할인 (value = 할인 %, maxDiscount 적용) |

### 3.4 CouponStatus (enum)

| 값         | 설명              |
|-----------|-----------------|
| AVAILABLE | 사용 가능 (발급 직후)   |
| USED      | 사용 완료 (주문에 적용됨) |
| EXPIRED   | 만료됨             |

---

## 4. 도메인 규칙 (추가/변경)

### 4.1 쿠폰 템플릿 규칙

- 어드민만 쿠폰 템플릿을 생성/수정/삭제할 수 있다
- 템플릿 삭제(soft delete) 시 이미 발급된 쿠폰(AVAILABLE)은 **상태 유지** — 사용 가능. 신규 발급만 차단
- 만료된 템플릿에서는 신규 발급 불가

### 4.2 쿠폰 발급 규칙

- 사용자당 동일 템플릿 **1회만** 발급 가능 (1인 1매)
- totalQuantity 설정 시 `issuedCount < totalQuantity`일 때만 발급 가능
- 발급 시 Coupon 템플릿에 **비관적 락** 적용 (선착순 동시성 제어)
- 삭제/만료된 템플릿에서는 발급 불가

### 4.3 쿠폰 사용 규칙 (주문 시)

- 주문 1건당 쿠폰 1장만 적용 가능
- 쿠폰 적용 조건 검증:
    - 존재하는 발급 쿠폰인가
    - 요청한 사용자 소유인가
    - 상태가 AVAILABLE인가
    - 해당 쿠폰의 템플릿이 만료되지 않았는가
- minOrderAmount가 설정된 경우, **쿠폰 적용 전 금액(originalPrice)** 기준으로 확인
- 조건 불충족 시 주문 전체 실패

### 4.4 할인 계산 규칙

- **FIXED**: `discountAmount = value` (단, originalPrice보다 클 수 없음)
- **RATE**: `discountAmount = min(originalPrice * value / 100, maxDiscount ?? Long.MAX_VALUE)`
- `totalPrice = originalPrice - discountAmount` (0 이상 보장)

### 4.5 주문 흐름 변경

Round 3의 주문 흐름에 쿠폰 처리가 추가된다.

**변경된 주문 처리 흐름:**

1. 시스템이 각 상품의 존재 여부 및 판매 가능 상태를 확인한다
2. 시스템이 각 상품의 재고를 확인하고 차감한다 (비관적 락, all-or-nothing)
    - 재고 차감 후 stock == 0이면 status → `SOLD_OUT` 자동 전환
3. **[신규] couponId가 있으면 발급 쿠폰을 비관적 락으로 조회하고 유효성 검증**
    - 존재하지 않거나 / 타 유저 소유 / 이미 사용 / 만료 → 주문 전체 실패 (재고 롤백)
4. **[신규] 할인 금액을 계산하고 originalPrice/discountAmount/totalPrice를 결정**
    - minOrderAmount 미충족 시 주문 실패
5. **[신규] 쿠폰 상태를 USED로 변경**
6. 주문 시점의 상품 정보(이름, 가격)를 스냅샷으로 저장한다
7. 주문이 생성되고 (status: CREATED) 사용자에게 응답한다

**추가된 예외 흐름:**

| 조건                 | 응답  | 설명          |
|--------------------|-----|-------------|
| 존재하지 않는 쿠폰         | 400 | NOT_FOUND   |
| 타 유저 소유 쿠폰         | 400 | 사용 권한 없음    |
| 이미 사용된 쿠폰 (USED)   | 400 | 이미 사용된 쿠폰   |
| 만료된 쿠폰 (EXPIRED)   | 400 | 만료된 쿠폰      |
| minOrderAmount 미충족 | 400 | 최소 주문 금액 미달 |

### 4.6 도메인 경계 정의 (Round 3 + Round 4)

| 도메인 경계      | 포함 객체                                                | 근거                                  |
|-------------|------------------------------------------------------|-------------------------------------|
| **User**    | User                                                 | 인증/프로필 책임                           |
| **Catalog** | Product, Brand, Stock(VO), ProductStatus             | 상품 탐색에 필요한 정보                       |
| **Like**    | Like                                                 | 사용자-상품 관심 표현                        |
| **Order**   | Order, OrderItem, OrderProductData, OrderStatus      | 주문 생명주기                             |
| **Coupon**  | Coupon, IssuedCoupon, CouponType, CouponStatus       | 쿠폰 정의/발급/사용 생명주기. 독립적 정책 발전 가능 (신규) |

**도메인 경계에 따른 서비스 배치 (추가):**

| 조합                                   | 경계 판단          | 서비스 위치                                            |
|--------------------------------------|----------------|---------------------------------------------------|
| Coupon + IssuedCoupon (발급)  | 같은 경계 (Coupon) | UseCase (`IssueCouponUseCase`) — Repository 직접 호출 |
| Product + Coupon + Order    | 다른 경계          | UseCase (`PlaceOrderUseCase`)                     |

> Coupon BC 내에 Domain Service가 필요한가? — 현재는 불필요. 발급(Coupon.issue() + IssuedCoupon 생성)은 단순 조합이므로 UseCase가 직접 처리. 향후 쿠폰 정책이
> 복잡해지면 Domain Service 도입 검토.

---

## 5. API 변경사항

### 5.1 쿠폰 대고객 API (신규)

| METHOD | URI                                | 인증 | 설명         |
|--------|------------------------------------|----|------------|
| POST   | `/api/v1/coupons/{couponId}/issue` | O  | 쿠폰 발급 요청   |
| GET    | `/api/v1/users/me/coupons`         | O  | 내 쿠폰 목록 조회 |

**쿠폰 발급 요청 예외 흐름:**

| 조건             | 응답  | 설명               |
|----------------|-----|------------------|
| 인증 헤더 누락/실패    | 401 | 인증 필요            |
| 존재하지 않는 쿠폰 템플릿 | 404 | NOT_FOUND        |
| 삭제된 쿠폰 템플릿     | 404 | NOT_FOUND        |
| 만료된 쿠폰 템플릿     | 400 | 만료된 쿠폰           |
| 발급 수량 소진       | 400 | 발급 수량 초과         |
| 이미 발급받은 쿠폰     | 409 | 중복 발급 불가 (1인 1매) |

**내 쿠폰 목록 조회 응답:**

쿠폰 상태(AVAILABLE / USED / EXPIRED)를 함께 반환한다. 쿠폰 템플릿 정보(name, type, value 등)도 포함.

### 5.2 쿠폰 어드민 API (신규)

| METHOD | URI                                                      | 인증   | 설명              |
|--------|----------------------------------------------------------|------|-----------------|
| GET    | `/api-admin/v1/coupons?page=0&size=20`                   | LDAP | 쿠폰 템플릿 목록 조회    |
| GET    | `/api-admin/v1/coupons/{couponId}`                       | LDAP | 쿠폰 템플릿 상세 조회    |
| POST   | `/api-admin/v1/coupons`                                  | LDAP | 쿠폰 템플릿 등록       |
| PUT    | `/api-admin/v1/coupons/{couponId}`                       | LDAP | 쿠폰 템플릿 수정       |
| DELETE | `/api-admin/v1/coupons/{couponId}`                       | LDAP | 쿠폰 템플릿 삭제       |
| GET    | `/api-admin/v1/coupons/{couponId}/issues?page=0&size=20` | LDAP | 특정 쿠폰의 발급 내역 조회 |

**쿠폰 템플릿 등록 요청:**

```json
{
  "name": "신규가입 10% 할인",
  "type": "RATE",
  "value": 10,
  "maxDiscount": 5000,
  "minOrderAmount": 10000,
  "totalQuantity": 100,
  "expiredAt": "2026-12-31T23:59:59"
}
```

### 5.3 주문 API 변경

**요청 변경 (couponId 추가):**

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 3,
      "quantity": 1
    }
  ],
  "couponId": 42
}
```

> `couponId`는 nullable. 미적용 시 생략 가능.

**응답 변경 (할인 정보 포함):**

기존 응답에 originalPrice, discountAmount 필드가 추가된다.

### 5.4 인증 경로 추가

| 경로 패턴                      | 인터셉터            | 비고                           |
|----------------------------|-----------------|------------------------------|
| `/api/v1/coupons/**`       | AuthInterceptor | 쿠폰 발급 요청                     |
| `/api/v1/users/me/coupons` | AuthInterceptor | 기존 `/api/v1/users/**` 패턴에 포함 |

---

## 6. 동시성 제어 전략

### 6.1 전략 요약

| 대상        | 락 전략      | 메서드                                   | 근거                                             |
|-----------|-----------|---------------------------------------|------------------------------------------------|
| 상품 재고 차감  | 비관적 락     | `findAllByIdsForUpdate(ids)`          | 다수 동시 주문, 정합성 필수. 기존 구현 유지                     |
| 좋아요 수 증감  | 비관적 락     | `findByIdForUpdate(id)`               | likeCount 정합성 보장. 기존 구현 유지                     |
| 좋아요 토글    | 비관적 락     | `findByUserIdAndProductIdForUpdate()` | 중복 좋아요 방지. 기존 구현 유지                            |
| **쿠폰 사용** | **비관적 락** | `findByIdForUpdate(id)`               | 동일 쿠폰 동시 주문 시 단 1회만 사용. 대기 후 두 번째는 USED 상태로 실패 |
| **쿠폰 발급** | **비관적 락** | `findByIdForUpdate(id)` (Coupon 템플릿)  | 선착순 발급 시 수량 초과 방지. 재고 차감과 유사한 구조               |

### 6.2 주문 트랜잭션 내 락 순서

데드락 방지를 위해 락 획득 순서를 일관되게 유지한다:

```
1. Product (ID 오름차순 정렬 — 기존)
2. IssuedCoupon (단일 row)
```

### 6.3 이전 라운드 동시성 결정 상태 업데이트

| 항목 (Round 3)      | 상태     | Round 4 변경사항                                    |
|-------------------|--------|-------------------------------------------------|
| 포인트 동시성 — 비관적 락   | **삭제** | Point 도메인 제거로 해당 없음                             |
| 재고 동시성 — Redis 전략 | **변경** | DB 비관적 락으로 해결 (이미 구현됨). Redis는 향후 성능 최적화 시 검토   |
| 2-Phase 주문        | **보류** | 보류 유지. 현재 규모에서는 단일 트랜잭션 유지                      |

---

## 7. 아키텍처 결정

### 7.1 패키지 구조 (추가분)

```
com.loopers/
├── interfaces/api/
│   ├── coupon/              ← 신규
│   │   ├── CouponV1Controller.kt        (대고객)
│   │   ├── CouponV1ApiSpec.kt
│   │   └── dto/
│   └── admin/coupon/        ← 신규
│       ├── CouponAdminV1Controller.kt
│       ├── CouponAdminV1ApiSpec.kt
│       └── dto/
├── application/
│   └── coupon/              ← 신규
│       ├── IssueCouponUseCase.kt
│       ├── GetMyCouponsUseCase.kt
│       ├── CreateCouponAdminUseCase.kt
│       ├── UpdateCouponAdminUseCase.kt
│       ├── DeleteCouponAdminUseCase.kt
│       ├── GetCouponsAdminUseCase.kt
│       ├── GetCouponAdminUseCase.kt
│       └── GetCouponIssuesAdminUseCase.kt
├── domain/
│   └── coupon/              ← 신규
│       ├── model/
│       │   ├── Coupon.kt            (템플릿)
│       │   ├── IssuedCoupon.kt      (발급)
│       │   ├── CouponType.kt        (FIXED/RATE)
│       │   └── CouponStatus.kt      (AVAILABLE/USED/EXPIRED)
│       ├── CouponRepository.kt
│       └── IssuedCouponRepository.kt
└── infrastructure/
    └── coupon/              ← 신규
        ├── CouponEntity.kt
        ├── CouponRepositoryImpl.kt
        ├── IssuedCouponEntity.kt
        └── IssuedCouponRepositoryImpl.kt
```

### 7.2 Order Domain Model 변경

```kotlin
class Order private constructor(
    val id: OrderId = OrderId(0),
    val refUserId: UserId,
    status: OrderStatus,
    totalPrice: Money,        // 최종 주문 금액 (할인 후)
    originalPrice: Money,     // 할인 전 금액 (신규)
    discountAmount: Money,    // 할인 금액 (신규, 쿠폰 미적용 시 0)
    refCouponId: Long?,       // 사용된 발급 쿠폰 ID (신규, nullable)
    val items: List<OrderItem> = emptyList(),
    val deletedAt: ZonedDateTime? = null,
)
```

> 쿠폰 미적용 시: `originalPrice == totalPrice`, `discountAmount == Money(0)`, `refCouponId == null`

### 7.3 Coupon BC에 Domain Service 불필요

Coupon 도메인 내에 Domain Service를 두지 않는다.

- 쿠폰 발급: Coupon.issue() + IssuedCoupon 생성 — 단순 조합이므로 UseCase가 직접 처리
- 할인 계산: Coupon.calculateDiscount() — Domain Model 메서드로 충분
- "여러 Entity 간 원자적 얽힘"이 아닌 "순차적 조립"이므로 Domain Service가 불필요

---

## 8. 테스트 전략

### 8.1 동시성 테스트

`CountDownLatch` + `ExecutorService` (또는 `CompletableFuture`)로 멀티스레드 동시 실행.
성공 수, 실패 수, DB 최종 상태를 모두 검증한다.

**필수 동시성 테스트 목록:**

| 테스트            | 시나리오                            | 검증 포인트                                |
|----------------|---------------------------------|---------------------------------------|
| 좋아요 동시 요청      | N명이 동시에 같은 상품에 좋아요 요청           | likeCount == N, Like 레코드 N개           |
| 쿠폰 동시 사용 (주문)  | 동일 쿠폰으로 여러 기기에서 동시 주문           | 1건만 성공, 나머지 실패. 쿠폰 상태 USED            |
| 재고 동시 차감 (주문)  | 재고 10개 상품에 N명 동시 주문             | 성공 수 + 실패 수 == N, 최종 재고 >= 0          |
| 쿠폰 동시 발급 (선착순) | totalQuantity=10인 템플릿에 N명 동시 발급 | 발급 성공 수 <= 10, issuedCount == 발급 성공 수 |

### 8.2 단위 테스트 (추가)

| 대상           | 검증 내용                                                     |
|--------------|-----------------------------------------------------------|
| Coupon       | 할인 계산 (FIXED/RATE), maxDiscount 적용, 발급 가능 여부, 만료 확인       |
| IssuedCoupon | 사용 처리, 상태 전이 (AVAILABLE→USED), 중복 사용 방지                   |
| Order (확장)   | 할인 적용된 주문 생성, originalPrice/discountAmount/totalPrice 정합성 |

### 8.3 E2E 테스트 (추가)

| 대상          | 검증 내용                                      |
|-------------|--------------------------------------------|
| 쿠폰 발급       | 정상 발급, 중복 발급 거부, 수량 소진 거부, 만료 거부           |
| 쿠폰 적용 주문    | 정상 할인 적용, 쿠폰 USED 전환, 할인 금액 정합성 검증           |
| 쿠폰 적용 실패 주문 | 타인 쿠폰, 이미 사용된 쿠폰, 만료 쿠폰, minOrderAmount 미달 |
| 어드민 쿠폰 CRUD | 템플릿 생성/수정/삭제/조회, 발급 내역 조회                  |

---

## 9. 잠재 리스크

### 기존 리스크 상태 업데이트

| 리스크 (Round 3)         | 상태     | Round 4 변경사항                              |
|-----------------------|--------|-------------------------------------------|
| 주문 트랜잭션 비대화           | **변경** | 포인트 차감 제거로 축소, 쿠폰 검증/사용 처리 추가. 전체적으로 유사 규모 |
| 포인트 정합성               | **삭제** | Point 도메인 제거로 해당 없음                          |
| Catalog UseCase 비대화   | **유지** | 변경 없음                                       |
| Fake Repository 유지 비용 | **유지** | Point Fake 2개 삭제, Coupon Fake 2개 추가. 총 수 유사 |

### 신규 리스크

| 리스크                | 영향                                                   | 현재 대응                                        | 향후 대응                          |
|--------------------|------------------------------------------------------|----------------------------------------------|--------------------------------|
| 주문 트랜잭션에 3개 도메인 참여 | Product + Coupon + Order가 하나의 트랜잭션 → 락 경합 가능        | 단일 트랜잭션으로 처리 (현재 규모)                         | 이벤트 기반 분리 (재고 선점 → 쿠폰 적용 → 확정) |
| 쿠폰 만료 처리           | AVAILABLE 상태의 만료 쿠폰이 DB에 남아있음                        | 주문/조회 시점에 만료 여부 확인                           | 배치 작업으로 EXPIRED 일괄 전환 검토       |
| 쿠폰 + 재고 동시 롤백      | 쿠폰 검증 실패 시 재고 차감도 롤백 필요                               | 단일 트랜잭션 내 처리로 자동 롤백                          | —                              |
| 할인 금액 > 주문 금액      | FIXED 쿠폰이 주문 금액보다 큰 경우                               | `discountAmount = min(value, originalPrice)` | —                              |
| 결제 개념 부재           | 포인트 제거 후 금액 차감 없이 주문 생성                               | 현재는 결제 없이 주문 생성 (재고 + 쿠폰만)                   | 향후 PG 연동 시 결제 모듈 추가 예정         |

---

## 10. 설계 결정 사항

### Coupon을 독립 바운디드 컨텍스트로 분리하는 이유

- **결정**: Coupon(템플릿) + IssuedCoupon(발급)을 별도 BC로 관리한다
- **근거**: 기존 패턴(Like 등 독립 BC)과 일관. 쿠폰 정책(발급 조건, 할인 규칙)이 주문과 독립적으로 발전할 수 있다. 어드민 CRUD가 있으므로 Order에 포함하면 경계가 모호해진다

### 2모델 분리 (Coupon + IssuedCoupon)

- **결정**: 쿠폰 템플릿과 발급 쿠폰을 별도 Domain Model로 관리한다
- **근거**: 템플릿은 어드민이 관리하는 "정의", 발급 쿠폰은 사용자가 소유하는 "인스턴스"다. 역할과 생명주기가 다르다. 1:N 관계(하나의 템플릿에서 여러 발급)를 자연스럽게 표현

### 비관적 락 일관 전략

- **결정**: 쿠폰 사용/발급 모두 비관적 락(SELECT FOR UPDATE)을 사용한다
- **근거**: 프로젝트 전체가 비관적 락으로 통일되어 있다(Product, Like). 쿠폰도 동일 패턴 유지. 낙관적 락은 학습 목표에 포함되어 있으나, 현재 프로젝트에서는 비관적 락이 기존 구현과
  일관되므로 우선 적용. 낙관적 락은 별도 비교 학습으로 다룸

### Order에 할인 필드 직접 추가

- **결정**: 별도 VO(OrderPrice)를 도입하지 않고, Order에 originalPrice, discountAmount, refCouponId 필드를 직접 추가한다
- **근거**: 필드가 3개로 VO로 캡슐화할 만큼 복잡하지 않다. 기존 Order 패턴(totalPrice 직접 보유)과 일관. 향후 할인 정책이 복잡해지면 VO 도입 검토

### 정률 쿠폰에 maxDiscount 적용

- **결정**: CouponTemplate에 maxDiscount(nullable) 필드를 추가한다
- **근거**: 요구사항에 명시되지 않았으나, 실무에서 정률 쿠폰의 무제한 할인은 비현실적. null이면 한도 없이 동작하므로 하위 호환 유지

### totalQuantity를 통한 선착순 발급

- **결정**: Coupon 템플릿에 totalQuantity(nullable) + issuedCount 필드를 추가한다
- **근거**: 쿠폰 발급 API(`POST /api/v1/coupons/{couponId}/issue`)가 있으므로 선착순 시나리오가 자연스럽다. null이면 무제한 발급

### Point 도메인 제거

- **결정**: Point 도메인(UserPoint, PointHistory, PointDeductor, PointCharger)을 완전히 제거한다
- **근거**: 포인트 충전 → 결제라는 케이스가 현업에서 직관적이지 않다는 피드백. 향후 PG 연동 시 결제 모듈을 별도로 추가할 예정. 현재는 재고 차감 + 쿠폰 할인만으로 주문 생성

### 쿠폰 템플릿 삭제 시 발급 쿠폰 유지

- **결정**: 템플릿이 삭제되어도 이미 발급된 AVAILABLE 쿠폰은 상태 유지 (사용 가능)
- **근거**: 사용자 권리 보호. Brand 삭제 시 Product cascade와 달리, 이미 발급된 쿠폰은 사용자의 소유이므로 일방적 회수 불가. 신규 발급만 차단

---

## 11. 어드민/대고객 권한 경계 (추가)

기존 Round 3 권한 경계에 Coupon 도메인을 추가한다.

| 대상              | 대고객                       | 어드민              |
|-----------------|---------------------------|------------------|
| Coupon Template | 활성 목록만 조회 불가 (발급 API만 존재) | 전체 조회/생성/수정/삭제   |
| IssuedCoupon    | 본인 쿠폰만 조회, 주문 시 사용        | 특정 템플릿의 발급 내역 조회 |
| Brand           | 활성만 조회 (기존)               | 전체 조회/수정/복구 (기존) |
| Product         | 활성만 조회 (기존)               | 전체 조회/수정/복구 (기존) |
| Order           | 본인 주문만 (기존)               | 전체 조회 (기존)       |
| Like            | 삭제/HIDDEN 상품 좋아요 불가 (기존)  | — (기존)           |

---

## 12. 기존 설계 산출물 영향 분석

Round 4 요구사항으로 인해 업데이트가 필요한 설계 산출물 목록:

| 산출물                                   | 필요 작업                                       | 우선순위 |
|---------------------------------------|---------------------------------------------|------|
| `docs/design/01-requirements.md`      | Coupon 도메인 용어 + 도메인 경계 추가                   | 상    |
| `docs/design/02-sequence-diagrams.md` | 쿠폰 발급 흐름, 쿠폰 적용 주문 흐름 시퀀스 추가                | 상    |
| `docs/design/03-class-diagram.md`     | Coupon, IssuedCoupon 클래스 + Order 필드 변경 반영   | 상    |
| `docs/design/04-erd.md`               | coupon, issued_coupon 테이블 + order 테이블 컬럼 추가 | 상    |
| `docs/design/05-flowcharts.md`        | 주문 흐름에 쿠폰 검증/적용 단계 추가                       | 중    |

> **직접 수정하지 않는다** — 각 스킬(`/sequence`, `/class-diagram`, `/erd`)로 위임한다.

---

## 체크리스트

### Coupon 도메인 (신규)

- [ ] 쿠폰은 사용자가 소유하고 있으며, 이미 사용된 쿠폰은 사용할 수 없어야 한다
- [ ] 쿠폰 종류는 정액(FIXED) / 정률(RATE)로 구분되며, 각 할인 계산 로직을 구현하였다
- [ ] 정률 쿠폰에 maxDiscount가 설정된 경우 할인 한도를 초과하지 않는다
- [ ] 각 발급된 쿠폰은 최대 한 번만 사용될 수 있다
- [ ] 사용자당 동일 템플릿 1회만 발급 가능하다
- [ ] totalQuantity 설정 시 초과 발급되지 않는다

### 주문 (변경)

- [ ] 주문 전체 흐름에 대해 원자성이 보장되어야 한다
- [ ] 사용 불가능하거나 존재하지 않는 쿠폰일 경우 주문은 실패해야 한다
- [ ] 재고가 존재하지 않거나 부족할 경우 주문은 실패해야 한다
- [ ] 쿠폰, 재고 처리 등 하나라도 작업이 실패하면 모두 롤백 처리되어야 한다
- [ ] 주문 성공 시 모든 처리는 정상 반영되어야 한다
- [ ] 주문 스냅샷에 originalPrice, discountAmount, totalPrice가 모두 포함된다

### 동시성 테스트

- [ ] 동일한 상품에 대해 여러 명이 좋아요/싫어요를 요청해도, 상품의 좋아요 수가 정상 반영되어야 한다
- [ ] 동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한 번만 사용되어야 한다
- [ ] 동일한 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정상적으로 차감되어야 한다
- [ ] 선착순 쿠폰 발급 시 totalQuantity를 초과하여 발급되지 않아야 한다

### 어드민 API

- [ ] 쿠폰 템플릿 CRUD가 정상 동작한다
- [ ] 쿠폰 템플릿 삭제 시 이미 발급된 쿠폰은 상태가 유지된다
- [ ] 특정 쿠폰의 발급 내역을 페이징으로 조회할 수 있다
