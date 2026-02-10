# 클래스 다이어그램: Loopers E-Commerce

---

## 1. 전체 아키텍처 구조

### 목적
- 레이어드 아키텍처의 계층 분리 확인
- 의존 방향 검증 (상위 → 하위, Domain은 독립)

### 다이어그램

```mermaid
classDiagram
    direction TB

    namespace Interfaces {
        class UserV1Controller {
            -userService: UserService
            +signup(request): ApiResponse
            +getMyInfo(loginId, loginPw): ApiResponse
            +changePassword(loginId, loginPw, request): ApiResponse
        }
        class ProductV1Controller {
            -productService: ProductService
            +getProducts(brandId, sort, pageable): ApiResponse
            +getProduct(productId): ApiResponse
        }
        class OrderV1Controller {
            -orderService: OrderService
            +createOrder(loginId, loginPw, request): ApiResponse
            +getOrders(loginId, loginPw, startAt, endAt): ApiResponse
            +getOrder(loginId, loginPw, orderId): ApiResponse
        }
        class LikeV1Controller {
            -likeService: LikeService
            +addLike(loginId, loginPw, productId): ApiResponse
            +removeLike(loginId, loginPw, productId): ApiResponse
            +getMyLikes(loginId, loginPw, userId): ApiResponse
        }
    }

    namespace Domain {
        class UserService {
            -userRepository: UserRepository
            -passwordEncoder: PasswordEncoder
            +createUser(): UserModel
            +getUserByUserId(): UserModel
            +authenticate(): UserModel
            +changePassword(): void
        }
        class ProductService {
            -productRepository: ProductRepository
            -brandRepository: BrandRepository
            +getProducts(): Page~ProductModel~
            +getProduct(): ProductModel
            +decreaseStock(): void
        }
        class OrderService {
            -orderRepository: OrderRepository
            -productService: ProductService
            -userService: UserService
            +createOrder(): Order
            +getOrders(): List~Order~
            +getOrder(): Order
        }
        class LikeService {
            -likeRepository: LikeRepository
            -productRepository: ProductRepository
            +addLike(): Like
            +removeLike(): void
            +getLikesByUserId(): List~Like~
        }
        class BrandService {
            -brandRepository: BrandRepository
            -productService: ProductService
            +getBrand(): BrandModel
            +createBrand(): BrandModel
            +deleteBrand(): void
        }
    }

    namespace Domain_Model {
        class UserModel {
            -id: Long
            -userId: String
            -encryptedPassword: String
            -name: String
            -birthDate: LocalDate
            -email: String
            -createdAt: LocalDateTime
            +updatePassword(newPassword): void
            +getMaskedName(): String
        }
        class BrandModel {
            -id: Long
            -name: String
            -description: String
            -deletedAt: LocalDateTime
            -createdAt: LocalDateTime
        }
        class ProductModel {
            -id: Long
            -brandId: Long
            -name: String
            -price: BigDecimal
            -stock: Int
            -likeCount: Int
            -deletedAt: LocalDateTime
            -createdAt: LocalDateTime
            +decreaseStock(quantity): void
        }
        class Order {
            -id: Long
            -userId: Long
            -status: OrderStatus
            -totalAmount: BigDecimal
            -items: List~OrderItem~
            -createdAt: LocalDateTime
        }
        class OrderItem {
            -id: Long
            -orderId: Long
            -productId: Long
            -productName: String
            -productPrice: BigDecimal
            -quantity: Int
        }
        class Like {
            -id: Long
            -userId: Long
            -productId: Long
            -createdAt: LocalDateTime
        }
    }

    namespace Domain_Repository {
        class UserRepository {
            <<interface>>
            +save(user): UserModel
            +findByUserId(userId): UserModel?
            +existsByUserId(userId): Boolean
        }
        class BrandRepository {
            <<interface>>
            +save(brand): BrandModel
            +findById(id): BrandModel?
            +findAll(pageable): Page~BrandModel~
        }
        class ProductRepository {
            <<interface>>
            +save(product): ProductModel
            +findById(id): ProductModel?
            +findAllByCondition(brandId, sort, pageable): Page~ProductModel~
            +decreaseStock(productId, quantity): Int
        }
        class OrderRepository {
            <<interface>>
            +save(order): Order
            +findById(id): Order?
            +findByUserIdAndDateRange(userId, startAt, endAt): List~Order~
        }
        class LikeRepository {
            <<interface>>
            +save(like): Like
            +delete(like): void
            +findByUserIdAndProductId(userId, productId): Like?
            +findAllByUserId(userId): List~Like~
        }
    }

    namespace Infrastructure {
        class JpaUserRepository {
            +save(user): UserModel
            +findByUserId(userId): UserModel?
            +existsByUserId(userId): Boolean
        }
        class JpaProductRepository {
            +save(product): ProductModel
            +findById(id): ProductModel?
            +findAllByCondition(): Page~ProductModel~
        }
        class JpaOrderRepository {
            +save(order): Order
            +findById(id): Order?
        }
    }

    %% Layer Dependencies
    UserV1Controller --> UserService
    ProductV1Controller --> ProductService
    OrderV1Controller --> OrderService
    LikeV1Controller --> LikeService

    UserService --> UserRepository
    ProductService --> ProductRepository
    ProductService --> BrandRepository
    OrderService --> OrderRepository
    OrderService --> ProductService
    OrderService --> UserService
    LikeService --> LikeRepository
    LikeService --> ProductRepository
    BrandService --> BrandRepository
    BrandService --> ProductService

    JpaUserRepository ..|> UserRepository
    JpaProductRepository ..|> ProductRepository
    JpaOrderRepository ..|> OrderRepository

    Order "1" *-- "N" OrderItem : contains
```

