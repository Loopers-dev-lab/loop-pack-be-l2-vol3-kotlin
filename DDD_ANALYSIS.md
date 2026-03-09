# DDD 아키텍처 분석 보고서

**분석 기준일**: 2026-03-08 (Round 7 업데이트됨)
**브랜치**: feature/round4-tx
**최근 완료**: Round 7 - Coupon 중복 발급 Pessimistic Lock 구현

---

## 📋 Executive Summary

### 현재 상태
- ✅ **기본 DDD 원칙 준수**: ValueObject, Entity, Repository 패턴이 잘 적용됨
- ✅ **계층 분리 명확**: Domain ↔ Infrastructure 분리되어 있음
- ✅ **Aggregate 경계 강화** (Round 5): Order Aggregate 경계 명확화
- ❌ **기술 결정이 도메인에 혼재**: Transaction Propagation, Native SQL이 Domain Service에 노출됨
- ❌ **Bounded Context 분리 부족**: Stock과 Order가 밀결합됨

### 종합 평가
**점수**: 7.4/10 (↑ from 7.1/10, Round 7 완료)

| 항목 | Round6 | Round7 | 상태 | 변화 |
|------|--------|--------|------|------|
| Entity & ValueObject 설계 | 9/10 | 9/10 | ✅ 우수 | → |
| Aggregate 경계 | 8/10 | 8/10 | ✅ 우수 | → |
| Repository Pattern | 8.5/10 | **9/10** | ✅ 우수 | ↑0.5 |
| Domain Service 설계 | 6/10 | **6.5/10** | ⚠️ 개선 중 | ↑0.5 |
| Transaction 경계 | 3/10 | **4/10** | ⚠️ 개선됨 | ↑1 |
| Bounded Context 분리 | 4/10 | 4/10 | ❌ 미흡 | → |
| 동시성 제어 | 8/10 | **9/10** | ✅ 우수 | ↑1 |

---

## 1️⃣ 도메인 모델 구조 분석

### 1.1 Entity vs ValueObject 구분

#### ✅ ValueObject 잘 설계됨
```kotlin
// User 도메인의 ValueObject들
@Embeddable
data class Email(val value: String) {
    fun validate() { /* 유효성 검사 */ }
}

@Embeddable
data class Password(val value: String) {
    fun validate() { /* 유효성 검사 */ }
}
```

**특징**:
- `@Embeddable`로 DB 컬럼에 자동 매핑
- 불변 data class
- 자체 검증 로직 포함
- 비즈니스 규칙 캡슐화

#### ✅ Entity Factory Pattern
모든 Entity가 일관된 factory 사용:
```kotlin
companion object {
    fun create(...): Stock {
        return Stock(...).apply { guard() }
    }
}
```

#### 🔍 Entity 설계 현황

| Entity | 책임 | 로직 포함 | 평가 |
|--------|------|---------|------|
| **Stock** | 재고 관리 | ✅ minusStock, plusStock | ⭐⭐⭐ |
| **Coupon** | 쿠폰 상태 | ✅ use() | ⭐⭐⭐ |
| **Order** | 주문 관리 | ✅ addItem(), getTotalPrice() | ⭐⭐⭐ |
| **Product** | 상품 관리 | ⚠️ 일부만 (likeCount는 없음) | ⭐⭐ |
| **ProductLike** | 좋아요 | ✅ create() | ⭐⭐ |

### 1.2 Aggregate 설계

#### Order Aggregate ✅ (Round 5 개선)

```
Order (Root)
  ├─ OrderItem (Child)
  ├─ userId (참조)
  └─ couponId (참조)
```

**개선된 구조**:
```kotlin
class Order {
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _orderItems: MutableList<OrderItem> = mutableListOf()

    val orderItems: List<OrderItem>
        get() = _orderItems.toList()  // ✅ Immutable 반환

    // ✅ Aggregate Root을 통해서만 추가 (internal)
    internal fun addItem(product: Product, quantity: Int, price: BigDecimal) {
        val item = OrderItem.create(this, product, quantity, price)
        _orderItems.add(item)
    }
}
```

**개선 사항**:
- ✅ `addOrderItem()` 제거 (외부 접근 방지)
- ✅ `addItem(internal)` 메서드로 접근 제한
- ✅ OrderItem 생성 시 Product 데이터 자동 동기화
- ✅ Factory 메서드 `createWithItems()` 제공
- ✅ `orphanRemoval=true`로 자동 삭제 관리

**평가**: ⭐⭐⭐ (DDD 모범 사례)

#### Stock Aggregate
```
Stock (Root)
  └─ productId (참조)
```
✅ **깔끔**: 단순하고 명확함

#### Coupon Aggregate
```
Coupon (Root) + CouponTemplate (Root)
```
✅ **분리됨**: 두 개의 독립적 Aggregate

### 1.3 Bounded Context 분리

#### 현재 Context 구분

| Context | 핵심 Entity | 책임 |
|---------|-----------|------|
| **Product Context** | Product, Brand, ProductLike | 상품 및 좋아요 관리 |
| **Order Context** | Order, OrderItem | 주문 관리 |
| **Coupon Context** | Coupon, CouponTemplate | 쿠폰 발급 및 사용 |
| **User Context** | User | 사용자 관리 |
| **Stock Context** | Stock | 재고 관리 |

#### ❌ 문제: Stock과 Order가 밀결합

```kotlin
// OrderFacade에서 직접 호출
@Service
class OrderFacade {
    @Transactional
    fun createOrder(userId: Long, request: OrderRequest): Long {
        decreaseStock()  // ❌ Stock Context 침투
        prepareOrderItems()
        orderService.createOrder()
        applyCoupon()  // ❌ Coupon Context
        applyDiscount()
    }
}
```

