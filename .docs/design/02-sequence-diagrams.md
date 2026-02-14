# 02. 시퀀스 다이어그램

## 개요

주요 UseCase의 호출 흐름을 시퀀스 다이어그램으로 표현합니다.
각 다이어그램에서 **책임 객체**(Controller, Facade, Service, Repository)가 명확히 드러나도록 설계합니다.

---

## 0. 에러 전파 흐름 (공통)

도메인/서비스에서 발생하는 `CoreException`이 클라이언트까지 전달되는 공통 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Domain as Domain Layer<br/>(Service / Model)
    participant Facade as Application Layer<br/>(Facade)
    participant Advice as ApiControllerAdvice
    participant Client

    Note over Domain: 비즈니스 규칙 위반 감지

    Domain->>Domain: throw CoreException(ErrorType)

    Note over Domain,Facade: 예외가 @Transactional 경계를 통과<br/>→ 트랜잭션 롤백

    Domain-->>Facade: CoreException 전파
    Facade-->>Advice: CoreException 전파

    Note over Advice: @ExceptionHandler(CoreException::class)

    Advice->>Advice: ErrorType에서 status, code, message 추출
    Advice->>Advice: ErrorResponse(code, message) 생성

    Advice-->>Client: HTTP {status}<br/>ApiResponse(ERROR, ErrorResponse)
```

**설계 포인트:**
- `CoreException`은 도메인/서비스 어디서든 발생 가능
- `ErrorType` enum이 HTTP 상태코드, 에러 코드, 메시지를 모두 캡슐화
- Facade의 `@Transactional`이 예외 시 자동 롤백 보장
- Controller는 예외를 잡지 않음 — `ApiControllerAdvice`가 전역 처리

---

## 1. 브랜드 등록 (Brand Registration)

브랜드명 검증 → 중복 확인 → 브랜드 생성 → 저장의 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as BrandV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as BrandFacade
    participant Register as BrandRegister
    participant Repository as BrandRepository

    Client->>Controller: POST /api/v1/brands<br/>(headers + RegisterRequest)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Controller: request.toCommand()
    Controller->>Facade: register(command)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Register: register(name)

    Note over Register: VO 생성은 도메인 서비스에서

    Register->>Register: BrandName(name)

    Register->>Repository: existsByName(brandName)
    Repository-->>Register: false

    alt 이미 존재하는 브랜드명
        Register-->>Register: throw CoreException(DUPLICATE_BRAND_NAME)
    end

    Register->>Register: Brand(name, ACTIVE)
    Register->>Repository: save(brand)
    Repository-->>Register: Brand (id 할당됨)
    Register-->>Facade: Brand

    Facade->>Facade: BrandInfo.Detail.from(brand)
    Facade-->>Controller: BrandInfo.Detail
    deactivate Facade

    Controller->>Controller: BrandV1Dto.RegisterResponse.from(info)
    Controller-->>Client: ApiResponse<RegisterResponse>
```

**책임 분배:**
- `Controller`: HTTP 매핑, 인증 위임, Dto↔Command 변환
- `BrandFacade`: 트랜잭션 경계, Domain→Info 변환
- `BrandRegister`: VO 생성, 중복명 검증, 브랜드 생성/저장

---

## 2. 상품 등록 (Product Registration)

브랜드 존재 검증 → 상품 생성 → 저장의 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as ProductV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as ProductFacade
    participant Register as ProductRegister
    participant BrandReader as BrandReader
    participant Repository as ProductRepository

    Client->>Controller: POST /api/v1/products<br/>(headers + RegisterRequest)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Controller: request.toCommand()
    Controller->>Facade: register(command)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Register: register(brandId, name, price, description)

    Note over Register: VO 생성은 도메인 서비스에서

    Register->>Register: ProductName(name)
    Register->>Register: ProductPrice(price)
    Register->>Register: ProductDescription(description)

    Register->>BrandReader: getById(brandId)
    BrandReader->>BrandReader: 브랜드 존재 + ACTIVE 검증
    BrandReader-->>Register: Brand

    Register->>Register: Product(brandId, name, price, description, SELLING)
    Register->>Repository: save(product)
    Repository-->>Register: Product (id 할당됨)
    Register-->>Facade: Product

    Facade->>Facade: ProductInfo.Detail.from(product)
    Facade-->>Controller: ProductInfo.Detail
    deactivate Facade

    Controller->>Controller: ProductV1Dto.RegisterResponse.from(info)
    Controller-->>Client: ApiResponse<RegisterResponse>
