# 02. 시퀀스 다이어그램

---

## 1. 상품 조회 + 좋아요 흐름

### 다이어그램의 목적

사용자가 상품을 탐색하고 좋아요를 누르는 전체 흐름에서 **각 레이어의 책임**과 **인증 처리 시점**을 검증한다.

### 검증 포인트
- 비인증 API(상품 조회)와 인증 API(좋아요)의 처리 경로 차이
- Interceptor에서 인증이 분리되어 Controller가 비즈니스 로직에만 집중하는지
- 좋아요 등록 시 상품 존재 확인이 Facade에서 이루어지는지 (cross-domain 접근은 Facade 레벨)
- 좋아요 등록/취소의 멱등성 처리 방향

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as ProductController / LikeController
    participant Facade as ProductFacade / LikeFacade
    participant Cache as ProductCacheStore
    participant ProductSvc as ProductService
    participant LikeSvc as LikeService
    participant DB as Database

    Note over User, DB: 1. 상품 목록 조회 (비인증, cache-aside)
    User->>Controller: GET /api/v1/products?brandId=1&sort=latest&size=20
    Controller->>Facade: getProducts(brandId, sort, size, cursor)
    Facade->>Cache: getProductList(cacheKey)
    alt Cache HIT
        Cache-->>Facade: ProductListResult
    else Cache MISS
        Facade->>ProductSvc: getProducts(condition)
        ProductSvc->>DB: SELECT products (+ brand join)
        DB-->>ProductSvc: Product list
        ProductSvc-->>Facade: Product list
        Facade->>Cache: putProductList(cacheKey, result)
    end
    Facade-->>Controller: ProductInfo list
    Controller-->>User: 200 OK (상품 목록)

    Note over User, DB: 2. 상품 상세 조회 (비인증, cache-aside)
    User->>Controller: GET /api/v1/products/{productId}
    Controller->>Facade: getProduct(productId)
    Facade->>Cache: getProduct(productId)
    alt Cache HIT
        Cache-->>Facade: ProductInfo
    else Cache MISS
        Facade->>ProductSvc: getProduct(productId)
        ProductSvc->>DB: SELECT product (+ brand)
        DB-->>ProductSvc: Product
        ProductSvc-->>Facade: Product
        Facade->>Cache: putProduct(productId, info)
    end
    Facade-->>Controller: ProductInfo
    Controller-->>User: 200 OK (상품 상세)

    Note over User, DB: 3. 좋아요 등록 (인증 필요, 멱등)
    User->>Interceptor: POST /api/v1/products/{productId}/likes
    Interceptor->>Interceptor: 헤더 검증 + 캐시 or authenticate()
    break 인증 실패
        Interceptor-->>User: 401 Unauthorized
    end
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: like(memberId, productId)
    Facade->>ProductSvc: getProduct(productId)
    ProductSvc->>DB: SELECT product WHERE id = productId
    break 상품 미존재
        ProductSvc-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 404 Not Found
        Controller-->>User: 404 Not Found
    end
    DB-->>ProductSvc: Product
    ProductSvc-->>Facade: Product
    Facade->>LikeSvc: like(memberId, productId)
    LikeSvc->>LikeSvc: findByMemberIdAndProductId 존재 확인
    break 이미 좋아요 존재
        Note right of LikeSvc: 이미 존재 → return (멱등, 200 OK)
        LikeSvc-->>Facade: OK
        Facade-->>Controller: OK
        Controller-->>User: 200 OK
    end
    LikeSvc->>DB: INSERT product_like (Unique Constraint 최종 방어)
    DB-->>LikeSvc: OK
    LikeSvc-->>Facade: OK
    Facade-->>Controller: OK
    Controller-->>User: 200 OK

    Note over User, DB: 4. 좋아요 취소 (인증 필요, 멱등)
    User->>Interceptor: DELETE /api/v1/products/{productId}/likes
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: unlike(memberId, productId)
    Facade->>LikeSvc: unlike(memberId, productId)
    LikeSvc->>DB: DELETE product_like WHERE member_id, product_id
    Note right of LikeSvc: affected rows = 0이어도 200 OK (멱등)
    DB-->>LikeSvc: OK
    LikeSvc-->>Facade: OK
    Facade-->>Controller: OK
    Controller-->>User: 200 OK

    Note over User, DB: 5. 내 좋아요 목록 조회 (인증 필요)
    User->>Interceptor: GET /api/v1/likes
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: getMyLikes(memberId)
    Facade->>LikeSvc: getLikedProductIds(memberId)
    LikeSvc->>DB: SELECT product_id FROM product_like WHERE member_id = ?
    DB-->>LikeSvc: productId list
    LikeSvc-->>Facade: productId list
    Facade->>ProductSvc: getProductsByIds(productIds)
    ProductSvc->>DB: SELECT products WHERE id IN (...) AND status = 'ACTIVE'
    DB-->>ProductSvc: Product list
    ProductSvc-->>Facade: Product list
    Note right of Facade: BrandService로 brandName 배치 조회
    Facade-->>Controller: LikedProductInfo list
    Controller-->>User: 200 OK (좋아요 상품 목록)
