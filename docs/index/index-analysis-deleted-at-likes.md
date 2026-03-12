# `findAllByDeletedAtIsNull` 쿼리 인덱스 비교 분석 (likes 정렬)

## 분석 대상 쿼리

```sql
SELECT id, name, description, price, likes, stock_quantity,
       brand_id, created_at, updated_at, deleted_at
FROM products
WHERE deleted_at IS NULL
ORDER BY likes DESC
LIMIT 20 OFFSET 0
```

**발생 위치**: `ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByDeletedAtIsNull(pageable)`
**정렬 조건**: `sort=likes_desc`

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
| rows | 99,394 |
| filtered | 10.00% |
| Extra | **Using where; Using filesort** |
| **실제 실행 시간** | **45.0ms** |

```
-> Limit: 20 row(s)  (cost=10284 rows=20) (actual time=45..45 rows=20 loops=1)
    -> Sort: products.likes DESC, limit input to 20 row(s) per chunk  (cost=10284 rows=99394) (actual time=45..45 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (cost=10284 rows=99394) (actual time=0.0186..33.2 rows=98000 loops=1)
            -> Table scan on products  (cost=10284 rows=99394) (actual time=0.0178..28.8 rows=100000 loops=1)
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
| rows | 49,697 |
| filtered | 100.00% |
| Extra | **Using index condition; Using filesort** |
| **실제 실행 시간** | **61.0ms** |

```
-> Limit: 20 row(s)  (cost=6004 rows=20) (actual time=61..61 rows=20 loops=1)
    -> Sort: products.likes DESC, limit input to 20 row(s) per chunk  (cost=6004 rows=49697) (actual time=61..61 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL)  (actual time=0.0156..50.8 rows=98000 loops=1)
```

**분석**: 인덱스로 `deleted_at IS NULL` 조건은 해소했지만, 98,000건에 대한 **랜덤 I/O** + **filesort** 발생.
인덱스를 사용했음에도 시나리오 1보다 **더 느림**.

> **핵심**: `deleted_at IS NULL`이 전체의 98%를 선택하므로 선택도(Selectivity)가 매우 낮다.

---

### 시나리오 3: `likes` 단일 인덱스

```sql
CREATE INDEX idx_likes ON products(likes);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_likes |
| key_len | 4 |
| rows | 20 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.042ms** |

```
-> Limit: 20 row(s)  (cost=1.87 rows=2) (actual time=0.0144..0.0417 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (cost=1.87 rows=2) (actual time=0.0139..0.0405 rows=20 loops=1)
        -> Index scan on products using idx_test (reverse)  (cost=1.87 rows=20) (actual time=0.0132..0.0387 rows=20 loops=1)
```

**분석**: `likes DESC` 역방향 인덱스 스캔 + LIMIT 20 조기 종료.
98%가 활성 데이터이므로, 인덱스를 역방향으로 읽으면서 20건을 빠르게 발견.
시나리오 1 대비 약 **1,071배 빠름**.

> **주의**: `deleted_at` 칼럼은 인덱스에 없어 테이블 행 접근으로 필터링.
> 삭제 비율이 높아질수록 LIMIT 충족 전에 더 많은 행을 스캔해야 함.

---

### 시나리오 4: `(deleted_at, likes)` 복합 인덱스

```sql
CREATE INDEX idx_deleted_at_likes ON products(deleted_at, likes);
```

| 항목 | 값 |
|------|-----|
| type | ref |
| key | idx_deleted_at_likes |
| key_len | 9 |
| rows | 49,697 |
| filtered | 100.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.036ms** |

```
-> Limit: 20 row(s)  (cost=6004 rows=20) (actual time=0.00771..0.0358 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (cost=6004 rows=49697) (actual time=0.00717..0.0343 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL) (reverse)  (cost=6004 rows=49697) (actual time=0.00667..0.033 rows=20 loops=1)
```

**분석**:
1. `deleted_at = NULL` 조건으로 인덱스의 해당 구간에 바로 접근 (type: ref)
2. 그 구간 내에서 `likes DESC` 역방향 스캔으로 20건 즉시 반환
3. filesort 없음