```

**책임 분배:**
- `Controller`: HTTP 매핑, 인증 위임, Dto↔Command 변환
- `AuthFacade`: 헤더 기반 인증
- `ProductFacade`: 트랜잭션 경계, UseCase 조합, Domain→Info 변환
- `ProductRegister`: VO 생성, 브랜드 검증, 상품 생성/저장
- `BrandReader`: 브랜드 조회 + 존재 검증

### 인프라스트럭처 레이어 상세

도메인 서비스의 `save()` 호출이 인프라 레이어에서 어떻게 처리되는지 상세 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Register as ProductRegister
    participant Repository as ProductRepository
    participant RepoImpl as ProductRepositoryImpl
    participant Mapper as ProductMapper
    participant JpaRepo as ProductJpaRepository
    participant Entity as ProductEntity

    Register->>Repository: save(product)

    Note over Repository,RepoImpl: Interface → Implementation<br/>(DIP)

    RepoImpl->>RepoImpl: resolveEntity(product)

    alt product.id == null (신규)
        RepoImpl->>Mapper: toEntity(product)
        Mapper-->>RepoImpl: ProductEntity (new)
    else product.id != null (수정)
        RepoImpl->>JpaRepo: getReferenceById(product.id)
        JpaRepo-->>RepoImpl: ProductEntity (proxy)
        RepoImpl->>Mapper: update(entity, product)
        Note over Mapper: entity의 필드를 domain 값으로 갱신
    end

    RepoImpl->>JpaRepo: save(entity)

    Note over JpaRepo: @PrePersist / @PreUpdate<br/>→ createdAt, updatedAt 자동 설정

    JpaRepo-->>RepoImpl: ProductEntity (id 할당)
    RepoImpl->>Mapper: toDomain(entity)
    Mapper-->>RepoImpl: Product (domain model)
    RepoImpl-->>Register: Product
```

**설계 포인트:**
- `resolveEntity()` 패턴: 신규(INSERT)와 수정(UPDATE)을 하나의 `save()` 메서드로 통합
- `getReferenceById()`: 프록시 로딩으로 불필요한 SELECT 방지, dirty checking 활용
- `Mapper`: Domain ↔ Entity 양방향 변환 책임 전담

---

## 3. 주문 생성 (Order Creation)

상품 검증 → 스냅샷 생성 → 총 금액 계산 → 주문 저장의 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as OrderV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as OrderFacade
    participant Register as OrderRegister
    participant ProductReader as ProductReader
    participant Repository as OrderRepository

    Client->>Controller: POST /api/v1/orders<br/>(headers + CreateOrderRequest)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Controller: request.toCommand(memberId)
    Controller->>Facade: createOrder(command)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Register: register(memberId, orderItemCommands)

    Note over Register: 주문 상품이 1개 이상인지 검증

    loop 각 주문 상품에 대해
        Register->>ProductReader: getById(productId)
        ProductReader->>ProductReader: 상품 존재 + SELLING 검증
        ProductReader-->>Register: Product

        Note over Register: 상품 스냅샷 생성<br/>OrderItem(productId, product.name, product.price, quantity)
    end

    Register->>Register: 총 금액 계산<br/>SUM(price * quantity)
    Register->>Register: Order(memberId, ORDERED, orderItems, totalPrice)
    Register->>Repository: save(order)
    Repository-->>Register: Order (id 할당됨)
    Register-->>Facade: Order

    Facade->>Facade: OrderInfo.Detail.from(order)
    Facade-->>Controller: OrderInfo.Detail
    deactivate Facade

    Controller->>Controller: OrderV1Dto.CreateOrderResponse.from(info)
    Controller-->>Client: ApiResponse<CreateOrderResponse>
