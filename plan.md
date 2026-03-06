# Round 4 — 쿠폰 도메인 구현 Plan

## 개요

쿠폰 도메인(Coupon + IssuedCoupon)을 신규 도입하고, 주문 흐름에 쿠폰 할인을 통합한다.
기존 DDD 아키텍처(Domain Model/JPA Entity 분리, UseCase 패턴, 비관적 락)를 그대로 유지한다.

## 참조 규칙

- `.claude/rules/test-patterns.md`
- `.claude/rules/kotlin-spring-jpa.md`
- `.claude/rules/parallel-workflow.md`
- `.claude/rules/code-guidelines.md`
- `.claude/rules/architecture-compliance.md`
- 각 레이어 CLAUDE.md (`domain/`, `application/`, `infrastructure/`, `interfaces/`)

## 베이스 경로

```
apps/commerce-api/src/main/kotlin/com/loopers/  → SRC
apps/commerce-api/src/test/kotlin/com/loopers/  → TEST
```

---

## CP1: Domain Model + Enum + Fake Repository (테스트 인프라)

> 목표: 쿠폰 도메인의 핵심 모델, 열거형, Repository 인터페이스, Fake Repository를 구축하여 이후 모든 단위 테스트의 기반을 마련한다.

### 1-1. Coupon Domain Model

- [ ] [GREEN] `Coupon.kt` 생성
  - 파일: `SRC/domain/coupon/model/Coupon.kt`
  - 생성 패턴: **독립적 생성** (Product 패턴) — `public constructor` + `init { validate() }`
  - **`enum class CouponType`을 Coupon 내부에 선언** (별도 파일 X, 기존 OrderStatus/ProductStatus 패턴 준수)
    - FIXED, RATE
  - 필드: id(Long), name(String), type(CouponType), value(Long), maxDiscount(Money?), minOrderAmount(Money?), totalQuantity(Int?), issuedCount(Int=0), expiredAt(ZonedDateTime), deletedAt(ZonedDateTime?=null)
  - issuedCount는 `var` (private set)
  - 행위 메서드: `canIssue()`, `issue()`, `calculateDiscount(orderAmount: Money): Money`, `isExpired()`, `isDeleted()`
  - init 블록 검증:
    - name 비어있으면 CoreException
    - value <= 0이면 CoreException
    - RATE 타입: value < 1 || value > 100이면 CoreException
    - totalQuantity != null && totalQuantity <= 0이면 CoreException
    - issuedCount < 0이면 CoreException
  - `canIssue()`: !isExpired() && !isDeleted() && (totalQuantity == null || issuedCount < totalQuantity)
  - `issue()`: canIssue() 실패 시 CoreException, 성공 시 issuedCount++
  - `calculateDiscount(orderAmount)`:
    - FIXED: min(Money(BigDecimal(value)), orderAmount)
    - RATE: min(orderAmount * value / 100, maxDiscount ?: 매우 큰 값) — 0원 이상 보장
  - `isExpired()`: expiredAt.isBefore(ZonedDateTime.now())
  - `update(name, type, value, maxDiscount, minOrderAmount, totalQuantity, expiredAt)` — 어드민 수정용
  - `delete()`: deletedAt = ZonedDateTime.now() (멱등)

### 1-2. IssuedCoupon Domain Model

- [ ] [GREEN] `IssuedCoupon.kt` 생성
  - 파일: `SRC/domain/coupon/model/IssuedCoupon.kt`
  - 생성 패턴: **독립적 생성** (public constructor)
  - **`enum class CouponStatus`를 IssuedCoupon 내부에 선언** (별도 파일 X, 기존 패턴 준수)
    - AVAILABLE, USED, EXPIRED
  - 필드: id(Long), refCouponId(Long), refUserId(UserId), status(CouponStatus=CouponStatus.AVAILABLE), usedAt(ZonedDateTime?=null), createdAt(ZonedDateTime)
  - status는 `var` (private set), usedAt는 `var` (private set)
  - 행위 메서드: `use()`, `isAvailable()`, `isOwnedBy(userId: UserId)`
  - `use()`: status != AVAILABLE이면 CoreException, 성공 시 status = USED, usedAt = ZonedDateTime.now()
  - `isAvailable()`: status == AVAILABLE
  - `isOwnedBy(userId)`: refUserId == userId

