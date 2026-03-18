# `findAllByBrandIdAndDeletedAtIsNull` 쿼리 인덱스 비교 분석

## 분석 대상 쿼리

`ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)`

```sql
SELECT * FROM products
WHERE brand_id = ?
  AND deleted_at IS NULL
ORDER BY {created_at DESC | price ASC | likes DESC}
LIMIT 20 OFFSET 0
```

| 정렬 조건 | 컨트롤러 파라미터 | 관련 칼럼 |
|----------|----------------|----------|
| `created_at DESC` | `sort=latest` (기본값) | brand_id, deleted_at, created_at |
| `price ASC` | `sort=price_asc` | brand_id, deleted_at, price |
| `likes DESC` | `sort=likes_desc` | brand_id, deleted_at, likes |

## 테스트 환경

| 항목 | 값 |
|------|-----|
| MySQL 버전 | 8.0 |
| 전체 데이터 | 100,000건 |
| 테스트 brand_id | 1 (총 ~10,000건 / 활성 ~9,800건 / 삭제 ~200건) |
| 삭제 비율 (brand_id=1) | 약 2% |
| 기존 인덱스 | `idx_products_brand_id_likes (brand_id, likes)` |

---

## 시나리오 구성

3개 칼럼(brand_id, deleted_at, 정렬칼럼)의 모든 조합:
1(없음) + 3(단일) + 6(2컬럼) + 6(3컬럼) = **16 시나리오** × 3개 정렬

---

## 1. ORDER BY created_at DESC

### 결과 요약

| # | 인덱스 구성 | type | filesort | 실행 시간 | 비고 |
|---|-----------|------|----------|----------|------|
| 1 | 없음 (brand_id_likes) | ref | **O** | 28.1ms | 기준선 |
| 2 | `(brand_id)` | ref | **O** | 7.15ms | |
| 3 | `(deleted_at)` | ref | **O** | 9.84ms | 기존 인덱스 선택됨 |
| 4 | `(created_at)` | index | X | 0.227ms | |
| 5 | `(brand_id, deleted_at)` | ref | **O** | 8.06ms | |
| **6** | **`(brand_id, created_at)`** | **ref** | **X** | **0.098ms** | **Entity 선언 인덱스** |
| 7 | `(deleted_at, brand_id)` | ref | **O** | 7.93ms | |
| 8 | `(deleted_at, created_at)` | range | X | 0.268ms | |
| 9 | `(created_at, brand_id)` | index | X | 0.725ms | |
| 10 | `(created_at, deleted_at)` | index | X | 0.232ms | |
| 11 | `(brand_id, deleted_at, created_at)` | ref | X | 0.118ms | filtered: 100% |
| 12 | `(brand_id, created_at, deleted_at)` | ref | X | 0.088ms | 커버링 |
| **13** | **`(deleted_at, brand_id, created_at)`** | **ref** | **X** | **0.087ms** | **최속** |
| 14 | `(deleted_at, created_at, brand_id)` | range | X | 0.181ms | 커버링 |
| 15 | `(created_at, brand_id, deleted_at)` | index | X | 0.904ms | |
| 16 | `(created_at, deleted_at, brand_id)` | index | X | 0.207ms | |

<details>
<summary>주요 EXPLAIN ANALYZE</summary>

**시나리오 1: 없음 — 기준선 (28.1ms)**
```
-> Limit: 20 row(s)  (actual time=28.1..28.1 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=28..28.1 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.806..26.6 rows=9728 loops=1)
            -> Index lookup on products using idx_products_brand_id_likes (brand_id=1)
               (actual time=0.798..26.1 rows=9906 loops=1)
```

**시나리오 6: (brand_id, created_at) — Entity 선언 (0.098ms)**
```
-> Limit: 20 row(s)  (actual time=0.0935..0.0983 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0931..0.0971 rows=20 loops=1)
        -> Index lookup on products using idx_brand_id_created_at (brand_id=1) (reverse)
           (actual time=0.0924..0.0955 rows=20 loops=1)
```

**시나리오 13: (deleted_at, brand_id, created_at) — 최속 (0.087ms)**
```
-> Limit: 20 row(s)  (actual time=0.0832..0.0876 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0828..0.0863 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL, brand_id=1) (reverse)
           (actual time=0.0821..0.0847 rows=20 loops=1)
```

</details>

### 안정성 비교