```

### 해석

- **비인증/인증 경로 분리**: 대고객 API는 `@MemberAuthenticated` 어노테이션으로 선택적 인증을 적용한다. 상품 조회는 어노테이션 없이 Controller로 직행하고, 좋아요는 어노테이션이 있어 Interceptor에서 인증 후 `AuthenticatedMember`가 주입된다.
- **상품 조회 캐시 (cache-aside)**: `ProductFacade`가 `ProductCacheStore`를 통해 cache-aside 패턴을 적용한다. 캐시 히트 시 DB 조회를 건너뛰고, 캐시 미스 시 DB 조회 후 캐시에 저장한다. 상품 상세는 TTL 5분, 목록은 TTL 1분으로 운영한다. 어드민 CUD 시 상세 캐시를 즉시 evict한다.
- **상품 존재 확인은 Facade에서**: `LikeFacade`가 `ProductService.getProduct()`를 호출하여 상품 존재를 확인한다 (BR-L4). cross-domain 접근은 Facade 레벨에서 조합하는 원칙을 따른다.
- **좋아요/취소 멱등 처리**: 좋아요 등록 시 존재 여부를 먼저 확인하고, 이미 존재하면 즉시 반환(멱등). UNIQUE Constraint는 최종 방어선으로 유지한다. 좋아요 취소 시 대상이 없어도(affected rows = 0) 200 OK를 반환한다. 결과 상태("좋아요 있음/없음")가 요청 의도와 일치하면 성공이다.
- **내 좋아요 목록 조회**: `GET /api/v1/likes`로 인증된 본인의 좋아요만 조회한다. `LikeFacade`가 `LikeService`에서 productId 목록을 조회한 뒤, `ProductService`로 ACTIVE 상품 정보를, `BrandService`로 브랜드명을 배치 조회하여 조합한다. userId 경로 변수와 FORBIDDEN 검증은 제거되었다 (Decision 27).
- **좋아요 수 조회**: 상품 조회 시 `product` 테이블의 `like_count` 컬럼을 직접 반환한다. 배치가 주기적으로 갱신하므로 런타임 COUNT 부하가 없다.

---

## 2. 주문 생성 흐름

### 다이어그램의 목적

여러 상품을 한 번에 주문하는 핵심 플로우에서 **도메인 모델의 재고 불변식 보호**와 **스냅샷 저장 시점**을 검증한다.

### 검증 포인트
- 재고 검증이 도메인 모델 내부에서 이루어지는지 (Facade는 비즈니스 판단 없음)
- 상품 스냅샷이 주문 시점에 저장되는지
- 재고 부족 시 실패 처리가 명확한지

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as OrderController
    participant Facade as OrderFacade
    participant ProductSvc as ProductService
    participant OrderSvc as OrderService
    participant DB as Database

    User->>Interceptor: POST /api/v1/orders (items: [{productId:1, qty:2}, {productId:3, qty:1}])
    Interceptor->>Interceptor: 인증 처리 (캐시 or authenticate)
    Interceptor->>Controller: AuthenticatedMember 주입

    Controller->>Facade: createOrder(memberId, items)

    Note over Facade, DB: @Transactional 시작
    activate Facade

    Facade->>ProductSvc: getProductsByIds([1, 3])
    ProductSvc->>DB: SELECT products WHERE id IN (1, 3) AND status = 'ACTIVE'
    DB-->>ProductSvc: Product list
    Note right of Facade: 요청 ID 수와 조회 결과 수 불일치 시 NOT_FOUND
    break DELETED 상품 포함
        Facade-->>Controller: 404 Not Found
        Controller-->>User: 404 Not Found
    end
    ProductSvc-->>Facade: Product list

    Facade->>ProductSvc: deductStock(productId:1, qty:2)
    Note right of ProductSvc: SELECT FOR UPDATE (비관적 락)
    Note right of ProductSvc: ProductModel.deductStock(2)
    Note right of ProductSvc: 도메인 모델이 stockQuantity >= qty 검증
    break 재고 부족
        ProductSvc-->>Facade: CoreException(BAD_REQUEST, "재고 부족")
        Facade-->>Controller: 400 Bad Request
        Controller-->>User: 400 Bad Request
    end
    ProductSvc->>DB: UPDATE product SET stock_quantity = stock_quantity - 2

    Facade->>ProductSvc: deductStock(productId:3, qty:1)
    ProductSvc->>DB: UPDATE product SET stock_quantity = stock_quantity - 1

    Facade->>OrderSvc: createOrder(memberId, products, items)
    OrderSvc->>OrderSvc: 스냅샷 생성 (상품명, 가격, 브랜드명)
    OrderSvc->>DB: INSERT orders (order_number: UUID)
    OrderSvc->>DB: INSERT order_items (스냅샷 포함)
    DB-->>OrderSvc: Order (with items)
    OrderSvc-->>Facade: Order

    deactivate Facade
    Note over Facade, DB: @Transactional 커밋

    Facade-->>Controller: OrderInfo
    Controller-->>User: 201 Created (주문 정보)
```

### 해석

- **ACTIVE 상품만 주문 가능**: `getProductsByIds`에서 `status = 'ACTIVE'` 조건으로 조회한다. DELETED 상품이 요청에 포함되면 조회 결과 수 불일치로 NOT_FOUND를 반환한다. 재고가 0인 상품도 ACTIVE 상태이므로 조회는 되지만, `deductStock()`에서 재고 부족으로 실패한다.
- **도메인 모델의 불변식 보호**: `ProductModel.deductStock(qty)`이 내부에서 `stockQuantity >= qty`를 검증한다. 재고 부족 시 도메인 모델이 예외를 던지며, Facade는 비즈니스 판단 로직을 갖지 않는다.
- **트랜잭션 경계**: Facade 레이어에서 `@Transactional`로 재고 차감과 주문 생성을 하나의 트랜잭션으로 묶는다. 재고 차감 후 주문 생성이 실패하면 전체 롤백된다.
- **스냅샷 저장**: OrderItem에 주문 시점의 상품명(`productName`), 가격(`productPrice`), 브랜드명(`brandName`)을 복사하여 저장한다 (BR-O2).
- **재고 동시성 제어**: `ProductService.deductStock()`에서 비관적 락(`SELECT FOR UPDATE`)을 적용하여 동시 주문 시 재고 정합성을 보장한다. 재고 차감은 정합성이 핵심이므로 Phase 1(기능 구현)에 포함한다.