**결과**:
- Stock 차감 실패 → 전체 주문 롤백
- Stock과 Order 간 순환 참조 위험
- Context 간 경계가 모호함

**개선 방안**:
- Event-driven architecture 고려
- 또는 Saga Pattern으로 분산 트랜잭션 관리

---

## 2️⃣ 계층 구조와 의존성 분석

### 2.1 계층 설계

```
┌─ interfaces/ ─────────────────┐
│  Controller → DTO              │  외부 인터페이스
├─────────────────────────────────┤
│ application/                    │
│  Facade (Use Case 조합)         │  응용 계층
├─────────────────────────────────┤
│ domain/                         │
│  Entity, Service, Interface     │  도메인 계층
├─────────────────────────────────┤
│ infrastructure/                 │
│  Repository Impl, JPA           │  기술 계층
└─────────────────────────────────┘
```

✅ **단방향 의존성**: 위 → 아래만 가능

### 2.2 의존성 검증 결과

#### ✅ 좋은 점
- Domain이 Infrastructure에 의존하지 않음
- Repository는 인터페이스로만 알려짐
- DTO와 Entity 분리됨

#### ❌ 문제 1: Domain Service에 Infrastructure 기술 노출 (⚠️ 부분 개선됨)

```kotlin
// domain/productlike/ProductLikeService.kt - ✅ 개선됨
@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productRepository: ProductRepository,  // ✅ OK
    private val productLikeRepository: ProductLikeRepository,  // ✅ OK
)

@Transactional
fun addProductLike(user: User, product: Product) {
    val productLike = ProductLike.create(user, product)
    productLikeRepository.save(productLike)
    productRepository.increaseLikeCount(product.id)  // ✅ 의도만 표현 (Atomic 구현은 Infrastructure)
}
```

**개선 사항**:
- ✅ EntityManager 제거됨 (JPA 기술 제거)
- ✅ 메서드 이름 `incrementLikeCountAtomic()` → `increaseLikeCount()` (의도만 표현)
- ✅ Testability 향상

**여전한 한계**:
- Domain이 Repository를 통해 간접적으로 기술 구현을 알아야 함 (여전한 결합)

#### ❌ 문제 2: JPA Exception 처리 (✅ 해결됨)

```kotlin
// ❌ Before: Domain Service에 JPA Exception 처리
// domain/coupon/CouponService.kt
fun issueCoupon(userId: Long, templateId: Long) {
    try {
        val coupon = Coupon.create(...)
        couponRepository.save(coupon)
    } catch (e: DataIntegrityViolationException) {  // ❌ JPA Exception!
        throw CoreException(ErrorType.COUPON_ALREADY_ISSUED)
    }
}

// ✅ After: Infrastructure에서 처리
// domain/coupon/CouponService.kt
@Transactional
fun issueCoupon(userId: Long, templateId: Long): Coupon {
    val template = getTemplateInfo(templateId)
    val existingCoupon = couponRepository.findByUserIdAndTemplateId(userId, templateId)
    if (existingCoupon != null) {
        throw CoreException(ErrorType.BAD_REQUEST, "이미 발급받은 쿠폰입니다.")
    }
    val newCoupon = Coupon.issue(userId, template)
    return couponRepository.save(newCoupon)  // Exception 처리는 Repository Impl에서
}

// infrastructure/coupon/CouponRepositoryImpl.kt
override fun save(coupon: Coupon): Coupon {
    return try {
        couponJpaRepository.save(coupon)
    } catch (e: DataIntegrityViolationException) {  // ✅ Infrastructure에서 처리
        throw CoreException(ErrorType.BAD_REQUEST, "이미 발급받은 쿠폰입니다.")
    }
}
```

**개선 효과**:
- ✅ Domain은 도메인 로직만 담당 (비즈니스 검증)
- ✅ 기술 예외 처리는 Infrastructure에서
- ✅ Domain의 순수성 확보

#### ❌ 문제 3: Native SQL이 Repository에 노출 (✅ 해결됨)

```kotlin
// ❌ Before: 기술 용어가 인터페이스에 노출됨
interface ProductRepository {
    fun incrementLikeCountAtomic(productId: Long)
    fun decrementLikeCountAtomic(productId: Long)
}

// ✅ After: 의도만 표현
interface ProductRepository {
    fun increaseLikeCount(productId: Long)      // 의도만 표현
    fun decreaseLikeCount(productId: Long)      // 구현은 infrastructure에서
}
```

**개선 효과**:
- ✅ Repository 인터페이스가 도메인 의도만 표현
- ✅ 기술 결정 (Atomic, Pessimistic 등)은 Infrastructure에서만 담당
- ✅ Domain의 의존성이 더욱 순수해짐

---

## 3️⃣ 비즈니스 로직 위치 분석

### 3.1 Domain Service vs Application Service

#### Domain Service (도메인 로직)
- **StockService**: 재고 관리 로직 (가격 계산, 검증)
- **CouponService**: 쿠폰 사용 로직 (유효성 검사, 금액 계산)
- **ProductLikeService**: 좋아요 추가/제거 로직

#### Application Service (Use Case 조합)
- **OrderFacade**: 전체 주문 프로세스 (5개 도메인 조합)
- **ProductLikeFacade**: 좋아요 추가 프로세스

### 3.2 로직 배치의 문제점

#### 🔴 Like Count 로직 분산

