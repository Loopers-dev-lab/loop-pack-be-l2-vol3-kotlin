# 02. 시퀀스 다이어그램

---

## 왜 시퀀스 다이어그램이 필요한가

시퀀스 다이어그램으로 검증하려는 것:
1. **책임 분리**: 각 레이어(Controller, Facade, Service, Repository)가 어떤 역할을 하는가
2. **호출 순서**: 어떤 순서로 로직이 실행되는가
3. **트랜잭션 경계**: 어디서 트랜잭션이 시작/종료되는가
4. **예외 처리**: 어느 시점에 실패하면 어떻게 처리되는가

---

## 0. 인증 처리 흐름 (Security Filter Chain)

> 인증은 **횡단 관심사**이므로 모든 시퀀스에 중복하지 않고, 여기서 한 번만 정리한다.
> 이후 시퀀스(1~4)는 **인증 통과 후** 흐름으로 읽으면 된다.

```mermaid
sequenceDiagram
    actor Client as Client
    participant Filter as SecurityFilterChain<br/>(Spring Security)
    participant EntryPoint as AuthenticationEntryPoint
    participant Controller as Controller<br/>(interfaces)

    Note over Filter: 경로별 FilterChain 분기<br/>/api-admin/** → AdminFilterChain (@Order 1)<br/>/api/** → UserFilterChain (@Order 2)

    rect rgb(230, 245, 255)
        Note over Client,Controller: 회원 인증 (X-Loopers-LoginId + LoginPw)
        Client->>Filter: GET /api/v1/orders<br/>Headers: X-Loopers-LoginId, X-Loopers-LoginPw

        Filter->>Filter: 헤더 추출 → DB 유저 조회 + 비밀번호 검증

        alt 인증 성공
            Filter->>Filter: SecurityContext에 ROLE_USER + userId 설정
            Filter->>Controller: 요청 전달 (인증 완료)
        end

        alt 인증 실패 (헤더 누락, 유저 없음, 비밀번호 불일치)
            Filter->>EntryPoint: AuthenticationException
            EntryPoint-->>Client: 401 UNAUTHORIZED<br/>{"meta":{"result":"FAIL","errorCode":"Unauthorized"}}
            Note over EntryPoint: Controller에 도달하지 않음
        end
    end

    rect rgb(255, 240, 230)
        Note over Client,Controller: 관리자 인증 (X-Loopers-Ldap)
        Client->>Filter: DELETE /api-admin/v1/brands/1<br/>Header: X-Loopers-Ldap: loopers.admin

        Filter->>Filter: LDAP 헤더 값 확인

        alt 인증 성공
            Filter->>Filter: SecurityContext에 ROLE_ADMIN 설정
            Filter->>Controller: 요청 전달 (인증 완료)
        end

        alt 인증 실패 (헤더 누락, 값 불일치)
            Filter->>EntryPoint: AuthenticationException
            EntryPoint-->>Client: 401 UNAUTHORIZED
        end
    end

    rect rgb(240, 255, 240)
        Note over Client,Controller: 비회원 접근 (인증 불필요)
        Client->>Filter: GET /api/v1/products<br/>(인증 헤더 없음)

        Filter->>Filter: permitAll 경로 → 인증 건너뜀
        Filter->>Controller: 요청 전달
    end
```

### 이 다이어그램에서 봐야 할 포인트

1. **Controller 도달 전 차단**: 인증 실패 시 Security Filter에서 401을 반환. Controller 코드가 실행되지 않음
2. **Dual FilterChain**: `/api-admin/**`과 `/api/**`가 각각 독립적인 필터 체인으로 처리
3. **ApiResponse 형식 통일**: `CustomAuthenticationEntryPoint`에서 JSON 401 응답을 `ApiResponse` 형식으로 반환
4. **permitAll 경로**: 상품 목록/상세, 브랜드 조회 등 비회원 API는 인증을 건너뜀
5. **@AuthenticationPrincipal**: 인증 성공 시 SecurityContext에 담긴 유저 정보를 Controller에서 `@AuthenticationPrincipal`로 주입받아 사용

