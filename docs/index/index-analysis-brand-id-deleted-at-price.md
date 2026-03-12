# `findAllByBrandIdAndDeletedAtIsNull` 쿼리 인덱스 비교 분석 (price 정렬)

## 분석 대상 쿼리

```sql
SELECT id, name, description, price, likes, stock_quantity,
       brand_id, created_at, updated_at, deleted_at
FROM products
WHERE brand_id = ?
  AND deleted_at IS NULL
ORDER BY price ASC
LIMIT 20 OFFSET 0
```

**발생 위치**: `ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)`
**정렬 조건**: `sort=price_asc`

## 테스트 환경

| 항목 | 값 |
|------|-----|
| MySQL 버전 | 8.0 |
| 전체 데이터 | 100,000건 |
| 테스트 brand_id | 1 (총 10,028건 / 활성 9,812건 / 삭제 216건) |
| 삭제 비율 (brand_id=1) | 약 2.2% |
| 기존 인덱스 | `idx_products_brand_id_likes (brand_id, likes)` |

---

## 시나리오별 EXPLAIN 결과

> 3개 칼럼(brand_id, deleted_at, price)의 모든 조합: 1(없음) + 3(단일) + 6(2컬럼) + 6(3컬럼) = **16 케이스**

---

### [단일 인덱스]

#### 시나리오 1: 인덱스 없음 (기준선)

```sql
-- 기존 idx_products_brand_id_likes (brand_id, likes) 만 존재
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_products_brand_id_likes |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **26.0ms** |

```
-> Limit: 20 row(s)  (actual time=26..26 rows=20 loops=1)
    -> Sort: products.price, limit input to 20 row(s) per chunk  (actual time=26..26 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.0928..23.4 rows=9812 loops=1)
            -> Index lookup on products using idx_products_brand_id_likes (brand_id=1)
               (actual time=0.0795..22.8 rows=10028 loops=1)
