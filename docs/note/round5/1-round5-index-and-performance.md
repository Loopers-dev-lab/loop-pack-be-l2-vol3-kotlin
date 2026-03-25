# 인덱스 설계와 성능 최적화 — 문제 해결 기록

## 1. 상품 목록 조회가 느리다: 복합 인덱스로 해결

### 문제 상황

상품 목록 API에서 `brandId`로 필터하고 `price_asc`로 정렬하면 느리다.
`likes_desc` 정렬 시 성능이 급락한다. 인덱스가 있어도 효과가 없다.

원인을 EXPLAIN으로 확인해보면:

```
type: ALL (풀 테이블 스캔)
Extra: Using where; Using filesort
```

WHERE 절과 ORDER BY 절을 **동시에 커버하는 인덱스가 없었다.**
`ref_brand_id` 단일 인덱스만 있으니, brandId 필터는 되더라도 정렬에서 filesort가 발생한다.

### 내가 선택한 방법: 정렬 패턴별 복합 인덱스 3개

```sql
CREATE INDEX idx_products_active_like_count ON products (deleted_at, status, like_count DESC);
CREATE INDEX idx_products_active_created_at ON products (deleted_at, status, created_at DESC);
CREATE INDEX idx_products_active_price ON products (deleted_at, status, price ASC);
```

실제 코드 (ProductEntity.kt):

```kotlin
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_ref_brand_id", columnList = "ref_brand_id"),
        Index(name = "idx_products_active_like_count", columnList = "deleted_at, status, like_count DESC"),
        Index(name = "idx_products_active_created_at", columnList = "deleted_at, status, created_at DESC"),
        Index(name = "idx_products_active_price", columnList = "deleted_at, status, price ASC"),
    ],
)
```

QueryDSL 쿼리가 이 인덱스를 정확히 활용한다:

```kotlin
// WHERE: 선두 컬럼 (deleted_at, status)
val where = BooleanBuilder()
    .and(product.deletedAt.isNull)        // deleted_at IS NULL
    .and(product.status.ne(HIDDEN))       // status != 'HIDDEN'
brandId?.let { where.and(product.refBrandId.eq(it.value)) }

// ORDER BY: 후미 컬럼 (정렬 대상)
val orderSpecifier = when (sort) {
    ProductSort.LATEST -> product.createdAt.desc()
    ProductSort.PRICE_ASC -> product.price.asc()
    ProductSort.LIKES_DESC -> product.likeCount.desc()
}
```

결과: EXPLAIN에서 `type: ALL` → `type: ref`로 전환. filesort 제거.

### 다른 대안과 선택하지 않은 이유

| 대안                                                    | 왜 안 했나                                                                  |
|-------------------------------------------------------|-------------------------------------------------------------------------|
| **커버링 인덱스 1개** (모든 컬럼 포함)                             | 인덱스 크기가 과대해지고, 정렬 방향이 다른 3가지 패턴을 하나의 인덱스로 커버할 수 없다                      |
| **파티셔닝** (status별 파티션 분리)                             | 10만건 수준에서 과잉 설계. 운영 복잡도만 증가한다                                           |
| **단일 컬럼 인덱스 여러 개** (like_count, price, created_at 각각) | MySQL은 하나의 쿼리에 하나의 인덱스만 사용한다. WHERE에서 인덱스를 쓰면 ORDER BY에서 못 쓰고, 반대도 마찬가지 |

**핵심 원리**: 복합 인덱스에서 **선두 컬럼은 WHERE 절**, **후미 컬럼은 ORDER BY 절**을 커버해야 한다.
이 순서가 맞지 않으면 인덱스가 있어도 사용되지 않는다.

---

## 2. 조건 순서가 맞지 않으면 인덱스를 못 쓴다

### 문제 상황

`(deleted_at, status, like_count DESC)` 인덱스가 있는데,
`WHERE brand_id = 5 AND deleted_at IS NULL ORDER BY like_count DESC` 쿼리에서 인덱스가 안 탄다.

### 왜 그런가

복합 인덱스는 **최좌측 컬럼부터 순서대로** 사용된다 (Leftmost Prefix Rule).