### permitAll 경로 목록 (인증 불필요)

| METHOD | URI | 설명 |
|--------|-----|------|
| GET | `/api/v1/brands/{brandId}` | 브랜드 조회 |
| GET | `/api/v1/products?...` | 상품 목록 조회 |
| GET | `/api/v1/products/{productId}` | 상품 상세 조회 |

> 위 경로 외 `/api/**`는 인증 필수, `/api-admin/**`은 LDAP 인증 필수.

---

## 1. 주문 생성 흐름 (정상 + 예외)

> 이 다이어그램이 가장 중요한 이유: 주문은 재고 차감, 스냅샷 저장, 금액 계산이 **하나의 트랜잭션** 안에서 일어나야 하며, 실패 시 전체 롤백이 필요하다.

```mermaid
sequenceDiagram
    actor Client as Client
    participant Controller as OrderController<br/>(interfaces)
    participant Facade as OrderFacade<br/>(application)
    participant OrderService as OrderService<br/>(domain)
    participant ProductService as ProductService<br/>(domain)
    participant OrderRepo as OrderRepository<br/>(domain I/F)
    participant ProductRepo as ProductRepository<br/>(domain I/F)
    participant DB as Database

    Client->>Controller: POST /api/v1/orders<br/>{items: [{productId:1, qty:2}, {productId:3, qty:1}]}

    Note over Controller: @AuthenticationPrincipal userId 주입

    Controller->>Facade: createOrder(userId, items)

    Note over Facade: 트랜잭션 시작 (@Transactional)

    loop 각 주문 상품에 대해
        Facade->>ProductService: findProductById(productId)
        ProductService->>ProductRepo: findById(productId)
        ProductRepo->>DB: SELECT
        DB-->>ProductRepo: Product
        ProductRepo-->>ProductService: Product

        alt 상품이 존재하지 않음
            ProductService-->>Facade: CoreException(NOT_FOUND)
            Facade-->>Controller: 예외 전파
            Controller-->>Client: 404 "존재하지 않는 상품입니다"
        end

        alt 상품이 판매 중지 상태
            ProductService-->>Facade: CoreException(BAD_REQUEST)
            Facade-->>Controller: 예외 전파
            Controller-->>Client: 400 "판매 중지된 상품입니다"
        end

        Facade->>ProductService: decreaseStock(productId, quantity)
        ProductService->>ProductRepo: decreaseStock(productId, quantity)
        ProductRepo->>DB: UPDATE stock = stock - qty<br/>WHERE stock >= qty

        alt 재고 부족 (affected rows = 0)
            ProductRepo-->>ProductService: 업데이트 실패
            ProductService-->>Facade: CoreException(BAD_REQUEST)
            Note over Facade: 트랜잭션 롤백
            Facade-->>Controller: 예외 전파
            Controller-->>Client: 400 "상품의 재고가 부족합니다"
        end

        DB-->>ProductRepo: 재고 차감 완료
    end

    Facade->>OrderService: createOrder(user, products, quantities)

    Note over OrderService: Order 엔티티 생성
    Note over OrderService: OrderItem 생성 (스냅샷 포함)<br/>- ProductSnapshot (상품명, 브랜드명)<br/>- PriceSnapshot (원가, 최종가)
    Note over OrderService: 주문 총액 계산

    OrderService->>OrderRepo: save(order)
    OrderRepo->>DB: INSERT Order + OrderItems
    DB-->>OrderRepo: 저장 완료
    OrderRepo-->>OrderService: Order

    OrderService-->>Facade: Order

    Note over Facade: 트랜잭션 커밋

    Facade-->>Controller: OrderInfo
    Controller-->>Client: 200 OK<br/>ApiResponse(OrderResponse)
```

### 이 다이어그램에서 봐야 할 포인트

