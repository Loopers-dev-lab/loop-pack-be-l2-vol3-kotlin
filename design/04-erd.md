# 커머스 API - ERD

---

## 📊 ERD (Mermaid)

```mermaid
erDiagram
  USER ||--o{ ORDER : ""
  USER ||--o{ PRODUCT_LIKE : ""
  PRODUCT ||--o{ PRODUCT_LIKE : ""
  BRAND ||--o{ PRODUCT : ""
  PRODUCT ||--o{ ORDER_ITEM : ""
  ORDER ||--o{ ORDER_ITEM : ""

  USER {
    bigint id PK
    string login_id UK "unique, not null"
    string password "encrypted, not null"
    string name
    timestamp created_at
    timestamp updated_at
  }

  BRAND {
    bigint id PK
    string name UK "unique, not null"
    string description
    timestamp created_at
    timestamp updated_at
    timestamp deleted_at "null = active, not null = deleted (soft delete)"
  }

  PRODUCT {
    bigint id PK
    bigint brand_id FK "not null, indexed"
    string name "not null, unique per non-deleted"
    decimal price "not null, >= 0"
    int stock "not null, >= 0"
    string status "ACTIVE, OUT_OF_STOCK, INACTIVE"
    timestamp created_at
    timestamp updated_at
    timestamp deleted_at "null = active, not null = deleted (soft delete)"
  }

  ORDER {
    bigint id PK
    bigint user_id FK "not null, indexed"
    timestamp created_at
    timestamp updated_at
  }

  ORDER_ITEM {
    bigint id PK
    bigint order_id FK "not null, indexed"
    bigint product_id FK "not null (historical reference only)"
    int quantity "not null, > 0"
    decimal price "snapshot: product price at order time"
    string product_name "snapshot: product name at order time"
    timestamp created_at
  }

  PRODUCT_LIKE {
    bigint id PK
    bigint user_id FK "not null, indexed"
    bigint product_id FK "not null, indexed"
    timestamp created_at
    string unique_constraint "user_id + product_id"
  }
```

---

## 📋 테이블 상세 설명

### 1. BRAND 테이블
**설계 의도**:
- `deleted_at IS NULL` → 활성 브랜드
- `deleted_at IS NOT NULL` → 삭제된 브랜드

---

### 2. PRODUCT 테이블
**설계 의도**:
- `brand_id`: 상품은 하나의 브랜드에만 속함
- `stock`: 실시간으로 증감
- `status`: 노출 여부 제어 (활성/품절은 노출, 비활성은 미노출)

---

### 3. ORDER 테이블
**설계 의도**:
- `user_id`: 주문자, FK로 사용자와 연결
- `created_at`: 기간 범위 조회에 사용

---

### 4. ORDER_ITEM 테이블
**설계 의도**:
- `price`, `product_name`: 주문 당시의 실제 값
- 상품 정보가 변경되어도 주문 기록은 불변

---

### 5. PRODUCT_LIKE 테이블
**설계 의도**:
- `uk_user_product`: 사용자당 상품별 최대 1개의 좋아요만 허용

---

## 🔍 ERD 해석 포인트
| 항목 | 설계 선택 | 이유           |
|------|---------|--------------|
| **Soft Delete** | Brand, Product에만 적용 | 외부 참조 안전성    |
| **스냅샷** | OrderItem.price, productName | 주문 이력 불변성 보장 |

---