---

## 3. 어드민 브랜드/상품 관리 흐름

### 다이어그램의 목적

어드민이 브랜드와 상품을 관리하는 흐름에서 **LDAP 인증 처리**, **소프트 삭제 캐스케이드**, **어드민 전용 Facade 분리**를 검증한다.

### 검증 포인트
- 어드민 인증이 대고객 인증과 독립적으로 동작하는지
- 브랜드 삭제 시 하위 상품이 소프트 삭제 처리되는지
- 상품 등록 시 브랜드 존재 여부 검증이 이루어지는지
- 어드민 전용 Facade를 통해 요청이 처리되는지

```mermaid
sequenceDiagram
    actor Admin
    participant Interceptor as AdminInterceptor
    participant Controller as AdminBrandController / AdminProductController
    participant Facade as AdminBrandFacade / AdminProductFacade
    participant BrandSvc as BrandService
    participant ProductSvc as ProductService
    participant DB as Database

    Note over Admin, DB: 1. 브랜드 등록
    Admin->>Interceptor: POST /api-admin/v1/brands (X-Loopers-Ldap: loopers.admin)
    Interceptor->>Interceptor: LDAP 헤더 검증
    Interceptor->>Controller: 요청 전달
    Controller->>Facade: createBrand(name, description, imageUrl)
    Facade->>BrandSvc: createBrand(command)
    BrandSvc->>DB: INSERT brand
    DB-->>BrandSvc: Brand
    BrandSvc-->>Facade: Brand
    Facade-->>Controller: BrandInfo
    Controller-->>Admin: 201 Created

    Note over Admin, DB: 2. 상품 등록 (브랜드 존재 검증)
    Admin->>Interceptor: POST /api-admin/v1/products {brandId: 1, name: "상품A", price: 10000, stock: 100}
    Interceptor->>Interceptor: LDAP 헤더 검증
    Interceptor->>Controller: 요청 전달
    Controller->>Facade: createProduct(command)
    Facade->>BrandSvc: getBrand(brandId)
    BrandSvc->>DB: SELECT brand WHERE id = 1
    break 브랜드 미존재
        BrandSvc-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 404 Not Found
        Controller-->>Admin: 404 Not Found
    end
    DB-->>BrandSvc: Brand
    BrandSvc-->>Facade: Brand
    Facade->>ProductSvc: createProduct(command)
    ProductSvc->>DB: INSERT product
    DB-->>ProductSvc: Product
    ProductSvc-->>Facade: Product
    Facade-->>Controller: ProductInfo
    Controller-->>Admin: 201 Created

    Note over Admin, DB: 3. 브랜드 삭제 (소프트 삭제 캐스케이드)
    Admin->>Interceptor: DELETE /api-admin/v1/brands/{brandId}
    Interceptor->>Interceptor: LDAP 헤더 검증
    Interceptor->>Controller: 요청 전달
    Controller->>Facade: deleteBrand(brandId)
    activate Facade
    Facade->>ProductSvc: deleteProductsByBrandId(brandId)
    ProductSvc->>DB: UPDATE products SET status='DELETED', deleted_at=now() WHERE brand_id=brandId
    DB-->>ProductSvc: OK
    ProductSvc-->>Facade: OK
    Facade->>BrandSvc: deleteBrand(brandId)
    BrandSvc->>DB: UPDATE brand SET status='DELETED', deleted_at=now() WHERE id=brandId
    DB-->>BrandSvc: OK
    BrandSvc-->>Facade: OK
    deactivate Facade
    Facade-->>Controller: OK
    Controller-->>Admin: 200 OK
```

### 해석

- **어드민 Facade 분리**: `AdminBrandFacade`, `AdminProductFacade`가 어드민 전용 유스케이스를 처리한다. 고객용 Facade와 분리되어 독립적으로 진화 가능하다. Service는 공유한다.
- **어드민 인증 분리**: `AdminAuthenticationInterceptor`가 `X-Loopers-Ldap` 헤더를 검증한다. 대고객 `MemberAuthenticationInterceptor`와 독립적으로 동작하며, `@AdminAuthenticated` 어노테이션으로 클래스 레벨 일괄 적용된다. 대고객 인증(`@MemberAuthenticated`)과 동일한 어노테이션 기반 패턴으로 일관성을 유지한다 (어드민은 전 엔드포인트 인증 필수).
- **소프트 삭제 캐스케이드**: 브랜드 삭제 시 하위 상품을 먼저 소프트 삭제(`status=DELETED`, `deleted_at=now()`)한 후 브랜드를 소프트 삭제한다. 주문 스냅샷이 상품을 참조하므로 물리 삭제 불가.
- **상품 등록 시 브랜드 검증**: `AdminProductFacade`가 `BrandService.getBrand()`을 호출하여 브랜드 존재 여부를 확인한다 (BR-P1). cross-domain 접근은 Facade 레벨에서 조합한다.
- **상품 등록 시 좋아요 수 초기화**: `product` 테이블의 `like_count` 컬럼이 DEFAULT 0으로 정의되어, 상품 등록 시 별도 초기화 없이 자동으로 0이 설정된다.
- **브랜드 삭제 시 좋아요 데이터 유지**: 캐스케이드 범위는 상품 soft delete까지이며, `product_like` 데이터는 삭제하지 않는다. 마케팅/추천 데이터로 활용 가능하며, 내 좋아요 목록 조회 시 ACTIVE 필터링으로 자연스럽게 제외된다.