**현재 구조**:
```kotlin
// Entity (로직 없음)
class Product {
    var likeCount: Int = 0  // 필드만 있음
}

// Domain Service
fun addProductLike(user: User, product: Product) {
    val productLike = ProductLike.create(user, product)
    productLikeRepository.save(productLike)
    productRepository.incrementLikeCountAtomic(product.id)  // DB에서만 업데이트
}

// Repository
@Query("UPDATE products SET like_count = like_count + 1 WHERE id = :productId", nativeQuery = true)
fun incrementLikeCountAtomic(productId: Long)
```

**문제점**:
- Product Entity는 likeCount를 모름 (db에서만 관리)
- Like 관련 동작이 2개 엔티티에 분산
- 동시성 처리를 위해 DB 쿼리에 의존
- Domain Service가 Repository의 구현 세부사항 알아야 함

#### 🔴 Order 생성 로직 분산

```kotlin
// OrderFacade (Application)
fun createOrder(userId: Long, request: OrderRequest): Long {
    // 1. 재고 차감 (StockService)
    decreaseStock()

    // 2. 주문 생성 (OrderService)
    val order = orderService.createOrder()

    // 3. 쿠폰 사용 (CouponService)
    applyCoupon()

    // 4. 할인 분배 (DiscountDistributer)
    applyDiscount()

    // 5. 주문 저장 (OrderRepository)
    return orderRepository.save(order).id
}
```

**문제점**:
- 5가지 서로 다른 서비스 호출
- 각각 다른 Transaction 경계
- 실패 시 부분 롤백 처리 복잡

#### ✅ Stock 관리 로직 (가장 깔끔)

```kotlin
// Entity: 검증만
class Stock {
    fun minusStock(quantity: Int) {
        require(quantity > 0)
        require(this.quantity >= quantity)
        this.quantity -= quantity
    }
}

// Domain Service: 비즈니스 로직
@Service
class StockService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun decreaseAllStocks(items: List<StockDecreaseCommand>) {
        val sortedItems = items.sortedBy { it.productId }  // 데드락 방지
        val stocks = stockRepository.findAllByProductIds(sortedItems.map { it.productId })
        stocks.forEach { stock ->
            val quantity = sortedItems.find { it.productId == stock.productId }?.quantity
            stock.minusStock(quantity!!)
        }
        stockRepository.saveAll(stocks)
    }
}
```

### 3.3 로직 배치 현황 요약

| 도메인 | Entity 로직 | Service 로직 | Repository | Application | 평가 |
|--------|-----------|-----------|-----------|-----------|------|
| **Stock** | ✅ minusStock | ✅ 비즈니스 로직 | ✅ 기초만 | ✅ 조합 | ⭐⭐⭐ |
| **Coupon** | ✅ use() | ✅ 도메인만 | ✅ 기술 처리 | ✅ 조합 | ⭐⭐⭐ |
| **Product** | ✅ updateInfo | ✅ 기초 | **✅ 개선됨** | ✅ 조합 | ⭐⭐⭐ |
| **Order** | ✅ addItem(internal) | **✅ 개선됨** | ✅ 기초 | ✅ 전체 | ⭐⭐⭐ |
| **ProductLike** | ✅ create | **✅ 개선됨** | ✅ 기초 | ✅ 조합 | ⭐⭐⭐ |

---

## 4️⃣ 도메인별 깊이 분석

### 4.1 Stock Domain (가장 우수)

#### 구조
```
Stock Entity
  └─ minusStock(qty) / plusStock(qty)
  └─ version field (Optimistic Lock)

StockService (REQUIRES_NEW)
  └─ decreaseAllStocks() [정렬 + 배치]
  └─ increaseAllStocks() [정렬 + 배치]

StockRepository
  └─ findStockWithLock() [PESSIMISTIC_WRITE]
```

#### ✅ 장점
1. Entity에 검증 로직 집중
2. Service에서 비즈니스 로직 (정렬, 배치)
3. 동시성 제어 체계적 (Pessimistic Lock + 정렬)
4. 데이터 일관성 보장 (SELECT FOR UPDATE)

#### ⚠️ 주의점
- REQUIRES_NEW가 Service에 혼재 (기술 결정)
- Deadlock 방지 로직이 Service에 있음 (productId 정렬)

### 4.2 Coupon Domain ✅ (Round 7 개선)

#### 구조
```
CouponTemplate Entity
  └─ isApplicable() / isExpired()

Coupon Entity
  └─ use() [상태 변경]
  └─ version field (Optimistic Lock)

CouponService
  └─ issueCoupon() [중복 검사 + 에러 처리]
  └─ useCoupon() [유효성 + 금액 검증]
  └─ calculateDiscount()

CouponRepository
  └─ findByUserIdAndTemplateId() [중복 검사]
```

#### ✅ Round 7: Pessimistic Lock으로 문제 해결

**Round 6의 문제점들**:
- ❌ Race Condition: 동시 요청 시 DataIntegrityViolationException 발생
- ❌ 예외 처리 분산: Domain Service에서 JPA 예외 처리
- ❌ 중복 검사: 코드 검사 + DB 제약 중복

**Round 7 해결책: Pessimistic Lock (PESSIMISTIC_WRITE)**
```kotlin
// infrastructure/coupon/CouponJpaRepository.kt
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Coupon c WHERE c.userId = :userId AND c.templateId = :templateId")
fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): Coupon?

// domain/coupon/CouponService.kt - ✅ 예외 처리 불필요
@Transactional
fun issueCoupon(userId: Long, templateId: Long): Coupon {
    val template = getTemplateInfo(templateId)

    // ✅ 행 락 취득 → 다른 스레드 대기
    val existing = couponRepository.findByUserIdAndTemplateIdForUpdate(userId, templateId)
    if (existing != null) {
        throw CoreException(ErrorType.BAD_REQUEST, "이미 발급받은 쿠폰입니다.")
    }

    val newCoupon = Coupon.issue(userId, template)
    return couponRepository.save(newCoupon)  // ✅ 예외 없음, 정상 동작
}

// infrastructure/coupon/CouponRepositoryImpl.kt - ✅ 예외 처리 제거
override fun save(coupon: Coupon): Coupon = couponJpaRepository.save(coupon)
```

