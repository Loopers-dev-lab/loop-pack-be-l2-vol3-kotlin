# 4주차 동시성 제어 전략 정리

## 전체 구조 한눈에 보기

```
OrderFacade.createOrder() — @Transactional
├─ productService.findByIds()              [조회]
├─ couponService.useCouponForOrder()       [⚡ @Version 낙관적 락]
├─ productService.decreaseStock() × N      [⚡ Atomic Update] (ID 정렬 → 데드락 방지)
├─ brandService.findByIds()                [조회]
└─ orderService.createOrder()              [INSERT]
    └─ orderDomainService.placeOrder()     [순수 객체 협력]

LikeFacade.like()
├─ likeService.like()                      [INSERT + UK 중복 방지]
└─ productService.increaseLikeCount()      [⚡ Atomic Update]

LikeFacade.unlike()
├─ likeService.unlike()                    [soft delete]
└─ productService.decreaseLikeCount()      [⚡ Atomic Update]
```

---

## 전략 매핑 테이블

| 대상 | 전략 | 구현 | 실패 유형 | 재시도 | 비고 |
|------|------|------|----------|--------|------|
| 재고 차감 | Atomic Update | `@Modifying @Query` UPDATE SET stock = stock - ? WHERE stock >= ? | 비즈니스 (재고 부족) | ❌ 불필요 | affectedRows=0 → 예외 |
| 좋아요 +1 | Atomic Update | `@Modifying @Query` UPDATE SET likeCount = likeCount + 1 | 없음 (항상 성공) | ❌ 불필요 | |
| 좋아요 -1 | Atomic Update | `@Modifying @Query` UPDATE SET likeCount = likeCount - 1 WHERE likeCount > 0 | 없음 | ❌ 불필요 | 0 이하 방지 |
| 쿠폰 사용 | @Version 낙관적 락 | CouponIssue Entity `@Version` + dirty checking | 비즈니스 (이미 사용됨) | ❌ 불필요 | 경합 주체 = 같은 유저뿐 |
| 다중 상품 재고 차감 | ID 정렬 | `products.sortedBy { it.id }` 후 순차 차감 | — | — | 데드락 방지 |

---

## 전략별 상세

### 1. Atomic Update — 재고 차감

```sql
UPDATE product
SET stock_quantity = stock_quantity - :quantity
WHERE id = :productId
  AND stock_quantity >= :quantity
  AND deleted_at IS NULL
```

**왜 Atomic Update?**
- 단순 산술 연산 (stock - N) → DB가 한 방에 처리
- 엔티티 메서드(product.decreaseStock())로 하면 조회→가공→쓰기 패턴 → Lost Update 위험
- `@Version`을 Product에 걸면? → 좋아요/재고/상품수정이 같은 version 공유 → 불필요한 경합

**실패 시:**
```
affectedRows = 0 → "상품의 재고가 부족합니다" (CoreException)
→ 재고가 없는 건 다시 해도 없음 → 재시도 무의미
```

### 2. Atomic Update — 좋아요 수

```sql
-- 증가
UPDATE product SET like_count = like_count + 1
WHERE id = :productId AND deleted_at IS NULL

-- 감소
UPDATE product SET like_count = like_count - 1
WHERE id = :productId AND like_count > 0 AND deleted_at IS NULL
```

**왜 @Version이 아닌가?**
- 인기 상품에 다수 유저가 동시 좋아요 → 높은 경합
- Product에 @Version 걸면 재고 차감/상품 수정과 version 충돌 (false contention)
- Atomic Update → 동시성 안전 + 성능 최고

### 3. @Version 낙관적 락 — 쿠폰 사용

```kotlin
// CouponIssue Entity
@Version
var version: Long = 0

fun use() {
    if (status != CouponIssueStatus.AVAILABLE) throw ...
    this.status = CouponIssueStatus.USED
    this.usedAt = ZonedDateTime.now()
}
```

**JPA가 생성하는 SQL:**
```sql
UPDATE coupon_issues
SET status = 'USED', used_at = ?, version = 1
WHERE id = ? AND version = 0
```

**왜 낙관적 락?**
- CouponIssue는 독립 엔티티 → false contention 없음
- 경합 주체 = 같은 유저의 더블클릭뿐 → 경합 극히 낮음
- 엔티티 메서드(`use()`)를 자연스럽게 사용 가능

**왜 비관적 락이 아닌가?**
- 비관적 = "모두 기다려서라도 순서대로 성공시키겠다"
- 쿠폰은 1명만 성공하면 됨 → FOR UPDATE 비용이 과함

**충돌 시:**
```
OptimisticLockingFailureException
→ ApiControllerAdvice에서 409 CONFLICT 반환
→ "동시 요청으로 인해 처리에 실패했습니다"
→ 재시도해봐야 status=USED → 재시도 무의미
```

### 4. ID 정렬 — 데드락 방지

```kotlin
// OrderFacade.createOrder()
products.sortedBy { it.id }.forEach { product ->
    productService.decreaseStock(product.id, quantities[product.id]!!)
}
```

**왜?**
```
정렬 없이 TX-A: [5, 3, 1] / TX-B: [1, 3, 5] 순서로 락 획득
→ TX-A: lock(5) → wait(3, TX-B가 보유)
→ TX-B: lock(1) → wait(5, TX-A가 보유)
→ 💀 DEADLOCK

정렬 후 TX-A: [1, 3, 5] / TX-B: [1, 3, 5] 동일 순서
→ 순차적으로 락 획득 → 데드락 없음
```

---

## 전략 선택 기준

### 각 전략의 본질 — "누가 데이터를 처리하는가?"