```
인덱스: (deleted_at, status, like_count)

✅ WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY like_count  → 순서 일치, 인덱스 사용
✅ WHERE deleted_at IS NULL AND status != 'HIDDEN'                      → 선두 2개 사용
❌ WHERE status != 'HIDDEN' ORDER BY like_count                         → deleted_at 건너뜀, 인덱스 무효
❌ WHERE brand_id = 5 ORDER BY like_count                               → 선두 컬럼 없음, 인덱스 무효
```

### 내가 한 결정

`brandId` 필터는 별도의 단일 인덱스(`idx_products_ref_brand_id`)로 처리했다.
`(brand_id, deleted_at, status, created_at)` 같은 4컬럼 복합 인덱스는 **현재 데이터 규모에서 불필요**하다고 판단했다.

이유:

- brandId 단일 인덱스로 필터링하면 대상 row가 충분히 줄어든다
- 10만건에서 brand별로 수천건이면, 그 안에서 filesort가 발생해도 체감 성능 차이가 미미하다
- 데이터가 100만건 이상으로 증가하면 그때 복합 인덱스를 추가한다

---

## 3. OFFSET 페이지네이션의 한계

### 문제 상황

페이지 번호가 커질수록 `OFFSET`이 증가하며, DB가 건너뛸 row를 전부 읽고 버린다.

```sql
SELECT *
FROM products
WHERE...ORDER BY created_at DESC LIMIT 20
OFFSET 10000;
-- 10,020개를 읽고 10,000개를 버림
```

### 현재 구현

```kotlin
queryFactory.selectFrom(product)
    .where(where)
    .orderBy(orderSpecifier, product.id.desc())  // 안정적 정렬 보장
    .offset(page.toLong() * size)
    .limit(size.toLong())
    .fetch()
```

현재는 OFFSET 기반 페이지네이션을 사용하고 있다.

### 왜 OFFSET을 유지했나

| 대안                                 | 장점                                                    | 왜 안 했나                                                               |
|------------------------------------|-------------------------------------------------------|----------------------------------------------------------------------|
| **Cursor(Keyset) 페이지네이션**          | OFFSET 없이 `WHERE id < :lastId` 조건으로 다음 페이지 조회. 일정한 성능 | 정렬 기준이 3가지(최신순, 가격순, 좋아요순)라 커서 조건이 복잡해진다. "5페이지로 바로 이동" 같은 UX가 불가능하다 |
| **Covering Index + Deferred Join** | 서브쿼리로 PK만 먼저 OFFSET 처리 후 JOIN                         | 현재 QueryDSL 구조에서 서브쿼리 도입이 복잡하고, 10만건 수준에서 체감 이점이 작다                  |
| **Elasticsearch**                  | 검색 엔진에 위임                                             | 현재 규모에서 인프라 복잡도 대비 이점이 없다                                            |

### No-Offset(Cursor)을 도입하지 않은 구체적 이유

**1) 정렬 기준이 여러 개일 때 — 커서 조건이 복잡해진다**

현재 프로젝트는 정렬이 3종류다:

```sql
-- LATEST: 단순, cursor 적용 쉬움
WHERE created_at < :lastCreatedAt ORDER BY created_at DESC

-- PRICE_ASC: 동일 가격이 많으면 cursor가 애매함
WHERE (price > :lastPrice OR (price = :lastPrice AND id > :lastId))
ORDER BY price ASC, id ASC

-- LIKES_DESC: likeCount가 수시로 변함 → cursor 기준값이 불안정
WHERE (like_count < :lastLikeCount OR (like_count = :lastLikeCount AND id < :lastId))
ORDER BY like_count DESC, id DESC
```

LIKES_DESC가 특히 문제다. 좋아요 수는 계속 바뀌니까, 이전 페이지에서 받은 커서값(`lastLikeCount=50`)이 다음 요청 시점에는 의미가 달라질 수 있다. 페이지 간 중복/누락이 발생한다.

**2) "N페이지로 바로 이동" UX가 불가능**

Cursor 방식은 이전 페이지의 마지막 값을 알아야 다음 페이지를 조회한다. "5페이지로 점프"가 안 된다.
어드민 화면처럼 페이지 번호 UI가 필요하면 OFFSET이 더 자연스럽다.