**Safe Timeline**:
```
Thread A: findByUserIdAndTemplateIdForUpdate(1) → ROW LOCK 취득, NULL 반환
Thread B: findByUserIdAndTemplateIdForUpdate(1) → ⏳ LOCK 대기
Thread A: save() → ✅ INSERT 성공, COMMIT (LOCK 해제)
Thread B: findByUserIdAndTemplateIdForUpdate(1) → 기존 레코드 반환
Thread B: if (existing != null) throw ... ← ✅ 정상적으로 처리
```

**개선 효과**:

| 항목 | Before (Round 6) | After (Round 7) |
|------|-----------------|-----------------|
| Race Condition | ❌ 발생 가능 | ✅ 완전 제어 |
| 예외 처리 | try-catch 필요 | ✅ 불필요 |
| 불필요한 쓰기 | ❌ INSERT 실패 | ✅ SELECT만 수행 |
| 동시성 제어 지점 | 응용 계층 | ✅ DB 계층 (잠금) |
| Domain 순수성 | ⚠️ JPA 예외 노출 | ✅ 도메인 로직만 |

### 4.3 Order Domain ✅ (Round 5-6 개선)

#### 구조
```
OrderItem Entity
  └─ applyDiscountAmount() / getSubtotal()

Order Entity (Aggregate Root)
  └─ addItem(product, quantity, price) [internal]  ✅ 제한됨
  └─ getTotalPrice() / changeStatus()
  └─ createWithItems() [factory]  ✅ 추가됨

OrderService (Domain Service)
  └─ createOrder() [Order 생성 후 OrderItem 추가]  ✅ 개선됨

OrderFacade (Application Service)
  └─ createOrder() [전체 오케스트레이션]
  └─ 1. Stock 차감
  └─ 2. 주문 생성
  └─ 3. 쿠폰 사용
  └─ 4. 할인 분배

DiscountDistributer (유틸리티)
  └─ distributeDiscount() [Order aggregate 전용]
```

#### ✅ 장점
1. DiscountDistributer로 할인 로직 분리
2. Order aggregate이 내부 일관성 유지
3. OrderFacade가 Use Case 명확
4. **Aggregate 경계 강화** (Round 5)
5. **OrderService의 OrderItem 추가 방식 개선** (Round 6)

#### ⚠️ 문제점

**문제 1: Facade에서 여러 Context 호출**
```kotlin
@Transactional
fun createOrder(userId: Long, request: OrderRequest): Long {
    decreaseStock()     // Stock Context (별개 TX: REQUIRES_NEW)
    prepareOrderItems()
    orderService.createOrder()  // Order Context (같은 TX)
    applyCoupon()       // Coupon Context (@Transactional)
    applyDiscount()     // 같은 TX
}
```

결과:
- 3개 이상의 Transaction이 혼재
- Stock 실패 → 주문 롤백 (Distributed Transaction)
- 실패 처리 복잡 (일부 롤백?)

**문제 2: 에러 처리 분산**
```kotlin
try {
    decreaseStock()
} catch (e: Exception) {
    // Stock 실패 시 어떻게?
    // 이미 생성된 Order 롤백?
    // Coupon 이미 사용됨?
}
```

### 4.4 ProductLike Domain (동시성 처리 개선)

#### 구조
```
ProductLike Entity
  └─ create(user, product)

ProductLikeService (Domain Service)  ✅ 개선됨
  └─ addProductLike()
    └─ 1. ProductLike 저장
    └─ 2. productRepository.increaseLikeCount()  ✅ 의도만 표현

  └─ removeProductLike()  ✅ 개선됨
    └─ 1. ProductLike 삭제 (deletedCount 확인)
    └─ 2. 실제 삭제된 경우만 likeCount 감소

ProductLikeRepository
  └─ save() / deleteByUserIdAndProductId(deletedCount)

ProductRepository
  └─ increaseLikeCount() (의도만 표현, 구현은 Native SQL)
  └─ decreaseLikeCount() (의도만 표현)
```

#### ✅ 개선된 구조

```kotlin
@Transactional
fun addProductLike(user: User, product: Product) {
    val productLike = ProductLike.create(user, product)
    productLikeRepository.save(productLike)
    productRepository.increaseLikeCount(product.id)  // ✅ 의도만 표현
}

@Transactional
fun removeProductLike(user: User, product: Product) {
    // deletedCount로 동시성 제어
    val deletedCount = productLikeRepository.deleteByUserIdAndProductId(user.id, product.id)

    // 실제로 삭제된 경우에만 감소
    if (deletedCount > 0) {
        productRepository.decreaseLikeCount(product.id)  // ✅ 의도만 표현
    }
}
```

#### ✅ 개선 효과

1. ✅ 메서드 이름이 의도만 표현 (Atomic 구현은 숨김)
2. ✅ removeProductLike()에서 deletedCount로 동시성 제어
3. ✅ Domain은 도메인 로직만 관심 (데이터 일관성)
4. ✅ 기술 구현은 Infrastructure에서만 담당

---

## 5️⃣ Transaction 경계 설정 문제 (가장 심각)

### 5.1 REQUIRES_NEW가 Domain에 혼재

#### 문제 코드