### 1-3. CouponRepository 인터페이스

- [ ] [GREEN] `CouponRepository.kt` 생성
  - 파일: `SRC/domain/coupon/CouponRepository.kt`
  - 메서드:
    - `save(coupon: Coupon): Coupon`
    - `findById(id: Long): Coupon?`
    - `findByIdForUpdate(id: Long): Coupon?` — 비관적 락 (발급 시 선착순)
    - `findAll(page: Int, size: Int): PageResult<Coupon>` — deletedAt IS NULL
    - `findAllIncludeDeleted(page: Int, size: Int): PageResult<Coupon>` — 어드민용

### 1-4. IssuedCouponRepository 인터페이스

- [ ] [GREEN] `IssuedCouponRepository.kt` 생성
  - 파일: `SRC/domain/coupon/IssuedCouponRepository.kt`
  - 메서드:
    - `save(issuedCoupon: IssuedCoupon): IssuedCoupon`
    - `findById(id: Long): IssuedCoupon?`
    - `findByIdForUpdate(id: Long): IssuedCoupon?` — 비관적 락 (주문 시 쿠폰 사용)
    - `findByRefCouponIdAndRefUserId(couponId: Long, userId: UserId): IssuedCoupon?` — 중복 발급 검증
    - `findAllByRefUserId(userId: UserId): List<IssuedCoupon>` — 내 쿠폰 목록
    - `findAllByRefCouponId(couponId: Long, page: Int, size: Int): PageResult<IssuedCoupon>` — 어드민 발급 내역

### 1-5. FakeCouponRepository

- [ ] [GREEN] `FakeCouponRepository.kt` 생성
  - 파일: `TEST/domain/coupon/FakeCouponRepository.kt`
  - 패턴: 기존 FakeProductRepository 참조
  - mutableListOf, sequence ID 자동 할당
  - Reflection으로 id 설정 (public constructor 모델이므로 생성자에 id 포함하여 새 인스턴스 생성 방식도 가능)
  - findByIdForUpdate는 findById와 동일 동작 (Fake에서는 락 불필요)

### 1-6. FakeIssuedCouponRepository

- [ ] [GREEN] `FakeIssuedCouponRepository.kt` 생성
  - 파일: `TEST/domain/coupon/FakeIssuedCouponRepository.kt`
  - 동일 패턴

### CP1 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP2: 쿠폰 단위 테스트 (Domain Model 비즈니스 로직)

> 목표: Coupon, IssuedCoupon의 모든 비즈니스 규칙을 단위 테스트로 검증한다.

### 2-1. Coupon 단위 테스트

- [ ] [RED] Coupon 생성 및 검증 테스트 작성
  - 파일: `TEST/domain/coupon/model/CouponTest.kt`
  - @Nested + @DisplayName(한국어) BDD, 3A 원칙
  - 테스트 케이스:
    - 정상 생성 (FIXED, RATE 각각)
    - name 빈 문자열 → 예외
    - value <= 0 → 예외
    - RATE 타입 value 범위 (0, 101) → 예외
    - totalQuantity <= 0 → 예외

- [ ] [GREEN] Coupon 생성 검증 통과 확인 (CP1에서 이미 구현, 테스트가 통과하는지 검증)

- [ ] [RED] Coupon.canIssue() 테스트 작성
  - 발급 가능한 경우 (수량 잔여 + 미만료 + 미삭제)
  - 만료된 쿠폰 → false
  - 삭제된 쿠폰 → false
  - 수량 소진 (issuedCount == totalQuantity) → false
  - totalQuantity == null (무제한) → true

- [ ] [GREEN] canIssue() 테스트 통과 확인

- [ ] [RED] Coupon.issue() 테스트 작성
  - 정상 발급 → issuedCount 증가
  - canIssue() false → 예외

- [ ] [GREEN] issue() 테스트 통과 확인

- [ ] [RED] Coupon.calculateDiscount() 테스트 작성
  - FIXED: value=5000, orderAmount=10000 → 5000
  - FIXED: value=15000, orderAmount=10000 → 10000 (orderAmount 초과 방지)
  - RATE: value=10, orderAmount=100000, maxDiscount=null → 10000
  - RATE: value=10, orderAmount=100000, maxDiscount=5000 → 5000 (한도 적용)
  - RATE: value=50, orderAmount=10000, maxDiscount=null → 5000