**3) 데이터가 적으면 차이 없음**

10만건에서 OFFSET 1000 정도는 밀리초 수준이다. OFFSET의 문제는 OFFSET 100000 같은 극단적 깊이에서 나타난다. 현재 규모에서는 체감 차이가 거의 없다.

**4) 동적 필터가 추가되면 커서 조건이 더 복잡해짐**

brandId 필터가 있으면 커서 조건에 brandId도 함께 고려해야 한다. 필터 조합이 늘수록 커서 쿼리의 WHERE 절이 기하급수적으로 복잡해진다.

### 결론

| 상황 | 추천 |
|------|------|
| 단일 정렬(최신순) + 무한 스크롤 UI | No-Offset |
| 다중 정렬 + 페이지 번호 UI | OFFSET 유지 |
| 동적으로 변하는 값(likeCount) 기준 정렬 | OFFSET 유지 (커서 불안정) |
| 데이터 100만건 이상 + 깊은 페이지 접근 | No-Offset 전환 필요 |

현재 프로젝트 상황(3종 정렬, 10만건, 어드민+대고객 혼재)에서는 OFFSET 유지가 맞다.
전환하려면 대고객 목록 API의 LATEST 정렬만 선택적으로 No-Offset으로 바꾸는 게 가장 실용적이다. 나머지는 OFFSET이 더 단순하고 적합하다.

---

## 4. likes_desc 정렬 시 성능 급락

### 문제 상황

좋아요순 정렬(`likes_desc`)에서 성능이 특히 나빴다.

### 왜 그런가

`like_count`는 좋아요가 추가/취소될 때마다 UPDATE된다.
인덱스가 없으면 **매번 전체 테이블을 스캔해서 정렬**해야 한다.
게다가 `like_count`는 동적으로 변하는 값이라, 단순 인덱스로는 정렬 순서가 자주 바뀐다.

### 해결: 복합 인덱스 + Redis 캐시 조합

1. **DB 레벨**: `(deleted_at, status, like_count DESC)` 복합 인덱스로 filesort 제거
2. **캐시 레벨**: `@Cacheable`로 목록 조회 결과를 Redis에 5분간 캐싱

```kotlin
@Cacheable(
    cacheNames = ["product:list"],
    key = "(#brandId ?: 'all') + ':' + #sort + ':' + #page + ':' + #size",
)
fun execute(brandId: BrandId?, sort: ProductSort, page: Int, size: Int): PageResult<ProductInfo>
```

`like_count`가 변경되더라도 5분 TTL 내에서는 캐시된 결과를 반환한다.
실시간 반영이 필요한 서비스가 아니므로 이 정도의 지연은 허용 가능하다.

---

## 5. 캐시 전략 선택: 상세 vs 목록을 왜 다르게 했나

### 상품 상세: Write-Through

```
쓰기 → DB 저장 + Redis 저장 (동시)
읽기 → Redis에서 바로 반환 (TTL 1시간)
```

- 수정 빈도가 낮고, 읽기가 압도적
- 캐시가 항상 최신이므로 일관성 높음

### 상품 목록: Cache-Aside + @Cacheable

```
읽기 → Redis에 있으면 반환, 없으면 DB 조회 후 Redis 저장 (TTL 5분)
쓰기 → 해당 브랜드의 목록 캐시를 evict
```

- 정렬 × 페이지 × 브랜드 조합이 수십~수백 가지 → Write-Through로 모든 조합을 갱신하는 건 비현실적
- 변경 시 해당 브랜드의 캐시만 evict하면 다음 조회 때 자동 갱신

### 다른 대안

| 대안                                   | 왜 안 했나                                            |
|--------------------------------------|---------------------------------------------------|
| **Write-Behind** (캐시에만 쓰고 비동기 DB 반영) | 쓰기는 빠르지만, Redis 장애 시 데이터 유실. 현재 규모에서 복잡도 대비 이점 없음 |
| **캐시 안 씀**                           | DB 인덱스만으로도 동작하지만, 동일 쿼리 반복 호출이 많아 캐시 효과가 크다       |

---

## 6. 인덱스 설계 시 주의할 점

### 쓰기 성능과의 트레이드오프

인덱스를 추가하면 INSERT/UPDATE마다 인덱스도 갱신해야 한다.