1. **트랜잭션 경계**: Facade에서 `@Transactional`을 시작. 재고 차감과 주문 생성이 같은 트랜잭션 → 중간에 실패하면 전체 롤백
2. **재고 차감 방식**: `UPDATE ... WHERE stock >= qty` — DB 원자적 갱신. affected rows가 0이면 재고 부족
3. **스냅샷 생성 시점**: OrderItem 생성 시 Product의 현재 정보를 복사
4. **예외 발생 위치**: 상품 미존재(404), 판매 중지(400), 재고 부족(400) — 각각 다른 시점에서 실패

---

## 2. 상품 좋아요 등록/취소 흐름

> 이 다이어그램으로 검증하려는 것: 좋아요의 중복 방지, likeCount 동기화, 삭제된 상품에 대한 처리

```mermaid
sequenceDiagram
    actor Client as Client
    participant Controller as LikeController<br/>(interfaces)
    participant Facade as LikeFacade<br/>(application)
    participant LikeService as LikeService<br/>(domain)
    participant ProductService as ProductService<br/>(domain)
    participant LikeRepo as LikeRepository<br/>(domain I/F)
    participant ProductRepo as ProductRepository<br/>(domain I/F)
    participant DB as Database

    Note over Client,DB: === 좋아요 등록 ===

    Client->>Controller: POST /api/v1/products/{productId}/likes

    Note over Controller: @AuthenticationPrincipal userId 주입

    Controller->>Facade: addLike(userId, productId)

    Facade->>ProductService: findProductById(productId)
    ProductService->>ProductRepo: findById(productId)
    ProductRepo->>DB: SELECT

    alt 상품이 존재하지 않음
        ProductService-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 예외 전파
        Controller-->>Client: 404 "존재하지 않는 상품입니다"
    end

    DB-->>ProductRepo: Product
    ProductRepo-->>ProductService: Product
    ProductService-->>Facade: Product

    Facade->>LikeService: addLike(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT
    DB-->>LikeRepo: Like?

    alt 이미 좋아요 상태
        LikeRepo-->>LikeService: Like 존재
        LikeService-->>Facade: CoreException(CONFLICT)
        Facade-->>Controller: 예외 전파
        Controller-->>Client: 409 "이미 좋아요한 상품입니다"
    end

    LikeRepo-->>LikeService: null (좋아요 없음)

    Note over LikeService: Like 엔티티 생성

    LikeService->>LikeRepo: save(like)
    LikeRepo->>DB: INSERT
    DB-->>LikeRepo: 저장 완료

    LikeService-->>Facade: Like

    Facade->>ProductService: increaseLikeCount(productId)
    ProductService->>ProductRepo: increaseLikeCount(productId)
    ProductRepo->>DB: UPDATE likeCount = likeCount + 1
    DB-->>ProductRepo: 완료

    ProductService-->>Facade: 완료
    Facade-->>Controller: 완료
    Controller-->>Client: 200 OK

    Note over Client,DB: === 좋아요 취소 ===

    Client->>Controller: DELETE /api/v1/products/{productId}/likes

    Note over Controller: @AuthenticationPrincipal userId 주입

    Controller->>Facade: removeLike(userId, productId)
    Facade->>LikeService: removeLike(userId, productId)
    LikeService->>LikeRepo: findByUserIdAndProductId(userId, productId)
    LikeRepo->>DB: SELECT
    DB-->>LikeRepo: Like?

    alt 좋아요가 존재하지 않음
        LikeRepo-->>LikeService: null
        LikeService-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 예외 전파
        Controller-->>Client: 404 "좋아요 정보가 없습니다"
    end

    LikeRepo-->>LikeService: Like 존재
    LikeService->>LikeRepo: delete(like)
    LikeRepo->>DB: DELETE
    DB-->>LikeRepo: 삭제 완료

    LikeService-->>Facade: 완료

    Facade->>ProductService: decreaseLikeCount(productId)
    ProductService->>ProductRepo: decreaseLikeCount(productId)
    ProductRepo->>DB: UPDATE likeCount = likeCount - 1
    DB-->>ProductRepo: 완료

    ProductService-->>Facade: 완료
    Facade-->>Controller: 완료
    Controller-->>Client: 200 OK
```