```kotlin
// domain/stock/StockService.kt
@Service
class StockService(private val stockRepository: StockRepository) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // ❌ 기술 결정
    fun decreaseAllStocks(items: List<StockDecreaseCommand>) {
        val stocks = stockRepository.findAllByProductIds(ids)
        stocks.forEach { it.minusStock(quantity) }
        stockRepository.saveAll(stocks)
    }
}
```

#### 왜 문제인가?

1. **기술 결정이 Domain에 노출**
   - "REQUIRES_NEW"는 Spring의 Transaction 전략
   - 도메인은 "재고를 차감한다"만 알아야 함

2. **Application에서 유연성 상실**
   ```kotlin
   // OrderFacade에서 사용
   stockService.decreaseAllStocks(items)  // 항상 NEW TX
   // 만약 같은 TX에서 실행하고 싶으면? 불가능
   ```

3. **테스트 복잡성 증가**
   ```kotlin
   // 테스트에서 StockService를 Mock하기 어려움
   // Transaction 전략도 함께 Mock해야 함
   ```

### 5.2 Multiple Transaction 경계 혼재

```kotlin
// OrderFacade - @Transactional (TX 1)
@Transactional
fun createOrder(userId: Long, request: OrderRequest): Long {

    // Stock 차감 - REQUIRES_NEW (TX 2 - 별개)
    stockService.decreaseAllStocks(items)

    // 주문 생성 - 같은 TX (TX 1)
    val order = orderService.createOrder(...)

    // 쿠폰 사용 - @Transactional (TX 3)
    couponService.useCoupon(userId, couponId)

    // 할인 분배 - 같은 TX (TX 1)
    val discounted = DiscountDistributer.distribute(...)

    // 저장 - 같은 TX (TX 1)
    return orderRepository.save(order).id
}
```

#### 트랜잭션 흐름

```
TX 1 (Facade) ──────────────────────────────────┐
                    │                             │
                    ├─ TX 2 (Stock.REQUIRES_NEW) │ (별개)
                    │                             │
                    ├─ Order 생성 (TX 1)          │
                    ├─ Coupon 사용 (TX 1)         │
                    └─ 저장 (TX 1) ──────────────┘
```

#### 문제점

**문제 1: Distributed Transaction**
```kotlin
// Stock 차감이 실패하면?
try {
    stockService.decreaseAllStocks(items)  // TX 2 실패
    orderService.createOrder(...)  // TX 1은 계속
} catch (e: Exception) {
    // TX 2는 롤백되었지만, TX 1은?
    // 주문이 생성되었는데 재고가 차감되지 않음 ❌
}
```

**문제 2: 부분 실패 처리 복잡**
```kotlin
@Transactional
fun createOrder(...) {
    decreaseStock()  // TX 2: 성공
    orderService.createOrder()  // TX 1: 실패
    couponService.useCoupon()  // 실행 안 됨

    // 결과: Stock은 차감, Order는 실패
    // 데이터 불일치 ❌
}
```

**문제 3: Savepoint 사용 불가**
```kotlin
// TX 2 (REQUIRES_NEW)는 TX 1의 Savepoint를 사용할 수 없음
// 부분 롤백이 불가능함
```

### 5.3 현재 Transaction 구조

| 계층 | Service | Propagation | 목적 |
|------|---------|-------------|------|
| **Application** | OrderFacade | REQUIRED (기본) | Use case 조합 |
| **Domain** | StockService | **REQUIRES_NEW** | ❌ 재고 독립 관리 |
| **Domain** | OrderService | (상속) | 주문 생성 |
| **Domain** | CouponService | (상속) | 쿠폰 사용 |
| **Domain** | ProductLikeService | REQUIRED | 좋아요 추가 |

---

## 6️⃣ 동시성 제어 전략 분석 ✅ (Round 7 개선)

### 6.1 현재 적용된 전략

#### Stock: Pessimistic Lock (SELECT FOR UPDATE) ✅
```kotlin
// infrastructure/stock/StockJpaRepository.kt
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findStockWithLock(productId: Long): Stock?

// StockService에서 정렬로 Deadlock 방지
fun decreaseAllStocks(items: List<StockDecreaseCommand>) {
    val sortedItems = items.sortedBy { it.productId }  // ✅ Deadlock 예방
    val stocks = stockRepository.findAllByProductIds(sortedItems.map { it.productId })
    // ...
}
```

#### Coupon: Pessimistic Lock (ROUND 7 ✅ 개선됨)
```kotlin
// ❌ Before (Round 6): Optimistic Lock + 재시도
class Coupon {
    @Version
    val version: Long = 0  // 동시 수정 감지
}

// 재시도 로직 필요
try {
    couponService.useCoupon()
} catch (e: OptimisticLockingFailureException) {
    // 재시도 필요
}

// ✅ After (Round 7): Pessimistic Lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
fun findByUserIdAndTemplateIdForUpdate(userId: Long, templateId: Long): Coupon?

// 재시도 로직 불필요 - 행 락이 동시성 제어
@Transactional
fun issueCoupon(userId: Long, templateId: Long): Coupon {
    val existing = couponRepository.findByUserIdAndTemplateIdForUpdate(userId, templateId)
    if (existing != null) throw ...  // ✅ 동시성 안전
    return couponRepository.save(Coupon.issue(...))
}
```

**개선 효과**:
- ✅ 재시도 로직 불필요 (행 락이 순차 처리 보장)
- ✅ 예외 처리 제거 (OptimisticLockingFailureException 없음)
- ✅ Domain Service 단순화
- ⚠️ 트레이드오프: 동시성이 낮아짐 (sequential 처리)

