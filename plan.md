# Round 3 전체 구현 계획

## 개요

Round 3 설계 기준으로 전체 도메인(Catalog BC, Point, Like, Order)을 구현한다.
기존 User 도메인 위에 증분 구현하며, 각 checkpoint 단위로 `ktlintCheck` + `test` 검증 후 진행한다.

- **TDD 사이클**: Red → Green → Refactor. 모든 구현 항목을 `[RED]` 테스트 → `[GREEN]` 구현 쌍으로 구성
- **테스트 전략**: Fake Repository 기반 단위 테스트 중심. Domain Service는 Fake/Stub 주입, E2E 테스트 병행
- **아키텍처**: CatalogService(Domain Service), PointChargingService, UserFacade 등
- **DTO 변환**: 같은 BC 내 조합 → domain 레이어 데이터 클래스(`ProductDetail`), BC 간 조합 → application 레이어 Info 객체(`LikeInfo` 등)

---

## 구현 계획

### 1. Catalog BC — 도메인 모델

#### Value Objects

- [x] [RED] BrandName VO 테스트 — 빈 값 시 CoreException(BAD_REQUEST), 유효값 시 정상 생성
- [x] [GREEN] BrandName VO 구현

- [x] [RED] Price VO 테스트 — 음수 시 CoreException(BAD_REQUEST), 0 이상 정상 생성
- [x] [GREEN] Price VO 구현

- [x] [RED] Stock VO 테스트 — 음수 시 CoreException, decrease/increase 동작, 재고 부족 시 예외
- [x] [GREEN] Stock VO 구현

#### Brand Entity

- [x] [RED] Brand Entity 테스트 — 생성, update(name), guard()에서 BrandName VO 검증
- [x] [GREEN] Brand Entity 구현 — BaseEntity 상속, name 필드, update(name), guard() override

#### Product Entity

- [x] ProductStatus inner enum — ON_SALE, SOLD_OUT, HIDDEN

- [x] [RED] Product Entity 생성 테스트 — guard()에서 Price/Stock VO 검증, 초기 status 결정 (stock > 0 → ON_SALE, stock == 0 → SOLD_OUT)
- [x] [GREEN] Product Entity 구현 — refBrandId, name, price, stock, status, likeCount, guard() override

- [x] [RED] Product 상태 전이 테스트 — decreaseStock → SOLD_OUT, increaseStock → ON_SALE, HIDDEN 상태에서 재고 변동 시 상태 유지
- [x] [GREEN] Product 상태 전이 구현 — adjustStatusByStock() private 메서드

- [x] [RED] Product.update() 테스트 — HIDDEN 명시 시 자동 전이보다 우선
- [x] [GREEN] Product.update() 구현

- [x] [RED] Product likeCount 테스트 — increaseLikeCount(), decreaseLikeCount()
- [x] [GREEN] Product likeCount 구현

- [x] Product.isDeleted() — deletedAt != null 여부

#### 기타

- [x] ProductSort enum — latest, price_asc, likes_desc (상품 목록 동적 정렬)
- [x] CatalogCommand — CreateBrand, UpdateBrand, CreateProduct, UpdateProduct

### 2. Catalog BC — 저장소

- [x] BrandRepository 인터페이스 정의 (domain 레이어)
- [x] BrandJpaRepository + BrandRepositoryImpl (infrastructure 레이어)
- [x] FakeBrandRepository 구현 (테스트용 인메모리)

- [x] ProductRepository 인터페이스 정의 — findActiveProducts(페이징/정렬/필터), findAllByBrandId 등
- [x] ProductJpaRepository + ProductRepositoryImpl
- [x] Querydsl 동적 쿼리 — brandId 필터(nullable) + ProductSort 기반 정렬 + 페이징
- [x] FakeProductRepository 구현 (테스트용 인메모리)

--- checkpoint: Catalog BC 도메인 모델 + 저장소 (`ktlintCheck` + `test`) ---

### 3. Catalog BC — CatalogService