- [ ] [GREEN] calculateDiscount() 테스트 통과 확인

- [ ] [RED] Coupon.isExpired() 테스트 작성
  - 미래 만료일 → false
  - 과거 만료일 → true

- [ ] [GREEN] isExpired() 테스트 통과 확인

### 2-2. IssuedCoupon 단위 테스트

- [ ] [RED] IssuedCoupon 비즈니스 로직 테스트 작성
  - 파일: `TEST/domain/coupon/model/IssuedCouponTest.kt`
  - 테스트 케이스:
    - use(): AVAILABLE → USED 전환, usedAt 설정됨
    - use(): 이미 USED인 쿠폰 → 예외
    - use(): EXPIRED인 쿠폰 → 예외
    - isAvailable(): AVAILABLE → true, USED → false, EXPIRED → false
    - isOwnedBy(): 본인 → true, 타인 → false

- [ ] [GREEN] IssuedCoupon 테스트 통과 확인

### CP2 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP3: 쿠폰 UseCase (발급, 내 쿠폰 조회) + 단위 테스트

> 목표: 대고객 쿠폰 UseCase를 구현하고 Fake Repository 기반 단위 테스트로 검증한다.

### 3-1. CouponInfo DTO

- [ ] [GREEN] `CouponInfo.kt` 생성
  - 파일: `SRC/application/coupon/CouponInfo.kt`
  - CouponInfo: id, name, type(String), value, maxDiscount(BigDecimal?), minOrderAmount(BigDecimal?), totalQuantity, issuedCount, expiredAt(String)
  - IssuedCouponInfo: id, couponId, userId, status(String), usedAt(String?), createdAt(String)
  - MyCouponInfo: IssuedCoupon 정보 + Coupon 템플릿 정보 결합 (id, couponName, couponType, couponValue, maxDiscount, status, usedAt, createdAt, expiredAt)
  - 각각 `companion object { fun from(...) }` 팩토리 메서드
  - 원시 타입만 사용 (Money → BigDecimal, Enum → String, ZonedDateTime → String)

### 3-2. IssueCouponUseCase

- [ ] [RED] IssueCouponUseCase 단위 테스트 작성
  - 파일: `TEST/application/coupon/IssueCouponUseCaseTest.kt`
  - Fake Repository 사용 (FakeCouponRepository, FakeIssuedCouponRepository)
  - 테스트 케이스:
    - 정상 발급 → IssuedCoupon 생성, Coupon.issuedCount 증가
    - 존재하지 않는 쿠폰 → NOT_FOUND 예외
    - 삭제된 쿠폰 → NOT_FOUND 예외
    - 만료된 쿠폰 → BAD_REQUEST 예외
    - 수량 소진 → BAD_REQUEST 예외
    - 이미 발급받은 쿠폰 (1인 1매) → BAD_REQUEST 예외

- [ ] [GREEN] `IssueCouponUseCase.kt` 구현
  - 파일: `SRC/application/coupon/IssueCouponUseCase.kt`
  - @Component, @Transactional
  - execute(userId: Long, couponId: Long): IssuedCouponInfo
  - 흐름:
    1. couponRepository.findByIdForUpdate(couponId) — 비관적 락
    2. 존재/삭제/만료 검증
    3. issuedCouponRepository.findByRefCouponIdAndRefUserId() — 중복 발급 검증
    4. coupon.issue() — issuedCount 증가
    5. IssuedCoupon 생성 + save
    6. couponRepository.save(coupon) — 변경된 issuedCount 반영
    7. IssuedCouponInfo 반환

### 3-3. GetMyCouponsUseCase

- [ ] [RED] GetMyCouponsUseCase 단위 테스트 작성
  - 파일: `TEST/application/coupon/GetMyCouponsUseCaseTest.kt`
  - 테스트 케이스:
    - 발급된 쿠폰 목록 조회 → 쿠폰 템플릿 정보 포함
    - 쿠폰이 없으면 빈 리스트

- [ ] [GREEN] `GetMyCouponsUseCase.kt` 구현
  - 파일: `SRC/application/coupon/GetMyCouponsUseCase.kt`
  - @Component
  - execute(userId: Long): List<MyCouponInfo>
  - 흐름:
    1. issuedCouponRepository.findAllByRefUserId(UserId(userId))
    2. 각 IssuedCoupon의 refCouponId로 Coupon 조회
    3. MyCouponInfo 조합 반환