```

**책임 분배:**
- `Controller`: HTTP 매핑, 인증, memberId 주입
- `OrderFacade`: 트랜잭션 경계 (Order + OrderItem 단일 트랜잭션)
- `OrderRegister`: 상품 검증, 스냅샷 생성, 금액 계산, 주문 생성
- `ProductReader`: 상품 존재/상태 검증 (다른 Aggregate 조회)

**설계 포인트:**
- OrderItem은 Order와 같은 Aggregate이므로 단일 트랜잭션에서 함께 저장
- Product는 다른 Aggregate이므로 ID 참조 + 스냅샷 복사

---

## 4. 좋아요 등록 (Like Registration)

중복 체크 → 상품 검증 → 좋아요 저장의 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as LikeV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as LikeFacade
    participant Register as LikeRegister
    participant ProductReader as ProductReader
    participant Repository as LikeRepository

    Client->>Controller: POST /api/v1/likes<br/>(headers + LikeRequest)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Controller: request.toCommand(memberId)
    Controller->>Facade: register(command)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Register: register(memberId, productId)

    Register->>Repository: existsByMemberIdAndProductId(memberId, productId)
    Repository-->>Register: false

    alt 이미 좋아요한 상품
        Register-->>Register: throw CoreException(ALREADY_LIKED)
    end

    Register->>ProductReader: getById(productId)
    ProductReader->>ProductReader: 상품 존재 + SELLING 검증
    ProductReader-->>Register: Product

    Register->>Register: Like(memberId, productId)
    Register->>Repository: save(like)
    Repository-->>Register: Like (id 할당됨)
    Register-->>Facade: Like

    Facade-->>Controller: (void 또는 LikeInfo)
    deactivate Facade

    Controller-->>Client: ApiResponse<Void>
```

---

## 5. 좋아요 목록 조회 (Like List)

내 좋아요 목록을 조회하며, 크로스 도메인 데이터를 Facade에서 조합하는 핵심 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as LikeV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as LikeFacade
    participant LikeReader as LikeReader
    participant ProductReader as ProductReader
    participant BrandReader as BrandReader

    Client->>Controller: GET /api/v1/likes/me<br/>(headers)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Facade: getMyLikes(memberId)
    activate Facade

    Note over Facade: @Transactional(readOnly = true)

    Facade->>LikeReader: getAllByMemberId(memberId)
    LikeReader-->>Facade: List<Like>

    Note over Facade: 크로스 도메인 데이터 조합<br/>(Facade의 본업)

    Facade->>ProductReader: getAllByIds(productIds)
    ProductReader-->>Facade: List<Product>

    Facade->>BrandReader: getAllByIds(brandIds)
    BrandReader-->>Facade: List<Brand>

    Facade->>Facade: Like + Product + Brand 조합<br/>LikeInfo.Detail 리스트 생성

    Facade-->>Controller: List<LikeInfo.Detail>
    deactivate Facade

    Controller->>Controller: LikeV1Dto.LikeListResponse.from(infos)
    Controller-->>Client: ApiResponse<LikeListResponse>