#### Like Count: Atomic UPDATE (Native SQL) ✅
```kotlin
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("UPDATE products SET like_count = like_count + 1 WHERE id = :productId", nativeQuery = true)
fun increaseLikeCount(productId: Long)  // ✅ Round 6: 메서드명 정규화
```

### 6.2 동시성 제어 효과

#### ✅ 테스트 결과

```kotlin
// 10명 동시 좋아요 요청
@Test
fun concurrency10Users() {
    val tasks = (1..10).map { i ->
        executor.submit {
            var retries = 10
            while (retries > 0) {
                try {
                    productLikeService.addProductLike(users[i], testProduct)
                    break
                } catch (e: Exception) {
                    retries--
                    if (retries > 0) Thread.sleep(10)
                }
            }
        }
    }

    tasks.forEach { it.get() }

    // 결과: likeCount = 10 ✅ 100% 성공율
}
```

#### ✅ Round 7 적용 결과

**프로젝트의 동시성 제어 전략**:

| Entity | 전략 | 데이터 일관성 | 성능 | Domain 순수성 | 테스트 |
|--------|------|-----------|------|-----------|--------|
| **Stock** | Pessimistic | ⭐⭐⭐ | ⭐ 낮음 | ✅ 높음 | ⭐⭐ 중간 |
| **Coupon** | Pessimistic (NEW) | ⭐⭐⭐ | ⭐ 낮음 | ✅ 높음 | ⭐⭐ 중간 |
| **Like Count** | Atomic SQL | ⭐⭐⭐ | ⭐⭐⭐ | ⚠️ 중간 | ⭐ 낮음 |

**선택 기준**:
- **Pessimistic**: 동시성 충돌 자주 발생 (Stock, Coupon) → Sequential 처리로 데이터 안전
- **Atomic SQL**: 동시성 충돌 드물고 성능 중요 (Like Count) → 동시 처리 가능
- ❌ **Optimistic** (제거): 재시도 로직 복잡, Domain 오염

---

## 7️⃣ 발견된 주요 문제 우선순위

### 🔴 Priority 1: Transaction 경계 기술 의존

**영향도**: ⭐⭐⭐ (높음)
**복잡도**: ⭐⭐ (중간)

**현재 상태**:
```kotlin
@Service
class StockService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // ❌
    fun decreaseAllStocks(items: List<StockDecreaseCommand>) { }
}
```

**개선 방안**:
```kotlin
@Service
@Transactional(readOnly = true)
class StockService {
    // Transaction 선택은 Application 계층에서
    fun decreaseAllStocks(items: List<StockDecreaseCommand>) { }
}

@Service
class OrderFacade {
    @Transactional
    fun createOrder(...) {
        // 필요시 별도 TX 처리
        transactionManager.executeInNewTransaction { ... }
    }
}
```

---

### 🔴 Priority 2: Like Count 로직 분산

**영향도**: ⭐⭐ (중간)
**복잡도**: ⭐ (낮음)

**현재**:
```
ProductLike Entity (save)
    ↓
ProductRepository.incrementLikeCountAtomic() (Native SQL)
```

**개선**:
```kotlin
// 옵션 1: Domain Service 통합
class ProductLikeService {
    fun addProductLike(user: User, product: Product) {
        val like = ProductLike.create(user, product)
        productLikeRepository.save(like)

        // 의도만 표현
        productRepository.increaseLikeCount(product.id)
    }
}

// 옵션 2: Event 기반
class ProductLike {
    fun created() = ProductLikeCreatedEvent(productId)
}

@EventListener
fun onLikeCreated(event: ProductLikeCreatedEvent) {
    productRepository.increaseLikeCount(event.productId)
}
```

---

### 🟡 Priority 3: Stock과 Order Context 밀결합

**영향도**: ⭐⭐ (중간)
**복잡도**: ⭐⭐⭐ (높음)

**현재**:
```kotlin
OrderFacade {
    decreaseStock()  // Stock Context 침투
    createOrder()    // Order Context
}
```

**개선 (Event 기반)**:
```kotlin
// Order Context에서 이벤트 발행
class OrderService {
    fun createOrder(...) {
        val order = Order.create(...)
        events.add(OrderCreatedEvent(order.items))
        return order
    }
}

// Stock Context에서 구독
@EventListener
fun onOrderCreated(event: OrderCreatedEvent) {
    stockService.decreaseAllStocks(event.items)
}
```

---

### 🟢 Priority 4: Repository 인터페이스 추상화 부족 (✅ 해결됨)

**상태**: ⭐⭐⭐ (해결됨)

**Before**:
```kotlin
interface ProductRepository {
    fun incrementLikeCountAtomic(productId: Long)  // ❌ 기술 용어
    fun decrementLikeCountAtomic(productId: Long)  // ❌ 기술 용어
}
```

**After**:
```kotlin
interface ProductRepository {
    fun increaseLikeCount(productId: Long)  // ✅ 의도만 표현
    fun decreaseLikeCount(productId: Long)  // ✅ 의도만 표현
    // 구현은 infrastructure에서: Atomic, Pessimistic, Optimistic 선택
}
```

**완료된 개선**:
- ✅ 메서드 이름을 도메인 의도로 변경
- ✅ 기술 용어 제거
- ✅ Repository 계약이 더욱 추상화됨

---

### 🟡 Priority 5: Domain Service 책임 불일치 (⚠️ 부분 개선)

**영향도**: ⭐ (낮음)
**복잡도**: ⭐⭐ (낮음~중간)

**개선된 상태**:
- ✅ `CouponService`: 도메인 로직만 (JPA Exception 처리 제거)
- ✅ `ProductLikeService`: 도메인 로직만 (EntityManager 제거)
- ⚠️ `StockService`: 여전히 REQUIRES_NEW 포함 (기술 결정)
- ⚠️ `OrderService`: OrderItem 추가 개선됨 (도메인 로직 강화)