| | `(brand_id, created_at)` | `(brand_id, deleted_at, created_at)` | `(deleted_at, brand_id, created_at)` |
|--|--|--|--|
| 실행 시간 | 0.098ms | 0.118ms | **0.087ms** |
| filtered | 10% | 100% | 100% |
| 삭제 비율 증가 시 | 소폭 저하 | **안정적** | **안정적** |
| Entity 선언 | **O** | X | X |

---

## 2. ORDER BY price ASC

### 결과 요약

| # | 인덱스 구성 | type | filesort | 실행 시간 | 비고 |
|---|-----------|------|----------|----------|------|
| 1 | 없음 (brand_id_likes) | ref | **O** | 26.0ms | 기준선 |
| 2 | `(brand_id)` | ref | **O** | 7.46ms | |
| 3 | `(deleted_at)` | ref | **O** | 8.95ms | 기존 인덱스 선택됨 |
| 4 | `(price)` | index | X | 0.136ms | |
| 5 | `(brand_id, deleted_at)` | ref | **O** | 7.17ms | |
| **6** | **`(brand_id, price)`** | **ref** | **X** | **0.026ms** | **Entity 선언 인덱스** |
| 7 | `(deleted_at, brand_id)` | ref | **O** | 7.19ms | |
| 8 | `(deleted_at, price)` | range | X | 0.119ms | |
| 9 | `(price, brand_id)` | index | X | 0.027ms | |
| 10 | `(price, deleted_at)` | index | X | 0.109ms | |
| 11 | `(brand_id, deleted_at, price)` | range | X | 0.028ms | filtered: 100% |
| 12 | `(brand_id, price, deleted_at)` | ref | X | 0.031ms | 커버링 |
| 13 | `(deleted_at, brand_id, price)` | range | X | 0.027ms | filtered: 100% |
| 14 | `(deleted_at, price, brand_id)` | range | X | 0.028ms | 커버링 |
| **15** | **`(price, brand_id, deleted_at)`** | **index** | **X** | **0.026ms** | **최속** |
| 16 | `(price, deleted_at, brand_id)` | index | X | 0.033ms | |

<details>
<summary>주요 EXPLAIN ANALYZE</summary>

**시나리오 6: (brand_id, price) — Entity 선언 (0.026ms)**
```
-> Limit: 20 row(s)  (actual time=0.00621..0.0257 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.00575..0.0245 rows=20 loops=1)
        -> Index lookup on products using idx_test (brand_id=1)
           (actual time=0.00517..0.0229 rows=22 loops=1)
```

**시나리오 11: (brand_id, deleted_at, price) — filtered: 100% (0.028ms)**
```
-> Limit: 20 row(s)  (actual time=0.00846..0.0283 rows=20 loops=1)
    -> Index range scan on products using idx_test over (brand_id = 1 AND deleted_at = NULL),
       with index condition: ((products.brand_id = 1) and (products.deleted_at is null))
       (actual time=0.00796..0.0268 rows=20 loops=1)
```

</details>

### 안정성 비교

| | `(brand_id, price)` | `(brand_id, deleted_at, price)` | `(deleted_at, brand_id, price)` |
|--|--|--|--|
| 실행 시간 | **0.026ms** | 0.028ms | 0.027ms |
| filtered | 10% | 100% | 100% |
| 삭제 비율 증가 시 | 소폭 저하 | **안정적** | **안정적** |
| Entity 선언 | **O** | X | X |

---

## 3. ORDER BY likes DESC

### 결과 요약

> **특이사항**: 기존 `idx_products_brand_id_likes(brand_id, likes)`가 이 쿼리에 이미 최적화되어 있다.
> 16개 시나리오 중 14개에서 옵티마이저가 기존 인덱스를 선택했고, **filesort가 한 번도 발생하지 않았다**.