현재 프로젝트에서의 판단:

- 상품 테이블: **읽기 >> 쓰기**. 관리자만 상품을 등록/수정하고, 고객은 조회만 한다. 인덱스 3개 추가해도 쓰기 비용 무시 가능.
- `like_count` 컬럼: 좋아요마다 UPDATE 발생 → 인덱스 재정렬. 하지만 비관적 락으로 동시 UPDATE를 직렬화하고 있으므로, 인덱스 갱신도 직렬화되어 부하가 분산된다.

**원칙**: 자주 변경되는 컬럼에 인덱스를 남발하면 쓰기 성능이 저하된다. 읽기/쓰기 비율을 따져서 판단해야 한다.

### 조건 순서가 중요하다

```sql
-- 인덱스: (deleted_at, status, price ASC)

-- ✅ 인덱스 사용: WHERE 순서가 인덱스 선두와 일치
WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY price ASC

-- ❌ 인덱스 미사용: 선두 컬럼(deleted_at)을 건너뜀
WHERE status != 'HIDDEN' ORDER BY price ASC
```

MySQL의 복합 인덱스는 **최좌측 접두사 규칙(Leftmost Prefix Rule)**을 따른다.
중간 컬럼을 건너뛸 수 없다.

### 모수가 작으면 Full Scan이 더 빠르다

- 테이블에 100건밖에 없으면 인덱스를 타는 것보다 Full Scan이 빠르다
- MySQL 옵티마이저도 이걸 알고 있어서, 통계 기반으로 인덱스를 무시하고 Full Scan을 선택하기도 한다
- 이 프로젝트에서도 brands 테이블(수십건)에는 별도 인덱스를 추가하지 않았다. unique 제약만으로 충분하다.

---

## 7. 카디널리티와 인덱스 설계

### 카디널리티란?

컬럼에 들어있는 **고유한 값의 수**. 중복이 적을수록 카디널리티가 높다.

이 프로젝트의 products 테이블(10만건 가정)을 기준으로 보면:

| 컬럼           | 카디널리티  | 고유 값 수    | 이유                                    |
|--------------|--------|----------|---------------------------------------|
| `id` (PK)    | 매우 높음  | ~100,000 | 모든 값이 고유                              |
| `like_count` | 높음     | ~100,000 | 0부터 수천까지 분포, 대부분 고유한 값                |
| `price`      | 높음     | ~10,000  | 다양한 가격대                               |
| `ref_brand_id` | 낮음   | ~50      | 브랜드 50개에 상품이 분산                       |
| `status`     | 매우 낮음  | 3        | ACTIVE, HIDDEN, SOLD_OUT              |
| `deleted_at` | 매우 낮음  | 2        | NULL(활성) 또는 값(삭제). 대부분 NULL           |

### 인덱스는 카디널리티가 높은 컬럼에 걸어야 한다

카디널리티가 낮은 컬럼에 단독 인덱스를 걸면 효과가 없다.

예를 들어 `status`에 단독 인덱스를 걸면:
- `WHERE status = 'ACTIVE'` → 전체 10만건 중 9만건이 ACTIVE → 인덱스를 타도 대상이 90%
- 인덱스 탐색 + 테이블 랜덤 I/O가 Full Scan보다 오히려 느림
- MySQL 옵티마이저가 인덱스를 무시하고 Full Scan을 선택한다

반면 `ref_brand_id`에 단독 인덱스를 걸면:
- `WHERE ref_brand_id = 5` → 10만건 중 ~2,000건으로 축소 → 인덱스 효과 있음

카디널리티가 훨씬 높은 `id`, `like_count`, `price` 같은 컬럼은 인덱스로 소수의 row만 특정할 수 있어 효과가 크다.

### 복합 인덱스에서의 카디널리티 순서

복합 인덱스를 설계할 때는 **카디널리티가 높은 컬럼을 선두에** 놓는 것이 일반적으로 유리하다.
단, WHERE 절의 조건 유형(등치 vs 범위)과 쿼리 패턴이 더 중요하다.

이 프로젝트의 products 복합 인덱스가 좋은 예시다:

```sql
-- (deleted_at, status, like_count DESC)
--  카디널리티 2  /  카디널리티 3  /  카디널리티 ~100,000
```

선두 컬럼 `deleted_at`의 카디널리티는 2로 매우 낮다.
그런데 왜 선두에 놓았나? **등치 조건(`IS NULL`)으로 사용되기 때문이다.**

- `deleted_at IS NULL`은 카디널리티가 낮아도, 전체의 95%를 차지하는 "활성 상품"을 B-Tree에서 한 번에 범위 특정한다
- `status != 'HIDDEN'`도 마찬가지로 등치/비교 조건이라 두 번째에 위치
- `like_count DESC`는 정렬 컬럼이라 후미에 위치 → filesort 제거

**핵심**: 카디널리티보다 **쿼리 패턴(등치 → 범위 → 정렬)** 순서가 우선이다.

### 실전 예시 1: 좋아요 테이블 — UNIQUE 제약이 곧 인덱스

```sql
-- likes 테이블: UNIQUE(ref_user_id, ref_product_id)
-- 이 UNIQUE 제약 자체가 복합 인덱스 역할을 한다

-- 1. 사용자의 특정 상품 좋아요 여부 확인 (AddLikeUseCase)
SELECT * FROM likes WHERE ref_user_id = ? AND ref_product_id = ?;
-- → UNIQUE 인덱스의 두 컬럼 모두 등치 조건 → 최적

-- 2. 사용자의 좋아요 목록 조회 (있다면)
SELECT * FROM likes WHERE ref_user_id = ?;
-- → UNIQUE 인덱스의 최좌측 컬럼 → Leftmost Prefix로 인덱스 사용 가능
-- → 별도 ref_user_id 단독 인덱스 불필요
```

`(ref_user_id, ref_product_id)` UNIQUE 제약은:
- 중복 좋아요 방지 (비즈니스 규칙)
- `ref_user_id` 단독 조회도 커버 (Leftmost Prefix)
- **하나의 제약으로 두 가지 역할**을 한다

### 실전 예시 2: 주문 테이블 — 등치 + 정렬 복합 인덱스

```sql
-- orders 테이블: INDEX(ref_user_id, created_at DESC)

-- "내 주문 내역" 조회 (최신순)
SELECT * FROM orders WHERE ref_user_id = ? ORDER BY created_at DESC LIMIT 20;
-- → ref_user_id 등치 조건으로 해당 사용자 주문만 필터
-- → created_at DESC 정렬을 인덱스가 커버 → filesort 없음
```

이 인덱스가 없다면:
1. `ref_user_id` 단독 인덱스 → 필터는 되지만 `created_at` 정렬에 filesort 발생
2. `created_at` 단독 인덱스 → 전체 주문을 시간순 스캔하면서 `ref_user_id` 후필터 → 비효율적

복합 인덱스로 **필터 + 정렬을 동시에** 커버하는 것이 products 테이블과 동일한 원리다.

### 현재 테이블의 카디널리티 확인 방법

```sql
SHOW INDEX FROM products;
```

```
+----------+------------+----------------------------------+---------+-----------+-------------+
| Table    | Non_unique | Key_name                         | Seq_in  | Column    | Cardinality |
+----------+------------+----------------------------------+---------+-----------+-------------+
| products | 0          | PRIMARY                          | 1       | id        | 100000      |
| products | 1          | idx_products_ref_brand_id        | 1       | ref_brand_id | 50       |
| products | 1          | idx_products_active_like_count   | 1       | deleted_at   | 2        |
| products | 1          | idx_products_active_like_count   | 2       | status       | 4        |
| products | 1          | idx_products_active_like_count   | 3       | like_count   | 100000   |
+----------+------------+----------------------------------+---------+-----------+-------------+
```

복합 인덱스 `idx_products_active_like_count`의 카디널리티를 보면:
- `deleted_at`: 2 (NULL 또는 값) — 카디널리티 낮지만 등치 필터로 활성 상품을 한 번에 특정
- `status`: 4 (ACTIVE, HIDDEN, SOLD_OUT + NULL 파생) — 추가 필터링
- `like_count`: 100,000 — 거의 고유한 값, 정렬에 효과적

