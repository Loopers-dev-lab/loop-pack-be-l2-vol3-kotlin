# ERD (Entity Relationship Diagram)

## 전체 데이터베이스 스키마

```mermaid
erDiagram
    %% 사용자 엔티티
    tb_user {
        BIGINT user_id PK
        VARCHAR(100) login_id UK "NOT NULL"
        VARCHAR(100) name "NOT NULL"
        VARCHAR(100) password "NOT NULL"
        VARCHAR(100) email UK "NOT NULL"
        DATE birth_date "NOT NULL"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
        DATETIME deleted_at
    }

    %% 좋아요 엔티티
    tb_like {
        BIGINT like_id PK
        BIGINT product_id "NOT NULL"
        BIGINT user_id "NOT NULL"
        %% UK(user_id, product_id) 복합 유니크
    }

    %% 브랜드 엔티티
    tb_brand {
        BIGINT brand_id PK
        VARCHAR(100) name UK "NOT NULL"
        TEXT description
        VARCHAR(255) logo_image_url
        VARCHAR(100) business_number UK
        VARCHAR(100) email
        VARCHAR(100) phone_number
        VARCHAR(10) zip_code "NOT NULL"
        VARCHAR(255) road_address "NOT NULL"
        VARCHAR(255) detail_address "NOT NULL"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
        DATETIME deleted_at
    }

    %% 상품 엔티티
    tb_product {
        BIGINT product_id PK
        BIGINT brand_id "NOT NULL"
        VARCHAR(100) name "NOT NULL"
        TEXT description
        BIGINT price "NOT NULL"
        BIGINT like_count "NOT NULL"
        VARCHAR(255) image_url
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
        DATETIME deleted_at
    }

    %% 상품 재고 엔티티
    tb_product_inventory {
        BIGINT product_id PK "NOT NULL"
        BIGINT stock "NOT NULL"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
        DATETIME deleted_at
    }

    %% 주문 엔티티
    tb_order {
        BIGINT order_id PK
        BIGINT user_id "NOT NULL"
        BIGINT total_amount "NOT NULL"
        VARCHAR(100) status "NOT NULL"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    %% 주문 상세 엔티티
    tb_order_item {
        BIGINT order_item_id PK
        BIGINT order_id "NOT NULL"
        BIGINT brand_id "NOT NULL"
        BIGINT product_id "NOT NULL"
        BIGINT quantity "NOT NULL"
        BIGINT unit_price "NOT NULL"
        VARCHAR(100) product_name "NOT NULL"
        VARCHAR(255) image_url
    }

    %% 관계 정의
    tb_brand ||--o{ tb_product : "소유"
    tb_product ||--|| tb_product_inventory : "재고 보유"
    direction LR
    tb_user ||--o{ tb_like : "좋아요 등록"
    tb_product ||--o{ tb_like : "좋아요 받음"
    tb_user ||--o{ tb_order : "주문 생성"
    tb_order ||--|{ tb_order_item : "포함"
    tb_product ||--o{ tb_order_item : "참조됨"
```

## 주요 설계 포인트

### 1. 소프트 삭제 (Soft Delete)
- **적용 테이블**: USER, BRAND, PRODUCT
- **구현**: `deletedAt` 타임스탬프 사용
- **목적**: 데이터 복구 가능성 및 이력 추적

### 2. 스냅샷
- **적용 테이블**: ORDER_ITEM
- **구현**: `unit_price`, `product_name`, `image_url`
- **목적**: 주문 시점의 상품 정보 보존 (상품 정보 변경에도 주문 내역 일관성 유지)

### 3. 재고 관리
- **PRODUCT_INVENTORY**: Product와 1:1 관계
- **productId가 PK**: 상품당 하나의 재고 레코드만 존재
- **트랜잭션 보장**: 주문 생성 시 재고 감소를 원자적으로 처리

### 4. 좋아요 시스템
- **복합 유니크 제약**: (userId, productId) 조합으로 중복 좋아요 방지
- **비정규화**: Product 테이블의 `likeCount`로 성능 최적화
- **이벤트 기반 동기화**: 좋아요 추가/삭제 시 Product의 likeCount 업데이트

### 5. 주문 시스템
- **ORDER와 ORDER_ITEM**: 1:N 관계 (Composition)
- **주문 상태**: status 필드로 주문 진행 상태 추적 (PENDING_PAYMENT, PAID, PREPARING, SHIPPED, DELIVERED, COMPLETED, CANCELLED, EXCHANGE_REQUESTED, EXCHANGING, EXCHANGED, RETURN_REQUESTED, RETURNING, RETURNED)
- **금액 계산**: totalAmount는 OrderItem들의 subtotal 합계
