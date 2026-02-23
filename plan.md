# Round 3 — Architecture Migration & Quality Improvement

## 개요

핵심 목표: **Domain Model과 JPA Entity의 완벽 분리**.
현재 Domain Model이 JPA Entity를 겸하는 구조를 순수 POJO(Domain)와 JPA Entity(Infrastructure)로 분리한다.
Value Object를 적극 도입하고, Facade를 UseCase 패턴으로 전환한다.
아키텍처 마이그레이션(Phase 0) 완료 후, 새 구조 위에서 버그 수정과 테스트를 추가한다(Phase 1).

### 설계 원칙

- **Tidy First**: 구조적 변경(Phase 0)을 행위적 변경(Phase 1)보다 먼저 수행
- **기존 테스트가 안전망**: 각 리팩토링 단계 후 기존 테스트 전체 통과 검증
- **도메인별 순차 마이그레이션**: 한 도메인씩 분리하여 리스크 최소화

### 마이그레이션 패턴 (모든 도메인 공통)

1. Domain Model → 순수 POJO (JPA 애노테이션 제거, BaseEntity 상속 해제, VO 적용)
2. XxxEntity 생성 (infrastructure, BaseEntity 상속, `fromDomain()`/`toDomain()` 매핑)
3. XxxRepositoryImpl에 XxxJpaRepository `internal interface`로 통합 + Entity 매핑 적용
4. 별도 XxxJpaRepository.kt 파일 삭제
5. FakeXxxRepository 업데이트 (순수 Domain Model 저장, id 할당 로직 조정)
6. Service/Command/Controller/Dto 수정

### 설계 결정

| 결정               | 선택                        | 근거                                                                     |
|------------------|---------------------------|------------------------------------------------------------------------|
| 가격 VO 네이밍        | `Money` (`domain.common`) | 범용적 금액 표현. Product, OrderItem, OrderProductInfo 공유                     |
| VO 선언 방식         | `@JvmInline value class`  | 단일 값 래핑 + 타입 안전성. Money, Email, LoginId, Name, BrandName, Stock, Point |
| Password VO      | 일반 `class` 유지             | 생성자에 `birthDate` 의존 → `@JvmInline` 불가                                  |
| Domain Model 패키지 | `domain/xxx/entity/` 유지   | DDD Entity ≠ JPA Entity. 기존 import 경로 변경 최소화                           |
| Facade → UseCase | 단일 책임 UseCase 클래스         | Facade 해체 → RegisterUserUseCase, PlaceOrderUseCase 등                   |

---

## Phase 0: Architecture Migration

### CP0-1: 공통 기반 준비 — Value Object 생성/전환

**신규 생성:**

- [x] [REFACTOR] `Money` VO 생성 (`domain/common/Money.kt`, `@JvmInline value class`, `BigDecimal` 래핑, 음수 검증)
- [x] [REFACTOR] `Email` VO 생성 (`domain/user/vo/Email.kt`, `@JvmInline value class`, 정규식 검증)
- [x] [REFACTOR] `BrandName` VO 생성 (`domain/catalog/brand/vo/BrandName.kt`, `@JvmInline value class`, 빈 문자열 검증)

**기존 VO → `@JvmInline value class` 전환:**

- [x] [REFACTOR] `LoginId` → `@JvmInline value class` 전환 (기존 정규식 검증 유지)
- [x] [REFACTOR] `Name` → `@JvmInline value class` 전환 (`masked()` 메서드 유지)
- [x] [REFACTOR] `Stock` → `@JvmInline value class` 전환 (`decrease()`/`increase()` 메서드 유지)
- [x] [REFACTOR] `Point` → `@JvmInline value class` 전환 (`plus()`/`minus()` 메서드 유지)

--- checkpoint: VO 생성/전환 완료. 기존 코드에 미적용 상태이므로 빌드 영향 없음. ---

### CP0-2: Catalog 도메인 분리 (Brand + Product)