---

## 4. 주문 조회 흐름

### 다이어그램의 목적

대고객의 주문 조회 흐름에서 **도메인 모델의 접근 제어**(본인 주문만 조회)와 **스냅샷 데이터 반환**을 검증한다. 어드민 주문 조회와의 차이(본인 검증 유무)를 대비한다.

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as OrderController
    participant Facade as OrderFacade
    participant Service as OrderService
    participant DB as Database

    Note over User, DB: 유저 주문 목록 조회 (기간 필터)
    User->>Interceptor: GET /api/v1/orders?startAt=2026-01-31&endAt=2026-02-10
    Interceptor->>Controller: AuthenticatedMember(id=1) 주입
    Controller->>Facade: getOrders(memberId=1, startAt, endAt)
    Facade->>Service: getOrdersByMember(memberId=1, startAt, endAt)
    Service->>DB: SELECT orders WHERE member_id = 1 AND ordered_at BETWEEN ...
    DB-->>Service: Order list
    Service-->>Facade: Order list (with snapshot items)
    Facade-->>Controller: OrderInfo list
    Controller-->>User: 200 OK (주문 목록 + 스냅샷 상품 정보)

    Note over User, DB: 단일 주문 상세 조회 (도메인 모델의 본인 검증)
    User->>Interceptor: GET /api/v1/orders/{orderId}
    Interceptor->>Controller: AuthenticatedMember(id=1) 주입
    Controller->>Facade: getOrder(memberId=1, orderId)
    Facade->>Service: getOrder(orderId, memberId)
    Service->>DB: SELECT order + order_items WHERE order_id = ...
    DB-->>Service: Order with items
    Note right of Service: OrderModel.validateOwner(memberId)
    Note right of Service: 도메인 모델이 본인 여부 검증
    break 본인 주문 아님
        Service-->>Facade: CoreException(FORBIDDEN)
        Facade-->>Controller: 403 Forbidden
        Controller-->>User: 403 Forbidden
    end
    Service-->>Facade: Order
    Facade-->>Controller: OrderInfo (스냅샷 포함)
    Controller-->>User: 200 OK (주문 상세)
```

```mermaid
sequenceDiagram
    actor Admin
    participant Interceptor as AdminInterceptor
    participant Controller as AdminOrderController
    participant Facade as AdminOrderFacade
    participant Service as OrderService
    participant DB as Database

    Note over Admin, DB: 어드민 주문 상세 조회 (본인 검증 없음)
    Admin->>Interceptor: GET /api-admin/v1/orders/{orderId}
    Interceptor->>Interceptor: LDAP 헤더 검증
    Interceptor->>Controller: 요청 전달
    Controller->>Facade: getOrder(orderId)
    Facade->>Service: getOrderById(orderId)
    Service->>DB: SELECT order + order_items WHERE order_id = ...
    DB-->>Service: Order with items
    Note right of Service: validateOwner 호출 없음
    Service-->>Facade: Order
    Facade-->>Controller: OrderInfo (스냅샷 포함)
    Controller-->>Admin: 200 OK (주문 상세)
```

### 해석

- **도메인 모델의 접근 제어**: 대고객 경로에서 `OrderModel.validateOwner(memberId)`가 본인 여부를 검증한다. "본인 주문만 조회 가능" 규칙은 Order 도메인의 비즈니스 규칙이므로, 도메인 모델이 자기 규칙을 보호한다.
- **어드민 주문 조회**: `AdminOrderFacade`는 `OrderService.getOrderById(orderId)`를 호출하며, `validateOwner`를 수행하지 않는다. 어드민은 모든 주문을 조회할 수 있다.
- **스냅샷 반환**: OrderItem에 저장된 스냅샷(상품명, 가격, 브랜드명)을 그대로 반환한다. 현재 상품 정보와 무관하게 주문 당시의 정보를 보여준다.
- **기간 필터**: 대고객 주문 목록에서 `startAt`, `endAt` 파라미터로 `ordered_at` 기준 범위 검색을 수행한다.

---

## 5. 쿠폰 발급 + 사용 흐름

### 다이어그램의 목적

쿠폰 발급과 주문 시 쿠폰 적용 흐름에서 **템플릿 유효성 검증**, **낙관적 락을 통한 동시 사용 방지**, **주문 흐름 내 쿠폰 차감 시점**을 검증한다.

### 검증 포인트
- 쿠폰 발급 시 템플릿 존재 및 발급 가능 상태 검증
- 주문 시 쿠폰 차감이 재고 락 획득 전에 수행되는지 (D34)
- 낙관적 락 충돌 시 409 CONFLICT 처리

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as CouponV1Controller
    participant Facade as CouponFacade
    participant Service as CouponService
    participant DB as Database

    Note over User, DB: 1. 쿠폰 발급 (인증 필요)
    User->>Interceptor: POST /api/v1/coupons/templates/{templateId}/issue
    Interceptor->>Interceptor: 헤더 검증 + 캐시 or authenticate()
    break 인증 실패
        Interceptor-->>User: 401 Unauthorized
    end
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: issueCoupon(memberId, templateId)
    Facade->>Service: getTemplate(templateId)
    Service->>DB: SELECT coupon_template WHERE id = templateId
    break 템플릿 미존재
        Service-->>Facade: CoreException(NOT_FOUND)
        Facade-->>Controller: 404 Not Found
        Controller-->>User: 404 Not Found
    end
    DB-->>Service: CouponTemplateModel
    Service-->>Facade: CouponTemplateModel
    Note right of Facade: template.isIssuable() 검증<br/>(ACTIVE 상태 + 만료 여부)
    break 발급 불가 (INACTIVE or 만료됨)
        Facade-->>Controller: CoreException(BAD_REQUEST)
        Controller-->>User: 400 Bad Request
    end
    Note right of Facade: calculateExpiredAt(policy)<br/>FIXED_DATE: 템플릿 expiredAt 복사<br/>DAYS_FROM_ISSUE: 발급 시점 + validDays
    Facade->>Service: saveIssuedCoupon(memberId, templateId, expiredAt)
    Service->>DB: INSERT issued_coupon
    DB-->>Service: IssuedCouponModel
    Service-->>Facade: IssuedCouponModel
    Facade-->>Controller: IssuedCouponInfo
    Controller-->>User: 200 OK (발급된 쿠폰 정보)
```

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as CouponV1Controller
    participant Facade as CouponFacade
    participant Service as CouponService
    participant DB as Database

    Note over User, DB: 2. 내 쿠폰 목록 조회 (인증 필요)
    User->>Interceptor: GET /api/v1/members/me/coupons
    Interceptor->>Interceptor: 헤더 검증 + 캐시 or authenticate()
    break 인증 실패
        Interceptor-->>User: 401 Unauthorized
    end
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: getMyIssuedCoupons(memberId)
    Facade->>Service: getIssuedCoupons(memberId)
    Service->>DB: SELECT issued_coupon WHERE member_id = ?
    DB-->>Service: IssuedCoupon list
    Service-->>Facade: IssuedCoupon list
    Note right of Facade: IssuedCouponInfo.effectiveStatus 계산<br/>USED > EXPIRED > AVAILABLE 우선순위
    Facade-->>Controller: IssuedCouponInfo list
    Controller-->>User: 200 OK (내 쿠폰 목록)