### 이 다이어그램에서 봐야 할 포인트

1. **중복 좋아요 방지**: `findByUserIdAndProductId`로 기존 좋아요 확인 후, 있으면 409 CONFLICT
2. **likeCount 동기화**: Facade가 LikeService → ProductService를 순서대로 호출하여 좋아요 저장 + likeCount 증감을 조합
3. **좋아요 취소 시**: 물리 삭제 (Like 테이블에서 DELETE). 좋아요 이력이 필요하면 Soft Delete로 전환 가능
4. **상품 존재 확인을 먼저**: 삭제된 상품에 좋아요를 시도하면 404 반환
5. **서비스 간 직접 호출 없음**: LikeService와 ProductService가 서로 의존하지 않고, Facade가 조합하는 역할

---

## 3. 어드민 — 브랜드 삭제 흐름 (연쇄 삭제)

> 이 다이어그램으로 검증하려는 것: 브랜드 삭제 시 상품 연쇄 삭제의 범위와 트랜잭션 경계

```mermaid
sequenceDiagram
    actor Admin as Admin
    participant Controller as BrandAdminController<br/>(interfaces)
    participant Facade as BrandFacade<br/>(application)
    participant BrandService as BrandService<br/>(domain)
    participant ProductService as ProductService<br/>(domain)
    participant LikeService as LikeService<br/>(domain)
    participant DB as Database

    Admin->>Controller: DELETE /api-admin/v1/brands/{brandId}

    Controller->>Facade: deleteBrand(brandId)

    Note over Facade: 트랜잭션 시작

    Facade->>BrandService: findBrandById(brandId)

    alt 브랜드가 존재하지 않음
        BrandService-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 예외 전파
        Controller-->>Admin: 404 "존재하지 않는 브랜드입니다"
    end

    BrandService-->>Facade: Brand

    Facade->>ProductService: findProductsByBrandId(brandId)
    ProductService-->>Facade: List<Product>

    loop 해당 브랜드의 각 상품에 대해
        Facade->>LikeService: deleteLikesByProductId(productId)
        LikeService->>DB: UPDATE likes SET deleted_at = NOW()<br/>WHERE product_id = ?
        DB-->>LikeService: Soft Delete 완료

        Facade->>ProductService: deleteProduct(productId)
        ProductService->>DB: UPDATE products SET deleted_at = NOW()<br/>WHERE product_id = ?
        DB-->>ProductService: Soft Delete 완료
    end

    Facade->>BrandService: deleteBrand(brandId)
    BrandService->>DB: UPDATE brands SET deleted_at = NOW()<br/>WHERE brand_id = ?
    DB-->>BrandService: Soft Delete 완료

    Note over Facade: 트랜잭션 커밋

    Facade-->>Controller: 완료
    Controller-->>Admin: 200 OK
```

### 이 다이어그램에서 봐야 할 포인트

1. **Facade의 역할**: 여러 도메인 서비스(Brand, Product, Like)를 조합하는 건 Facade의 책임. 각 서비스는 자기 도메인만 관리
2. **Soft Delete**: `BaseEntity.delete()` → `deletedAt = NOW()`. 주문 스냅샷에 브랜드/상품 정보가 남아있으므로 물리 삭제 불가
3. **삭제 순서**: 좋아요 → 상품 → 브랜드 (의존 관계 역순)
4. **트랜잭션 범위**: 전체가 하나의 트랜잭션. 중간에 실패하면 모두 롤백

### 잠재 리스크

