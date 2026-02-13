# ERD

**왜 필요한가**: Soft Delete 정책과 주문 스냅샷 구조를 검증하기 위함입니다. 특히 `deletedAt` 필드 위치와 OrderItem의 스냅샷 컬럼이 핵심입니다.

```mermaid
erDiagram
    User {
        bigint id PK "BaseEntity"
        string login_id UK
        string password "암호화"
        string name
        string birth "YYYY-MM-DD"
        string email
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    Admin {
        bigint id PK "BaseEntity"
        string login_id UK
        string password
        string name
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    Brand {
        bigint id PK "BaseEntity"
        string name
        string description
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    Product {
        bigint id PK "BaseEntity"
        bigint brand_id
        string name
        string description
        bigint price
        int stock_quantity
        int like_count "비정규화"
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    ProductLike {
        bigint id PK "BaseEntity"
        bigint user_id
        bigint product_id
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    Order {
        bigint id PK "BaseEntity"
        bigint user_id
        bigint total_price
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    OrderItem {
        bigint id PK "BaseEntity"
        bigint order_id
        bigint product_id "참조용, 삭제되어도 유지"
        string product_name "스냅샷"
        bigint product_price "스냅샷"
        int quantity
        datetime created_at "BaseEntity"
        datetime updated_at "BaseEntity"
        datetime deleted_at "BaseEntity, nullable"
    }

    Brand ||--o{ Product : "has"
    Product ||--o{ ProductLike : "has"
    User ||--o{ ProductLike : "likes"
    User ||--o{ Order : "places"
    Order ||--|{ OrderItem : "contains"
    Product ||--o{ OrderItem : "referenced by"
```

**해석 포인트**:
- 모든 엔티티가 `BaseEntity`를 상속 → `id`, `created_at`, `updated_at`, `deleted_at` 공통 제공
- `deleted_at`은 모든 테이블에 존재하지만, 실제 Soft Delete 활용은 Brand, Product에서 주로 사용
- `User`는 기존 구현된 도메인 구조 반영 → `login_id`, `birth` 필드 포함, 비밀번호 암호화
- `OrderItem`은 `product_id`를 참조하되, `product_name`과 `product_price`를 스냅샷으로 저장