```

```mermaid
sequenceDiagram
    actor User
    participant Interceptor as AuthInterceptor
    participant Controller as OrderV1Controller
    participant Facade as OrderFacade
    participant CouponSvc as CouponService
    participant ProductSvc as ProductService
    participant OrderSvc as OrderService
    participant DB as Database

    Note over User, DB: 3. 주문 시 쿠폰 적용 (기존 주문 흐름 확장)
    User->>Interceptor: POST /api/v1/orders (items + couponId)
    Interceptor->>Controller: AuthenticatedMember 주입
    Controller->>Facade: createOrder(memberId, items, couponId)

    Note over Facade, DB: @Transactional 시작
    activate Facade

    Note right of Facade: 쿠폰 차감은 재고 락 획득 전에 수행 (D34)

    Facade->>CouponSvc: getIssuedCouponById(couponId)
    CouponSvc->>DB: SELECT issued_coupon WHERE id = ?
    DB-->>CouponSvc: IssuedCouponModel
    CouponSvc-->>Facade: IssuedCouponModel

    Note right of Facade: validateOwner(memberId) → 본인 쿠폰 확인
    break 타인 쿠폰
        Facade-->>Controller: CoreException(FORBIDDEN)
        Controller-->>User: 403 Forbidden
    end
    Note right of Facade: status = AVAILABLE 확인
    break 이미 사용됨 or 만료됨
        Facade-->>Controller: CoreException(BAD_REQUEST)
        Controller-->>User: 400 Bad Request
    end
    Note right of Facade: expiredAt > now() 확인
    Note right of Facade: totalAmount >= minOrderAmount 확인
    Note right of Facade: 할인 금액 계산

    Facade->>CouponSvc: saveIssuedCoupon(coupon.use())
    Note right of CouponSvc: @Version 낙관적 락으로 UPDATE
    CouponSvc->>DB: UPDATE issued_coupon SET status=USED, version=version+1
    break 버전 충돌 (낙관적 락 실패)
        DB-->>CouponSvc: OptimisticLockingFailureException
        CouponSvc-->>Facade: 409 Conflict
        Facade-->>Controller: 409 Conflict
        Controller-->>User: 409 Conflict (전체 롤백)
    end
    DB-->>CouponSvc: OK

    loop 각 주문 항목 (productId 오름차순)
        Facade->>ProductSvc: getProductWithLock(productId)
        ProductSvc->>DB: SELECT ... FOR UPDATE (비관적 락)
        DB-->>ProductSvc: ProductModel
        Facade->>ProductSvc: deductStock(productId, quantity)
        ProductSvc->>DB: UPDATE stock_quantity
    end

    Facade->>OrderSvc: createOrder(memberId, items, discountAmount)
    OrderSvc->>DB: INSERT order + order_items
    DB-->>OrderSvc: OrderModel
    OrderSvc-->>Facade: OrderModel

    deactivate Facade
    Note over Facade, DB: @Transactional 커밋

    Facade-->>Controller: OrderInfo (할인 적용 금액 포함)
    Controller-->>User: 201 Created (주문 정보)