- **대량 상품이 있는 브랜드 삭제 시**: 하나의 트랜잭션이 비대해질 수 있음
  - 대안 1: 배치 처리로 분리 (상품 삭제를 비동기로)
  - 대안 2: 브랜드만 Soft Delete하고, 상품은 조회 시 브랜드 상태를 체크
  - 현재 과제에서는 동기 처리로 충분. 상품 수가 극단적으로 많지 않다는 전제

---

## 4. 상품 등록 흐름 (어드민)

> 이 다이어그램으로 검증하려는 것: 브랜드 존재 확인, 상품 필드 검증

```mermaid
sequenceDiagram
    actor Admin as Admin
    participant Controller as ProductAdminController<br/>(interfaces)
    participant Facade as ProductFacade<br/>(application)
    participant ProductService as ProductService<br/>(domain)
    participant BrandService as BrandService<br/>(domain)
    participant ProductRepo as ProductRepository<br/>(domain I/F)
    participant DB as Database

    Admin->>Controller: POST /api-admin/v1/products<br/>{brandId, name, price, stock, ...}

    Note over Controller: DTO 검증 (Bean Validation)

    Controller->>Facade: createProduct(command)

    Facade->>BrandService: findBrandById(brandId)

    alt 브랜드가 존재하지 않음
        BrandService-->>Facade: CoreException(BAD_REQUEST)
        Facade-->>Controller: 예외 전파
        Controller-->>Admin: 400 "존재하지 않는 브랜드입니다"
    end

    BrandService-->>Facade: Brand

    Facade->>ProductService: createProduct(brand, command)

    Note over ProductService: Product 엔티티 생성<br/>(init 블록에서 필드 검증)

    alt 필드 검증 실패 (가격 0 이하, 이름 공백 등)
        ProductService-->>Facade: CoreException(BAD_REQUEST)
        Facade-->>Controller: 예외 전파
        Controller-->>Admin: 400 "상품명은 필수입니다"
    end

    ProductService->>ProductRepo: save(product)
    ProductRepo->>DB: INSERT
    DB-->>ProductRepo: 저장 완료
    ProductRepo-->>ProductService: Product

    ProductService-->>Facade: Product
    Facade-->>Controller: ProductInfo
    Controller-->>Admin: 200 OK<br/>ApiResponse(ProductResponse)
```

### 이 다이어그램에서 봐야 할 포인트

1. **브랜드 존재 확인이 먼저**: 상품을 만들기 전에 브랜드가 유효한지 검증
2. **엔티티 내 검증**: Product의 `init` 블록에서 필드 검증 (1주차 User 패턴과 동일)
3. **Facade가 조합**: BrandService와 ProductService를 순서대로 호출

---

## 5. 컴포넌트 책임 테이블

| 레이어 | 클래스 | 책임 |
|--------|--------|------|
| **interfaces** | `OrderController` | HTTP 요청 수신, DTO 변환, @AuthenticationPrincipal 주입 |
| **interfaces** | `OrderDto` | Request/Response 직렬화 |
| **application** | `OrderFacade` | 여러 도메인 서비스 조합, 트랜잭션 경계 |
| **application** | `OrderInfo` | 응답용 데이터 (Entity → Info 변환) |
| **domain** | `OrderService` | 주문 생성, 상태 관리, 비즈니스 규칙 |
| **domain** | `ProductService` | 상품 조회, 재고 차감, 좋아요 수 관리 |
| **domain** | `BrandService` | 브랜드 CRUD, 존재 확인 |
| **domain** | `LikeService` | 좋아요 등록/취소, 중복 확인 |
| **domain** | `Order`, `OrderItem` | 주문 엔티티, 스냅샷 포함 |
| **domain** | `Product` | 상품 엔티티, 재고, 상태 관리 |
| **domain** | `Brand` | 브랜드 엔티티 |
| **domain** | `Like` | 좋아요 엔티티 (userId + productId) |
| **infrastructure** | `*RepositoryImpl` | 도메인 Repository 인터페이스의 JPA 구현체 |