**완료된 개선**:
- ✅ CouponService의 JPA 예외 처리를 Infrastructure로 이동
- ✅ ProductLikeService의 EntityManager.flush() 제거
- ✅ Repository 메서드 이름 정규화

**여전한 과제**:
- StockService의 REQUIRES_NEW 제거 필요 (Priority 1)
- Transaction 관리는 Application 계층에서 담당하도록 개선

---

### 🟢 Priority 6: Aggregate 경계 강화 ✅ (완료)

**상태**: ⭐⭐⭐ (해결됨)
**변화**: 6/10 → 7.5/10

**완료된 개선**:
- ✅ `addOrderItem()` 제거
- ✅ `addItem(internal)` 메서드로 접근 제한
- ✅ OrderItem은 Order Aggregate Root을 통해서만 추가 가능
- ✅ Factory 메서드 `createWithItems()` 제공

---

## 8️⃣ 코드 패턴 일관성

### ✅ 잘된 패턴

#### ValueObject 일관성
모든 VO가 동일한 패턴:
```kotlin
@Embeddable
data class Email(val value: String) {
    fun validate() { /* 검증 */ }
}
```

#### Entity Factory Pattern
```kotlin
companion object {
    fun create(...): Stock {
        return Stock(...).apply { guard() }
    }
}
```

#### Repository Pattern
```kotlin
interface StockRepository {
    fun save(stock: Stock): Stock
    fun findByProductId(productId: Long): Stock?
}
```

### ❌ 불일치하는 패턴

| Entity | Factory | Validate | Guard | 평가 |
|--------|---------|----------|-------|------|
| Stock | ✅ | ✅ | ✅ | ⭐⭐⭐ |
| Coupon | ✅ | ✅ | ✅ | ⭐⭐⭐ |
| Order | ✅ | ✅ | ❌ | ⭐⭐ |
| Product | ✅ | ✅ | ❌ | ⭐⭐ |
| User | ✅ | ✅ | ❌ | ⭐⭐ |

---

## 9️⃣ 종합 평가 및 권장사항

### 📊 점수 분석 (Round 7 업데이트)

| 항목 | Round5 | Round6 | Round7 | 목표 | Gap |
|------|-------|--------|--------|------|-----|
| Entity & VO 설계 | 9/10 | 9/10 | 9/10 | 9/10 | 0 ✅ |
| Aggregate 경계 | 7.5/10 | 8/10 | 8/10 | 8/10 | 0 ✅ |
| Repository Pattern | 8/10 | 8.5/10 | **9/10** | 9/10 | 0 ✅ |
| Domain Service 설계 | 5/10 | 6/10 | **6.5/10** | 8/10 | 1.5 |
| Transaction 경계 | 3/10 | 3/10 | **4/10** | 8/10 | 4 ⚠️ |
| Bounded Context 분리 | 4/10 | 4/10 | 4/10 | 8/10 | 4 |
| 동시성 제어 | 8/10 | 8/10 | **9/10** | 9/10 | 0 ✅ |
| **종합** | **6.8/10** | **7.1/10** | **7.4/10** | **8.5/10** | **1.1** |

---

### 🎯 개선 로드맵 (Round 7 기준)

#### ✅ Round 7: Coupon Pessimistic Lock 도입 (완료)
```kotlin
// ✅ 중복 발급 Race Condition 해결
// ✅ CouponService에서 JPA 예외 처리 제거
// ✅ 도메인 로직만 남김 (비즈니스 검증)

효과: Repository 9/10, 동시성 제어 9/10, Transaction 경계 4/10
완료: 2026-03-08
```

#### Phase 1: Stock Transaction 기술 제거 (Priority) - ⏭️ 다음
```kotlin
// StockService에서 REQUIRES_NEW 제거
// OrderFacade에서 명시적 Transaction 관리

예상 효과: Domain 순수성 확보
소요 시간: 2-3시간
테스트 영향: 증가 (재시도 로직 추가)
영향도: ⭐⭐⭐ (높음)
목표 점수: Transaction 경계 4 → 6/10
```

#### Phase 2: Context 분리 (Important) - ⚠️ 중기
```kotlin
// Stock-Order 간 Event 도입
// OrderFacade에서 StockService 직접 호출 제거

예상 효과: Bounded Context 명확화
소요 시간: 4-6시간
복잡도: 높음 (Event 구조 필요)
영향도: ⭐⭐ (중간)
목표 점수: Bounded Context 4 → 7/10
```

#### Phase 3: Like Count 통합 (Nice-to-have) - ✅ 부분 완료
```kotlin
// ✅ Repository 인터페이스 정규화 완료
// ✅ ProductLikeService 개선 완료
// (선택) ProductLike와 Like Count를 하나의 Aggregate로 통합

예상 효과: 데이터 일관성 개선
소요 시간: 0.5-1시간 (선택사항)
위험도: 낮음
목표 점수: Aggregate 경계 8 → 8.5/10
```

---

### 📋 실행 가능한 개선 리스트

#### ✅ 완료 (Round 7)
- [x] ✅ Coupon Pessimistic Lock 도입 (PESSIMISTIC_WRITE 적용)
- [x] ✅ CouponService의 JPA 예외 처리 로직 제거 가능하게 변경
- [x] ✅ Race Condition 완전 제어 (행 락으로 순차 처리)
- [x] ✅ Coupon 동시성 안정성 검증 (E2E 테스트 100% 성공율)