```

### 해석

- **템플릿 유효성**: `isIssuable()`이 ACTIVE 상태와 만료 여부를 동시 검증한다. 두 조건을 하나의 도메인 메서드로 캡슐화하여 Facade가 판단 로직을 갖지 않는다.
- **만료 정책 이중 지원**: `FIXED_DATE`는 템플릿의 `expiredAt`을 그대로 복사하고, `DAYS_FROM_ISSUE`는 발급 시점 + `validDays`로 계산한다. 발급 시점에 만료일이 확정되므로 이후 템플릿 변경에 영향받지 않는다.
- **삭제된 템플릿**: 신규 발급은 차단하되, 기발급 쿠폰은 `expiredAt`까지 사용 가능하다. 삭제 여부는 IssuedCoupon에 저장된 만료일과 무관하다.
- **낙관적 락**: IssuedCoupon은 1인 소유 자원이므로 동시 사용 충돌 빈도가 낮다. 비관적 락은 과도하며, `@Version` 낙관적 락으로 충분하다 (D31). 충돌 시 409 CONFLICT를 반환하고 전체 트랜잭션을 롤백한다.
- **주문 내 쿠폰 차감 시점**: 재고 비관적 락 획득 전에 쿠폰을 먼저 검증/차감한다 (D34). 쿠폰 실패 시 재고 락 대기 없이 즉시 실패하므로 불필요한 락 경쟁을 방지한다.

---

## 6. Event-Command-Handler 흐름 (D52)

---

### 6-1. 주문 요청 이벤트 흐름 (선차감 후주문)

#### 다이어그램의 목적

주문 생성 요청 시 `OrderRequestedEvent`를 통해 재고 차감 → 쿠폰 사용 → 주문 생성을 순차 오케스트레이션하는 흐름을 검증한다. 각 CommandHandler가 독립 트랜잭션(`REQUIRES_NEW`)으로 실행되어 부분 실패 시 보상 트랜잭션(주문 취소)이 동작하는 구조를 확인한다.

```mermaid
sequenceDiagram
    actor User
    participant Controller as OrderV1Controller
    participant Facade as OrderFacade
    participant OrderSvc as OrderService
    participant Publisher as ApplicationEventPublisher
    participant OEH as OrderRequestedEventHandler
    participant StockCH as StockCommandHandler
    participant CouponCH as CouponCommandHandler
    participant OrderCH as OrderCommandHandler
    participant DB as Database

    User->>Controller: POST /api/v1/orders (items + couponId)
    Controller->>Facade: createOrder(memberId, items, couponId)

    Note over Facade, DB: @Transactional 시작
    activate Facade
    Facade->>OrderSvc: requestOrder(memberId, items, couponId)
    OrderSvc->>DB: INSERT order (status=ORDERED, 임시 생성)
    DB-->>OrderSvc: OrderModel
    OrderSvc->>Publisher: publishEvent(OrderRequestedEvent)
    Note over Publisher: 이벤트를 AFTER_COMMIT 큐에 등록
    OrderSvc-->>Facade: OrderModel
    deactivate Facade
    Note over Facade, DB: @Transactional 커밋

    Note over OEH: AFTER_COMMIT — 커밋 후 실행
    Publisher-->>OEH: OrderRequestedEvent

    OEH->>StockCH: DeductStockCommand(productId 오름차순)
    Note over StockCH: @Transactional(REQUIRES_NEW)
    activate StockCH
    StockCH->>DB: SELECT ... FOR UPDATE (비관적 락, productId 오름차순)
    StockCH->>DB: UPDATE stock_quantity
    deactivate StockCH

    break 재고 부족
        StockCH-->>OEH: CoreException(BAD_REQUEST)
        OEH->>OrderCH: CancelOrderCommand
        Note over OrderCH: @Transactional(REQUIRES_NEW)
        OrderCH->>DB: UPDATE order SET status=CANCELLED
    end

    OEH->>CouponCH: UseCouponCommand(couponId)
    Note over CouponCH: @Transactional(REQUIRES_NEW)
    activate CouponCH
    CouponCH->>DB: UPDATE issued_coupon SET status=USED (@Version 낙관적 락)
    deactivate CouponCH

    break 쿠폰 사용 실패
        CouponCH-->>OEH: CoreException
        OEH->>StockCH: RestoreStockCommand (재고 복원)
        OEH->>OrderCH: CancelOrderCommand
    end

    OEH->>OrderCH: CompleteOrderCommand
    Note over OrderCH: @Transactional(REQUIRES_NEW)
    activate OrderCH
    OrderCH->>DB: UPDATE order SET status=COMPLETED (스냅샷 포함)
    deactivate OrderCH

    Facade-->>Controller: OrderInfo
    Controller-->>User: 201 Created