```

**설계 포인트 (ADR: 크로스 도메인 데이터 조합):**

| 대안 | 핵심 | 얻는 것 | 잃는 것 |
|------|------|---------|---------|
| A. Repository JOIN | Like + Product + Brand 한 번에 조회 | 쿼리 1회 | 도메인 경계 파괴, Aggregate 독립성 ↓ |
| B. Facade에서 조합 | 각 Reader를 호출하여 조합 | 도메인 경계 존중, 재사용성 | N+1 가능성 (batch로 해결) |
| C. CQRS Read Model | 별도 조회 전용 모델 | 성능 최적 | 과도한 복잡성 |

**결정: B (Facade 조합)** — Use Case 조합이 Facade의 본업. `getAllByIds()` 메서드로 배치 조회하여 N+1 방지. 현재 규모에서 CQRS는 오버엔지니어링.

---

## 6. 브랜드 삭제 — Restrict 정책 (Brand Deactivation)

활성 상품 존재 여부 검증 → 비활성화의 흐름입니다.
Restrict 정책으로 인해 **상품이 있으면 삭제가 거부되는** 흐름이 핵심입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as BrandV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as BrandFacade
    participant Remover as BrandRemover
    participant BrandReader as BrandReader
    participant ProductReader as ProductReader
    participant BrandRepo as BrandRepository

    Client->>Controller: DELETE /api/v1/brands/{id}<br/>(headers)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Facade: remove(brandId)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Remover: remove(brandId)

    Remover->>BrandReader: getById(brandId)
    BrandReader-->>Remover: Brand

    alt 이미 INACTIVE
        Remover-->>Remover: throw CoreException(BRAND_ALREADY_INACTIVE)
    end

    Remover->>ProductReader: existsSellingByBrandId(brandId)
    ProductReader-->>Remover: true/false

    alt 판매 중인 상품 존재
        Remover-->>Remover: throw CoreException(BRAND_HAS_ACTIVE_PRODUCTS)
    end

    Note over Remover: brand.deactivate()

    Remover->>BrandRepo: save(brand)
    BrandRepo-->>Remover: Brand (status=INACTIVE)
    Remover-->>Facade: (void)

    Facade-->>Controller: (void)
    deactivate Facade

    Controller-->>Client: ApiResponse<Void>
```

**설계 포인트 (ADR: 크로스 도메인 의존):**

> **위화감**: BrandRemover가 다른 도메인의 데이터를 참조해야 한다.
> 어떻게 도메인 경계를 존중하면서 검증할 것인가?

| 대안 | 핵심 | 얻는 것 | 잃는 것 |
|------|------|---------|---------|
| A. ProductRepository 직접 참조 | BrandRemover → ProductRepository | 단순한 구현 | 도메인 간 결합 |
| B. ProductReader 서비스를 통해 간접 참조 | BrandRemover → ProductReader | 도메인 경계 존중 | 한 단계 간접 호출 추가 |
| C. 이벤트 기반 검증 | Brand 삭제 이벤트 → Product가 검증 | 완전한 분리 | 과도한 복잡성 |

**결정: B (ProductReader를 통한 간접 참조)** — 같은 application 내에서 다른 도메인 서비스를 통해 조회하는 것이 도메인 경계를 존중하면서도 적절한 복잡도를 유지한다.

---

## 7. 주문 취소 (Order Cancellation)

소유권 검증 → 상태 검증 → 취소의 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller as OrderV1Controller
    participant AuthFacade as AuthFacade
    participant Facade as OrderFacade
    participant Canceller as OrderCanceller
    participant Repository as OrderRepository

    Client->>Controller: PATCH /api/v1/orders/{id}/cancel<br/>(headers)
    Controller->>AuthFacade: authenticate(loginId, password)
    AuthFacade-->>Controller: 인증 성공

    Controller->>Facade: cancel(orderId, memberId)
    activate Facade

    Note over Facade: @Transactional

    Facade->>Canceller: cancel(orderId, memberId)

    Canceller->>Repository: findById(orderId)
    Repository-->>Canceller: Order

    alt 주문이 존재하지 않음
        Canceller-->>Canceller: throw CoreException(ORDER_NOT_FOUND)
    end

    alt 본인의 주문이 아님
        Canceller-->>Canceller: throw CoreException(ORDER_NOT_OWNER)
    end

    Note over Canceller: order.cancel()

    alt 이미 CANCELLED 상태
        Canceller-->>Canceller: throw CoreException(ORDER_ALREADY_CANCELLED)
    end

    Canceller->>Repository: save(order)
    Repository-->>Canceller: Order (status=CANCELLED)
    Canceller-->>Facade: (void)

    Facade-->>Controller: (void)
    deactivate Facade

    Controller-->>Client: ApiResponse<Void>
```

**설계 포인트:**
- `order.cancel()` 행위 메서드 내에서 상태 검증을 수행 — 도메인 모델이 자신의 비즈니스 규칙을 지킨다
- 소유권 검증(`memberId` 일치)은 도메인 서비스에서 수행