### CP3 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP4: Order 변경 (할인 필드 추가, PlaceOrderUseCase 확장) + 단위 테스트

> 목표: Order에 할인 관련 필드를 추가하고, PlaceOrderUseCase에 쿠폰 검증/할인 적용 로직을 통합한다.

### 4-1. Order Domain Model 변경

- [ ] [RED] Order 할인 필드 관련 테스트 작성
  - 파일: `TEST/domain/order/model/OrderTest.kt` (기존 파일 확인, 없으면 신규 생성)
  - 테스트 케이스:
    - 쿠폰 미적용 Order.create() → originalPrice == totalPrice, discountAmount == 0, refCouponId == null
    - 쿠폰 적용 Order.create() → originalPrice, discountAmount, totalPrice 정합성 (totalPrice = originalPrice - discountAmount)

- [ ] [GREEN] `Order.kt` 수정
  - 파일: `SRC/domain/order/model/Order.kt`
  - 필드 추가: originalPrice(Money), discountAmount(Money), refCouponId(Long?)
  - `create()` 시그니처 변경: `create(userId, items, discountAmount: Money = Money(BigDecimal.ZERO), refCouponId: Long? = null)`
  - create() 내부: originalPrice 계산 → totalPrice = originalPrice - discountAmount
  - `fromPersistence()` 시그니처 변경: originalPrice, discountAmount, refCouponId 파라미터 추가

### 4-2. PlaceOrderCommand 변경

- [ ] [GREEN] `PlaceOrderCommand.kt` 수정
  - 파일: `SRC/application/order/PlaceOrderCommand.kt`
  - couponId(Long?) 필드 추가

### 4-3. PlaceOrderUseCase 확장

- [ ] [RED] PlaceOrderUseCase 쿠폰 적용 테스트 작성
  - 파일: `TEST/application/order/PlaceOrderUseCaseTest.kt` (기존 파일 수정/확장)
  - Fake Repository 사용 (FakeCouponRepository, FakeIssuedCouponRepository 추가 주입)
  - 테스트 케이스:
    - 쿠폰 미적용 주문 → 기존과 동일 동작 (originalPrice == totalPrice)
    - 쿠폰 적용 주문 (FIXED) → 할인 적용된 totalPrice
    - 쿠폰 적용 주문 (RATE) → 할인 적용된 totalPrice
    - 존재하지 않는 발급 쿠폰 → BAD_REQUEST 예외
    - 타인 소유 쿠폰 → BAD_REQUEST 예외
    - 이미 사용된 쿠폰 → BAD_REQUEST 예외
    - minOrderAmount 미충족 → BAD_REQUEST 예외
    - 쿠폰 사용 후 IssuedCoupon.status == USED 확인

- [ ] [GREEN] `PlaceOrderUseCase.kt` 수정
  - 파일: `SRC/application/order/PlaceOrderUseCase.kt`
  - 의존성 추가: CouponRepository, IssuedCouponRepository
  - execute() 시그니처 변경: couponId 포함된 command 사용
  - 쿠폰 로직 추가 (재고 차감 이후):
    1. couponId != null이면 issuedCouponRepository.findByIdForUpdate(couponId)
    2. 존재/소유자/상태 검증
    3. couponRepository.findById(issuedCoupon.refCouponId)로 템플릿 조회
    4. 만료 검증, minOrderAmount 검증
    5. discountAmount = coupon.calculateDiscount(originalPrice)
    6. issuedCoupon.use()
    7. issuedCouponRepository.save(issuedCoupon)
    8. Order.create(userId, items, discountAmount, couponId)

### 4-4. OrderInfo 변경

- [ ] [GREEN] `OrderInfo.kt` 수정
  - 파일: `SRC/application/order/OrderInfo.kt`
  - originalPrice(BigDecimal), discountAmount(BigDecimal), couponId(Long?) 필드 추가
  - from() 매핑 업데이트

### 4-5. 기존 Order 테스트 수정

- [ ] [GREEN] 기존 PlaceOrderUseCase 테스트가 변경된 시그니처에 맞게 수정되어 통과하는지 확인
  - 기존 테스트에서 Order.create() 호출부 업데이트 (기본값 사용으로 변경 최소화)

### CP4 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP5: Infrastructure (JPA Entity, RepositoryImpl)

