# `findAllByBrandIdAndDeletedAtIsNull` 쿼리 인덱스 비교 분석 (likes 정렬)

## 분석 대상 쿼리

```sql
SELECT id, name, description, price, likes, stock_quantity,
       brand_id, created_at, updated_at, deleted_at
FROM products
WHERE brand_id = ?
  AND deleted_at IS NULL
ORDER BY likes DESC
LIMIT 20 OFFSET 0
```

**발생 위치**: `ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByBrandIdAndDeletedAtIsNull(brandId, pageable)`
**정렬 조건**: `sort=likes_desc`

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

> 3개 칼럼(brand_id, deleted_at, likes)의 모든 조합: 1(없음) + 3(단일) + 6(2컬럼) + 6(3컬럼) = **16 케이스**
>
> **특이사항**: 기존 `idx_products_brand_id_likes(brand_id, likes)` 인덱스가 이미 이 쿼리에 최적화되어 있어,
> 대부분의 시나리오에서 옵티마이저가 기존 인덱스를 선택한다.

---

### [단일 인덱스]

#### 시나리오 1: 인덱스 없음 (기준선 — 기존 brand_id_likes 활용)

```sql
-- 기존 idx_products_brand_id_likes (brand_id, likes) 만 존재
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_products_brand_id_likes |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.122ms** |

```
-> Limit: 20 row(s)  (actual time=0.0867..0.122 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0861..0.12 rows=20 loops=1)
        -> Index lookup on products using idx_products_brand_id_likes (brand_id=1) (reverse)
           (actual time=0.0853..0.118 rows=20 loops=1)
```

**분석**: 기존 `(brand_id, likes)` 인덱스가 이미 최적 동작.
`brand_id=1`로 인덱스 구간 접근 → `likes DESC` 역방향 스캔 → 20건 조기 종료.
**filesort 없음**. 기존 인덱스만으로 충분히 빠름.

> **핵심**: `(brand_id, likes)` 인덱스가 WHERE + ORDER BY 를 모두 처리.
> `deleted_at IS NULL`은 테이블 행 접근으로 필터링하지만, 삭제 비율이 2%이므로 거의 모든 행이 통과.

---

#### 시나리오 2: `(brand_id)` 단일 인덱스

```sql
CREATE INDEX idx_brand_id ON products(brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스 유지) |
| rows | 18,628 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.044ms** |

**분석**: 옵티마이저가 기존 `brand_id_likes` 인덱스를 선택. 동일한 실행 계획.

---

#### 시나리오 3: `(deleted_at)` 단일 인덱스

```sql
CREATE INDEX idx_deleted_at ON products(deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스 유지) |
| rows | 18,704 |
| filtered | 50.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.035ms** |

**분석**: 옵티마이저가 `deleted_at` 인덱스를 무시하고 기존 `brand_id_likes` 선택.
`brand_id + likes` 복합이 WHERE + ORDER를 동시 처리하므로 더 효율적이라 판단.

---

#### 시나리오 4: `(likes)` 단일 인덱스

```sql
CREATE INDEX idx_likes ON products(likes);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스 유지) |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.038ms** |

**분석**: `likes` 단독 인덱스보다 `(brand_id, likes)` 복합이 brand_id 필터링까지 처리하므로 옵티마이저가 기존 인덱스 선택.

---

### [2컬럼 복합 인덱스]

#### 시나리오 5: `(brand_id, deleted_at)`

```sql
CREATE INDEX idx_brand_id_deleted_at ON products(brand_id, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | **idx_products_brand_id_likes** |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using index condition; Using where; Backward index scan** |
| **실제 실행 시간** | **0.037ms** |

**분석**: `likes` 정렬이 없는 `(brand_id, deleted_at)`보다 기존 `(brand_id, likes)` 인덱스가 정렬까지 처리하므로 더 효율적.

---

#### 시나리오 6: `(brand_id, likes)` ← 기존 인덱스와 동일

```sql
CREATE INDEX idx_brand_id_likes ON products(brand_id, likes);
-- ※ idx_products_brand_id_likes 와 동일
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_products_brand_id_likes |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.036ms** |

**분석**: 기존 인덱스와 동일 구조. `brand_id=1` 접근 → `likes DESC` 역방향 스캔 → 20건 조기 종료.

---