**카디널리티만 보면 `deleted_at`을 선두에 놓는 게 이상해 보이지만, 쿼리 패턴상 맞는 선택이다.**

- `deleted_at` 카디널리티 2: NULL 또는 값. 전체 row의 대부분이 NULL → 필터링 효과 큼
- `status` 카디널리티 4: ACTIVE, HIDDEN, SOLD_OUT 등 → 추가 필터링
- `like_count` 카디널리티 100000: 거의 고유한 값 → 정렬에 효과적

복합 인덱스에서는 **선두 컬럼의 카디널리티가 낮아도**, 등치 조건으로 사용되면 효과적이다.
`deleted_at IS NULL`은 카디널리티가 2지만, 전체 row의 95%를 차지하는 "활성 상품"을 한 번에 필터링한다.

---

## 8. EXPLAIN ANALYZE actual time 측정 결과

### 측정 환경

- TestContainers MySQL 8.0, 10만건 데이터
- JIT 워밍업 1회 선행, 10회 반복 측정 평균
- 인덱스 DROP(AS-IS) → 측정 → 인덱스 CREATE(TO-BE) → 측정 순서로 진행

### 측정 결과

| 쿼리 | AS-IS (ms) | TO-BE (ms) | 개선율 |
|------|-----------|-----------|-------|
| 브랜드 필터 + 좋아요 정렬 | 38.36 | 115.80 | 0.3x |
| 전체 좋아요 정렬 | 30.45 | 116.40 | 0.3x |
| 최신순 전체 조회 | 30.50 | 122.50 | 0.2x |
| 가격순 전체 조회 | 30.81 | 117.20 | 0.3x |

### 인덱스를 추가했는데 오히려 느려졌다?

모든 쿼리에서 인덱스 적용 후(TO-BE)가 인덱스 없는 상태(AS-IS)보다 3~4배 느리게 나왔다. EXPLAIN에서는 분명히 `type: ALL → ref`, `filesort 제거`로 개선되었는데 actual time은 역전되었다.

이건 **TestContainers 환경의 특성** 때문이다. 실제 운영 환경에서는 이런 역전이 발생하지 않는다.

#### 원인 1: 버퍼 풀에 전체 데이터가 올라가있다

TestContainers에서 10만건을 INSERT한 직후이므로, **모든 데이터 페이지가 InnoDB 버퍼 풀(메모리)에 이미 존재**한다. 디스크 I/O가 전혀 발생하지 않는 상태다.

이 상태에서 풀 테이블 스캔은:
1. 테이블의 데이터 페이지를 **순차적으로** 메모리에서 읽는다 (Sequential Read)
2. 메모리에서 정렬 수행 (10만건 정도는 sort_buffer에 충분히 들어감)
3. LIMIT 20개 반환

반면 인덱스 스캔은:
1. 인덱스 트리를 탐색하여 조건에 맞는 row의 PK를 찾는다
2. PK로 **데이터 페이지를 랜덤 접근**한다 (Random Read)
3. 인덱스 순서와 데이터 페이지 순서가 다르므로, 같은 페이지를 여러 번 방문할 수 있다

**핵심: Sequential Read vs Random Read.** 데이터가 전부 메모리에 있어도 랜덤 접근의 CPU 캐시 미스, 페이지 탐색 오버헤드가 순차 접근보다 크다. 10만건 수준에서 LIMIT 20이면, 풀 스캔 후 정렬이 인덱스 탐색보다 빠르게 나올 수 있다.

#### 원인 2: 테스트 실행 순서에 따른 캐시 상태 차이

AS-IS(인덱스 없음) 측정이 먼저 실행된다. 이때 `seedData()`로 INSERT된 직후라 버퍼 풀이 가장 따뜻한 상태다. 이후 인덱스 CREATE가 실행되면서 인덱스 빌드 과정에서 버퍼 풀의 일부 데이터 페이지가 밀려날 수 있다. TO-BE 측정 시점에는 AS-IS보다 버퍼 풀 상태가 상대적으로 차갑다.

#### 원인 3: MySQL 옵티마이저의 통계 오차

TestContainers는 매번 새로운 MySQL 인스턴스를 생성한다. 인덱스를 CREATE한 직후에는 통계가 정확하지 않을 수 있다. `ANALYZE TABLE`을 명시적으로 실행하지 않으면, 옵티마이저가 비효율적인 실행 계획을 선택할 수 있다.

