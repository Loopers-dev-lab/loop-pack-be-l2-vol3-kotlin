# Architecture Overview — Order x Product Aggregate 간 협력 구조

## Aggregate 구성

```mermaid
graph LR
    subgraph Order Aggregate
        Order[Order - Aggregate Root]
        OrderItem[OrderItem]
        OrderStatus[OrderStatus]
    end

    subgraph Product Aggregate
        Product[Product - Aggregate Root]
        Stock[Stock]
        ProductName[ProductName]
        ProductPrice[ProductPrice]
    end

    Order --- OrderItem
    Order --- OrderStatus
    Product --- Stock
    Product --- ProductName
    Product --- ProductPrice
```

## Application Layer 구조

```mermaid
graph TD
    subgraph Application Layer
        OrderFacade[OrderFacade — 흐름 표시, @Transactional]
        OrderService[OrderService — Aggregate 간 협력 조율]
    end

    OrderFacade --> OrderService
    OrderFacade --> OrderReader
```

- **Facade**: 트랜잭션 경계, 도메인 흐름만 보여줌
- **Service**: 여러 Aggregate의 도메인 서비스를 조합하여 협력 로직 구현

## 레이어별 클래스와 책임

```mermaid
graph TD
    subgraph Application
        OrderFacade[OrderFacade — 흐름 표시]
        OrderService[OrderService — Aggregate 간 협력]
    end

    subgraph Domain - Order
        OrderRegister[OrderRegister — 주문 생성]
        OrderReader[OrderReader — 주문 조회]
        OrderCanceller[OrderCanceller — 주문 취소]
        OrderRepository[OrderRepository — Port]
    end

    subgraph Domain - Product
        ProductStockDeductor[ProductStockDeductor — 재고 차감/복원]
        ProductReader[ProductReader — 상품 조회]
        ProductRepository[ProductRepository — Port]
    end

    OrderFacade --> OrderService
    OrderFacade --> OrderReader

    OrderService --> OrderRegister
    OrderService --> OrderCanceller
    OrderService --> ProductStockDeductor

    OrderRegister --> OrderRepository
    OrderReader --> OrderRepository
    OrderCanceller --> OrderReader
    OrderCanceller --> OrderRepository

    ProductStockDeductor --> ProductReader
    ProductStockDeductor --> ProductRepository
```

## Aggregate 간 협력 (Application Layer에서 조율)

```mermaid
graph LR
    OrderService -- 재고 차감 후 주문 생성 --> OrderRegister
    OrderService -- 재고 차감 --> ProductStockDeductor
    OrderService -- 주문 취소 후 재고 복원 --> OrderCanceller
    OrderService -- 재고 복원 --> ProductStockDeductor
```

Domain 서비스 간 직접 호출 없이, **Application Layer의 OrderService가 Aggregate 경계를 넘는 협력을 조율**한다.

## 전체 구조 (Order x Product)

```mermaid
graph TD
    %% Application Layer
    subgraph Application Layer
        OrderFacade[OrderFacade — 흐름 표시, @Transactional]
        OrderService[OrderService — Aggregate 간 협력]
    end

    %% Order Aggregate
    subgraph Order Aggregate
        OrderRegister[OrderRegister — 주문 생성]
        OrderReader[OrderReader — 주문 조회]
        OrderCanceller[OrderCanceller — 주문 취소]
        OrderRepository[OrderRepository — Port]
        Order[Order]
        OrderItem[OrderItem]
    end

    %% Product Aggregate
    subgraph Product Aggregate
        ProductStockDeductor[ProductStockDeductor — 재고 차감/복원]
        ProductReader[ProductReader — 상품 조회]
        ProductRepository[ProductRepository — Port]
        Product[Product]
        Stock[Stock]
    end

    %% Application 내부
    OrderFacade --> OrderService
    OrderFacade --> OrderReader

    %% Service → Domain (Aggregate 간 협력)
    OrderService --> OrderRegister
    OrderService --> OrderCanceller
    OrderService --> ProductStockDeductor

    %% Order 내부
    OrderRegister --> OrderRepository
    OrderReader --> OrderRepository
    OrderCanceller --> OrderReader
    OrderCanceller --> OrderRepository
    OrderRepository -.-> Order
    Order --- OrderItem

    %% Product 내부
    ProductStockDeductor --> ProductReader
    ProductStockDeductor --> ProductRepository
    ProductRepository -.-> Product
    Product --- Stock

    linkStyle 4 stroke:blue,stroke-width:2px
    linkStyle 5 stroke:blue,stroke-width:2px
```

파란 선이 **OrderService가 Aggregate 경계를 넘어 조율하는 호출**이다.

## 설계 결정

### Before: Domain 서비스가 다른 Aggregate 직접 호출

```
OrderRegister (Domain) ──재고 차감──▶ ProductStockDeductor (Domain) ❌
OrderCanceller (Domain) ──재고 복원──▶ ProductStockDeductor (Domain) ❌
```

- Domain 서비스가 다른 Aggregate의 도메인 서비스를 직접 의존
- Aggregate 경계가 무너짐

### After: Application Service가 협력 조율

```
OrderService (Application) ──재고 차감──▶ ProductStockDeductor (Domain) ✅
OrderService (Application) ──주문 생성──▶ OrderRegister (Domain)         ✅
```

- Domain 서비스는 자기 Aggregate 안에서만 동작
- Aggregate 간 협력은 Application Layer(OrderService)에서 조율
- Facade는 OrderService를 호출하는 흐름만 표시