- [x] ProductDetail 데이터 클래스 — domain/catalog/ 패키지, Product + Brand 조합 결과

- [x] [RED] CatalogService Brand 생성/수정 테스트 — 정상 생성, 정상 수정, 삭제된 브랜드 수정 시 NOT_FOUND
- [x] [GREEN] createBrand(command), updateBrand(brandId, command) 구현

- [x] [RED] CatalogService Brand 삭제 테스트 — soft delete + 소속 상품 cascade soft delete, 멱등 처리
- [x] [GREEN] deleteBrand(brandId) 구현

- [x] [RED] CatalogService Brand 조회 테스트 — getActiveBrand(삭제 시 NOT_FOUND), getBrand(삭제 포함), getBrands(페이징)
- [x] [GREEN] getActiveBrand, getBrand, getBrands 구현

- [x] [RED] CatalogService Product 생성 테스트 — 정상 생성, 브랜드 유효성 검증 (미존재/삭제 시 NOT_FOUND)
- [x] [GREEN] createProduct(command) 구현

- [x] [RED] CatalogService Product 수정/삭제 테스트 — 수정 성공, 브랜드 변경 불가, 삭제 멱등
- [x] [GREEN] updateProduct, deleteProduct 구현

- [x] [RED] CatalogService 대고객 조회 테스트 — getProducts(삭제/HIDDEN 제외, 정렬/필터/페이징), getProductDetail(ProductDetail 반환, Product + Brand 조합)
- [x] [GREEN] getProducts, getProductDetail 구현

- [x] [RED] CatalogService 내부용 조회 테스트 — getProduct(삭제 포함), getActiveProduct(삭제/HIDDEN 시 NOT_FOUND), getActiveProductsByIds(삭제/HIDDEN 제외 일괄 조회), getProductsForOrder(존재 + ON_SALE 검증)
- [x] [GREEN] getProduct, getActiveProduct, getActiveProductsByIds, getProductsForOrder 구현

- [x] [RED] CatalogService 재고 차감 테스트 — decreaseStocks 성공, 부족 시 CoreException
- [x] [GREEN] decreaseStocks 구현

- [x] [RED] CatalogService likeCount 테스트 — increaseLikeCount, decreaseLikeCount
- [x] [GREEN] increaseLikeCount, decreaseLikeCount 구현

- [x] [RED] CatalogService 어드민 조회 테스트 — getAdminProducts(삭제 포함), getAdminProduct(삭제 포함)
- [x] [GREEN] getAdminProducts, getAdminProduct 구현

--- checkpoint: CatalogService (`ktlintCheck` + `test`) ---

### 4. Point 도메인 — 모델 + 저장소

#### Value Objects

- [x] [RED] Point VO 테스트 — 음수 시 CoreException, plus/minus 연산, 음수 결과 시 예외
- [x] [GREEN] Point VO 구현

#### Entities

- [x] [RED] UserPoint Entity 테스트 — charge(잔액 증가), use(잔액 차감, 부족 시 CoreException), canAfford, guard()에서 Point VO 검증
- [x] [GREEN] UserPoint Entity 구현 — BaseEntity 상속, refUserId(unique), balance(Long)

- [x] PointHistoryType enum — CHARGE, USE
- [x] PointHistory Entity — BaseEntity 미상속 (id + refUserPointId + type + amount + refOrderId + createdAt)

#### 저장소

- [x] UserPointRepository 인터페이스 + UserPointJpaRepository + UserPointRepositoryImpl
- [x] FakeUserPointRepository 구현 (테스트용 인메모리)
- [x] PointHistoryRepository 인터페이스 + PointHistoryJpaRepository + PointHistoryRepositoryImpl
- [x] FakePointHistoryRepository 구현 (테스트용 인메모리)

#### Command

- [x] PointCommand — Charge (amount 필드)

### 5. Point 도메인 — 서비스

- [x] [RED] UserPointService 테스트 — createUserPoint(초기 잔액 0), getBalance(없으면 NOT_FOUND), usePoints(userId, amount, refOrderId)(차감 + PointHistory USE 기록 + refOrderId 저장, 부족 시 CoreException에 필요/현재 잔액 명시)
- [x] [GREEN] UserPointService 구현