### 📌 주요 확인 포인트

1. **의존 방향**: Controller → Service → Repository (단방향)
2. **Repository 인터페이스**: Domain에 정의, Infrastructure에서 구현
3. **도메인 모델 독립성**: Model 클래스는 외부 의존 없음
4. **서비스 간 의존**: OrderService → ProductService (재고 차감)

### 설계 의도
- 레이어드 아키텍처로 관심사 분리
- Repository 인터페이스를 통해 Infrastructure 교체 가능
- Domain 레이어는 프레임워크 독립적

---

## 2. 계층별 책임

### 2.1 Interfaces 계층

```mermaid
classDiagram
    direction LR

    namespace API_User {
        class UserV1Controller {
            +signup(SignupRequest): ApiResponse~UserResponse~
            +getMyInfo(loginId, loginPw): ApiResponse~UserResponse~
            +changePassword(loginId, loginPw, ChangePasswordRequest): ApiResponse
        }
        class SignupRequest {
            +userId: String
            +password: String
            +name: String
            +birthDate: String
            +email: String
        }
        class UserResponse {
            +id: Long
            +userId: String
            +name: String
            +email: String
        }
    }

    namespace API_Order {
        class OrderV1Controller {
            +createOrder(loginId, loginPw, CreateOrderRequest): ApiResponse~OrderResponse~
            +getOrders(loginId, loginPw, startAt, endAt): ApiResponse~List~
            +getOrder(loginId, loginPw, orderId): ApiResponse~OrderResponse~
        }
        class CreateOrderRequest {
            +items: List~OrderItemRequest~
        }
        class OrderItemRequest {
            +productId: Long
            +quantity: Int
        }
        class OrderResponse {
            +id: Long
            +status: String
            +totalAmount: BigDecimal
            +items: List~OrderItemResponse~
            +createdAt: LocalDateTime
        }
    }

    UserV1Controller ..> SignupRequest : uses
    UserV1Controller ..> UserResponse : returns
    OrderV1Controller ..> CreateOrderRequest : uses
    OrderV1Controller ..> OrderResponse : returns
```

**책임:**
- HTTP 요청/응답 처리
- DTO ↔ Domain Model 변환
- 인증 헤더 파싱 및 전달
- API 문서화 (Swagger)

