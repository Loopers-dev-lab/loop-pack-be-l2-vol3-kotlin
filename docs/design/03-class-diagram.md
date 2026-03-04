# 클래스 다이어그램

> 목적: 레이어 간 의존 방향, 도메인 클래스 구조, 클래스 간 관계 확인

---

## 목차

1. [레이어 아키텍처 전체 구조](#1-레이어-아키텍처-전체-구조)
2. [도메인 클래스 다이어그램](#2-도메인-클래스-다이어그램)

---

## 1. 레이어 아키텍처 전체 구조

### 다이어그램 목적
- 4개 레이어의 역할과 단방향 의존 확인
- 각 레이어가 무엇을 알고, 무엇을 모르는지 명확화

```mermaid
flowchart TB
    subgraph Interfaces["interfaces (API 계층)"]
        direction LR
        C1[UserController]
        C2[BrandController]
        C3[ProductController]
        C4[LikeController]
        C5[OrderController]
        C6[CouponAdminController]
        C7[CouponController]
    end

    subgraph Application["application (조합 계층)"]
        direction LR
        F1[UserFacade]
        F2[BrandFacade]
        F3[ProductFacade]
        F4[LikeFacade]
        F5[OrderFacade]
        F6[CouponService]
        F7[IssuedCouponService]
    end

    subgraph Domain["domain (비즈니스 계층)"]
        direction LR
        S1[UserService]
        S2[BrandService]
        S3[ProductService]
        S4[LikeService]
        S5[OrderService]
    end

    subgraph Infrastructure["infrastructure (영속성 계층)"]
        direction LR
        R1[UserRepository]
        R2[BrandRepository]
        R3[ProductRepository]
        R4[LikeRepository]
        R5[OrderRepository]
        R6[CouponRepository]
        R7[IssuedCouponRepository]
    end

    C1 --> F1
    C2 --> F2
    C3 --> F3
    C4 --> F4
    C5 --> F5
    C6 --> F6
    C6 --> F7
    C7 --> F7

    F1 --> S1
    F2 --> S2
    F2 -.-> S3
    F3 --> S3
    F3 -.-> S2
    F4 --> S4
    F4 -.-> S3
    F5 --> S5
    F5 -.-> S3
    F5 -.-> F6
    F5 -.-> F7

    S1 --> R1
    S2 --> R2
    S3 --> R3
    S4 --> R4
    S5 --> R5
    F6 --> R6
    F7 --> R7
```

### 핵심 포인트

| 레이어 | 역할 | 알아야 하는 것 | 몰라야 하는 것 |
|--------|------|---------------|---------------|
| **Controller** | 요청/응답 처리, DTO 변환 | Facade | Service, Repository, Entity |
| **Facade** | 도메인 간 조합, 트랜잭션 경계 | 여러 Service | Repository, DB |
| **Service** | 단일 도메인 로직 | 자기 Repository, Entity | 다른 Service |
| **Repository** | 영속성 처리 | Entity, DB | 비즈니스 로직 |

### 의존 방향 규칙

```
Controller → Facade → Service → Repository
     ↓           ↓         ↓          ↓
   (DTO)    (여러 Service) (Entity)   (DB)

※ 화살표 반대 방향 의존 금지
※ 같은 레이어 내 의존 금지 (Service → Service 금지)
```

---

## 2. 도메인 클래스 다이어그램

### 다이어그램 목적
- 도메인 Entity 간 관계와 책임 확인
- Soft Delete, 스냅샷 등 도메인 행위 명시

### 2.1 User

```mermaid
classDiagram
    class User {
        -id: Long
        -loginId: String
        -password: String
        -name: String
        -birthDate: LocalDate
        -email: String
        -createdAt: LocalDateTime
        -updatedAt: LocalDateTime
        +maskName() String
    }
```

| 메서드 | 책임 |
|--------|------|
| `maskName()` | 이름 마스킹 (홍길동 → 홍길*) |

---

### 2.2 Brand

```mermaid
classDiagram
    class Brand {
        -id: Long
        -name: String
        -description: String
        -createdAt: LocalDateTime
        -updatedAt: LocalDateTime
        -deletedAt: LocalDateTime
        +softDelete() void
        +isDeleted() boolean
    }

    class Product {
        -brandId: Long
    }

    Brand "1" <-- "*" Product : brandId
```

| 메서드 | 책임 |
|--------|------|
| `softDelete()` | deletedAt 설정 |
| `isDeleted()` | 삭제 여부 확인 |

---

### 2.3 Product

```mermaid
classDiagram
    class Product {
        -id: Long
        -brandId: Long
        -name: String
        -price: BigDecimal
        -stock: Int
        -description: String
        -imageUrl: String
        -createdAt: LocalDateTime
        -updatedAt: LocalDateTime
        -deletedAt: LocalDateTime
        +softDelete() void
        +isDeleted() boolean
        +decreaseStock(quantity) void
        +hasEnoughStock(quantity) boolean
    }
```

| 메서드 | 책임 |
|--------|------|
| `decreaseStock()` | 재고 차감 (stock >= 0 보장) |
| `hasEnoughStock()` | 재고 충분 여부 확인 |
| `softDelete()` | deletedAt 설정 |

---

### 2.4 Like

```mermaid
classDiagram
    class Like {
        -id: Long
        -userId: Long
        -productId: Long
        -createdAt: LocalDateTime
        -deletedAt: LocalDateTime
        +softDelete() void
        +restore() void
        +isDeleted() boolean
    }

    class User {
        -id: Long
    }

    class Product {
        -id: Long
    }

    User "1" <-- "*" Like : userId
    Product "1" <-- "*" Like : productId
```

| 메서드 | 책임 |
|--------|------|
| `softDelete()` | deletedAt 설정 |
| `restore()` | deletedAt = null (멱등성 복원) |

---

### 2.5 Order / OrderItem

```mermaid
classDiagram
    class Order {
        -id: Long
        -userId: Long
        -couponId: Long?
        -originalAmount: BigDecimal
        -discountAmount: BigDecimal
        -totalAmount: BigDecimal
        -createdAt: LocalDateTime
        +calculateTotalAmount() BigDecimal
    }

    class OrderItem {
        -id: Long
        -orderId: Long
        -productId: Long
        -quantity: Int
        -unitPrice: BigDecimal
        -productName: String
        -brandName: String
        +subtotal() BigDecimal
    }

    class User {
        -id: Long
    }

    class Product {
        -id: Long
    }

    class IssuedCoupon {
        -id: Long
    }

    User "1" <-- "*" Order : userId
    Order "1" *-- "*" OrderItem : contains
    Product "1" <.. "*" OrderItem : snapshot
    IssuedCoupon "0..1" <.. "0..*" Order : couponId (optional)
```

| 클래스 | 필드 | 설명 |
|--------|------|------|
| **Order** | couponId | 사용된 발급 쿠폰 ID (nullable) |
| **Order** | originalAmount | 쿠폰 적용 전 원래 금액 |
| **Order** | discountAmount | 쿠폰 할인 금액 (기본 0) |
| **Order** | totalAmount | 최종 결제 금액 (originalAmount - discountAmount) |
| **OrderItem** | unitPrice, productName, brandName | 주문 시점 스냅샷 |

| 메서드 | 책임 |
|--------|------|
| `calculateTotalAmount()` | 주문 총액 계산 |
| `subtotal()` | 항목별 소계 (unitPrice × quantity) |

---

### 2.6 Coupon

```mermaid
classDiagram
    class Coupon {
        -id: Long
        -name: String
        -type: CouponType
        -value: BigDecimal
        -minOrderAmount: BigDecimal?
        -expiredAt: ZonedDateTime
        -createdAt: LocalDateTime
        -updatedAt: LocalDateTime
        -deletedAt: LocalDateTime
        +calculateDiscount(orderAmount) BigDecimal
        +isExpired() boolean
        +validateMinOrderAmount(orderAmount) void
        +update(name, value, minOrderAmount, expiredAt) void
        +softDelete() void
        +isDeleted() boolean
    }

    class CouponType {
        <<enumeration>>
        FIXED
        RATE
    }

    Coupon --> CouponType : type
```

| 메서드 | 책임 |
|--------|------|
| `calculateDiscount()` | 할인 금액 계산 (FIXED: value, RATE: orderAmount × value/100) |
| `isExpired()` | 만료 여부 확인 |
| `validateMinOrderAmount()` | 최소 주문 금액 검증 |
| `update()` | 쿠폰 템플릿 수정 |
| `softDelete()` | deletedAt 설정 |

---

### 2.7 IssuedCoupon

```mermaid
classDiagram
    class IssuedCoupon {
        -id: Long
        -couponId: Long
        -userId: Long
        -status: IssuedCouponStatus
        -usedAt: ZonedDateTime?
        -createdAt: ZonedDateTime
        -updatedAt: ZonedDateTime
        +use() void
        +isUsable() boolean
        +validateUsable() void
        +validateOwner(userId) void
    }

    class IssuedCouponStatus {
        <<enumeration>>
        AVAILABLE
        USED
        EXPIRED
    }

    class Coupon {
        -id: Long
    }

    class User {
        -id: Long
    }

    IssuedCoupon --> IssuedCouponStatus : status
    Coupon "1" <-- "*" IssuedCoupon : couponId
    User "1" <-- "*" IssuedCoupon : userId
```

| 메서드 | 책임 |
|--------|------|
| `use()` | 쿠폰 사용 처리 (AVAILABLE → USED, usedAt 설정) |
| `isUsable()` | 사용 가능 여부 확인 |
| `validateUsable()` | 사용 불가 시 예외 |
| `validateOwner()` | 쿠폰 소유자 검증 (타인 쿠폰 → FORBIDDEN) |

**설계 특이사항**: `IssuedCoupon`은 `BaseEntity`를 상속하지 않고 독자 구현. Soft Delete 대상이 아니며 (사용된 쿠폰도 기록 보존), `status` 필드로 생명주기를 관리함.

### 관계 표기 설명
- `<--` : ID 참조 (느슨한 결합)
- `*--` : 컴포지션 (생명주기 동일, Aggregate)
- `<..` : 점선 = 스냅샷 참조 (실시간 참조 아님)

---

## 설계 원칙 요약

| 원칙 | 적용 |
|------|------|
| **Soft Delete** | Entity 메서드로 캡슐화 (`softDelete()`, `isDeleted()`) |
| **도메인 불변식** | Entity 내부에서 보장 (`decreaseStock()` → stock >= 0, `use()` → AVAILABLE 상태만) |
| **ID 참조** | 도메인 간 엔티티는 ID로만 참조 |
| **스냅샷** | OrderItem에 주문 시점 상품 정보 복사 |
| **느슨한 결합** | OrderFacade → ProductService (Facade에서 조합, Service 간 직접 참조 없음) |
| **독자 엔티티** | IssuedCoupon은 BaseEntity 미상속, status로 생명주기 관리 |