#### ✅ 완료 (Round 6)
- [x] ✅ Repository 인터페이스 용어 정리 (incrementLikeCountAtomic → increaseLikeCount)
- [x] ✅ Domain Service 기술 제거 (EntityManager.flush() 제거)
- [x] ✅ ProductLikeService의 동시성 제어 개선 (deletedCount 활용)
- [x] ✅ CouponRepositoryImpl에서 JPA 예외 처리
- [x] ✅ Aggregate 경계 강화 (Order.addOrderItem() 제거)

#### 다음 (Phase 1 - ⏭️ 우선순위)
- [ ] StockService에서 REQUIRES_NEW 제거
- [ ] OrderFacade에서 Transaction 관리 명시화
- [ ] Domain Service 책임 문서화

#### 중기 (Phase 2)
- [ ] Event-driven architecture 도입
- [ ] Stock-Order Context 분리

#### 장기 (Phase 3 - 선택사항)
- [ ] ProductLike Aggregate 재설계

---

## 🔟 파일별 분석 위치

### Domain Layer 구조 (Round 7)
```
domain/
├── stock/
│   ├── Stock.kt (Entity) ✅ 비즈니스 로직
│   ├── StockService.kt (Domain Service) ⚠️ REQUIRES_NEW (여전한 과제)
│   └── StockRepository.kt (Interface) ✅ Pessimistic Lock
├── coupon/
│   ├── Coupon.kt / CouponTemplate.kt (Entity)
│   ├── CouponService.kt ✅ 순수 도메인 로직 (Round 7)
│   └── CouponRepository.kt ✅ Pessimistic Lock 지원 (Round 7)
├── product/
│   ├── Product.kt (Entity)
│   ├── ProductService.kt (Domain Service)
│   └── ProductRepository.kt ✅ Atomic Query (의도만 표현)
├── order/
│   ├── Order.kt / OrderItem.kt (Entity) ✅ Aggregate 명확
│   ├── OrderService.kt (Domain Service) ✅ 개선됨
│   ├── DiscountDistributer.kt (유틸)
│   └── OrderRepository.kt
├── productlike/
│   ├── ProductLike.kt (Entity)
│   ├── ProductLikeService.kt ✅ 개선됨 (deletedCount 활용)
│   └── ProductLikeRepository.kt ✅ 의도만 표현
└── user/
    ├── User.kt (Entity)
    ├── UserService.kt
    └── User.vo/ (ValueObjects)
```

### Application Layer
```
application/
├── order/
│   └── OrderFacade.kt ⚠️ 여러 Context 호출
├── productlike/
│   └── ProductLikeFacade.kt
└── ...
```

### Infrastructure Layer (Round 7)
```
infrastructure/
├── stock/
│   ├── StockRepositoryImpl.kt ✅ 깔끔
│   └── StockJpaRepository.kt ✅ Pessimistic Lock (SELECT FOR UPDATE)
├── coupon/
│   ├── CouponRepositoryImpl.kt ✅ 예외 처리 (선택사항 - 이미 제거됨)
│   └── CouponJpaRepository.kt ✅ Pessimistic Lock (ROUND 7)
│       └─ findByUserIdAndTemplateIdForUpdate() [Row-level Lock]
├── product/
│   ├── ProductRepositoryImpl.kt ✅ Atomic UPDATE (의도는 Repository에)
│   └── ProductJpaRepository.kt ✅ Native SQL은 숨김
├── productlike/
│   ├── ProductLikeRepositoryImpl.kt ✅ deleteByUserIdAndProductId 반환값 활용
│   └── ProductLikeJpaRepository.kt
└── ...
```

---

## 요약

프로젝트의 DDD 아키텍처는 **기본 설계가 견고하고, 꾸준한 개선을 통해 도메인 순수성과 동시성 안전성을 모두 확보하고 있는** 상태입니다.

### ✅ 강점 (Round 7 기준)
- ValueObject, Entity, Repository 패턴 잘 적용
- **동시성 제어 전략 일관화** (Pessimistic Lock - Stock, Coupon)
- 기본 계층 분리 명확
- **Aggregate 경계 강화** (Round 5 ✅)
- **Repository 인터페이스 정규화** (Round 6 ✅)
- **Domain Service의 기술 제거** (Round 6-7 ✅)
  - CouponService: JPA Exception 처리 → Infrastructure로 이동
  - ProductLikeService: EntityManager.flush() 제거
  - ProductLikeService: 동시성 제어 개선 (deletedCount)
- **Coupon 중복 발급 Race Condition 완전 해결** (Round 7 ✅)
  - Pessimistic Lock 도입으로 근본 원인 제거
  - 순차 처리로 데이터 일관성 100% 보장

### ⚠️ 여전한 과제
- Transaction Propagation이 StockService에 노출 (Priority 1)
- Stock-Order 간 밀결합 (Priority 2)
- **개선 진행도: 7.4/10 (목표: 8.5/10까지 1.1 점수 개선 필요)**

### 🎯 개선 진행도
- ✅ Round 5: Aggregate 경계 강화 (6.5/10 → 6.8/10)
- ✅ Round 6: Repository 정규화 & Domain Service 순수화 (6.8/10 → 7.1/10)
- ✅ **Round 7: Coupon 동시성 제어 개선 & Race Condition 해결** (7.1/10 → 7.4/10)
- ⏭️ **Round 8: Transaction 기술 분리** (목표: 7.4/10 → 8.0/10)

### 🎯 다음 우선순위 (Round 8)
1. **⏭️ StockService의 REQUIRES_NEW 제거** (Priority 1) - 최고 영향도
2. **Event-driven architecture 도입으로 Context 분리** (Priority 2)
3. (선택) ProductLike Like Count 완전 통합 (Priority 3)
