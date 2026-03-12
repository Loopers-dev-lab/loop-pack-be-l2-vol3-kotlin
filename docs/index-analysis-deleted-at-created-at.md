# `findAllByDeletedAtIsNull` 쿼리 인덱스 비교 분석

## 분석 대상 쿼리

```sql
SELECT id, name, description, price, likes, stock_quantity,
       brand_id, created_at, updated_at, deleted_at
FROM products
WHERE deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0
```

**발생 위치**: `ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByDeletedAtIsNull(pageable)`

## 테스트 환경

| 항목 | 값 |
|------|-----|
| MySQL 버전 | 8.0 |
| 전체 데이터 | 100,000건 |
| deleted_at IS NULL (활성) | 98,000건 (98%) |
| deleted_at IS NOT NULL (삭제) | 2,000건 (2%) |

---

## 시나리오별 EXPLAIN 결과

### 시나리오 1: 인덱스 없음 (기준선)

```sql
-- 적용 인덱스: PRIMARY KEY(id) 만 존재
```

| 항목 | 값 |
|------|-----|
| type | ALL |
| key | NULL |
| rows | 98,671 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **53.3ms** |

```
-> Limit: 20 row(s)  (cost=10212 rows=20) (actual time=53.3..53.3 rows=20 loops=1)
    -> Sort: products.created_at DESC  (cost=10212 rows=98671) (actual time=53.3..53.3 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.139..42.7 rows=98000 loops=1)
            -> Table scan on products  (actual time=0.137..37.9 rows=100000 loops=1)
```

**분석**: 풀 테이블 스캔 후 98,000건 필터링, 그 뒤 filesort 정렬. 가장 느림.

---

### 시나리오 2: `deleted_at` 단일 인덱스

```sql
CREATE INDEX idx_deleted_at ON products(deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_deleted_at |
| key_len | 9 |
| rows | 49,335 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **76.1ms** |

```
-> Limit: 20 row(s)  (cost=5968 rows=20) (actual time=76.1..76.1 rows=20 loops=1)
    -> Sort: products.created_at DESC  (cost=5968 rows=49335) (actual time=76.1..76.1 rows=20 loops=1)
        -> Index lookup on products using idx_deleted_at (deleted_at=NULL)  (actual time=0.0983..67.9 rows=98000 loops=1)