> 목표: 쿠폰 도메인의 JPA Entity와 Repository 구현체를 작성하고, Order Entity의 할인 필드를 추가한다.

### 5-1. CouponEntity

- [ ] [GREEN] `CouponEntity.kt` 생성
  - 파일: `SRC/infrastructure/coupon/CouponEntity.kt`
  - BaseEntity 상속, @Entity, @Table(name = "coupons")
  - 컬럼: name, type(EnumType.STRING), value, max_discount(nullable), min_order_amount(nullable), total_quantity(nullable), issued_count, expired_at
  - fromDomain() / toDomain() 매핑 (Coupon.fromPersistence() 사용)

### 5-2. CouponRepositoryImpl

- [ ] [GREEN] `CouponRepositoryImpl.kt` 생성
  - 파일: `SRC/infrastructure/coupon/CouponRepositoryImpl.kt`
  - CouponJpaRepository (같은 파일에 선언) + CouponRepositoryImpl
  - JpaRepository 메서드:
    - findById (기본 제공, deletedAt 필터 없음 — 단건)
    - @Lock(PESSIMISTIC_WRITE) findByIdWithLock(id: Long): Optional<CouponEntity>
    - findAllByDeletedAtIsNull(pageable: Pageable): Page<CouponEntity> — 다건
    - findAll(pageable: Pageable): Page<CouponEntity> — 어드민용 (삭제 포함)
  - 구현체:
    - save, findById, findByIdForUpdate, findAll, findAllIncludeDeleted

### 5-3. IssuedCouponEntity

- [ ] [GREEN] `IssuedCouponEntity.kt` 생성
  - 파일: `SRC/infrastructure/coupon/IssuedCouponEntity.kt`
  - BaseEntity 상속, @Entity, @Table(name = "issued_coupons")
  - 컬럼: ref_coupon_id, ref_user_id, status(EnumType.STRING), used_at(nullable)
  - fromDomain() / toDomain() 매핑 (IssuedCoupon.fromPersistence() 사용)

### 5-4. IssuedCouponRepositoryImpl

- [ ] [GREEN] `IssuedCouponRepositoryImpl.kt` 생성
  - 파일: `SRC/infrastructure/coupon/IssuedCouponRepositoryImpl.kt`
  - IssuedCouponJpaRepository + IssuedCouponRepositoryImpl
  - JpaRepository 메서드:
    - @Lock(PESSIMISTIC_WRITE) findByIdWithLock(id: Long): Optional<IssuedCouponEntity>
    - findByRefCouponIdAndRefUserId(couponId: Long, userId: Long): IssuedCouponEntity?
    - findAllByRefUserId(userId: Long): List<IssuedCouponEntity>
    - findAllByRefCouponId(couponId: Long, pageable: Pageable): Page<IssuedCouponEntity>

### 5-5. OrderEntity 변경

- [ ] [GREEN] `OrderEntity.kt` 수정
  - 파일: `SRC/infrastructure/order/OrderEntity.kt`
  - 필드 추가: original_price(BigDecimal), discount_amount(BigDecimal), ref_coupon_id(Long?, nullable)
  - fromDomain() / toDomain() 매핑 업데이트

### CP5 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP6: 어드민 UseCase + 단위 테스트

> 목표: 쿠폰 어드민 CRUD UseCase를 구현하고 단위 테스트로 검증한다.

### 6-1. CouponCommand (Application Command)

- [ ] [GREEN] `CouponCommand.kt` 생성
  - 파일: `SRC/application/coupon/CouponCommand.kt`
  - CouponCommand.CreateCoupon: name, type(String), value, maxDiscount(BigDecimal?), minOrderAmount(BigDecimal?), totalQuantity(Int?), expiredAt(String)
  - CouponCommand.UpdateCoupon: name(String?), type(String?), value(Long?), maxDiscount(BigDecimal?), minOrderAmount(BigDecimal?), totalQuantity(Int?), expiredAt(String?)

### 6-2. CreateCouponAdminUseCase

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/CreateCouponAdminUseCaseTest.kt`
  - 테스트: 정상 생성, 유효하지 않은 type → 예외

- [ ] [GREEN] `CreateCouponAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/CreateCouponAdminUseCase.kt`
  - Coupon 생성 + save → CouponInfo 반환

### 6-3. UpdateCouponAdminUseCase

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/UpdateCouponAdminUseCaseTest.kt`
  - 테스트: 정상 수정, 존재하지 않는 쿠폰 → NOT_FOUND, 삭제된 쿠폰 → NOT_FOUND