---

### 2.2 Domain 계층

```mermaid
classDiagram
    direction TB

    class UserService {
        -userRepository: UserRepository
        -passwordEncoder: PasswordEncoder
        +createUser(userId, password, name, birthDate, email): UserModel
        +getUserByUserId(userId): UserModel
        +authenticate(userId, password): UserModel
        +changePassword(userId, oldPassword, newPassword): void
        -validateUserId(userId): void
        -validatePassword(password, birthDate): void
        -validateEmail(email): void
        -validateBirthDate(birthDate): void
    }

    class OrderService {
        -orderRepository: OrderRepository
        -productService: ProductService
        -userService: UserService
        +createOrder(userId, items): Order
        +getOrders(userId, startAt, endAt): List~Order~
        +getOrder(userId, orderId): Order
        -validateOrderItems(items): void
        -checkAndDecreaseStock(items): void
        -createOrderSnapshot(products, items): List~OrderItem~
    }

    class ProductService {
        -productRepository: ProductRepository
        +getProducts(brandId, sort, pageable): Page~ProductModel~
        +getProduct(productId): ProductModel
        +decreaseStock(productId, quantity): void
        +existsById(productId): Boolean
    }

    UserService --> UserRepository
    OrderService --> OrderRepository
    OrderService --> ProductService
    OrderService --> UserService
    ProductService --> ProductRepository
```

**책임:**
- 비즈니스 로직 수행
- 유효성 검증 (도메인 규칙)
- 트랜잭션 관리
- 도메인 이벤트 발행 (확장 시)

---

### 2.3 Domain Model

```mermaid
classDiagram
    class UserModel {
        -id: Long
        -userId: String
        -encryptedPassword: String
        -name: String
        -birthDate: LocalDate
        -email: String
        -createdAt: LocalDateTime
        -updatedAt: LocalDateTime
        +updatePassword(newEncryptedPassword): void
        +getMaskedName(): String
    }

    class ProductModel {
        -id: Long
        -brandId: Long
        -name: String
        -description: String
        -price: BigDecimal
        -stock: Int
        -likeCount: Int
        -deletedAt: LocalDateTime
        -createdAt: LocalDateTime
        +decreaseStock(quantity): void
        +increaseStock(quantity): void
        +isDeleted(): Boolean
    }

    class Order {
        -id: Long
        -userId: Long
        -status: OrderStatus
        -totalAmount: BigDecimal
        -items: List~OrderItem~
        -createdAt: LocalDateTime
        +calculateTotalAmount(): BigDecimal
        +addItem(item): void
    }

    class OrderItem {
        -id: Long
        -orderId: Long
        -productId: Long
        -productName: String
        -productPrice: BigDecimal
        -quantity: Int
        +getSubtotal(): BigDecimal
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        PAID
        SHIPPED
        COMPLETED
        CANCELLED
    }

    Order "1" *-- "N" OrderItem
    Order --> OrderStatus
```

**책임:**
- 도메인 불변식(invariant) 보장
- 자체 상태 변경 로직 캡슐화
- 비즈니스 의미를 가진 메서드 제공

---

### 2.4 Infrastructure 계층

```mermaid
classDiagram
    direction TB

    namespace JPA_Repository {
        class JpaUserRepository {
            <<@Repository>>
            +save(user): UserModel
            +findByUserId(userId): UserModel?
            +existsByUserId(userId): Boolean
        }
        class JpaProductRepository {
            <<@Repository>>
            +save(product): ProductModel
            +findById(id): ProductModel?
            +findAllByBrandIdAndDeletedAtIsNull(): Page~ProductModel~
            +decreaseStock(productId, quantity): Int
        }
        class JpaOrderRepository {
            <<@Repository>>
            +save(order): Order
            +findById(id): Order?
            +findByUserIdAndCreatedAtBetween(): List~Order~
        }
    }

    namespace Domain_Interface {
        class UserRepository {
            <<interface>>
        }
        class ProductRepository {
            <<interface>>
        }
        class OrderRepository {
            <<interface>>
        }
    }

    JpaUserRepository ..|> UserRepository
    JpaProductRepository ..|> ProductRepository
    JpaOrderRepository ..|> OrderRepository
```

