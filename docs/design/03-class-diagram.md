# 도메인 객체 설계 (클래스 다이어그램)

> **문서 목적**: 도메인 객체의 **책임(메서드)**과 객체 간 **의존 방향**, **응집도**를 시각화합니다.
>
> **독자**: 개발자, 코드 리뷰어
>
> **이 문서로 확인할 수 있는 것**: 각 객체가 무엇을 할 수 있는가, 객체 간 결합도가 낮은가, 관련 없는 책임이 섞여 있지 않은가
>
> **관련 문서**: 데이터 저장 구조(테이블, 컬럼, 인덱스)는 [04-erd.md](./04-erd.md)에서 확인할 수 있습니다.

---

## 1. 클래스 다이어그램

### 배경

ERD만으로는 "이 객체가 무엇을 **할 수 있는가**"를 알 수 없습니다. 클래스 다이어그램은 각 도메인 객체의 **책임(메서드)**과 객체 간 **의존 방향**, **응집도**를 확인하기 위해 그렸습니다.

**이 다이어그램에서 확인할 것:**
1. 비즈니스 행위(재고 차감, 인기도 증감, 스냅샷 생성)가 어떤 객체에 있는가 (도메인 책임)
2. 객체 간 참조가 ID로만 이루어져 결합도가 낮은가 (의존 방향)
3. 하나의 객체에 관련 없는 책임이 섞여 있지 않은가 (응집도)

### 다이어그램

```mermaid
classDiagram
    class Brand {
        -String name
        -String description
        -String logoUrl
        -LocalDateTime deletedAt
        +create(name, description, logoUrl) Brand
        +update(name, description, logoUrl)
        +delete()
        +isDeleted() Boolean
    }

    class Product {
        -Long brandId
        -String name
        -String description
        -Money price
        -Stock stock
        -String imageUrl
        -Long likeCount
        -LocalDateTime deletedAt
        +create(brandId, name, ...) Product
        +update(name, description, price, stock, imageUrl)
        +delete()
        +isDeleted() Boolean
        +isAvailable() Boolean
        +decreaseStock(quantity)
        +increaseLikeCount()
        +decreaseLikeCount()
    }

    class Money {
        <<Value Object>>
        -Long amount
        +Money(amount)
    }

    class Stock {
        <<Value Object>>
        -Int quantity
        +Stock(quantity)
        +deduct(amount) Stock
    }

    class Quantity {
        <<Value Object>>
        -Int value
        +Quantity(value)
    }

    class Like {
        -Long userId
        -Long productId
        +create(userId, productId) Like
    }

    class Order {
        -Long userId
        -Money totalAmount
        -OrderStatus status
        +create(userId, totalAmount) Order
    }

    class OrderItem {
        -Long orderId
        -Long productId
        -String productName
        -Money productPrice
        -String brandName
        -String imageUrl
        -Quantity quantity
        +create(orderId, productId, productName, productPrice, brandName, imageUrl, quantity) OrderItem
    }

    class OrderStatus {
        <<enumeration>>
        ORDERED
    }

    %% User는 Round 1에서 정의된 도메인 객체입니다 (이 다이어그램에서는 관계만 표시)
    %% 화살표 방향 = 참조(ID 보유) 방향: Product가 brandId를 가지므로 Product → Brand
    Product "*" --> "1" Brand : brandId
    Product --> Money : price
    Product --> Stock : stock
    Like "*" --> "1" Product : productId
    Like "*" --> "1" User : userId
    Order "*" --> "1" User : userId
    Order --> Money : totalAmount
    Order --> OrderStatus : 상태
    OrderItem "*" --> "1" Order : orderId
    OrderItem --> Money : productPrice
    OrderItem --> Quantity : quantity
    OrderItem "*" ..> "1" Product : productId (스냅샷)
```

### 핵심 포인트

**Value Object — "도메인 규칙을 값에 내장한다":**