```

#### 해석

- **선차감 후주문**: `OrderRequestedEvent` AFTER_COMMIT 이후 재고 차감 → 쿠폰 사용 → 주문 완성 순서로 실행된다. 커밋 전 임시 `ORDERED` 상태로 주문이 생성되며, 모든 CommandHandler 성공 시 `COMPLETED`로 전환된다.
- **독립 트랜잭션(`REQUIRES_NEW`)**: 각 CommandHandler가 독립 트랜잭션으로 실행되므로 부분 성공/실패를 명시적으로 제어할 수 있다.
- **보상 트랜잭션**: 중간 단계 실패 시 `EventHandler`가 이미 성공한 CommandHandler를 역순으로 보상(재고 복원, 주문 취소)한다.
- **데드락 방지**: `StockCommandHandler`에서 재고 차감 시 `productId` 오름차순 정렬 후 `SELECT FOR UPDATE`를 실행한다 (D49).

---

### 6-2. 결제 성공 흐름 (PaymentSucceededEvent → Kafka → 주문 PAID)

#### 다이어그램의 목적

PG 결제 성공 후 `PaymentSucceededEvent`가 Kafka를 통해 `commerce-streamer`로 전달되어 주문 상태를 `PAID`로 전환하는 비동기 흐름을 검증한다. PG 외부 호출이 트랜잭션 밖에서 수행됨을 확인한다 (D46).

```mermaid
sequenceDiagram
    actor User
    participant Controller as PaymentV1Controller
    participant Facade as PaymentFacade
    participant PaymentSvc as PaymentService
    participant PgClient as PgClient (외부 PG)
    participant Publisher as ApplicationEventPublisher
    participant PSEH as PaymentSucceededEventHandler
    participant KafkaPublisher as CompleteOrderCommandPublisher
    participant Kafka as Kafka Topic
    participant Streamer as commerce-streamer
    participant OrderRepo as OrderRepository

    User->>Controller: POST /api/v1/payments/{orderId}
    Controller->>Facade: pay(memberId, orderId, pgToken)

    Note over Facade: requestPayment(@Transactional)
    activate Facade
    Facade->>PaymentSvc: requestPayment(orderId)
    PaymentSvc->>Publisher: publishEvent(PaymentRequestedEvent)
    deactivate Facade
    Note over Facade: @Transactional 커밋

    Note over PaymentSvc: callPg — 트랜잭션 밖 실행 (D46)
    PaymentSvc->>PgClient: HTTP 결제 요청 (no TX)
    PgClient-->>PaymentSvc: 결제 성공 응답

    Note over Facade: handleCallback(@Transactional)
    activate Facade
    Facade->>PaymentSvc: handlePaymentSuccess(orderId, pgResponse)
    PaymentSvc->>Publisher: publishEvent(PaymentSucceededEvent)
    deactivate Facade
    Note over Facade: @Transactional 커밋

    Note over PSEH: AFTER_COMMIT — 커밋 후 실행
    Publisher-->>PSEH: PaymentSucceededEvent

    PSEH->>KafkaPublisher: CompleteOrderCommand(orderId)
    Note over KafkaPublisher: TX 없음 — 외부 Kafka 발행
    KafkaPublisher->>Kafka: publish(order-complete-topic)

    Kafka-->>Streamer: consume(CompleteOrderCommand)
    Streamer->>OrderRepo: UPDATE order SET status=PAID

    Facade-->>Controller: PaymentInfo
    Controller-->>User: 200 OK (결제 완료)
```

#### 해석

- **PG 외부 호출은 트랜잭션 밖**: `callPg()`는 `@Transactional` 없이 실행하여 DB 커넥션 장기 점유를 방지한다 (D46).
- **Kafka 비동기 전달**: `PaymentSucceededEventHandler`가 AFTER_COMMIT 후 Kafka에 발행한다. `commerce-streamer`가 소비하여 주문 상태를 `PAID`로 전환한다.
- **트랜잭션 경계 3단 분리**: `requestPayment(@Tx)` → `callPg(no Tx)` → `handleCallback(@Tx)` (D46).

---

### 6-3. 결제 실패 보상 흐름 (PaymentFailedEvent → 재고/쿠폰 복원 + 주문 취소)

#### 다이어그램의 목적

PG 결제 실패 시 `PaymentFailedEvent`를 통해 재고 복원 → 쿠폰 복원 → 주문 취소가 Kafka 경유로 수행되는 보상 트랜잭션 흐름을 검증한다 (D47, D49).

```mermaid
sequenceDiagram
    participant Facade as PaymentFacade
    participant PaymentSvc as PaymentService
    participant PgClient as PgClient (외부 PG)
    participant Publisher as ApplicationEventPublisher
    participant PFEH as PaymentFailedEventHandler
    participant CompPublisher as PaymentCompensationPublisher
    participant Kafka as Kafka Topic
    participant Streamer as commerce-streamer
    participant StockCH as StockCommandHandler
    participant CouponCH as CouponCommandHandler
    participant OrderCH as OrderCommandHandler

    Note over PaymentSvc: callPg — 트랜잭션 밖 실행 (D46)
    PaymentSvc->>PgClient: HTTP 결제 요청 (no TX)

    alt PG 호출 자체 실패 (네트워크/타임아웃)
        PgClient-->>PaymentSvc: 예외 발생
        Note over PaymentSvc: 주문 status = ORDERED 유지 (재시도 가능, D47)
        PaymentSvc-->>Facade: PG 호출 실패 응답
    else PG 콜백 FAILED (결제 거절)
        PgClient-->>PaymentSvc: FAILED 응답
        Note over Facade: handleCallback(@Transactional)
        activate Facade
        Facade->>PaymentSvc: handlePaymentFailed(orderId, pgResponse)
        PaymentSvc->>Publisher: publishEvent(PaymentFailedEvent)
        deactivate Facade
        Note over Facade: @Transactional 커밋

        Note over PFEH: AFTER_COMMIT — 커밋 후 실행
        Publisher-->>PFEH: PaymentFailedEvent

        PFEH->>CompPublisher: PaymentCompensationCommand(orderId, items, couponId)
        Note over CompPublisher: TX 없음 — 외부 Kafka 발행
        CompPublisher->>Kafka: publish(payment-compensation-topic)

        Kafka-->>Streamer: consume(PaymentCompensationCommand)

        Note over Streamer: 보상 트랜잭션 실행 (D49)
        loop 각 상품 (productId 오름차순)
            Streamer->>StockCH: RestoreStockCommand(productId, qty)
            Note over StockCH: @Transactional(REQUIRES_NEW)
            StockCH->>StockCH: UPDATE stock_quantity (재고 복원)
        end

        Streamer->>CouponCH: RestoreCouponCommand(couponId)
        Note over CouponCH: @Transactional(REQUIRES_NEW)
        CouponCH->>CouponCH: UPDATE issued_coupon SET status=AVAILABLE

        Streamer->>OrderCH: CancelOrderCommand(orderId)
        Note over OrderCH: @Transactional(REQUIRES_NEW)
        OrderCH->>OrderCH: UPDATE order SET status=CANCELLED
    end