**책임:**
- Repository 인터페이스 구현
- JPA/DB 기술 세부사항 캡슐화
- 쿼리 최적화

---

## 3. 의존 관계 설명

### 3.1 서비스 간 의존

```mermaid
graph LR
    subgraph Controllers
        UC[UserController]
        PC[ProductController]
        OC[OrderController]
        LC[LikeController]
        BC[BrandController]
    end

    subgraph Services
        US[UserService]
        PS[ProductService]
        OS[OrderService]
        LS[LikeService]
        BS[BrandService]
    end

    UC --> US
    PC --> PS
    OC --> OS
    LC --> LS
    BC --> BS

    OS --> US
    OS --> PS
    LS --> PS
    BS --> PS

    style OS fill:#ffcccc
    style PS fill:#ccffcc
```

**의존 방향 원칙:**
- OrderService → ProductService: 주문 시 상품 조회/재고 차감
- OrderService → UserService: 주문자 인증 확인
- LikeService → ProductService: 좋아요 대상 상품 존재 확인
- BrandService → ProductService: 브랜드 삭제 시 상품 연쇄 처리

**순환 의존 방지:**
- ProductService는 다른 서비스에 의존하지 않음 (하위 레벨)
- UserService는 다른 서비스에 의존하지 않음 (하위 레벨)

---

### 3.2 Admin vs User 컨트롤러 분리

```mermaid
classDiagram
    direction LR

    namespace User_API {
        class ProductV1Controller {
            +getProducts(): Page
            +getProduct(): Product
        }
        class BrandV1Controller {
            +getBrand(): Brand
        }
    }

    namespace Admin_API {
        class ProductAdminV1Controller {
            +getProducts(): Page
            +getProduct(): Product
            +createProduct(): Product
            +updateProduct(): Product
            +deleteProduct(): void
        }
        class BrandAdminV1Controller {
            +getBrands(): Page
            +getBrand(): Brand
            +createBrand(): Brand
            +updateBrand(): Brand
            +deleteBrand(): void
        }
    }

    ProductV1Controller --> ProductService
    BrandV1Controller --> BrandService
    ProductAdminV1Controller --> ProductService
    BrandAdminV1Controller --> BrandService

    class ProductService {
        +getProducts()
        +getProduct()
        +createProduct()
        +updateProduct()
        +deleteProduct()
    }

    class BrandService {
        +getBrands()
        +getBrand()
        +createBrand()
        +updateBrand()
        +deleteBrand()
    }
```

**설계 의도:**
- API prefix로 구분: `/api/v1` vs `/api-admin/v1`
- 동일한 Service 공유, Controller에서 권한 체크
- 응답 DTO는 역할에 따라 다를 수 있음 (Admin은 더 많은 정보)

---

## 4. 확장 고려사항

### 4.1 이벤트 기반 확장

```mermaid
classDiagram
    direction TB

    class OrderService {
        -eventPublisher: ApplicationEventPublisher
        +createOrder(): Order
    }

    class OrderCreatedEvent {
        +orderId: Long
        +userId: Long
        +items: List~OrderItem~
    }

    class PaymentEventListener {
        -paymentService: PaymentService
        +handleOrderCreated(event): void
    }

    class InventoryEventListener {
        -productService: ProductService
        +handleOrderCreated(event): void
    }

    OrderService ..> OrderCreatedEvent : publishes
    PaymentEventListener ..> OrderCreatedEvent : listens
    InventoryEventListener ..> OrderCreatedEvent : listens
```

**확장 포인트:**
- 주문 생성 시 `OrderCreatedEvent` 발행
- 결제, 재고, 알림 등이 이벤트 구독
- 서비스 간 직접 의존 제거

---

**문서 작성일**: 2026-02-11
**버전**: 1.0
