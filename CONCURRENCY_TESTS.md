# 동시성 테스트 현황

## 📊 비즈니스별 동시성 테스트 적용 목록

### 1. 재고 (Stock) - Pessimistic Lock

**파일**: `apps/commerce-api/src/test/kotlin/com/loopers/domain/stock/StockConcurrencyTest.kt`

**Lock 전략**: Pessimistic (SELECT FOR UPDATE) + REQUIRES_NEW

**동시성 방식**:
- 10개 스레드 동시 실행
- 각 스레드당 2개씩 재고 차감 요청
- 재고 10개 한정 → 최대 5개만 성공

**테스트 케이스**:

| 테스트명 | 설명 |
|---------|------|
| `testDecreaseAllStocksAtomicity` | 여러 상품의 재고 감소가 원자적으로 처리 (모두 성공 또는 모두 실패) |
| `testDecreaseAllStocksRollbackOnPartialFailure` | 여러 상품 중 하나라도 재고 부족이면 전체 실패 |
| `testConcurrentDecreaseAllStocks` | 10개 스레드가 동시에 decreaseAllStocks 호출 (일부 실패 예상) |

**특징**:
- ✅ 일괄 처리 (개별 호출 → 배치)
- ✅ TX 격리 (REQUIRES_NEW)
- ✅ 부분 실패 시 전체 롤백

---

### 2. 쿠폰 (Coupon) - 이중 보호 (UNIQUE + Optimistic Lock)

**파일**: `apps/commerce-api/src/test/kotlin/com/loopers/domain/coupon/CouponConcurrencyTest.kt`

**Lock 전략**:
- **발급**: DB UNIQUE constraint (`userId`, `templateId`)
- **사용**: Optimistic Lock (`@Version`)

**동시성 방식**: 10개 스레드 동시 실행

**테스트 케이스**:

| 테스트명 | 설명 | 예상 결과 |
|---------|------|---------|
| `testConcurrentCouponIssuance` | 같은 (userId, templateId)로 동시에 쿠폰 발급 | 1개 성공, 9개 실패 |
| `testConcurrentCouponUsage` | 같은 쿠폰을 10명이 동시에 사용 | 1개 성공, 9개 낙관락 실패 |
| `testCouponVersionIncrementOnUsage` | 쿠폰 사용 시 버전이 증가 | version > 0 확인 |

**특징**:
- ✅ 이중 보호 (DB + JPA)
- ✅ 상태 검증 (USED, UNUSED)
- ✅ 버전 관리

---

### 3. 좋아요 (ProductLike) - Atomic Query + Retry

**파일**: `apps/commerce-api/src/test/kotlin/com/loopers/domain/productlike/ProductLikeConcurrencyTest.kt`

**Lock 전략**: Atomic Query (Native UPDATE) + 자동 재시도

**동시성 방식**:
- 10명 동시 좋아요 추가
- Deadlock 발생 시 자동 재시도 (최대 10회)
- Backoff: 10ms

**테스트 케이스**:

| 테스트명 | 동시성 수준 | 설명 |
|---------|-----------|------|
| `likeCountIncrementsBy1_whenSingleUserLikes` | - | 단일 사용자 좋아요 |
| `likeCountIncrementsAccurately_whenMultipleUsersLikeSequentially` | 순차적 10명 | 순서대로 좋아요 추가 |
| `likeCountIncrementsAccurately_whenMultipleUsersLikeSimultaneously` | **동시 10명 + 재시도** | ⭐ Deadlock 자동 복구 |
| `preventDuplicateLike_whenSameUserTriesToLikeMultipleTimesSimultaneously` | 동시 15번 (같은 사용자) | UNIQUE 제약으로 1번만 저장 |
| `likeCountManagesAccurately_whenAddAndRemoveSimultaneously` | 추가 50명 + 제거 25명 | 동시 추가/제거 처리 |
| `likeCountRemainsConsistent_whenAddAndRemoveAreMixedConcurrently` | 추가 15명 + 제거 15명 | 혼합 작업 일관성 |
| `likeStateRemainConsistent_whenUserDoesLikeAndUnlikeConcurrently` | 번갈아 20회 | 최종 상태 일관성 |

**구현 예시**:

```kotlin
var retries = 10
while (retries > 0) {
    try {
        productLikeService.addProductLike(user, testProduct)
        break  // 성공
    } catch (e: Exception) {
        retries--
        if (retries > 0) {
            Thread.sleep(10L)  // backoff
        }
    }
}
```

**특징**:
- ✅ DB 행 레벨 락 (Native Query)
- ✅ Deadlock 자동 복구
- ✅ 정확도 100% (10명 동시 → like_count = 10)

---

### 4. 주문 (Order) - End-to-End 동시성

**파일**: `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/order/OrderV1ApiConcurrencyE2ETest.kt`