> **핵심**: `deleted_at` 칼럼이 선두 키로 NULL 구간을 정확히 분리하고, `likes`가 정렬을 담당.
> 삭제 비율이 증가해도 **성능이 안정적**으로 유지됨.

---

### 시나리오 5: `(likes, deleted_at)` 복합 인덱스

```sql
CREATE INDEX idx_likes_deleted_at ON products(likes, deleted_at);
```

| 항목 | 값 |
|------|-----|
| type | index |
| key | idx_likes_deleted_at |
| key_len | 13 |
| rows | 20 |
| filtered | 10.00% |
| Extra | **Using where; Backward index scan** |
| **실제 실행 시간** | **0.032ms** |

```
-> Limit: 20 row(s)  (cost=1.87 rows=2) (actual time=0.00758..0.0319 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (cost=1.87 rows=2) (actual time=0.00717..0.0308 rows=20 loops=1)
        -> Index scan on products using idx_test (reverse)  (cost=1.87 rows=20) (actual time=0.00658..0.0292 rows=20 loops=1)
```

**분석**: 시나리오 3과 동일한 실행 계획이지만, `deleted_at`가 인덱스에 포함되어 테이블 행 접근 없이 인덱스 내에서 필터링 가능 (**Covering Index** 효과).
현재 데이터 분포에서 **가장 빠름**.

> **주의**: type이 `index` (full index scan)이라 MySQL 옵티마이저는 선택도를 낮게 평가(filtered: 10%).
> 삭제 비율이 높아지면 스캔 효율이 저하될 수 있음.

---

## 결과 요약

| 시나리오 | 인덱스 구성 | type | filesort | 실행 시간 | 기준 대비 |
|---------|-----------|------|----------|---------|---------|
| 1 | 없음 | ALL | O | 45.0ms | 1x (기준) |
| 2 | `(deleted_at)` | ref | O | 61.0ms | **1.36x 느림** |
| 3 | `(likes)` | index | X | 0.042ms | 1,071x 빠름 |
| 4 | `(deleted_at, likes)` | ref | X | 0.036ms | 1,250x 빠름 |
| **5** | **`(likes, deleted_at)`** | **index** | **X** | **0.032ms** | **1,406x 빠름** |

---

## 핵심 인사이트

### 1. `deleted_at` 단일 인덱스는 역효과

price, created_at 정렬 분석과 동일한 패턴. 선택도가 낮은 칼럼의 단독 인덱스는 의미가 없다.

### 2. 정렬 칼럼 인덱스 + LIMIT의 강력함

`ORDER BY likes DESC LIMIT 20`에서 `likes` 인덱스가 있으면 역방향 스캔 + 조기 종료로
filesort 없이 극적인 성능 개선이 가능하다.

### 3. 시나리오 4 vs 5: 안정성 vs 현재 성능

| | 시나리오 4 `(deleted_at, likes)` | 시나리오 5 `(likes, deleted_at)` |
|--|--|--|
| 현재 실행 시간 | 0.036ms | **0.032ms** |
| 옵티마이저 type | ref (정확한 선택도 인식) | index (full index scan) |
| 삭제 비율 증가 시 | **안정적** (deleted_at 구간만 처리) | 성능 저하 가능 |
| 쿼리 의도 반영 | O (WHERE → ORDER 순서와 일치) | 부분적 |

**장기적으로 안정적인 선택은 시나리오 4 `(deleted_at, likes)`이다.**
삭제 비율이 높아져도 `deleted_at = NULL` 구간만 탐색하므로 성능이 보장된다.

현재 시나리오 5가 더 빠른 이유는 **98%가 활성 데이터**라는 현재 분포 때문이며,
서비스가 성장해 삭제 데이터가 쌓이면 이 이점은 사라진다.

---

## 권장 인덱스

```sql
CREATE INDEX idx_products_deleted_at_likes ON products(deleted_at, likes);
```

- `WHERE deleted_at IS NULL` → 인덱스 선두 키로 해당 구간 직접 접근 (type: ref)
- `ORDER BY likes DESC` → 이미 정렬된 인덱스 구간 역방향 스캔, filesort 불필요
- 삭제 비율 변화에 관계없이 성능 안정적

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