```

**분석**: 인덱스로 `deleted_at IS NULL` 조건은 해소했지만, 98,000건에 대한 **랜덤 I/O** + **filesort** 발생.
인덱스를 사용했음에도 시나리오 1보다 **더 느림**. 삭제 비율이 낮을수록 이 인덱스는 역효과.

> **핵심**: `deleted_at IS NULL`이 전체의 98%를 선택하므로 선택도(Selectivity)가 매우 낮다.
> 인덱스 스캔 + 랜덤 I/O가 풀스캔보다 비용이 크다.

---

### 시나리오 3: `created_at` 단일 인덱스

```sql
CREATE INDEX idx_created_at ON products(created_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at |
| key_len | 8 |
| rows | 20 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.116ms** |

```
-> Limit: 20 row(s)  (cost=1.87 rows=2) (actual time=0.0528..0.116 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0523..0.115 rows=20 loops=1)
        -> Index scan on products using idx_created_at (reverse)  (actual time=0.0513..0.113 rows=20 loops=1)
```

**분석**: `created_at DESC` 역방향 인덱스 스캔 + LIMIT 20 조기 종료.
98%가 활성 데이터이므로, 인덱스를 역방향으로 읽으면서 20건을 빠르게 발견.
시나리오 1 대비 약 **460배 빠름**.

> **주의**: `deleted_at` 칼럼은 인덱스에 없어 테이블 행 접근으로 필터링.
> 삭제 비율이 높아질수록 LIMIT 충족 전에 더 많은 행을 스캔해야 함.

---

### 시나리오 4: `(deleted_at, created_at)` 복합 인덱스

```sql
CREATE INDEX idx_deleted_at_created_at ON products(deleted_at, created_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_deleted_at_created_at |
| key_len | 9 |
| rows | 49,335 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.104ms** |

```
-> Limit: 20 row(s)  (cost=5968 rows=20) (actual time=0.099..0.104 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (cost=5968 rows=49335) (actual time=0.0986..0.103 rows=20 loops=1)
        -> Index lookup on products using idx_deleted_at_created_at (deleted_at=NULL) (reverse)
           (actual time=0.0976..0.101 rows=20 loops=1)
```

**분석**:
1. `deleted_at = NULL` 조건으로 인덱스의 해당 구간에 바로 접근 (type: ref)
2. 그 구간 내에서 `created_at DESC` 역방향 스캔으로 20건 즉시 반환
3. filesort 없음, 추가 테이블 접근 없음

> **핵심**: `deleted_at` 칼럼이 선두 키로 NULL 구간을 정확히 분리하고, `created_at` 이 정렬을 담당.
> 삭제 비율이 증가해도 **성능이 안정적**으로 유지됨.

---

### 시나리오 5: `(created_at, deleted_at)` 복합 인덱스

```sql
CREATE INDEX idx_created_at_deleted_at ON products(created_at, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at_deleted_at |
| key_len | 17 |
| rows | 20 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.054ms** |

```
-> Limit: 20 row(s)  (cost=1.87 rows=2) (actual time=0.0259..0.0541 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0255..0.0529 rows=20 loops=1)
        -> Index scan on products using idx_created_at_deleted_at (reverse)  (actual time=0.0248..0.0513 rows=20 loops=1)
```

**분석**: 시나리오 3과 동일한 실행 계획이지만, `deleted_at`가 인덱스에 포함되어 테이블 행 접근 없이 인덱스 내에서 필터링 가능 (**Covering Index** 효과).
현재 데이터 분포에서 **가장 빠름**.

> **주의**: type이 `index` (full index scan)이라 MySQL 옵티마이저는 선택도를 낮게 평가(filtered: 10%).
> 삭제 비율이 높아지면 스캔 효율이 저하될 수 있음.

---

## 결과 요약

| 시나리오 | 인덱스 구성 | type | filesort | 실행 시간 | 기준 대비 |
|---------|-----------|------|----------|---------|---------|
| 1 | 없음 | ALL | O | 53.3ms | 1x (기준) |
| 2 | `(deleted_at)` | ref | O | 76.1ms | **1.43x 느림** |
| 3 | `(created_at)` | index | X | 0.116ms | 460x 빠름 |
| 4 | `(deleted_at, created_at)` | ref | X | 0.104ms | 513x 빠름 |
| **5** | **`(created_at, deleted_at)`** | **index** | **X** | **0.054ms** | **987x 빠름** |

---

## 핵심 인사이트

### 1. `deleted_at` 단일 인덱스는 역효과

`deleted_at IS NULL` 조건의 선택도가 2%에 불과하다 (98%가 NULL).
인덱스로 매칭된 98,000건에 대해 랜덤 I/O가 발생하므로 풀스캔보다 느리다.
**선택도가 낮은 칼럼에 단독 인덱스는 의미가 없다.**

### 2. 정렬 칼럼 인덱스 + LIMIT의 강력함

`ORDER BY created_at DESC LIMIT 20`처럼 정렬 + LIMIT 조합에서는
정렬 칼럼에 인덱스가 있으면 filesort 없이 역방향 스캔 + 조기 종료로 극적인 성능 개선이 가능하다.

### 3. 시나리오 4 vs 5: 안정성 vs 현재 성능

| | 시나리오 4 `(deleted_at, created_at)` | 시나리오 5 `(created_at, deleted_at)` |
|--|--|--|
| 현재 실행 시간 | 0.104ms | **0.054ms** |
| 옵티마이저 type | ref (정확한 선택도 인식) | index (full index scan) |
| 삭제 비율 증가 시 | **안정적** (deleted_at 구간만 처리) | 성능 저하 가능 |
| 쿼리 의도 반영 | O (WHERE → ORDER 순서와 일치) | 부분적 |

**장기적으로 안정적인 선택은 시나리오 4 `(deleted_at, created_at)`이다.**
삭제 비율이 높아져도 `deleted_at = NULL` 구간만 탐색하므로 성능이 보장된다.

현재 시나리오 5가 더 빠른 이유는 **98%가 활성 데이터**라는 현재 분포 때문이며,
서비스가 성장해 삭제 데이터가 쌓이면 이 이점은 사라진다.

---

## 권장 인덱스

```sql
CREATE INDEX idx_products_deleted_at_created_at ON products(deleted_at, created_at);
```

- `WHERE deleted_at IS NULL` → 인덱스 선두 키로 해당 구간 직접 접근 (type: ref)
- `ORDER BY created_at DESC` → 이미 정렬된 인덱스 구간 역방향 스캔, filesort 불필요
- 삭제 비율 변화에 관계없이 성능 안정적
- `findAllByBrandIdAndDeletedAtIsNull` 쿼리에는 별도로 `(brand_id, deleted_at, created_at)` 복합 인덱스 고려 필요

---

---

---

# `findAllByBrandIdAndDeletedAtIsNull` 쿼리 인덱스 비교 분석

## 분석 대상 쿼리

```sql
SELECT id, name, description, price, likes, stock_quantity,
       brand_id, created_at, updated_at, deleted_at
FROM products
WHERE brand_id = ?
  AND deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20 OFFSET 0
```

**발생 위치**: `ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)`

## 테스트 환경

| 항목 | 값 |
|------|-----|
| MySQL 버전 | 8.0 |
| 전체 데이터 | 100,000건 |
| 테스트 brand_id | 1 (총 9,906건 / 활성 9,728건 / 삭제 178건) |
| 삭제 비율 (brand_id=1) | 약 1.8% |
| 기존 인덱스 | `idx_products_brand_id_likes (brand_id, likes)` |

---

## 시나리오별 EXPLAIN 결과

> 3개 칼럼(brand_id, deleted_at, created_at)의 모든 조합: 1(없음) + 3(단일) + 6(2컬럼) + 6(3컬럼) = **16 케이스**

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
| rows | 18,816 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **28.1ms** |

```
-> Limit: 20 row(s)  (actual time=28.1..28.1 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=28..28.1 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.806..26.6 rows=9728 loops=1)
            -> Index lookup on products using idx_products_brand_id_likes (brand_id=1)
               (actual time=0.798..26.1 rows=9906 loops=1)
```

**분석**: `brand_id` 로 9,906건 필터 후 `deleted_at IS NULL` 확인(9,728건), 전체 filesort 발생.

---

#### 시나리오 2: `(brand_id)` 단일 인덱스

```sql
CREATE INDEX idx_brand_id ON products(brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id |
| rows | 18,336 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **7.15ms** |

```
-> Limit: 20 row(s)  (actual time=7.15..7.15 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=7.15..7.15 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.0498..6.36 rows=9728 loops=1)
            -> Index lookup on products using idx_test_brand_id (brand_id=1)
               (actual time=0.0491..5.97 rows=9906 loops=1)
```

**분석**: brand_id 조회로 9,906건 빠르게 접근하지만, `created_at` 정렬이 없어 filesort 발생.

---

#### 시나리오 3: `(deleted_at)` 단일 인덱스

```sql
CREATE INDEX idx_deleted_at ON products(deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (신규 인덱스 무시!) |
| rows | 18,816 |
| filtered | 50.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **9.84ms** |

```
-> Limit: 20 row(s)  (actual time=9.84..9.86 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=9.84..9.86 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.0579..8.98 rows=9728 loops=1)
            -> Index lookup on products using idx_products_brand_id_likes (brand_id=1)
               (actual time=0.0573..8.52 rows=9906 loops=1)
```

**분석**: MySQL 옵티마이저가 `deleted_at` 인덱스를 무시하고 기존 `brand_id_likes` 인덱스를 선택.
`deleted_at IS NULL`의 낮은 선택도(전체 98% 해당) 때문에 활용 가치 없다고 판단.

---

#### 시나리오 4: `(created_at)` 단일 인덱스

```sql
CREATE INDEX idx_created_at ON products(created_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at |
| rows | 104 |
| filtered | 1.91% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.227ms** |

```
-> Limit: 20 row(s)  (actual time=0.05..0.227 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0496..0.226 rows=20 loops=1)
        -> Index scan on products using idx_test_created_at (reverse)
           (actual time=0.0349..0.207 rows=139 loops=1)
```

**분석**: 전체 `created_at` 역방향 스캔 중 `brand_id=1 AND deleted_at IS NULL` 필터로 20건 조기 종료.
brand_id=1이 전체의 약 10%이므로 평균 ~139건만 스캔하면 20건 충족.

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
| rows | 18,146 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **8.06ms** |

```
-> Limit: 20 row(s)  (actual time=8.06..8.07 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=8.06..8.06 rows=20 loops=1)
        -> Index lookup on products using idx_brand_id_deleted_at (brand_id=1, deleted_at=NULL)
           (actual time=0.0645..7.22 rows=9728 loops=1)
```

**분석**: `brand_id + deleted_at` 조건은 인덱스로 완벽히 처리(filtered: 100%)했지만, `created_at` 정렬이 없어 9,728건 filesort 발생.

---

#### 시나리오 6: `(brand_id, created_at)` ← Entity 정의와 동일

```sql
CREATE INDEX idx_brand_id_created_at ON products(brand_id, created_at);
-- ※ Product 엔티티의 idx_products_brand_id_created_at 과 동일
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_created_at |
| rows | 18,716 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.098ms** |

```
-> Limit: 20 row(s)  (actual time=0.0935..0.0983 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0931..0.0971 rows=20 loops=1)
        -> Index lookup on products using idx_brand_id_created_at (brand_id=1) (reverse)
           (actual time=0.0924..0.0955 rows=20 loops=1)
```

**분석**:
1. `brand_id=1` 로 인덱스 구간 직접 접근 (type: ref)
2. 해당 구간 내 `created_at DESC` 역방향 스캔
3. `deleted_at IS NULL` 체크(테이블 행 접근)로 20건 조기 종료
4. filesort 없음

> `Product` 엔티티에 이미 `idx_products_brand_id_created_at` 로 선언된 인덱스.
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
| rows | 18,146 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **7.93ms** |

```
-> Limit: 20 row(s)  (actual time=7.93..7.93 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=7.93..7.93 rows=20 loops=1)
        -> Index lookup on products using idx_deleted_at_brand_id (deleted_at=NULL, brand_id=1)
           (actual time=0.051..7.09 rows=9728 loops=1)
```

**분석**: 시나리오 5와 유사. `deleted_at + brand_id` 조건 처리 후 9,728건 filesort. `created_at`이 없어 정렬 최적화 불가.

---

#### 시나리오 8: `(deleted_at, created_at)`

```sql
CREATE INDEX idx_deleted_at_created_at ON products(deleted_at, created_at);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_deleted_at_created_at |
| rows | 49,335 |
| filtered | 19.07% |
| Extra | **Using index condition; Using where; Backward index scan** |
| **실제 실행 시간** | **0.268ms** |

```
-> Limit: 20 row(s)  (actual time=0.0913..0.268 rows=20 loops=1)
    -> Filter: (products.brand_id = 1)  (actual time=0.0909..0.267 rows=20 loops=1)
        -> Index range scan on products using idx_deleted_at_created_at over (deleted_at = NULL) (reverse)
           (actual time=0.0889..0.261 rows=138 loops=1)
```

**분석**: `deleted_at IS NULL` 구간 역방향 스캔으로 `created_at DESC` 처리하고, `brand_id=1` 필터 적용.
전체 98,000건 중 역방향으로 읽으며 brand_id=1인 20건 발견(평균 ~138건 스캔).

---

#### 시나리오 9: `(created_at, brand_id)`

```sql
CREATE INDEX idx_created_at_brand_id ON products(created_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at_brand_id |
| rows | 104 |
| filtered | 1.91% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.725ms** |

```
-> Limit: 20 row(s)  (actual time=0.135..0.725 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.133..0.721 rows=20 loops=1)
        -> Index scan on products using idx_created_at_brand_id (reverse)
           (actual time=0.126..0.698 rows=139 loops=1)
```

**분석**: 시나리오 4와 유사하지만 `brand_id`가 인덱스에 포함되어 커버링 효과. 그러나 type=index(전체 스캔)이라 시나리오 6보다 느림.

---

#### 시나리오 10: `(created_at, deleted_at)`

```sql
CREATE INDEX idx_created_at_deleted_at ON products(created_at, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at_deleted_at |
| rows | 104 |
| filtered | 1.91% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.232ms** |

```
-> Limit: 20 row(s)  (actual time=0.0545..0.232 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0541..0.23 rows=20 loops=1)
        -> Index scan on products using idx_created_at_deleted_at (reverse)
           (actual time=0.0512..0.223 rows=139 loops=1)
```

**분석**: `deleted_at`이 인덱스에 있어 커버링 효과로 시나리오 9보다 빠름. 그러나 brand_id 조건이 인덱스 밖이라 type=index 전체 스캔.

---

### [3컬럼 복합 인덱스]

#### 시나리오 11: `(brand_id, deleted_at, created_at)`

```sql
CREATE INDEX idx_brand_id_deleted_at_created_at ON products(brand_id, deleted_at, created_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_deleted_at_created_at |
| rows | 18,600 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.118ms** |

```
-> Limit: 20 row(s)  (actual time=0.113..0.118 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.113..0.117 rows=20 loops=1)
        -> Index lookup on products using idx_brand_id_deleted_at_created_at (brand_id=1, deleted_at=NULL) (reverse)
           (actual time=0.112..0.115 rows=20 loops=1)
```

**분석**: `brand_id + deleted_at` 로 정확한 구간 접근(type: ref, filtered: 100%), `created_at` 역방향 스캔으로 20건 즉시 반환. 의도가 명확한 구조.

---

#### 시나리오 12: `(brand_id, created_at, deleted_at)`

```sql
CREATE INDEX idx_brand_id_created_at_deleted_at ON products(brand_id, created_at, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_brand_id_created_at_deleted_at |
| rows | 18,816 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.088ms** |

```
-> Limit: 20 row(s)  (actual time=0.0839..0.088 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0835..0.0867 rows=20 loops=1)
        -> Index lookup on products using idx_brand_id_created_at_deleted_at (brand_id=1) (reverse)
           (actual time=0.0829..0.0853 rows=20 loops=1)
```

**분석**: 시나리오 6 `(brand_id, created_at)`에 `deleted_at` 추가한 커버링 인덱스. 테이블 행 접근 없이 `deleted_at` 필터 처리 가능.

---

#### 시나리오 13: `(deleted_at, brand_id, created_at)`

```sql
CREATE INDEX idx_deleted_at_brand_id_created_at ON products(deleted_at, brand_id, created_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_deleted_at_brand_id_created_at |
| rows | 18,600 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.087ms** |

```
-> Limit: 20 row(s)  (actual time=0.0832..0.0876 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0828..0.0863 rows=20 loops=1)
        -> Index lookup on products using idx_deleted_at_brand_id_created_at (deleted_at=NULL, brand_id=1) (reverse)
           (actual time=0.0821..0.0847 rows=20 loops=1)
```

**분석**: `deleted_at=NULL + brand_id=1` 복합 등치 조건으로 정확한 구간 접근(filtered: 100%), `created_at` 역방향 스캔. 현재 테스트에서 가장 빠름.

---

#### 시나리오 14: `(deleted_at, created_at, brand_id)`

```sql
CREATE INDEX idx_deleted_at_created_at_brand_id ON products(deleted_at, created_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | idx_deleted_at_created_at_brand_id |
| rows | 49,335 |
| filtered | 19.07% |
| Extra | **Using index condition; Backward index scan** |
| **실제 실행 시간** | **0.181ms** |

```
-> Limit: 20 row(s)  (actual time=0.178..0.181 rows=20 loops=1)
    -> Index range scan on products using idx_deleted_at_created_at_brand_id over (deleted_at = NULL) (reverse),
       with index condition: ((products.brand_id = 1) and (products.deleted_at is null))
       (actual time=0.177..0.18 rows=20 loops=1)
```

**분석**: `deleted_at IS NULL` 구간에서 `created_at DESC` 역방향 + `brand_id` 인덱스 내 필터(커버링). 테이블 접근 없음("Using where" 없음). 단, brand_id가 세 번째라 range scan이 넓음.

---

#### 시나리오 15: `(created_at, brand_id, deleted_at)`

```sql
CREATE INDEX idx_created_at_brand_id_deleted_at ON products(created_at, brand_id, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at_brand_id_deleted_at |
| rows | 104 |
| filtered | 1.91% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.904ms** |

```
-> Limit: 20 row(s)  (actual time=0.207..0.904 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.206..0.901 rows=20 loops=1)
        -> Index scan on products using idx_created_at_brand_id_deleted_at (reverse)
           (actual time=0.198..0.88 rows=139 loops=1)
```

**분석**: 커버링 인덱스이지만 type=index(전체 스캔). 필터된 rows 추정이 부정확(1.91%)해 옵티마이저가 비효율적 계획 선택.

---

#### 시나리오 16: `(created_at, deleted_at, brand_id)`

```sql
CREATE INDEX idx_created_at_deleted_at_brand_id ON products(created_at, deleted_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_created_at_deleted_at_brand_id |
| rows | 104 |
| filtered | 1.91% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.207ms** |

```
-> Limit: 20 row(s)  (actual time=0.051..0.207 rows=20 loops=1)
    -> Filter: ((products.brand_id = 1) and (products.deleted_at is null))
               (actual time=0.0503..0.206 rows=20 loops=1)
        -> Index scan on products using idx_created_at_deleted_at_brand_id (reverse)
           (actual time=0.0468..0.198 rows=139 loops=1)
```

**분석**: 시나리오 15와 구조는 같으나 `deleted_at` 순서 앞에 위치해 커버링 필터 효율이 다소 높음.

---

## 결과 요약

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
| 11 | `(brand_id, deleted_at, created_at)` | ref | X | 0.118ms | |
| 12 | `(brand_id, created_at, deleted_at)` | ref | X | 0.088ms | 커버링 |
| **13** | **`(deleted_at, brand_id, created_at)`** | **ref** | **X** | **0.087ms** | **최속** |
| 14 | `(deleted_at, created_at, brand_id)` | range | X | 0.181ms | 커버링 |
| 15 | `(created_at, brand_id, deleted_at)` | index | X | 0.904ms | |
| 16 | `(created_at, deleted_at, brand_id)` | index | X | 0.207ms | |

---

## 핵심 인사이트

### 1. filesort가 발생하는 패턴

`created_at`이 인덱스에 없거나 brand_id/deleted_at 뒤에 오지 않으면 반드시 filesort 발생.
filesort 그룹(시나리오 1, 2, 3, 5, 7)은 모두 7ms 이상으로 느리다.

### 2. `(brand_id, created_at)` — Entity 선언 인덱스의 효과

```sql
-- Product.kt 엔티티에 이미 선언
Index(name = "idx_products_brand_id_created_at", columnList = "brand_id, created_at")
```

DB에 실제 생성되면 0.098ms로 2컬럼 중 최고 성능. 현재 DB에는 미적용 상태.

### 3. `created_at` 선두 인덱스 (시나리오 4, 9, 10, 15, 16)의 공통 약점

type=index (전체 인덱스 스캔)으로 brand_id 조건을 인덱스 단계에서 처리하지 못함.
`brand_id=1`이 전체의 약 10%이므로 평균 ~139건 스캔 후 20건 충족 — 현재 데이터에선 빠르지만
brand_id별 데이터 분포에 따라 성능이 가변적.

### 4. 안정성 비교: 시나리오 6 vs 11 vs 13

| | `(brand_id, created_at)` | `(brand_id, deleted_at, created_at)` | `(deleted_at, brand_id, created_at)` |
|--|--|--|--|
| 실행 시간 | 0.098ms | 0.118ms | **0.087ms** |
| type | ref | ref | ref |
| filtered | 10% | 100% | 100% |
| 삭제 비율 증가 시 | 소폭 저하 | **안정적** | **안정적** |
| 인덱스 크기 | 작음 | 큼 | 큼 |
| Entity 선언 여부 | **O** | X | X |

`(brand_id, created_at)` 은 `deleted_at`을 인덱스 밖에서 체크하지만, 삭제 비율이 낮게 유지되는 한 성능 차이가 미미하다.
`(brand_id, deleted_at, created_at)` 은 쿼리 의도(WHERE → ORDER)와 순서가 일치하고 filtered: 100%로 안정적.

---

## 권장 인덱스 (brandId 조회)

```sql
-- 현재 엔티티에 이미 선언됨 → DB에 적용 필요
CREATE INDEX idx_products_brand_id_created_at ON products(brand_id, created_at);
```

- 삭제 비율이 낮게 유지되는 일반적인 서비스에서 최적의 성능/크기 트레이드오프
- 삭제 비율이 높아질 경우 `(brand_id, deleted_at, created_at)` 으로 교체 검토

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
