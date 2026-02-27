---
name: architecture
description: 아키텍처 설계 및 패키지 구성 시 아래 전략을 따르세요.
---

아키텍처 설계 및 패키지 구성 시 아래 전략을 따르세요.

## 레이어드 아키텍처 + DIP

의존 방향: **Application → Domain ← Infrastructure**

도메인 계층이 중심이며, 모든 의존 방향은 도메인을 향한다.

## 각 레이어의 책임

### Interfaces Layer (`/interfaces/api/{domain}/`)
- 사용자와 직접 연결되는 Web/Controller
- Application Layer의 유스케이스 호출 책임만 가진다.
- 요청 객체 검증, 응답 객체 매핑을 수행한다.
- API request/response DTO가 위치한다.

### Application Layer (`/application/{domain}/`)
- 비즈니스 기능의 흐름을 조율하여 유스케이스를 완성한다.
- 실질적인 비즈니스 로직은 최대한 도메인으로 위임한다.
- 도메인 객체를 조합하는 orchestration 역할이다.
- Application 전용 DTO(Info)가 위치한다. (API DTO와 분리)

### Domain Layer (`/domain/{domain}/`)
- 비즈니스의 중심이며, 다른 계층에 의존하지 않는다.
- Entity, Value Object, Domain Service가 위치한다.
- Repository 인터페이스가 위치한다. (구현체가 아닌 계약)
- Command 등 도메인 전달 객체가 위치한다.

### Infrastructure Layer (`/infrastructure/{domain}/`)
- 도메인 계층이 원하는 기능의 구현체를 제공한다.
- JPA, Redis, Kafka 등 구체적인 외부 기술에 의존한다.
- Repository 구현체, JpaRepository가 위치한다.

## DIP (의존성 역전 원칙)

```kotlin
// Domain Layer - 인터페이스 정의
interface OrderRepository {
    fun save(order: Order): Order
}

// Infrastructure Layer - 구현체 제공
@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun save(order: Order): Order = orderJpaRepository.save(order)
}
```

- Repository Interface는 Domain Layer에 정의하고, 구현체는 Infrastructure에 위치시킨다.
- 테스트 시 Fake/Stub/InMemory 구현체를 활용하여 독립적인 테스트가 가능하도록 한다.

## 패키지 구성 전략

계층 패키지 하위에 도메인별로 패키징한다.

```
com.loopers/
├── interfaces/api/
│   ├── product/    → ProductV1Controller, ProductV1Dto
│   ├── order/      → OrderV1Controller, OrderV1Dto
│   └── like/       → LikeV1Controller, LikeV1Dto
├── application/
│   ├── product/    → ProductService, ProductFacade (2+ 서비스 조합 시만), ProductInfo
│   ├── order/      → OrderService, OrderFacade (2+ 서비스 조합 시만), OrderInfo
│   └── like/       → LikeService, LikeFacade (2+ 서비스 조합 시만)
├── domain/
│   ├── product/    → Product, ProductRepository, ProductCommand
│   ├── order/      → Order, OrderItem, OrderRepository
│   ├── like/       → Like, LikeRepository
│   └── brand/      → Brand, BrandRepository
└── infrastructure/
    ├── product/    → ProductRepositoryImpl, ProductJpaRepository
    ├── order/      → OrderRepositoryImpl, OrderJpaRepository
    ├── like/       → LikeRepositoryImpl, LikeJpaRepository
    └── brand/      → BrandRepositoryImpl, BrandJpaRepository
```