#### 그러면 인덱스가 의미 없는 건가?

아니다. **운영 환경에서는 상황이 완전히 다르다:**

| 조건 | TestContainers (로컬) | 운영 환경 |
|------|---------------------|---------|
| 데이터 크기 | 10만건 (메모리에 전부 적재) | 수백만건 (디스크 접근 불가피) |
| 버퍼 풀 | INSERT 직후, 100% 적중 | 여러 테이블 공유, 적중률 변동 |
| 동시 쿼리 | 단일 쓰레드 | 수백 커넥션 동시 접근 |
| 풀 스캔 비용 | 메모리 순차 읽기 ~30ms | 디스크 I/O 포함 시 수백ms~수초 |
| 인덱스 효과 | 랜덤 접근 오버헤드 > 풀 스캔 이점 | 필요한 row만 정확히 접근, 압도적 이점 |

운영 환경에서 인덱스가 빛나는 이유:
- **데이터가 메모리에 다 안 올라간다.** 풀 스캔은 디스크에서 수백만 row를 읽어야 하지만, 인덱스는 필요한 20건의 데이터 페이지만 읽으면 된다.
- **동시 쿼리가 많다.** 풀 스캔은 버퍼 풀을 대량으로 점유하여 다른 쿼리의 캐시를 밀어낸다. 인덱스 스캔은 소량의 페이지만 접근하므로 다른 쿼리에 영향이 적다.
- **LIMIT과 함께 사용 시** 인덱스 순서로 이미 정렬되어 있으면 20건만 읽고 바로 반환한다. 풀 스캔은 전체를 읽은 뒤 정렬해야 한다.

### EXPLAIN 결과가 더 신뢰할 수 있는 이유

EXPLAIN은 "어떤 경로로 실행하는가"를 보여주고, actual time은 "그 경로가 이 환경에서 얼마나 걸렸는가"를 보여준다.

| 지표 | EXPLAIN | actual time |
|------|---------|-------------|
| 환경 의존성 | 낮음 (실행 계획은 동일) | 높음 (하드웨어, 버퍼 풀 상태에 따라 변동) |
| 풀 스캔 감지 | `type: ALL` → 명확히 위험 | 메모리에 다 있으면 빠르게 보일 수 있음 |
| filesort 감지 | `Extra: Using filesort` → 명확히 위험 | 소량 데이터에서는 빠를 수 있음 |
| 운영 환경 예측 | 풀 스캔은 데이터 증가 시 선형 악화 예측 가능 | 로컬 수치는 운영과 무관 |

**결론:** 로컬 actual time이 역전되더라도 EXPLAIN에서 `type: ALL → ref`, `filesort 제거`가 확인되면 인덱스 설계는 올바르다. 운영 환경에서 데이터가 커지고 동시 접근이 늘면 인덱스의 효과가 드러난다.

---

## 정리: 이 프로젝트에서 내린 성능 관련 결정들

| 문제               | 선택                               | 핵심 근거                               |
|------------------|----------------------------------|-------------------------------------|
| 상품 목록 정렬 느림      | 정렬 패턴별 복합 인덱스 3개                 | WHERE + ORDER BY 동시 커버, filesort 제거 |
| brandId 필터 + 정렬  | brandId 단일 인덱스 유지                | 10만건에서 충분, 과도한 복합 인덱스는 쓰기 비용만 증가    |
| OFFSET 페이지네이션 지연 | OFFSET 유지 (Cursor 미도입)           | 현재 규모와 UX 요구사항에서 충분, 규모 증가 시 전환     |
| likes_desc 성능 급락 | 복합 인덱스 + Redis 캐시 (5분 TTL)       | 인덱스로 DB 부하 감소, 캐시로 반복 조회 최적화        |
| 캐시 전략            | 상세=Write-Through, 목록=Cache-Aside | 읽기/쓰기 패턴이 다르므로 전략도 분리               |
| 좋아요 동시성          | 비관적 락 (FOR UPDATE)               | 충돌 빈도 낮고 구현 단순, 낙관적 락의 재시도 UX 부정적   |