```

#### 해석

- **PG 실패 유형 구분 (D47)**: PG 호출 자체 실패(네트워크/타임아웃)는 주문을 `ORDERED` 상태로 유지하여 재시도를 허용한다. PG 콜백 `FAILED`는 결제가 확정적으로 거절된 것이므로 보상 트랜잭션을 실행한다.
- **보상 순서 (D49)**: 재고 복원은 `productId` 오름차순 정렬로 실행하여 데드락을 방지한다. 쿠폰 복원 → 주문 취소 순서로 마무리한다.
- **Kafka 비동기 보상**: `PaymentCompensationPublisher`가 Kafka에 보상 Command를 발행하고, `commerce-streamer`가 소비하여 각 CommandHandler를 독립 트랜잭션으로 실행한다.

---

### 6-4. 브랜드 삭제 연쇄 이벤트 흐름

#### 다이어그램의 목적

브랜드 삭제 시 `BrandDeletedEvent`가 하위 상품 소프트 삭제 → `ProductDeletedEvent` × N → 좋아요 삭제 + 캐시 evict로 연쇄되는 이벤트 체인을 검증한다.

```mermaid
sequenceDiagram
    actor Admin
    participant Controller as AdminBrandV1Controller
    participant Facade as AdminBrandFacade
    participant BrandSvc as BrandService
    participant Publisher as ApplicationEventPublisher
    participant BDEH as BrandDeletedEventHandler
    participant CascadeCH as CascadeDeleteProductsCommandHandler
    participant ProductSvc as ProductService
    participant PDLH as ProductDeletedLikeEventHandler
    participant DLCH as DeleteProductLikesCommandHandler
    participant CacheEH as CacheEvictEventHandler
    participant CacheCH as ProductCacheCommandHandler
    participant DB as Database

    Admin->>Controller: DELETE /api-admin/v1/brands/{brandId}
    Controller->>Facade: deleteBrand(brandId)

    Note over Facade: @Transactional 시작
    activate Facade
    Facade->>BrandSvc: deleteBrand(brandId)
    BrandSvc->>DB: UPDATE brand SET status=DELETED, deleted_at=now()
    BrandSvc->>Publisher: publishEvent(BrandDeletedEvent)
    Note over Publisher: AFTER_COMMIT 큐에 등록
    deactivate Facade
    Note over Facade: @Transactional 커밋

    Facade-->>Controller: OK
    Controller-->>Admin: 200 OK

    Note over BDEH: AFTER_COMMIT — 커밋 후 실행
    Publisher-->>BDEH: BrandDeletedEvent

    BDEH->>CascadeCH: CascadeDeleteProductsCommand(brandId)
    Note over CascadeCH: @Transactional(REQUIRES_NEW)
    activate CascadeCH
    CascadeCH->>ProductSvc: deleteProductsByBrandId(brandId)
    ProductSvc->>DB: UPDATE products SET status=DELETED, deleted_at=now() WHERE brand_id=brandId
    DB-->>ProductSvc: 삭제된 productId 목록
    loop 각 삭제된 상품
        CascadeCH->>Publisher: publishEvent(ProductDeletedEvent(productId))
    end
    deactivate CascadeCH

    Note over PDLH, CacheEH: 각 ProductDeletedEvent 처리 (AFTER_COMMIT)
    Publisher-->>PDLH: ProductDeletedEvent
    PDLH->>DLCH: DeleteProductLikesCommand(productId)
    Note over DLCH: TX 없음
    DLCH->>DB: DELETE product_like WHERE product_id = productId

    Publisher-->>CacheEH: ProductDeletedEvent
    CacheEH->>CacheCH: ProductCacheEvictCommand(productId)
    Note over CacheCH: TX 없음 — Redis evict
    CacheCH->>CacheCH: Redis DEL product:{productId}
    CacheCH->>CacheCH: Redis DEL product:list:*
```

#### 해석

- **이벤트 체인**: 브랜드 삭제 → `BrandDeletedEvent` → 상품 배치 소프트 삭제 → `ProductDeletedEvent` × N → 좋아요 삭제 + 캐시 evict. 각 단계가 이벤트로 분리되어 Facade가 직접 좋아요/캐시 처리 책임을 지지 않는다.
- **캐시 evict 이벤트 기반**: `BrandUpdatedEvent`, `ProductUpdatedEvent`, `ProductDeletedEvent` 모두 `CacheEvictEventHandler`가 수신하여 해당 캐시를 AFTER_COMMIT 후 즉시 evict한다. 기존 `AdminProductFacade`의 직접 evict 방식을 이벤트 기반으로 전환한 결과다.
- **좋아요 데이터 삭제**: 기존 아키텍처에서는 좋아요를 유지했으나 (3번 시퀀스 해석 참조), 이벤트 체인에서는 `DeleteProductLikesCommandHandler`가 삭제 처리한다. 마케팅 목적 유지 여부는 비즈니스 정책에 따라 결정한다.
- **TX 없는 CommandHandler**: 캐시 evict(`ProductCacheCommandHandler`)와 좋아요 삭제(`DeleteProductLikesCommandHandler`)는 트랜잭션이 필요 없어 TX 없이 실행한다.