```

**분석**: `brand_id`로 10,028건 필터 후 `deleted_at IS NULL` 확인(9,812건), 전체 filesort 발생.

---

#### 시나리오 2: `(brand_id)` 단일 인덱스

```sql
CREATE INDEX idx_brand_id ON products(brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id |
| rows | 18,628 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **7.46ms** |

```
-> Limit: 20 row(s)  (actual time=7.46..7.46 rows=20 loops=1)
    -> Sort: products.price, limit input to 20 row(s) per chunk  (actual time=7.46..7.46 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.00758..6.3 rows=9812 loops=1)
            -> Index lookup on products using idx_test (brand_id=1)
               (actual time=0.007..5.86 rows=10028 loops=1)
```

**분석**: brand_id 조회로 10,028건 접근하지만, `price` 정렬이 없어 filesort 발생.

---

#### 시나리오 3: `(deleted_at)` 단일 인덱스

```sql
CREATE INDEX idx_deleted_at ON products(deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (신규 인덱스 무시!) |
| rows | 18,704 |
| filtered | 50.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **8.95ms** |

```
-> Limit: 20 row(s)  (actual time=8.95..8.95 rows=20 loops=1)
    -> Sort: products.price, limit input to 20 row(s) per chunk  (actual time=8.94..8.95 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.00629..7.71 rows=9812 loops=1)
            -> Index lookup on products using idx_products_brand_id_likes (brand_id=1)
               (actual time=0.00571..7.3 rows=10028 loops=1)
```

**분석**: MySQL 옵티마이저가 `deleted_at` 인덱스를 무시하고 기존 `brand_id_likes` 인덱스를 선택.
`deleted_at IS NULL`의 낮은 선택도(전체 98% 해당) 때문에 활용 가치 없다고 판단.

---

#### 시나리오 4: `(price)` 단일 인덱스

```sql
CREATE INDEX idx_price ON products(price);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_price |
| key_len | 8 |
| rows | 106 |
| filtered | 1.88% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.136ms** |

```
-> Limit: 20 row(s)  (actual time=0.0297..0.136 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0292..0.135 rows=20 loops=1)
        -> Index scan on products using idx_test
           (actual time=0.0148..0.119 rows=141 loops=1)
```

**분석**: 전체 `price` 순방향 스캔 중 `brand_id=1 AND deleted_at IS NULL` 필터로 20건 조기 종료.
brand_id=1이 전체의 약 10%이므로 평균 ~141건만 스캔하면 20건 충족.

---

### [2컬럼 복합 인덱스]

#### 시나리오 5: `(brand_id, deleted_at)`

```sql
CREATE INDEX idx_brand_id_deleted_at ON products(brand_id, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_deleted_at |
| rows | 18,348 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **7.17ms** |

```
-> Limit: 20 row(s)  (actual time=7.17..7.17 rows=20 loops=1)
    -> Sort: products.price, limit input to 20 row(s) per chunk  (actual time=7.17..7.17 rows=20 loops=1)
        -> Index lookup on products using idx_test (brand_id=1, deleted_at=NULL)
           (actual time=0.00692..6.08 rows=9812 loops=1)
```

**분석**: `brand_id + deleted_at` 조건은 인덱스로 완벽히 처리(filtered: 100%)했지만, `price` 정렬이 없어 9,812건 filesort 발생.

---

#### 시나리오 6: `(brand_id, price)` ← Entity에 선언된 패턴과 유사

```sql
CREATE INDEX idx_brand_id_price ON products(brand_id, price);
-- ※ Product 엔티티의 idx_products_brand_id_price 과 동일
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_price |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.026ms** |

```
-> Limit: 20 row(s)  (actual time=0.00621..0.0257 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.00575..0.0245 rows=20 loops=1)
        -> Index lookup on products using idx_test (brand_id=1)
           (actual time=0.00517..0.0229 rows=22 loops=1)
```

**분석**:
1. `brand_id=1`로 인덱스 구간 직접 접근 (type: ref)
2. 해당 구간 내 `price ASC` 순방향 스캔
3. `deleted_at IS NULL` 체크(테이블 행 접근)로 20건 조기 종료
4. filesort 없음

> `Product` 엔티티에 이미 `idx_products_brand_id_price`로 선언된 인덱스.
> DB에 실제 반영되면 2컬럼 최고 성능 확보.

---

#### 시나리오 7: `(deleted_at, brand_id)`

```sql
CREATE INDEX idx_deleted_at_brand_id ON products(deleted_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_deleted_at_brand_id |
| rows | 18,348 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **7.19ms** |

```
-> Limit: 20 row(s)  (actual time=7.19..7.19 rows=20 loops=1)
    -> Sort: products.price, limit input to 20 row(s) per chunk  (actual time=7.19..7.19 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL, brand_id=1)
           (actual time=0.00975..6.11 rows=9812 loops=1)
```

**분석**: 시나리오 5와 유사. `deleted_at + brand_id` 조건 처리 후 9,812건 filesort. `price`가 없어 정렬 최적화 불가.

---

#### 시나리오 8: `(deleted_at, price)`

```sql
CREATE INDEX idx_deleted_at_price ON products(deleted_at, price);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_deleted_at_price |
| rows | 49,697 |
| filtered | 18.82% |
| Extra | **Using index condition; Using where** |
| **실제 실행 시간** | **0.119ms** |

```
-> Limit: 20 row(s)  (actual time=0.0186..0.119 rows=20 loops=1)
    -> Filter: (products.brand_id = 1)  (actual time=0.0181..0.117 rows=20 loops=1)
        -> Index range scan on products using idx_test over (deleted_at = NULL)
           (actual time=0.0127..0.112 rows=138 loops=1)
```

**분석**: `deleted_at IS NULL` 구간 순방향 스캔으로 `price ASC` 처리하고, `brand_id=1` 필터 적용.
전체 98,000건 중 순방향으로 읽으며 brand_id=1인 20건 발견(평균 ~138건 스캔).

---

#### 시나리오 9: `(price, brand_id)`

```sql
CREATE INDEX idx_price_brand_id ON products(price, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_price_brand_id |
| key_len | 16 |
| rows | 106 |
| filtered | 1.88% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.027ms** |

```
-> Limit: 20 row(s)  (actual time=0.00713..0.027 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.00667..0.0259 rows=20 loops=1)
        -> Index scan on products using idx_test
           (actual time=0.00521..0.0229 rows=22 loops=1)
```

**분석**: `price` 순서대로 인덱스 스캔 + `brand_id` 인덱스 내 필터링. 단 22건 스캔으로 20건 충족.
커버링 효과로 시나리오 4보다 빠르지만, type=index(전체 스캔)이라 brand별 분포에 의존.

---

#### 시나리오 10: `(price, deleted_at)`

```sql
CREATE INDEX idx_price_deleted_at ON products(price, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_price_deleted_at |
| key_len | 17 |
| rows | 106 |
| filtered | 1.88% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.109ms** |

```
-> Limit: 20 row(s)  (actual time=0.0119..0.109 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0115..0.108 rows=20 loops=1)
        -> Index scan on products using idx_test
           (actual time=0.00592..0.1 rows=138 loops=1)
```

**분석**: `deleted_at` 커버링 효과는 있으나 `brand_id`가 인덱스 밖이라 ~138건 스캔 필요.

---

### [3컬럼 복합 인덱스]

#### 시나리오 11: `(brand_id, deleted_at, price)`

```sql
CREATE INDEX idx_brand_id_deleted_at_price ON products(brand_id, deleted_at, price);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_brand_id_deleted_at_price |
| rows | 18,886 |
| filtered | 100.00% |
| Extra | **Using index condition** |
| **실제 실행 시간** | **0.028ms** |

```
-> Limit: 20 row(s)  (actual time=0.00846..0.0283 rows=20 loops=1)
    -> Index range scan on products using idx_test over (brand_id = 1 AND deleted_at = NULL),
       with index condition: ((products.brand_id = 1) and (products.deleted_at is null))
       (actual time=0.00796..0.0268 rows=20 loops=1)
```

**분석**: `brand_id + deleted_at` 로 정확한 구간 접근(filtered: 100%), `price` 순방향 스캔으로 20건 즉시 반환. 의도가 명확한 구조.

---

#### 시나리오 12: `(brand_id, price, deleted_at)`

```sql
CREATE INDEX idx_brand_id_price_deleted_at ON products(brand_id, price, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_price_deleted_at |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using index condition** |
| **실제 실행 시간** | **0.031ms** |

```
-> Limit: 20 row(s)  (actual time=0.00646..0.0309 rows=20 loops=1)
    -> Index lookup on products using idx_test (brand_id=1),
       with index condition: (products.deleted_at is null)
       (actual time=0.00588..0.0295 rows=20 loops=1)
```

**분석**: 시나리오 6 `(brand_id, price)`에 `deleted_at` 추가한 커버링 인덱스. 테이블 행 접근 없이 `deleted_at` 필터 처리 가능.

---

#### 시나리오 13: `(deleted_at, brand_id, price)`

```sql
CREATE INDEX idx_deleted_at_brand_id_price ON products(deleted_at, brand_id, price);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_deleted_at_brand_id_price |
| rows | 18,886 |
| filtered | 100.00% |
| Extra | **Using index condition** |
| **실제 실행 시간** | **0.027ms** |

```
-> Limit: 20 row(s)  (actual time=0.00796..0.0272 rows=20 loops=1)
    -> Index range scan on products using idx_test over (deleted_at = NULL AND brand_id = 1),
       with index condition: ((products.brand_id = 1) and (products.deleted_at is null))
       (actual time=0.0075..0.0257 rows=20 loops=1)
```

**분석**: `deleted_at=NULL + brand_id=1` 복합 등치 조건으로 정확한 구간 접근(filtered: 100%), `price` 순방향 스캔. 시나리오 11과 동일한 성능.

---

#### 시나리오 14: `(deleted_at, price, brand_id)`

```sql
CREATE INDEX idx_deleted_at_price_brand_id ON products(deleted_at, price, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_deleted_at_price_brand_id |
| rows | 49,697 |
| filtered | 18.82% |
| Extra | **Using index condition** |
| **실제 실행 시간** | **0.028ms** |

```
-> Limit: 20 row(s)  (actual time=0.00821..0.0276 rows=20 loops=1)
    -> Index range scan on products using idx_test over (deleted_at = NULL),
       with index condition: ((products.brand_id = 1) and (products.deleted_at is null))
       (actual time=0.00771..0.0262 rows=20 loops=1)
```

**분석**: `deleted_at IS NULL` 구간에서 `price ASC` 순방향 + `brand_id` 인덱스 내 필터(커버링). 테이블 접근 없음. 단, brand_id가 세 번째라 range scan이 넓음.

---

#### 시나리오 15: `(price, brand_id, deleted_at)`

```sql
CREATE INDEX idx_price_brand_id_deleted_at ON products(price, brand_id, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_price_brand_id_deleted_at |
| key_len | 25 |
| rows | 106 |
| filtered | 1.88% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.026ms** |

```
-> Limit: 20 row(s)  (actual time=0.00783..0.0257 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.00721..0.0243 rows=20 loops=1)
        -> Index scan on products using idx_test
           (actual time=0.006..0.0219 rows=20 loops=1)
```

**분석**: 커버링 인덱스. price 순서대로 스캔하면서 brand_id + deleted_at을 인덱스 내에서 필터링. 현재 테스트에서 **가장 빠름**.

---

#### 시나리오 16: `(price, deleted_at, brand_id)`

```sql
CREATE INDEX idx_price_deleted_at_brand_id ON products(price, deleted_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_price_deleted_at_brand_id |
| key_len | 25 |
| rows | 106 |
| filtered | 1.88% |
| Extra | **Using where** |
| **실제 실행 시간** | **0.033ms** |

```
-> Limit: 20 row(s)  (actual time=0.015..0.0334 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0145..0.0322 rows=20 loops=1)
        -> Index scan on products using idx_test
           (actual time=0.0133..0.0296 rows=20 loops=1)
```

**분석**: 시나리오 15와 구조는 같으나 `deleted_at`이 `brand_id`보다 앞이라 커버링 필터 순서가 다름.

---

## 결과 요약

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
| 11 | `(brand_id, deleted_at, price)` | range | X | 0.028ms | |
| 12 | `(brand_id, price, deleted_at)` | ref | X | 0.031ms | 커버링 |
| 13 | `(deleted_at, brand_id, price)` | range | X | 0.027ms | |
| 14 | `(deleted_at, price, brand_id)` | range | X | 0.028ms | 커버링 |
| **15** | **`(price, brand_id, deleted_at)`** | **index** | **X** | **0.026ms** | **최속** |
| 16 | `(price, deleted_at, brand_id)` | index | X | 0.033ms | |

---

## 핵심 인사이트

### 1. filesort가 발생하는 패턴

`price`가 인덱스에 없거나 brand_id/deleted_at 뒤에 오지 않으면 반드시 filesort 발생.
filesort 그룹(시나리오 1, 2, 3, 5, 7)은 모두 7ms 이상으로 느리다.

### 2. `(brand_id, price)` — Entity 선언 인덱스의 효과

```sql
-- Product.kt 엔티티에 이미 선언
Index(name = "idx_products_brand_id_price", columnList = "brand_id, price")
```

DB에 실제 생성되면 0.026ms로 2컬럼 중 최고 성능. `created_at` 정렬 분석의 `(brand_id, created_at)`과 동일한 패턴.

### 3. `price` 선두 인덱스 (시나리오 4, 9, 10, 15, 16)의 공통 약점

type=index (전체 인덱스 스캔)으로 brand_id 조건을 인덱스 단계에서 처리하지 못함.
`brand_id=1`이 전체의 약 10%이므로 평균 ~141건 스캔 후 20건 충족 — 현재 데이터에선 빠르지만
brand_id별 데이터 분포에 따라 성능이 가변적.

### 4. 안정성 비교: 시나리오 6 vs 11 vs 13

| | `(brand_id, price)` | `(brand_id, deleted_at, price)` | `(deleted_at, brand_id, price)` |
|--|--|--|--|
| 실행 시간 | **0.026ms** | 0.028ms | 0.027ms |
| type | ref | range | range |
| filtered | 10% | 100% | 100% |
| 삭제 비율 증가 시 | 소폭 저하 | **안정적** | **안정적** |
| 인덱스 크기 | 작음 | 큼 | 큼 |
| Entity 선언 여부 | **O** | X | X |

`(brand_id, price)`은 `deleted_at`을 인덱스 밖에서 체크하지만, 삭제 비율이 낮게 유지되는 한 성능 차이가 미미하다.
`(brand_id, deleted_at, price)`은 쿼리 의도(WHERE → ORDER)와 순서가 일치하고 filtered: 100%로 안정적.

---

## 권장 인덱스 (brandId + price 조회)

```sql
-- 현재 엔티티에 이미 선언됨 → DB에 적용 필요
CREATE INDEX idx_products_brand_id_price ON products(brand_id, price);
```

- 삭제 비율이 낮게 유지되는 일반적인 서비스에서 최적의 성능/크기 트레이드오프
- 삭제 비율이 높아질 경우 `(brand_id, deleted_at, price)` 으로 교체 검토

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