| 전략 | 본질 | 동시 요청 처리 |
|------|------|--------------|
| **Atomic Update** | "데이터를 안 봄. DB가 알아서 처리" | 일부 성공, 일부 실패 (조건 불충족 시) |
| **낙관적 락** | "데이터를 읽긴 하는데, 충돌 적으니 실패하면 버림" | 하나만 성공하면 충분 |
| **비관적 락** | "데이터를 직접 보고 판단해야 함. 락 잡고 → 조회 → 가공 → 저장" | 모두 기다려서라도 순서대로 성공 |

### 선택 흐름

```
Q1. 단순 산술 연산인가? (stock-1, count+1)
├─ Yes → Q2. UPDATE 후 그 값을 다시 읽어야 하나?
│        ├─ Yes → 낙관적/비관적 검토
│        └─ No  → ✅ Atomic Update ("DB야 니가 계산해")
└─ No  → Q3. 엔티티 메서드가 필요한가? (상태 전이, 비즈니스 검증)
         ├─ Yes → Q4. 동시 요청 중 하나만 성공하면 충분한가?
         │        ├─ Yes (충돌 적음) → ✅ 낙관적 락
         │        └─ No (모두 성공해야 함, 충돌 많음) → ✅ 비관적 락
         └─ No  → Atomic Update 재검토
```

**우선순위: Atomic Update > 낙관적 > 비관적 > 분산락** (가벼운 것부터)

### 우리 프로젝트에 적용한 판단

| 대상 | 선택 | 판단 근거 |
|------|------|----------|
| 재고 차감 | Atomic Update | 단순 산술 (stock - N). 데이터를 직접 볼 필요 없음 → DB가 처리 |
| 좋아요 수 | Atomic Update | 단순 산술 (count ± 1). 항상 성공 |
| 쿠폰 사용 | 낙관적 락 | 상태 전이(AVAILABLE→USED) + 검증 필요 → 엔티티 메서드. 같은 유저 더블클릭뿐이라 하나만 성공하면 충분 |

### 낙관적 vs 비관적 — 핵심 기준 요약

| 관점 | 낙관적 락 | 비관적 락 |
|------|----------|----------|
| 충돌 빈도 | 낮음 (같은 유저 더블클릭) | 높음 (다수 유저 동시 접근) |
| 성공 기대 | 하나만 성공하면 충분 | 모두 순서대로 성공해야 함 |
| 데이터 접근 | 읽긴 하지만 실패 감수 | 락 잡은 시점에 직접 보고 판단 |
| 예시 | 쿠폰 중복 사용 방지 | 선착순 좌석 예약 (잡은 순서대로 배정) |

### @Version 적용 시 주의: 불필요한 경합 (false contention)

```
⚠️ Product Entity에 @Version을 걸면?
   - 좋아요 증가 → version 증가
   - 재고 차감   → version 증가
   - 상품 수정   → version 증가
   → 전혀 관련 없는 작업끼리 version 충돌

✅ 해결:
   - 독립 엔티티(CouponIssue)에만 @Version 사용
   - Product는 Atomic Update로 각 필드를 독립적으로 처리
```

---

## 재시도(Retry) 판단 기준

### "비즈니스 실패" vs "충돌 실패"

| 실패 유형 | 원인 | 다시 하면? | 재시도 |
|-----------|------|-----------|--------|
| **비즈니스 실패** | 재고 부족, 쿠폰 이미 사용 | 같은 결과 | ❌ 무의미 |
| **충돌 실패** | 동시 수정으로 version 불일치 | 최신 데이터로 성공 가능 | ✅ 의미 있음 |

### 우리 프로젝트의 판단

| 전략 | 실패 원인 | 분류 | 재시도 |
|------|----------|------|--------|
| Atomic Update (재고) | affectedRows=0 → 재고 부족 | 비즈니스 실패 | ❌ |
| Atomic Update (좋아요) | 항상 성공 | — | ❌ |
| @Version (쿠폰) | 충돌 → 이미 USED | 비즈니스 실패 | ❌ |

**결론: 모든 동시성 실패가 비즈니스 실패 → 재시도 로직 불필요**

### 재시도가 필요한 케이스 (가상 시나리오)

```
좌석 배치도를 A와 B가 동시 편집
→ A 먼저 저장 → version 증가
→ B는 version 불일치로 실패
→ B가 최신 배치도를 다시 읽고 수정하면 성공 가능
→ ⚠️ 이 경우엔 재시도 필요!
```

---

## 트랜잭션 롤백 검증

Atomic Update(`@Modifying @Query`)도 같은 TX 안에 있으면 롤백 대상이다.

```
OrderFacade.createOrder() — @Transactional
├─ decreaseStock()     ← Atomic Update 성공 (stock 10→7)
├─ brandService.findByIds() ← 삭제된 브랜드 → 빈 리스트
└─ orderDomainService  ← "브랜드 정보가 없습니다" 예외
→ TX 롤백 → stock 다시 10
```

**E2E 테스트로 검증 완료** (`OrderV1ApiE2ETest.stockRollbackOnOrderFailure`)

---

## 동시성 테스트 현황

| 테스트 | 스레드 | 검증 내용 |
|--------|--------|----------|
| `ProductConcurrencyTest` — 재고 dirty checking | 100 | Lost Update 발생 증명 (기대: 0, 실제: 80+) |
| `ProductConcurrencyTest` — 재고 Atomic Update | 20 | 정확히 10건 성공, 10건 실패 (stock=10) |
| `ProductConcurrencyTest` — 좋아요 dirty checking | 100 | Lost Update 발생 증명 |
| `ProductConcurrencyTest` — 좋아요 Atomic Update | 20 | 20건 모두 성공, likeCount=20 |
| `CouponConcurrencyTest` — 쿠폰 @Version | 20 | 1건 성공, 19건 실패, version=1 |