**Brand:**

- [x] [REFACTOR] Brand Domain Model → 순수 POJO (JPA 애노테이션 제거, BaseEntity 상속 해제, `BrandName` VO 적용)
- [x] [REFACTOR] `BrandEntity` 생성 (`infrastructure/catalog/brand/`, BaseEntity 상속, `fromDomain()`/`toDomain()`)
- [x] [REFACTOR] `BrandRepositoryImpl`에 `BrandJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `BrandJpaRepository.kt` 삭제
- [x] [REFACTOR] `FakeBrandRepository` 업데이트

**Product:**

- [x] [REFACTOR] Product Domain Model → 순수 POJO (JPA 제거, `Money` VO 적용, `Stock` VO 유지)
- [x] [REFACTOR] `ProductEntity` 생성 (`infrastructure/catalog/product/`, BaseEntity 상속, `fromDomain()`/`toDomain()`,
  `Money`↔`BigDecimal` 매핑)
- [x] [REFACTOR] `ProductRepositoryImpl`에 `ProductJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `ProductJpaRepository.kt` 삭제
- [x] [REFACTOR] `FakeProductRepository` 업데이트

**연쇄 수정:**

- [x] [REFACTOR] `CatalogService` 수정 (VO 타입 대응, 명시적 save 추가)
- [x] [REFACTOR] `CatalogCommand` 수정 (`Money`/`BrandName` 타입 적용)
- [x] [REFACTOR] Catalog Controller/Dto 수정 (VO↔원시타입 변환)

--- checkpoint: Catalog 도메인 분리 완료. ktlintCheck + test 검수 ---

### CP0-3: User 도메인 분리

- [x] [REFACTOR] User Domain Model → 순수 POJO (JPA 제거, `LoginId`/`Email`/`Name` VO를 필드 타입으로 적용, `password: String` 유지)
- [x] [REFACTOR] `UserEntity` 생성 (`infrastructure/user/`, BaseEntity 상속, `fromDomain()`/`toDomain()`, VO↔원시타입 매핑)
- [x] [REFACTOR] `UserRepositoryImpl`에 `UserJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `UserJpaRepository.kt` 삭제
- [x] [REFACTOR] `FakeUserRepository` 업데이트
- [x] [REFACTOR] `UserService`, `UserCommand`, User Controller/Dto 수정

--- checkpoint: User 도메인 분리 완료. ktlintCheck + test 검수 ---

### CP0-4: Order 도메인 분리

**Order (Aggregate Root):**

- [x] [REFACTOR] Order Domain Model → 순수 POJO (JPA 제거, `Money` VO 적용, `create()` 팩토리 유지)
- [x] [REFACTOR] `OrderEntity` 생성 (`infrastructure/order/`, `fromDomain()`/`toDomain()`)
- [x] [REFACTOR] `OrderRepositoryImpl`에 `OrderJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `OrderJpaRepository.kt` 삭제

**OrderItem:**

- [x] [REFACTOR] OrderItem Domain Model → 순수 POJO (JPA 제거, `Money` VO 적용, `create()` 팩토리 유지)
- [x] [REFACTOR] `OrderItemEntity` 생성 (`infrastructure/order/`, `fromDomain()`/`toDomain()`)
- [x] [REFACTOR] `OrderItemRepositoryImpl`에 `OrderItemJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `OrderItemJpaRepository.kt` 삭제

**연쇄 수정:**

- [x] [REFACTOR] `FakeOrderRepository`, `FakeOrderItemRepository` 업데이트
- [x] [REFACTOR] `OrderService`, `OrderCommand`, `OrderProductInfo` 수정 (`Money` 적용)
- [x] [REFACTOR] Order Controller/Dto 수정

--- checkpoint: Order 도메인 분리 완료. ktlintCheck + test 검수 ---

### CP0-5: Point 도메인 분리

**UserPoint:**

- [x] [REFACTOR] UserPoint Domain Model → 순수 POJO (JPA 제거)
- [x] [REFACTOR] `UserPointEntity` 생성 (`infrastructure/point/`, `fromDomain()`/`toDomain()`, `@Lock(PESSIMISTIC_WRITE)`
  비관적 락 보존)
- [x] [REFACTOR] `UserPointRepositoryImpl`에 `UserPointJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `UserPointJpaRepository.kt` 삭제