| # | 인덱스 구성 | type | 실제 사용 인덱스 | 실행 시간 | 비고 |
|---|-----------|------|----------------|----------|------|
| 1 | 없음 (brand_id_likes) | ref | brand_id_likes | 0.122ms | 기존 인덱스로 충분 |
| 2 | `(brand_id)` | ref | brand_id_likes | 0.044ms | |
| 3 | `(deleted_at)` | ref | brand_id_likes | 0.035ms | |
| 4 | `(likes)` | ref | brand_id_likes | 0.038ms | |
| 5 | `(brand_id, deleted_at)` | range | brand_id_likes | 0.037ms | |
| **6** | **`(brand_id, likes)`** | **ref** | **brand_id_likes** | **0.036ms** | **기존 인덱스** |
| 7 | `(deleted_at, brand_id)` | range | brand_id_likes | 0.048ms | |
| 8 | `(deleted_at, likes)` | ref | brand_id_likes | 0.034ms | |
| 9 | `(likes, brand_id)` | ref | brand_id_likes | 0.043ms | |
| 10 | `(likes, deleted_at)` | ref | brand_id_likes | 0.032ms | |
| 11 | `(brand_id, deleted_at, likes)` | ref | **idx_test** | 0.043ms | filtered: 100% |
| 12 | `(brand_id, likes, deleted_at)` | ref | brand_id_likes | 0.034ms | |
| 13 | `(deleted_at, brand_id, likes)` | ref | **idx_test** | 0.040ms | filtered: 100% |
| 14 | `(deleted_at, likes, brand_id)` | ref | brand_id_likes | 0.035ms | |
| 15 | `(likes, brand_id, deleted_at)` | ref | brand_id_likes | 0.037ms | |
| 16 | `(likes, deleted_at, brand_id)` | ref | brand_id_likes | 0.038ms | |

기존 `(brand_id, likes)` 인덱스가 WHERE(`brand_id=1`) + ORDER(`likes DESC`)를 동시 처리한다.
`deleted_at IS NULL`은 테이블 행에서 필터링하지만, 삭제 비율 2%이므로 거의 모든 행이 통과.
**추가 인덱스 불필요.**

---

## 핵심 인사이트

### 1. filesort 발생 패턴

정렬칼럼이 인덱스에 없거나 등치 조건(brand_id, deleted_at) 뒤에 오지 않으면 반드시 filesort 발생.

| 정렬 | filesort 그룹 (시나리오) | 실행 시간 |
|------|----------------------|----------|
| created_at DESC | 1, 2, 3, 5, 7 | 7~28ms |
| price ASC | 1, 2, 3, 5, 7 | 7~26ms |
| likes DESC | **없음** (기존 인덱스가 처리) | — |

**공통점**: `(brand_id, deleted_at)`, `(deleted_at, brand_id)` 등 정렬칼럼 없는 조합은 모두 filesort.

### 2. Entity 선언 인덱스의 효과

```kotlin
Index(name = "idx_products_brand_id_created_at", columnList = "brand_id, created_at")
Index(name = "idx_products_brand_id_likes", columnList = "brand_id, likes")
Index(name = "idx_products_brand_id_price", columnList = "brand_id, price")
```

3개 인덱스 모두 `(brand_id, 정렬칼럼)` 패턴으로, brand_id 등치 조건으로 구간 접근(type: ref) 후
정렬칼럼 인덱스 스캔으로 filesort를 제거한다. 2컬럼 중 최고 성능.

### 3. `created_at`/`price` 선두 인덱스의 약점

type=index(전체 인덱스 스캔)로 brand_id 조건을 인덱스 단계에서 처리하지 못함.
`brand_id=1`이 전체의 약 10%이므로 ~140건 스캔 후 20건 충족 — 현재는 빠르지만
brand_id별 데이터 분포에 따라 성능이 가변적.

### 4. 삭제 비율에 따른 인덱스 전략

| 삭제 비율 | 권장 인덱스 | 이유 |
|----------|-----------|------|
| ~10% (일반적) | `(brand_id, 정렬칼럼)` 2컬럼 | 크기 작고 성능 충분 |
| 10~50% | `(brand_id, deleted_at, 정렬칼럼)` 3컬럼 검토 | filtered: 100%로 안정적 |
| 50%+ | 3컬럼 인덱스 필수 | 2컬럼은 LIMIT 충족까지 과도한 스캔 |

---

## 권장 인덱스 (현재 상태 기준)

```sql
-- 현재 엔티티에 이미 선언됨 → DB에 적용 필요
CREATE INDEX idx_products_brand_id_created_at ON products(brand_id, created_at);
CREATE INDEX idx_products_brand_id_price ON products(brand_id, price);
-- 이미 DB에 존재
-- idx_products_brand_id_likes (brand_id, likes)
```

- 삭제 비율이 낮게 유지되는 일반적인 서비스에서 **최적의 성능/크기 트레이드오프**
- 삭제 비율이 높아질 경우 `(brand_id, deleted_at, 정렬칼럼)` 3컬럼으로 교체 검토

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