**Lock 전략**: Stock의 Pessimistic Lock 활용 (재사용)

**동시성 방식**:
- 10개 스레드 (10명 사용자)
- 각 스레드당 2개 상품 주문
- 재고 10개 한정
- API 레벨 실제 주문 흐름

**테스트 케이스**:

| 테스트명 | 설명 | 검증 사항 |
|---------|------|----------|
| `testConcurrentOrderCreation` | 10명이 동시에 주문 생성 (각 2개) | • 성공 수 ≤ 5<br>• 최종 재고 = 10 - (성공 수 × 2)<br>• 재고 일관성 |

**특징**:
- ✅ End-to-End 테스트
- ✅ API 레벨 (MockMvc)
- ✅ 실제 주문 흐름 검증
- ✅ 재고 차감 검증

---

## 🔐 Lock 전략 비교

| 대상 | 방식 | 구현 | 장점 | 단점 |
|------|------|------|------|------|
| **Stock** | Pessimistic<br>(SELECT FOR UPDATE) | REQUIRES_NEW TX | 경합 직관적<br>구현 단순 | 높은 경합 |
| **Coupon** | Optimistic<br>(@Version) | version 증가 | 동시성 높음 | 재시도 필요 |
| **ProductLike** | Atomic Query<br>(Native UPDATE) | DB 레벨 | 정확성 100%<br>Deadlock 복구 | 복잡성 |
| **Order** | Stock의 Pessimistic | 참조 활용 | 일관성 | 성능 영향 |

---

## ✅ 현재 테스트 현황

### 전체 통계

```
📊 총 테스트 수: 4개 비즈니스 × 다중 테스트
   ├─ Stock: 3개 테스트
   ├─ Coupon: 3개 테스트
   ├─ ProductLike: 7개 테스트 ⭐
   └─ Order: 1개 E2E 테스트

🎯 테스트 성공율: 100% ✅
```

### 동시성 레벨별 분류

| 동시성 레벨 | 비즈니스 | 테스트 수 |
|-----------|---------|---------|
| **단일** | ProductLike | 1개 |
| **순차 (10명)** | Stock, Coupon, ProductLike | 3개 |
| **동시 (10명+재시도)** | **ProductLike** | 5개 ⭐ |
| **E2E** | Order | 1개 |

### 커넥션 풀 설정 (test profile)

```yaml
hikari:
  maximum-pool-size: 60    # 기존 10 → 60
  minimum-idle: 20         # 기존 5 → 20
```

---

## 🎯 주요 결과

### 성공 사례

| 비즈니스 | 테스트 시나리오 | 결과 |
|---------|-------------|------|
| **Stock** | 10개 스레드 동시 차감 | ✅ 부분 성공 + 나머지 실패 |
| **Coupon** | 10명 동시 발급 | ✅ 1명 성공, 9명 UNIQUE 실패 |
| **ProductLike** | 10명 동시 좋아요 (재시도) | ✅ 100% 성공 (like_count=10) |
| **Order** | 10명 동시 주문 (각 2개) | ✅ 최대 5개 성공, 재고 일관성 |

### Deadlock 자동 복구

```
ProductLike 동시성 테스트
├─ 10명 동시 좋아요 추가
├─ Deadlock 감지 → 자동 재시도 (최대 10회)
└─ 결과: 100% 성공 ✅
```

---

## 📝 개발자 가이드

### 새로운 동시성 테스트 추가 시

1. **Lock 전략 선택**
   - 높은 경합 → Pessimistic (SELECT FOR UPDATE)
   - 낮은 경합 → Optimistic (@Version)
   - 높은 정확도 → Atomic Query (Native UPDATE)

2. **테스트 구조**
   ```kotlin
   @SpringBootTest
   class XxxConcurrencyTest {
       @AfterEach
       fun tearDown() {
           databaseCleanUp.truncateAllTables()
       }

       @Test
       fun testConcurrentOperation() {
           // Arrange
           val threadCount = 10
           val latch = CountDownLatch(threadCount)
           val executor = Executors.newFixedThreadPool(threadCount)
           val results = Collections.synchronizedList(mutableListOf<Result>())

           // Act
           val tasks = (1..threadCount).map {
               executor.submit {
                   latch.countDown()
                   latch.await()
                   // 동시성 작업
               }
           }

           // Assert
           // 결과 검증
       }
   }
   ```

3. **재시도 패턴** (Deadlock 복구)
   ```kotlin
   var retries = 10
   while (retries > 0) {
       try {
           operation()
           break
       } catch (e: Exception) {
           retries--
           if (retries > 0) Thread.sleep(10L)
       }
   }
   ```

---

## 🔗 관련 파일

- 테스트 실행: `./gradlew test`
- 특정 테스트: `./gradlew test --tests StockConcurrencyTest`
- 성능 모니터링: `docker-compose -f ./docker/monitoring-compose.yml up`