**PointHistory:**

- [x] [REFACTOR] PointHistory Domain Model → 순수 POJO (JPA 제거)
- [x] [REFACTOR] `PointHistoryEntity` 생성 (`infrastructure/point/`, `fromDomain()`/`toDomain()`)
- [x] [REFACTOR] `PointHistoryRepositoryImpl`에 `PointHistoryJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `PointHistoryJpaRepository.kt` 삭제

**연쇄 수정:**

- [x] [REFACTOR] `FakeUserPointRepository`, `FakePointHistoryRepository` 업데이트
- [x] [REFACTOR] `UserPointService`, `PointChargingService` 수정 (명시적 save 추가)

--- checkpoint: Point 도메인 분리 완료. ktlintCheck + test 검수 ---

### CP0-6: Like 도메인 분리

- [x] [REFACTOR] Like Domain Model → 순수 POJO (JPA 제거)
- [x] [REFACTOR] `LikeEntity` 생성 (`infrastructure/like/`, `fromDomain()`/`toDomain()`, `@UniqueConstraint` 보존)
- [x] [REFACTOR] `LikeRepositoryImpl`에 `LikeJpaRepository` 통합 + Entity 매핑
- [x] [REFACTOR] `LikeJpaRepository.kt` 삭제
- [x] [REFACTOR] `FakeLikeRepository` 업데이트
- [x] [REFACTOR] `LikeService` 수정

--- checkpoint: Like 도메인 분리 완료. 전체 Domain/Entity 분리 종료. ktlintCheck + test 검수 ---

### CP0-7: Strict Layered Architecture — Facade → UseCase + Controller 도메인 참조 완전 제거

**원칙 (타협 없음):**
1. Controller는 Domain 객체(Command, VO, Enum, Service)를 절대 직접 참조하지 않는다
2. 모든 Controller는 반드시 Application Layer UseCase를 통과한다 (1줄 위임이라도 UseCase 명세를 남긴다)
3. Domain Enum은 도메인에 유지. Interfaces Layer DTO 안에 API 전용 Enum/String을 선언하고 매핑

**CP0-7a: Application Layer UseCase + Command/Info 객체 생성**

_User:_
- [x] [P-A] [REFACTOR] `RegisterUserUseCase` 생성 (UserFacade.signUp 이전)
- [x] [P-A] [REFACTOR] `GetUserInfoUseCase` 생성 (UserService.getUserInfo 래핑)
- [x] [P-A] [REFACTOR] `ChangePasswordUseCase` 생성 (UserService.changePassword 래핑)
- [x] [P-A] [REFACTOR] `application/user/` 에 `UserInfo` DTO 정의

_Catalog (Brand):_
- [x] [P-B] [REFACTOR] `CreateBrandUseCase`, `UpdateBrandUseCase`, `DeleteBrandUseCase`, `RestoreBrandUseCase` 생성
- [x] [P-B] [REFACTOR] `GetBrandUseCase`, `GetBrandsUseCase` 생성
- [x] [P-B] [REFACTOR] `application/catalog/brand/` 에 `BrandInfo` DTO 정의

_Catalog (Product):_
- [x] [P-C] [REFACTOR] `CreateProductUseCase`, `UpdateProductUseCase`, `DeleteProductUseCase`, `RestoreProductUseCase` 생성
- [x] [P-C] [REFACTOR] `GetProductUseCase`, `GetProductsUseCase` 생성
- [x] [P-C] [REFACTOR] `application/catalog/product/` 에 `ProductInfo`, `ProductDetailInfo` DTO 정의

_Order:_
- [x] [P-D] [REFACTOR] `PlaceOrderUseCase` 생성 (OrderFacade.createOrder 이전)
- [x] [P-D] [REFACTOR] `GetOrderUseCase`, `GetOrdersUseCase`, `GetOrderAdminUseCase`, `GetOrdersAdminUseCase` 생성
- [x] [P-D] [REFACTOR] `application/order/` 에 `PlaceOrderCommand`, `OrderInfo` DTO 정의

_Like:_
- [x] [P-E] [REFACTOR] `AddLikeUseCase`, `RemoveLikeUseCase`, `GetUserLikesUseCase` 생성 (LikeFacade 이전)
- [x] [P-E] [REFACTOR] `application/like/` 에 `LikeWithProductInfo` DTO 정의

_Point:_
- [x] [P-F] [REFACTOR] `GetUserPointUseCase`, `ChargePointUseCase` 생성
- [x] [P-F] [REFACTOR] `application/point/` 에 `PointBalanceInfo` DTO 정의

_Auth:_
- [x] [REFACTOR] `AuthenticateUserUseCase` 생성 (AuthInterceptor → Application Layer 통과)

--- checkpoint: CP0-7a UseCase + Application DTO 생성 완료. ktlintCheck + test 검수 ---

**CP0-7b: Controller 수정 — Domain 참조 완전 제거**

- [x] [P-A] [REFACTOR] User Controller → UseCase 의존 + DTO↔Application Command/Info 변환
- [x] [P-B] [REFACTOR] BrandAdmin/Brand Controller → UseCase 의존 + DTO 변환
- [x] [P-C] [REFACTOR] ProductAdmin/Product Controller → UseCase 의존 + DTO 변환
- [x] [P-D] [REFACTOR] Order Controller → UseCase 의존 + DTO 변환
- [x] [P-E] [REFACTOR] Like Controller → UseCase 의존 + DTO 변환
- [x] [P-F] [REFACTOR] Point Controller → UseCase 의존 + DTO 변환
- [x] [REFACTOR] Interfaces DTO 내 API 전용 Enum 선언 + Domain Enum 매핑 코드 작성
- [x] [REFACTOR] Controller에서 Domain import 가 0건인지 검증 (PageResult 제외 0건 확인)

--- checkpoint: CP0-7b Controller 도메인 참조 제거 완료. ktlintCheck + test 검수 ---

**CP0-7c: 기존 Facade 삭제 + 테스트 전환**

- [x] [REFACTOR] `UserFacade`, `OrderFacade`, `LikeFacade` 삭제
- [x] [REFACTOR] Facade 테스트 → UseCase 테스트로 전환
- [x] [REFACTOR] AuthInterceptor → `AuthenticateUserUseCase` 통과로 변경

--- checkpoint: CP0-7 전체 완료. ktlintCheck + test 검수 (275 tests pass) ---

### CP0-8: CLAUDE.md 문서 동기화

- [x] `application/CLAUDE.md`: Facade → UseCase 패턴 반영, Info DTO/Application Command 문서화
- [x] `infrastructure/CLAUDE.md`: Entity 매핑 패턴 현행화 (fromDomain/toDomain, BaseEntity 리플렉션, fromPersistence)
- [x] `domain/CLAUDE.md`: VO 목록 현행화 (Money, Email, BrandName 추가), fromPersistence 패턴, 명시적 save 규칙
- [x] `commerce-api/CLAUDE.md`: 요청 흐름을 Strict Layered Architecture로 현행화

--- checkpoint: Phase 0 완료. 전체 아키텍처 마이그레이션 종료. (275 tests pass) ---

---

## Phase 1: Bug Fix & Testing

### CP1-1: 구조 코드 수정 — Critical Bugs

- [x] [P-A] [RED] 주문 생성 시 동일 productId 중복 → BAD_REQUEST 테스트 (PlaceOrderUseCaseTest)
- [x] [P-A] [GREEN] PlaceOrderUseCase에 중복 productId 검증 추가
- [x] [P-B] [RED] LikeService 동시 좋아요 등록 시 DataIntegrityViolationException → 멱등 처리 테스트 (LikeServiceTest)
- [x] [P-B] [GREEN] LikeService.addLike에서 DataIntegrityViolationException catch → false 반환
- [x] [P-C] [RED] totalPrice 소수점 포함 시 포인트 차감 정확성 테스트 (PlaceOrderUseCaseTest)
- [x] [P-C] [GREEN] Money.toLong()에 setScale(0, RoundingMode.HALF_UP) 적용

--- checkpoint: Critical 버그 수정 완료. ktlintCheck + test 검수 (전체 통과) ---

### CP1-2: 구조 코드 수정 — Code Quality

- [x] [P-A] [REFACTOR] CatalogService.restoreBrand/restoreProduct가 복원된 객체를 직접 반환하도록 변경
- [x] [P-A] [REFACTOR] BrandAdminV1Controller/ProductAdminV1Controller에서 불필요한 2차 조회 제거

--- checkpoint: 코드 품질 수정 완료. ktlintCheck + test 검수 (전체 통과) ---

### CP1-3: 누락 단위 테스트 추가 (High)

- [x] [P-A] [RED] HIDDEN 상품에 좋아요 등록 → NOT_FOUND (AddLikeUseCaseTest)
- [x] [P-A] [RED] 좋아요 목록에서 HIDDEN 상품 미포함 (GetUserLikesUseCaseTest)
- [x] [P-B] [RED] 주문 항목 비어있음 → BAD_REQUEST (PlaceOrderUseCaseTest + OrderServiceTest)
- [x] [P-B] [RED+GREEN] 주문 수량 0 이하 → BAD_REQUEST (OrderServiceTest) — OrderService에 빈 items 검증 추가
- [x] [P-C] [RED] SOLD_OUT 상품 주문 → BAD_REQUEST (CatalogServiceTest.getProductsForOrder) — 기존 로직 정상 동작 확인
- [x] [P-D] [REFACTOR] RegisterUserUseCaseTest: mockk → Fake Repository 기반 전환 완료

--- checkpoint: High 누락 테스트 추가 완료. ktlintCheck + test 검수 (285 tests pass) ---

### CP1-4: 누락 E2E/Medium 테스트 추가

- [x] [P-A] [RED] E2E 주문 목록 조회 (OrderV1ApiE2ETest.GetOrders)
- [x] [P-A] [RED] E2E 주문 상세 조회 — 본인 조회 성공 + 타인 조회 404 (OrderV1ApiE2ETest.GetOrder)
- [x] [P-B] [RED] E2E 주문 목록 from/to 미입력 시 기본값 적용 검증 (OrderV1ApiE2ETest.GetOrders)
- [x] [P-B] [RED] E2E 회원가입 후 포인트 잔액 0 확인 (PointV1ApiE2ETest.GetBalance)

--- checkpoint: 전체 테스트 완료. ktlintCheck + test 최종 검수 (292 tests pass) ---

## 참고: 기존 판단 유지 항목

| 항목                                  | 판단     | 근거                                   |
|-------------------------------------|--------|--------------------------------------|
| restoreBrand가 소속 상품 미복구             | 의도된 동작 | 설계 문서: "브랜드 복구가 소속 상품을 연쇄 복구하지는 않는다" |
| PointHistory/Like에 updatedAt 없음     | 의도된 설계 | 불변 이력 데이터 / 물리 삭제 모델                 |
| LikeV1Controller @RequestMapping 없음 | 구조적 제약 | 2개 베이스 경로(/products/*, /users/*) 걸침  |
| BrandAdmin POST/PUT에 @RequestParam  | 기존 설계  | 필드 1개(name)뿐이라 현 구조 유지               |