- [ ] [GREEN] `UpdateCouponAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/UpdateCouponAdminUseCase.kt`
  - findById → isDeleted 검증 → update → save → CouponInfo 반환

### 6-4. DeleteCouponAdminUseCase

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/DeleteCouponAdminUseCaseTest.kt`
  - 테스트: 정상 삭제 (soft delete), 이미 삭제된 쿠폰 → 멱등 처리, 존재하지 않는 쿠폰 → 무시

- [ ] [GREEN] `DeleteCouponAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/DeleteCouponAdminUseCase.kt`
  - 패턴: DeleteProductUseCase 참조 (findById → isDeleted → delete → save)

### 6-5. GetCouponsAdminUseCase (목록)

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/GetCouponsAdminUseCaseTest.kt`
  - 테스트: 페이징 목록 조회 (삭제 포함)

- [ ] [GREEN] `GetCouponsAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/GetCouponsAdminUseCase.kt`
  - findAllIncludeDeleted(page, size) → PageResult<CouponInfo>

### 6-6. GetCouponAdminUseCase (상세)

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/GetCouponAdminUseCaseTest.kt`
  - 테스트: 정상 조회, NOT_FOUND

- [ ] [GREEN] `GetCouponAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/GetCouponAdminUseCase.kt`
  - findById → CouponInfo 반환

### 6-7. GetCouponIssuesAdminUseCase (발급 내역)

- [ ] [RED] 단위 테스트 작성
  - 파일: `TEST/application/coupon/GetCouponIssuesAdminUseCaseTest.kt`
  - 테스트: 특정 쿠폰의 발급 내역 페이징 조회

- [ ] [GREEN] `GetCouponIssuesAdminUseCase.kt` 구현
  - 파일: `SRC/application/coupon/GetCouponIssuesAdminUseCase.kt`
  - issuedCouponRepository.findAllByRefCouponId(couponId, page, size) → PageResult<IssuedCouponInfo>

### CP6 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP7: Controller + ApiSpec + Dto (대고객 + 어드민)

> 목표: 쿠폰 관련 Controller, ApiSpec, Dto를 작성하고, 기존 Order Controller/Dto를 업데이트한다.

### 7-1. 대고객 쿠폰 Dto

- [ ] [GREEN] `CouponV1Dto.kt` 생성
  - 파일: `SRC/interfaces/api/coupon/dto/CouponV1Dto.kt`
  - MyCouponResponse: `companion object { fun from(info: MyCouponInfo) }`

### 7-2. 대고객 쿠폰 ApiSpec

- [ ] [GREEN] `CouponV1ApiSpec.kt` 생성
  - 파일: `SRC/interfaces/api/coupon/spec/CouponV1ApiSpec.kt`
  - POST /api/v1/coupons/{couponId}/issue
  - GET /api/v1/users/me/coupons

### 7-3. 대고객 쿠폰 Controller

- [ ] [GREEN] `CouponV1Controller.kt` 생성
  - 파일: `SRC/interfaces/api/coupon/CouponV1Controller.kt`
  - IssueCouponUseCase, GetMyCouponsUseCase 주입
  - 패턴: 기존 OrderV1Controller 참조

### 7-4. 어드민 쿠폰 Dto

- [ ] [GREEN] `CouponAdminV1Dto.kt` 생성
  - 파일: `SRC/interfaces/api/admin/coupon/dto/CouponAdminV1Dto.kt`
  - CreateCouponRequest (name, type, value, maxDiscount?, minOrderAmount?, totalQuantity?, expiredAt) + toCommand()
  - UpdateCouponRequest (name?, type?, value?, maxDiscount?, minOrderAmount?, totalQuantity?, expiredAt?) + toCommand()
  - CouponAdminResponse: `companion object { fun from(info: CouponInfo) }`
  - IssuedCouponAdminResponse: `companion object { fun from(info: IssuedCouponInfo) }`

### 7-5. 어드민 쿠폰 ApiSpec

- [ ] [GREEN] `CouponAdminV1ApiSpec.kt` 생성
  - 파일: `SRC/interfaces/api/admin/coupon/spec/CouponAdminV1ApiSpec.kt`
  - GET/POST/PUT/DELETE /api-admin/v1/coupons, GET /api-admin/v1/coupons/{couponId}/issues

### 7-6. 어드민 쿠폰 Controller

- [ ] [GREEN] `CouponAdminV1Controller.kt` 생성
  - 파일: `SRC/interfaces/api/admin/coupon/CouponAdminV1Controller.kt`
  - 6개 UseCase 주입
  - 패턴: 기존 OrderAdminV1Controller + ProductAdminController 참조

### 7-7. WebMvcConfig 업데이트

- [ ] [GREEN] `WebMvcConfig.kt` 수정
  - 파일: `SRC/interfaces/support/config/WebMvcConfig.kt`
  - AuthInterceptor addPathPatterns에 `/api/v1/coupons/**` 추가
  - (어드민은 이미 `/api-admin/**` 패턴으로 포함됨)

### 7-8. Order Dto 업데이트

- [ ] [GREEN] `OrderV1Dto.kt` 수정
  - 파일: `SRC/interfaces/api/order/dto/OrderV1Dto.kt`
  - CreateOrderRequest에 couponId(Long?) 필드 추가, toCommand() 업데이트
  - OrderResponse에 originalPrice, discountAmount, couponId 필드 추가

- [ ] [GREEN] `OrderAdminV1Dto.kt` 수정
  - 파일: `SRC/interfaces/api/order/dto/OrderAdminV1Dto.kt`
  - OrderAdminResponse에 originalPrice, discountAmount, couponId 필드 추가

### CP7 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP8: E2E 테스트

> 목표: 쿠폰 관련 전체 흐름을 E2E 테스트로 검증한다.

### 8-1. 쿠폰 어드민 E2E 테스트

- [ ] [RED] 어드민 CRUD E2E 테스트 작성
  - 파일: `TEST/interfaces/api/admin/coupon/CouponAdminV1ApiE2ETest.kt`
  - @SpringBootTest(RANDOM_PORT) + TestRestTemplate
  - 테스트 케이스:
    - 쿠폰 템플릿 생성 → 200 OK + 생성된 데이터 확인
    - 쿠폰 템플릿 수정 → 200 OK + 수정된 데이터 확인
    - 쿠폰 템플릿 삭제 → 200 OK + 삭제 확인
    - 쿠폰 목록 조회 → 페이징 정상
    - 쿠폰 상세 조회 → 정상 데이터
    - 발급 내역 조회 → 발급 후 내역 확인

### 8-2. 쿠폰 발급 E2E 테스트

- [ ] [RED] 대고객 쿠폰 발급 E2E 테스트 작성
  - 파일: `TEST/interfaces/api/coupon/CouponV1ApiE2ETest.kt`
  - 테스트 케이스:
    - 정상 발급 → 200 OK
    - 중복 발급 → 400
    - 수량 소진 → 400
    - 만료된 쿠폰 → 400
    - 내 쿠폰 목록 조회 → 발급된 쿠폰 포함

### 8-3. 쿠폰 적용 주문 E2E 테스트

- [ ] [RED] 쿠폰 적용 주문 E2E 테스트 작성
  - 파일: `TEST/interfaces/api/order/OrderV1ApiE2ETest.kt` (기존 파일 확장)
  - 테스트 케이스:
    - 쿠폰 적용 주문 → 할인 적용된 totalPrice, 쿠폰 USED 전환
    - 쿠폰 미적용 주문 → 기존과 동일 (originalPrice == totalPrice)
    - 타인 쿠폰 적용 → 400
    - 이미 사용된 쿠폰 → 400

- [ ] [GREEN] E2E 테스트 모두 통과 확인

### CP8 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP9: 동시성 테스트

> 목표: CountDownLatch + ExecutorService로 동시성 시나리오를 검증한다.

### 9-1. 쿠폰 동시 발급 테스트

- [ ] [RED] 선착순 쿠폰 동시 발급 테스트 작성
  - 파일: `TEST/interfaces/api/coupon/CouponConcurrencyTest.kt`
  - @SpringBootTest(RANDOM_PORT) + TestRestTemplate
  - 시나리오: totalQuantity=10인 쿠폰에 50명 동시 발급 요청
  - 검증:
    - 발급 성공 수 == 10
    - DB의 issuedCount == 10
    - 발급 실패 수 == 40

### 9-2. 쿠폰 동시 사용 (주문) 테스트

- [ ] [RED] 동일 쿠폰 동시 주문 테스트 작성
  - 파일: `TEST/interfaces/api/order/OrderConcurrencyTest.kt`
  - 시나리오: 동일한 발급 쿠폰으로 여러 기기(다른 세션)에서 동시 주문
  - 검증:
    - 주문 성공 수 == 1
    - 쿠폰 상태 == USED
    - 나머지 주문은 실패

### 9-3. 재고 동시 차감 테스트

- [ ] [RED] 동시 주문 재고 차감 테스트 작성
  - 파일: `TEST/interfaces/api/order/OrderConcurrencyTest.kt` (같은 파일)
  - 시나리오: 재고 10개 상품에 20명 동시 주문 (각 1개)
  - 검증:
    - 성공 수 + 실패 수 == 20
    - 최종 재고 >= 0, 성공 수 <= 10

### 9-4. 좋아요 동시성 테스트

- [ ] [RED] 좋아요 동시 요청 테스트 작성
  - 파일: `TEST/interfaces/api/like/LikeConcurrencyTest.kt`
  - 시나리오: N명이 동시에 같은 상품에 좋아요
  - 검증:
    - likeCount == N
    - Like 레코드 N개

- [ ] [GREEN] 모든 동시성 테스트 통과 확인

### CP9 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과

---

## CP10: 설계 문서 업데이트 + HTTP 파일

> 목표: 설계 산출물을 Round 4 변경사항에 맞게 업데이트한다.

### 10-1. 설계 문서 업데이트

- [ ] `docs/design/01-requirements.md` — Coupon 도메인 용어 + 도메인 경계 추가
- [ ] `docs/design/02-sequence-diagrams.md` — 쿠폰 발급 흐름, 쿠폰 적용 주문 흐름 시퀀스 추가
- [ ] `docs/design/03-class-diagram.md` — Coupon, IssuedCoupon 클래스 + Order 필드 변경 반영
- [ ] `docs/design/04-erd.md` — coupon, issued_coupon 테이블 + order 테이블 컬럼 추가
- [ ] `docs/design/05-flowcharts.md` — 주문 흐름에 쿠폰 검증/적용 단계 추가

### 10-2. HTTP 파일

- [ ] `http/coupon.http` — 쿠폰 대고객/어드민 API HTTP 파일 작성
- [ ] `http/order.http` — 주문 API에 couponId 포함된 요청 예제 추가

### CP10 검증

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과 (최종)

---

## 병렬 위임 가이드

| CP | 의존 관계 | 병렬 가능 여부 |
|----|---------|-----------|
| CP1 | 없음 | 단독 실행 |
| CP2 | CP1 | CP1 이후 단독 |
| CP3 | CP1 | CP2와 병렬 가능 |
| CP4 | CP1 | CP2, CP3와 병렬 가능 |
| CP5 | CP1, CP4 | CP4 이후 단독 |
| CP6 | CP1, CP3 | CP5와 병렬 가능 |
| CP7 | CP3, CP4, CP6 | CP6 이후 |
| CP8 | CP5, CP7 | CP7 이후 |
| CP9 | CP5, CP7 | CP8과 병렬 가능 |
| CP10 | 전체 완료 후 | 마지막 |

**권장 실행 순서:**
1. CP1 (기반)
2. CP2 + CP3 + CP4 (병렬)
3. CP5 + CP6 (병렬)
4. CP7
5. CP8 + CP9 (병렬)
6. CP10

---

## 최종 체크리스트

- [ ] 쿠폰 종류(FIXED/RATE) 각각의 할인 계산이 정확하다
- [ ] 정률 쿠폰 maxDiscount 한도가 적용된다
- [ ] 발급 쿠폰은 1인 1매 제한이 동작한다
- [ ] totalQuantity 초과 발급이 차단된다
- [ ] 주문 시 쿠폰 유효성 검증이 모두 동작한다 (존재/소유/상태/만료/최소금액)
- [ ] 주문 스냅샷에 originalPrice, discountAmount, totalPrice가 정합적이다
- [ ] 쿠폰 삭제 시 이미 발급된 쿠폰은 상태 유지된다
- [ ] 동시성 테스트 4종이 모두 통과한다
- [ ] 전체 `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과