- [x] [RED] PointChargingService 테스트 — charge 성공(잔액 변경 + PointHistory CHARGE 기록), 충전 금액 0 이하 시 CoreException(BAD_REQUEST)
- [x] [GREEN] PointChargingService 구현

--- checkpoint: Point 도메인 (`ktlintCheck` + `test`) ---

### 6. 인증 인프라 + 회원가입 변경

- [x] Constants — HEADER_LDAP 상수 추가

- [x] [RED] AdminInterceptor 테스트 — 유효한 LDAP 헤더 시 통과, 누락/불일치 시 401
- [x] [GREEN] AdminInterceptor 구현 — X-Loopers-Ldap: loopers.admin 헤더 검증

- [x] WebMvcConfig — AdminInterceptor 등록 (/api-admin/**), AuthInterceptor 경로 확장 (likes, orders, points)

- [x] [RED] UserFacade 테스트 — signUp 시 User 생성 + UserPoint(잔액 0) 생성
- [x] [GREEN] UserFacade 구현 — UserService.signUp + UserPointService.createUserPoint

- [x] UserV1Controller — signUp에서 UserFacade 사용하도록 변경

--- checkpoint: 인증 + 회원가입 변경 (`ktlintCheck` + `test`) ---

### 7. Catalog API (Controller + DTO)

- [x] BrandV1Dto — 대고객 응답 DTO (Brand 엔티티 → Dto 변환)
- [x] BrandV1ApiSpec + BrandV1Controller (GET /api/v1/brands/{brandId})

- [x] BrandAdminV1Dto — 어드민 요청/응답 DTO
- [x] BrandAdminV1ApiSpec + BrandAdminV1Controller (CRUD + 목록)

- [x] CustomerProductDto, ProductDetailDto — 대고객 응답 DTO (ProductDetail(domain) → Dto 변환)
- [x] ProductV1ApiSpec + ProductV1Controller (목록 + 상세)

- [x] AdminProductDto — 어드민 응답 DTO (stock, deletedAt 포함)
- [x] ProductAdminV1ApiSpec + ProductAdminV1Controller (CRUD + 목록)

- [x] http/catalog.http — Brand/Product API 정상+에러 케이스

--- checkpoint: Catalog API (`ktlintCheck` + `test`) ---

### 8. Point API

- [x] PointV1Dto — 충전 요청, 잔액 응답 DTO
- [x] PointV1ApiSpec + PointV1Controller (충전 + 잔액 조회)

- [x] http/point.http — 포인트 충전/조회 API 정상+에러 케이스

--- checkpoint: Point API (`ktlintCheck` + `test`) ---

### 9. Like 도메인 — 전체

#### 엔티티 + 저장소

- [x] Like Entity — id, refUserId, refProductId (@Entity, unique constraint, BaseEntity 미상속)
- [x] LikeRepository 인터페이스 + LikeJpaRepository + LikeRepositoryImpl
- [x] FakeLikeRepository 구현 (테스트용 인메모리)

#### 서비스

- [x] [RED] LikeService 테스트 — addLike(멱등, 이미 존재 시 false 반환), removeLike(멱등, 없으면 false, Hard Delete), getLikesByUserId(id 역순 정렬)
- [x] [GREEN] LikeService 구현

- [x] [RED] LikeFacade 테스트 — addLike(상품 유효성 + 좋아요 + 조건부 likeCount 증가), removeLike(삭제 포함 상품 조회 + 좋아요 취소 + 삭제된 상품이면 likeCount 미갱신), getLikes(Like + Product 조합 → LikeInfo)
- [x] [GREEN] LikeFacade 구현 — LikeInfo는 application/like/ 패키지 (cross-domain 조합)

#### API

- [x] LikeV1Dto + LikeV1ApiSpec + LikeV1Controller (등록/취소/목록)
- [x] http/like.http — 좋아요 등록/취소/목록 API 정상+에러 케이스

--- checkpoint: Like 도메인 (`ktlintCheck` + `test`) ---

### 10. Order 도메인 — 전체

#### Value Object

- [x] [RED] Quantity VO 테스트 — 0 이하 시 CoreException(BAD_REQUEST), 1 이상 정상 생성
- [x] [GREEN] Quantity VO 구현

#### 엔티티

- [x] OrderStatus inner enum — CREATED, PAID, CANCELLED, FAILED

- [x] [RED] OrderItem Entity 테스트 — create 정적 팩토리(스냅샷 복사), Quantity VO 검증
- [x] [GREEN] OrderItem Entity 구현 — BaseEntity 상속, refProductId, productName, productPrice, quantity

- [x] [RED] Order Entity 테스트 — create 정적 팩토리(OrderItems 생성 + totalPrice 계산), 빈 items 예외, 중복 productId 예외
- [x] [GREEN] Order Entity 구현 — BaseEntity 상속, refUserId, status, totalPrice, items (@OneToMany cascade)

#### Command + 저장소

- [x] OrderCommand — CreateOrder(items: List\<CreateOrderItem\>), CreateOrderItem(productId, quantity)
- [x] OrderRepository 인터페이스 + OrderJpaRepository + OrderRepositoryImpl — 기간 조회 (userId + createdAt 범위)
- [x] FakeOrderRepository 구현 (테스트용 인메모리)

#### 서비스

- [x] [RED] OrderService 테스트 — createOrder(저장), getOrder(소유권 검증, 미소유 시 NOT_FOUND), getOrdersByUserId(기간 필터), 어드민 조회(소유권 검증 없음)
- [x] [GREEN] OrderService 구현

- [x] [RED] OrderFacade 테스트 — 정상 주문(상품 검증 + 재고 차감 + 스냅샷 + 포인트 차감), 재고 부족 시 전체 실패, 포인트 부족 시 전체 실패, 중복 productId 검증
- [x] [GREEN] OrderFacade 구현 — 상품 조회 → 재고 차감 → 주문 생성(Order.create()가 totalPrice 내부 계산) → 포인트 차감(order.totalPrice, orderId 전달) (all-or-nothing)

#### API

- [x] OrderV1Dto + OrderV1ApiSpec + OrderV1Controller (생성 + 목록 + 상세)
- [x] 주문 목록 시간대 처리 — Controller에서 ZonedDateTime.parse로 ISO 8601 문자열 변환
- [x] OrderAdminV1Dto + OrderAdminV1ApiSpec + OrderAdminV1Controller (목록 + 상세)
- [x] http/order.http — 주문 생성/목록/상세 API 정상+에러 케이스

--- checkpoint: Order 도메인 (`ktlintCheck` + `test`) ---

### 11. E2E 테스트

- [x] [RED] E2E 주문 생성 테스트 — 회원가입 → 포인트 충전 → 상품 등록 → 주문 생성 → 재고/포인트 차감 검증
- [x] [GREEN] E2E 주문 생성 통과 (Docker 환경 필요, 코드 작성 완료)

- [x] [RED] E2E 포인트 충전 테스트 — 회원가입 → 충전 → 잔액 확인
- [x] [GREEN] E2E 포인트 충전 통과 (Docker 환경 필요, 코드 작성 완료)

- [x] [RED] E2E 좋아요 멱등성 테스트 — 좋아요 등록 2회 → likeCount 1만 증가, 취소 2회 → likeCount 1만 감소
- [x] [GREEN] E2E 좋아요 멱등성 통과 (Docker 환경 필요, 코드 작성 완료)

--- checkpoint: 전체 통합 검수 (`ktlintCheck` + `test`) ---

### 12. 코드 리뷰 피드백 처리

> 멱등성(usePoints, chargePoints) 및 동시성 제어(decreaseStocks 락)는 `docs/note/round3-decisions.md` 7절에 기록 완료, 이번 범위 제외.

#### 12-1. Validation 인프라 (구조적 변경)

기존에 `MethodArgumentNotValidException`과 `ConstraintViolationException` 핸들러가 없어,
Bean Validation 어노테이션을 추가해도 400이 아닌 500이 반환된다. 행위 변경 전에 구조를 먼저 정비한다.

- [ ] ApiControllerAdvice에 `MethodArgumentNotValidException` 핸들러 추가 (400, 필드별 에러 메시지)
- [ ] ApiControllerAdvice에 `ConstraintViolationException` 핸들러 추가 (400, 파라미터별 에러 메시지)

--- checkpoint: Validation 인프라 검수 (`ktlintCheck` + `test`, 기존 테스트 전부 통과 확인) ---

#### 12-2. [P-A] OrderV1Dto Bean Validation

- [ ] [P-A] [RED] 빈 주문 항목(`items=[]`) 요청 시 400 반환 E2E 테스트
- [ ] [P-A] [RED] 상품 ID 0 이하(`productId=0`) 요청 시 400 반환 E2E 테스트
- [ ] [P-A] [RED] 수량 0 이하(`quantity=0`) 요청 시 400 반환 E2E 테스트
- [ ] [P-A] [GREEN] `CreateOrderRequest.items`에 `@field:NotEmpty` + `@field:Valid` 추가, `CreateOrderItemRequest.productId`에 `@field:Positive`, `quantity`에 `@field:Min(1)` 추가

#### 12-3. [P-B] ProductV1Controller 페이징 검증 + sort 타입 변경

- [ ] [P-B] [RED] `page=-1` 요청 시 400 반환 E2E 테스트
- [ ] [P-B] [RED] `size=0` 또는 `size=1000` 요청 시 400 반환 E2E 테스트
- [ ] [P-B] [GREEN] `ProductV1Controller`에 `@Validated` 추가, `page`에 `@Min(0)`, `size`에 `@Min(1)` `@Max(100)` 추가
- [ ] [P-B] [RED] 잘못된 sort 값(`"INVALID"`) 요청 시 400 반환 E2E 테스트 (현재 500 반환)
- [ ] [P-B] [GREEN] `sort` 파라미터 `String` → `ProductSort` 타입 변경 (`ProductV1ApiSpec` + `ProductV1Controller`), 수동 `valueOf()` 제거

--- checkpoint: Validation 검수 (`ktlintCheck` + `test`) ---

#### 12-4. 리팩토링 (구조적 변경)

- [ ] [P-C] `PointV1Controller` `.let` 체인을 명시적 변수로 단순화 (`chargePoints`, `getBalance` 2개 메서드)
- [ ] [P-D] `FakeProductRepository.save()`에서 `createdAt`/`updatedAt` 미초기화 시 현재 시각을 리플렉션으로 설정 (`@PrePersist` 동작 모방)
- [ ] [P-D] `FakeProductRepository` LATEST 정렬 기준을 `it.id` → `it.createdAt` 으로 수정

--- checkpoint: 리팩토링 검수 (`ktlintCheck` + `test`) ---

#### 12-5. Like HTTP 테스트 보강

- [ ] `like.http` 파일 상단에 `{{commerce-api}}` 변수 정의 주석 추가 (`http-client.env.json`의 `local` 환경 참조)
- [ ] 중복 좋아요 등록 케이스 추가 — 이미 좋아요한 상품에 재등록 시 200 OK (멱등 처리, likeCount 미증가)
- [ ] 좋아요하지 않은 상품 삭제 케이스 추가 — 좋아요 기록 없는 상품 삭제 시 200 OK (멱등 처리)
- [ ] 각 테스트 케이스에 예상 응답 본문 예시 주석 추가

> **범위 외 기록**: 좋아요 목록 페이징은 현재 API가 `List`를 반환하므로 `Page` 전환이 선행되어야 한다. 다른 사용자 좋아요 조회는 `@AuthUser`가 본인 조회만 허용하므로 별도 권한 검증 불필요.

--- checkpoint: 전체 리뷰 피드백 처리 검수 (`ktlintCheck` + `test`) ---