- **Money**: 금액을 나타내는 VO입니다. `amount >= 0` 불변식을 생성 시점에 보장합니다. Product의 가격, Order의 총액, OrderItem의 스냅샷 단가에 사용됩니다. 원 단위 정수(`Long`)로 저장하여 부동소수점 오류를 방지합니다. (→ 정책 29)
- **Stock**: 재고 수량을 나타내는 VO입니다. `quantity >= 0` 불변식을 보장하고, `deduct(amount)` 메서드로 차감 시 음수 방어를 VO 내부에서 수행합니다. (→ 정책 1, 2)
- **Quantity**: 주문 수량을 나타내는 VO입니다. `value >= 1` 불변식을 보장합니다. 0개 주문은 의미가 없으므로 1 이상만 허용합니다.

**도메인 책임 — "이 객체가 무엇을 할 수 있는가":**

- **Product.decreaseStock(quantity)**: 재고 차감은 Service가 아니라 Product 엔티티가 스스로 수행합니다. 내부적으로 `Stock.deduct()`를 호출하여 "재고가 충분한가?"라는 불변식을 검증합니다.
- **Product.isAvailable()**: 고객에게 "구매 가능 / 품절"을 표시하기 위한 메서드입니다. `stock > 0`이면 구매 가능, 아니면 품절입니다. `isDeleted()`와는 별개의 판단입니다 — 삭제된 상품은 조회 자체가 안 되므로, 이 메서드는 활성 상품 내에서의 구매 가능 여부만 판단합니다. (→ 정책 22)
- **Product.increaseLikeCount() / decreaseLikeCount()**: 인기도 증감은 Product의 책임입니다. 도메인 모델에서 음수 방어를 1차 수행하고, 실제 영속화 시에는 동시성과 원자성을 위해 DB 레벨 원자적 UPDATE(`like_count + 1`, `GREATEST(like_count - 1, 0)`)로 처리합니다(2차 안전장치). (→ 정책 15)
- **Order.create(userId, totalAmount)**: 주문 엔티티는 자신의 데이터(userId, totalAmount, status)만 책임집니다. totalAmount는 Facade에서 상품 가격 × 수량으로 계산하여 전달합니다. 주문 금액은 서버에서 계산하고, 클라이언트가 보내는 금액은 신뢰하지 않습니다. (→ 정책 28)
- **OrderItem.create(orderId, ...)**: 스냅샷 데이터를 받아 OrderItem을 생성합니다. Product/Brand의 현재 정보를 복사하는 것은 Facade의 책임이고, OrderItem은 전달받은 스냅샷 값을 보관하는 책임만 가집니다.

**의존 방향 — "모든 엔티티가 ID로만 연결된다":**

- **Product → brandId(Long)**: Product는 Brand 엔티티를 직접 참조하지 않고 brandId만 보유합니다. 목록 조회 시 brandName이 필요하면 인프라 레이어에서 JOIN으로 해결합니다.
- **Like → userId, productId**: Like도 User/Product를 직접 참조하지 않습니다. 도메인 간 결합도를 최소화합니다.
- **OrderItem → orderId**: OrderItem도 Order 엔티티를 직접 참조하지 않고 orderId만 보유합니다. 다른 도메인과 동일한 ID 참조 패턴을 따릅니다. 주문 상세 조회 시 `OrderItemRepository.findByOrderId(orderId)`로 별도 조회합니다.

**관계 — 선의 종류에 주목:**

| 선 | 의미 | 예시 |
|----|------|------|
| 실선 화살표 (`-->`) | ID 참조 | Product → Brand (brandId로 참조), OrderItem → Order (orderId로 참조) |
| 점선 (`..>`) | 느슨한 참조 (스냅샷) | OrderItem ..> Product (productId는 추적용, 스냅샷은 독립) |

**설계 결정:**

- **Order가 OrderItem을 직접 보유하지 않는 이유**: 프로젝트의 모든 엔티티는 다른 엔티티를 직접 참조하지 않고 ID만 보유합니다(ID 참조 원칙). Order-OrderItem도 이 원칙을 따릅니다. `@OneToMany` 양방향 매핑은 N+1 문제, cascade 사이드이펙트, 테스트 복잡도를 높이므로 사용하지 않습니다. OrderItem은 `orderId`를 통해 소속 주문을 식별하며, 주문 상세 조회 시 별도로 조회합니다.

- **주문 금액 계산이 Facade에 위치하는 이유**: Order가 OrderItem 목록을 들고 있지 않으므로, 총 금액 계산은 Facade에서 수행합니다. Facade가 상품 가격과 수량을 기반으로 totalAmount를 계산하고, 계산된 값을 Order.create()에 전달합니다.