#### 시나리오 7: `(deleted_at, brand_id)`

```sql
CREATE INDEX idx_deleted_at_brand_id ON products(deleted_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | range |
| key | **idx_products_brand_id_likes** |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using index condition; Using where; Backward index scan** |
| **실제 실행 시간** | **0.048ms** |

**분석**: 시나리오 5와 유사. 옵티마이저가 기존 인덱스를 선호.

---

#### 시나리오 8: `(deleted_at, likes)`

```sql
CREATE INDEX idx_deleted_at_likes ON products(deleted_at, likes);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** |
| rows | 18,704 |
| filtered | 50.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.034ms** |

**분석**: `(deleted_at, likes)` 인덱스는 brand_id 조건을 처리하지 못하므로, 기존 `(brand_id, likes)` 인덱스가 선택됨.

---

#### 시나리오 9: `(likes, brand_id)`

```sql
CREATE INDEX idx_likes_brand_id ON products(likes, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.043ms** |

**분석**: `likes` 선두인데 brand_id 등치 조건에 쓸 수 없으므로 기존 인덱스 선택.

---

#### 시나리오 10: `(likes, deleted_at)`

```sql
CREATE INDEX idx_likes_deleted_at ON products(likes, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.032ms** |

**분석**: 기존 인덱스가 brand_id + likes 모두 처리하므로 선택됨.

---

### [3컬럼 복합 인덱스]

#### 시나리오 11: `(brand_id, deleted_at, likes)`

```sql
CREATE INDEX idx_brand_id_deleted_at_likes ON products(brand_id, deleted_at, likes);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_test** (신규 인덱스 선택!) |
| rows | 18,398 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.043ms** |

```
-> Limit: 20 row(s)  (actual time=0.00704..0.043 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.00671..0.0421 rows=20 loops=1)
        -> Index lookup on products using idx_test (brand_id=1, deleted_at=NULL) (reverse)
           (actual time=0.00621..0.0405 rows=20 loops=1)
```

**분석**: `brand_id + deleted_at` 으로 정확한 구간 접근(type: ref, filtered: 100%), `likes` 역방향 스캔으로 20건 즉시 반환. 의도가 명확한 구조.

---

#### 시나리오 12: `(brand_id, likes, deleted_at)`

```sql
CREATE INDEX idx_brand_id_likes_deleted_at ON products(brand_id, likes, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스) |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.034ms** |

**분석**: 기존 `(brand_id, likes)` 인덱스와 구조가 유사. 옵티마이저가 기존 인덱스를 선택하거나 동일하게 동작.

---

#### 시나리오 13: `(deleted_at, brand_id, likes)`

```sql
CREATE INDEX idx_deleted_at_brand_id_likes ON products(deleted_at, brand_id, likes);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_test** (신규 인덱스 선택!) |
| rows | 18,398 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.040ms** |

```
-> Limit: 20 row(s)  (actual time=0.00662..0.0403 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.00625..0.0391 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL, brand_id=1) (reverse)
           (actual time=0.00575..0.0378 rows=20 loops=1)
```

**분석**: `deleted_at=NULL + brand_id=1` 복합 등치 조건으로 정확한 구간 접근(filtered: 100%), `likes` 역방향 스캔. 시나리오 11과 동일한 성능.

---

#### 시나리오 14: `(deleted_at, likes, brand_id)`

```sql
CREATE INDEX idx_deleted_at_likes_brand_id ON products(deleted_at, likes, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스) |
| rows | 18,704 |
| filtered | 50.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.035ms** |

**분석**: 옵티마이저가 기존 `(brand_id, likes)` 인덱스를 선택. `deleted_at` 선두 + `likes` 정렬은 brand_id 필터를 처리하지 못해 비효율.

---

#### 시나리오 15: `(likes, brand_id, deleted_at)`

```sql
CREATE INDEX idx_likes_brand_id_deleted_at ON products(likes, brand_id, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스) |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.037ms** |

**분석**: `likes` 선두라 brand_id 등치 조건 활용 불가. 기존 인덱스가 선택됨.

---

#### 시나리오 16: `(likes, deleted_at, brand_id)`

```sql
CREATE INDEX idx_likes_deleted_at_brand_id ON products(likes, deleted_at, brand_id);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | **idx_products_brand_id_likes** (기존 인덱스) |
| rows | 18,704 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.038ms** |

**분석**: 기존 인덱스가 더 효율적이라 선택됨.

---

## 결과 요약

| # | 인덱스 구성 | type | 실제 사용 인덱스 | filesort | 실행 시간 | 비고 |
|---|-----------|------|----------------|----------|----------|------|
| 1 | 없음 (brand_id_likes) | ref | brand_id_likes | X | 0.122ms | 기준선 |
| 2 | `(brand_id)` | ref | brand_id_likes | X | 0.044ms | |
| 3 | `(deleted_at)` | ref | brand_id_likes | X | 0.035ms | |
| 4 | `(likes)` | ref | brand_id_likes | X | 0.038ms | |
| 5 | `(brand_id, deleted_at)` | range | brand_id_likes | X | 0.037ms | |
| **6** | **`(brand_id, likes)`** | **ref** | **brand_id_likes** | **X** | **0.036ms** | **기존 인덱스** |
| 7 | `(deleted_at, brand_id)` | range | brand_id_likes | X | 0.048ms | |
| 8 | `(deleted_at, likes)` | ref | brand_id_likes | X | 0.034ms | |
| 9 | `(likes, brand_id)` | ref | brand_id_likes | X | 0.043ms | |
| 10 | `(likes, deleted_at)` | ref | brand_id_likes | X | 0.032ms | |
| 11 | `(brand_id, deleted_at, likes)` | ref | **idx_test** | X | 0.043ms | filtered: 100% |
| 12 | `(brand_id, likes, deleted_at)` | ref | brand_id_likes | X | 0.034ms | |
| 13 | `(deleted_at, brand_id, likes)` | ref | **idx_test** | X | 0.040ms | filtered: 100% |
| 14 | `(deleted_at, likes, brand_id)` | ref | brand_id_likes | X | 0.035ms | |
| 15 | `(likes, brand_id, deleted_at)` | ref | brand_id_likes | X | 0.037ms | |
| 16 | `(likes, deleted_at, brand_id)` | ref | brand_id_likes | X | 0.038ms | |

---

## 핵심 인사이트

### 1. 기존 `(brand_id, likes)` 인덱스가 이미 최적

16개 시나리오 중 14개에서 옵티마이저가 기존 `idx_products_brand_id_likes`를 선택했다.
이 인덱스가 `brand_id` 등치 조건과 `likes DESC` 정렬을 동시에 처리하기 때문이다.

**filesort가 한 번도 발생하지 않았다** — 모든 시나리오에서 인덱스 역방향 스캔으로 정렬 처리.

### 2. 추가 인덱스가 선택된 유일한 경우

시나리오 11 `(brand_id, deleted_at, likes)`와 시나리오 13 `(deleted_at, brand_id, likes)`만
신규 인덱스가 선택되었다. 이들은 `deleted_at` 조건까지 인덱스로 처리하여 filtered: 100%를 달성했지만,
실행 시간은 기존 인덱스와 거의 동일하다 (0.040~0.043ms vs 0.032~0.036ms).

### 3. 추가 인덱스 불필요

`deleted_at IS NULL` 필터를 인덱스에 포함해 filtered: 100%로 만들어도,
LIMIT 20 + 98% 활성 비율 조합에서는 실질적 성능 차이가 없다.
기존 `(brand_id, likes)` 인덱스만으로 ~20건만 스캔하면 20건이 모두 `deleted_at IS NULL`을 통과하기 때문이다.

### 4. 삭제 비율이 높아지면?

삭제 비율이 50%를 넘을 경우, 기존 `(brand_id, likes)` 인덱스는 LIMIT 충족을 위해
더 많은 행을 스캔해야 하므로 성능이 저하될 수 있다.
이 경우 `(brand_id, deleted_at, likes)` 인덱스가 안정적 대안이 된다.

---

## 권장 인덱스 (brandId + likes 조회)

```sql
-- 이미 존재: 추가 인덱스 불필요
-- idx_products_brand_id_likes (brand_id, likes)
```

- 기존 인덱스가 이 쿼리에 **이미 최적화**되어 있음
- `brand_id` 필터링 + `likes DESC` 정렬을 동시 처리, filesort 없음
- 삭제 비율이 극단적으로 높아지지 않는 한 추가 인덱스 불필요

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
