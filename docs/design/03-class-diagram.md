# 클래스 다이어그램 (도메인 계층)

**왜 필요한가**: 도메인 간 의존 방향과 책임 경계를 확인하기 위함입니다. 특히 Order가 Product를 직접 참조하지 않고 스냅샷만 가지는 구조가 핵심입니다.

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        #Long id
        #ZonedDateTime createdAt
        #ZonedDateTime updatedAt
        #ZonedDateTime deletedAt
        +delete()
        +restore()
        #guard()
    }

    class User {
        -String loginId
        -String password
        -String name
        -String birth
        -String email
        +matchPassword(password) Boolean
        +changePassword(newPassword)
    }

    class Admin {
        -String loginId
        -String password
        -String name
        +matchPassword(password) Boolean
    }

    class Brand {
        -String name
        -String description
    }

    class Product {
        -Brand brand
        -String name
        -String description
        -Long price
        -Int stockQuantity
        -Int likeCount
        +decreaseStock(quantity)
        +increaseLikeCount()
        +decreaseLikeCount()
    }

    class ProductLike {
        -Long userId
        -Long productId
    }

    class Order {
        -Long userId
        -List~OrderItem~ items
        -Long totalPrice
        +calculateTotalPrice()
    }

    class OrderItem {
        -Long productId
        -String productName
        -Long productPrice
        -Int quantity
    }

    BaseEntity <|-- User
    BaseEntity <|-- Admin
    BaseEntity <|-- Brand
    BaseEntity <|-- Product
    BaseEntity <|-- ProductLike
    BaseEntity <|-- Order
    BaseEntity <|-- OrderItem

    Brand "1" --> "*" Product : contains
    Product "1" --> "*" ProductLike : has
    User "1" --> "*" ProductLike : likes
    User "1" --> "*" Order : places
    Order "1" *-- "*" OrderItem : contains
    OrderItem ..> Product : references (weak)
```

**해석 포인트**:
- 모든 엔티티가 `BaseEntity`를 상속 → `id`, `createdAt`, `updatedAt`, `deletedAt`, `delete()`, `restore()` 공통 제공
- `Order` → `OrderItem`은 Composition (Order 삭제 시 함께 삭제)
- `OrderItem` → `Product`는 점선(약한 참조) → productId만 보관, 실제 객체 참조 아님
- `Product.decreaseStock()`은 재고 차감 책임을 가짐 → 재고 부족 시 예외 발생