- **OrderItem이 Product를 직접 참조하지 않고 스냅샷을 저장하는 이유**: 주문 후 상품의 이름, 가격, 브랜드가 변경되거나 삭제되더라도 주문 내역은 "주문 당시의 정보"를 보여줘야 합니다. OrderItem → Product는 점선(추적용)이며, OrderItem은 자체적으로 productName, productPrice, brandName을 보유합니다. (→ 정책 3)

- **likeCount가 Product에 있는 이유**: 좋아요 수 기반 정렬(인기순)을 위해, 매번 Like 테이블을 COUNT하는 대신 Product에 likeCount를 직접 관리합니다(반정규화). 동시성 이슈는 이후 고도화에서 해결합니다. (→ 정책 15)

- **Like에 deletedAt이 없는 이유**: 좋아요 취소 시 물리 삭제(hard delete)합니다. 법적 보존 의무가 없고, 등록/취소가 빈번하여 soft delete로 레코드를 쌓으면 데이터가 빠르게 증가합니다. (→ 정책 12)

- **OrderStatus가 현재 ORDERED 하나인 이유**: 결제 기능이 없으므로 주문 생성 = 주문 확정입니다. 이후 결제 연동 시 PAID, CANCELLED 등으로 확장할 수 있도록 enum으로 설계해두었습니다. COMPLETED가 아닌 ORDERED로 명명한 이유는, COMPLETED는 "배송 완료"까지 연상시키기 때문입니다. 현재 상태는 "주문이 접수됨"을 의미합니다.

**이 다이어그램에 포함하지 않은 것:**
- `id`, `createdAt`, `updatedAt`: BaseEntity를 상속하는 엔티티의 공통 인프라 필드입니다. 도메인 행위와 무관합니다.
- `deletedAt`: Brand, Product에만 표기했습니다. soft delete 여부가 비즈니스 로직(`isDeleted()`)에 영향을 주기 때문입니다.
- **BaseEntity**: `id`, `createdAt`, `updatedAt`만 제공합니다. `deletedAt`은 BaseEntity에 포함하지 않고, soft delete가 필요한 엔티티(Brand, Product)가 **직접 선언**합니다. 삭제의 맥락은 도메인마다 다르기 때문입니다(상품 판매 중지 vs 회원 탈퇴 vs 주문 취소). 공통 베이스로 묶으면 도메인별로 다른 "삭제 이후의 비즈니스 로직"을 처리하기 어려워집니다.
- Like, OrderItem은 **BaseEntity를 상속하지 않습니다.** Like는 물리 삭제 대상이므로 `updated_at`, `deleted_at`이 불필요하고, OrderItem은 불변 스냅샷이므로 독자적인 시점 관리가 불필요합니다. 각각 필요한 필드(`id`, `created_at` 등)만 직접 정의합니다.
- User: Round 1에서 이미 설계/구현된 도메인 객체입니다. 관계선에서만 참조하고, 이 다이어그램에서 클래스 정의는 생략했습니다.
- Repository, Service: 클래스 다이어그램은 도메인 모델만 다룹니다. 호출 흐름은 시퀀스 다이어그램에서 확인할 수 있습니다.

---

## 2. 관계 요약

| 관계 | 설명 | 비고 |
|------|------|------|
| Brand 1 : N Product | 하나의 브랜드에 여러 상품이 속합니다 | 브랜드 삭제 시 연쇄 soft delete |
| Product 1 : N Like | 하나의 상품에 여러 좋아요가 가능합니다 | 취소 시 hard delete |
| User 1 : N Like | 한 사용자가 여러 상품에 좋아요할 수 있습니다 | UNIQUE(user_id, product_id) |
| User 1 : N Order | 한 사용자가 여러 주문을 할 수 있습니다 | 타 유저 주문 접근 불가 (→ 정책 20) |
| Order 1 : N OrderItem | 하나의 주문에 여러 항목이 포함됩니다 | OrderItem이 orderId로 참조 (ID 참조) |
| Product 1 : N OrderItem | 참조용입니다 (스냅샷과 별개) | product_id는 추적용, 논리적 FK 아님 |
