# `findAllByDeletedAtIsNull` 쿼리 인덱스 비교 분석

## 분석 대상 쿼리

`ProductRepositoryImpl.findAll()` → `ProductJpaRepository.findAllByDeletedAtIsNull(pageable)`

```sql
SELECT * FROM products
WHERE deleted_at IS NULL
ORDER BY {created_at DESC | price ASC | likes DESC}
LIMIT 20 OFFSET 0
```

| 정렬 조건 | 컨트롤러 파라미터 | 관련 칼럼 |
|----------|----------------|----------|
| `created_at DESC` | `sort=latest` (기본값) | deleted_at, created_at |
| `price ASC` | `sort=price_asc` | deleted_at, price |
| `likes DESC` | `sort=likes_desc` | deleted_at, likes |

## 테스트 환경

| 항목 | 값 |
|------|-----|
| MySQL 버전 | 8.0 |
| 전체 데이터 | 100,000건 |
| deleted_at IS NULL (활성) | 98,000건 (98%) |
| deleted_at IS NOT NULL (삭제) | 2,000건 (2%) |

---

## 시나리오 구성

2개 칼럼(deleted_at, 정렬칼럼)의 모든 조합 = **5 시나리오** × 3개 정렬

| # | 인덱스 구성 | 설명 |
|---|-----------|------|
| 1 | 없음 | 기준선 (PK만 존재) |
| 2 | `(deleted_at)` | WHERE 칼럼 단일 |
| 3 | `(정렬칼럼)` | ORDER BY 칼럼 단일 |
| 4 | `(deleted_at, 정렬칼럼)` | WHERE → ORDER 순서 |
| 5 | `(정렬칼럼, deleted_at)` | ORDER → WHERE 순서 |

---

## 정렬별 결과

### 1. ORDER BY created_at DESC

| # | 인덱스 구성 | type | filesort | Extra | 실행 시간 |
|---|-----------|------|----------|-------|---------|
| 1 | 없음 | ALL | O | Using where; Using filesort | 53.3ms |
| 2 | `(deleted_at)` | ref | O | Using index condition; Using filesort | 76.1ms |
| 3 | `(created_at)` | index | X | Using where; Backward index scan | 0.116ms |
| **4** | **`(deleted_at, created_at)`** | **ref** | **X** | **Using where; Backward index scan** | **0.104ms** |
| 5 | `(created_at, deleted_at)` | index | X | Using where; Backward index scan | 0.054ms |

<details>
<summary>주요 EXPLAIN ANALYZE</summary>

**시나리오 1: 없음 (53.3ms)**
```
-> Limit: 20 row(s)  (actual time=53.3..53.3 rows=20 loops=1)
    -> Sort: products.created_at DESC  (actual time=53.3..53.3 rows=20 loops=1)
        -> Filter: (products.deleted_at is null)  (actual time=0.139..42.7 rows=98000 loops=1)
            -> Table scan on products  (actual time=0.137..37.9 rows=100000 loops=1)
```

**시나리오 4: (deleted_at, created_at) — 권장 (0.104ms)**
```
-> Limit: 20 row(s)  (actual time=0.099..0.104 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0986..0.103 rows=20 loops=1)
        -> Index lookup on products using idx_deleted_at_created_at (deleted_at=NULL) (reverse)
           (actual time=0.0976..0.101 rows=20 loops=1)
```

**시나리오 5: (created_at, deleted_at) — 현재 최속 (0.054ms)**
```
-> Limit: 20 row(s)  (actual time=0.0259..0.0541 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.0255..0.0529 rows=20 loops=1)
        -> Index scan on products using idx_created_at_deleted_at (reverse)
           (actual time=0.0248..0.0513 rows=20 loops=1)
```

</details>

---

### 2. ORDER BY price ASC

| # | 인덱스 구성 | type | filesort | Extra | 실행 시간 |
|---|-----------|------|----------|-------|---------|
| 1 | 없음 | ALL | O | Using where; Using filesort | 57.4ms |
| 2 | `(deleted_at)` | ref | O | Using index condition; Using filesort | 66.3ms |
| 3 | `(price)` | index | X | Using where | 0.039ms |
| **4** | **`(deleted_at, price)`** | **ref** | **X** | **Using index condition** | **0.027ms** |
| 5 | `(price, deleted_at)` | index | X | Using where | 0.038ms |

<details>
<summary>주요 EXPLAIN ANALYZE</summary>

**시나리오 4: (deleted_at, price) — 권장 + 최속 (0.027ms)**
```
-> Limit: 20 row(s)  (actual time=0.00713..0.0269 rows=20 loops=1)
    -> Index lookup on products using idx_test (deleted_at=NULL)
       (actual time=0.00654..0.0255 rows=20 loops=1)
```

</details>

---

### 3. ORDER BY likes DESC

| # | 인덱스 구성 | type | filesort | Extra | 실행 시간 |
|---|-----------|------|----------|-------|---------|
| 1 | 없음 | ALL | O | Using where; Using filesort | 45.0ms |
| 2 | `(deleted_at)` | ref | O | Using index condition; Using filesort | 61.0ms |
| 3 | `(likes)` | index | X | Using where; Backward index scan | 0.042ms |
| **4** | **`(deleted_at, likes)`** | **ref** | **X** | **Using where; Backward index scan** | **0.036ms** |
| 5 | `(likes, deleted_at)` | index | X | Using where; Backward index scan | 0.032ms |

<details>
<summary>주요 EXPLAIN ANALYZE</summary>

**시나리오 4: (deleted_at, likes) — 권장 (0.036ms)**
```
-> Limit: 20 row(s)  (actual time=0.00771..0.0358 rows=20 loops=1)
    -> Filter: (products.deleted_at is null)  (actual time=0.00717..0.0343 rows=20 loops=1)
        -> Index lookup on products using idx_test (deleted_at=NULL) (reverse)
           (actual time=0.00667..0.033 rows=20 loops=1)
```

</details>

---

## 핵심 인사이트

### 1. `deleted_at` 단일 인덱스는 역효과

3개 정렬 모두에서 `deleted_at` 단일 인덱스(시나리오 2)는 **인덱스 없는 것보다 느렸다**.

| 정렬 | 인덱스 없음 | `(deleted_at)` 단일 |
|------|-----------|-------------------|
| created_at DESC | 53.3ms | **76.1ms** (1.43x 느림) |
| price ASC | 57.4ms | **66.3ms** (1.16x 느림) |
| likes DESC | 45.0ms | **61.0ms** (1.36x 느림) |

`deleted_at IS NULL`이 전체의 98%를 선택하므로 선택도가 매우 낮다.
인덱스 스캔 + 랜덤 I/O가 풀스캔보다 비용이 크다.

### 2. 정렬 칼럼 인덱스 + LIMIT 조기 종료

정렬 칼럼에 인덱스가 있으면 filesort 없이 인덱스 스캔 + LIMIT 20 조기 종료로
**1,000배 이상** 성능 개선이 가능하다. 98%가 활성 데이터이므로 ~20건만 스캔하면 충분.

### 3. `(deleted_at, 정렬칼럼)` vs `(정렬칼럼, deleted_at)`: 안정성 vs 현재 성능

| | `(deleted_at, 정렬칼럼)` | `(정렬칼럼, deleted_at)` |
|--|--|--|
| 옵티마이저 type | **ref** (정확한 구간 접근) | index (full index scan) |
| 삭제 비율 증가 시 | **안정적** (NULL 구간만 처리) | 성능 저하 가능 |
| 쿼리 의도 반영 | O (WHERE → ORDER 순서) | 부분적 |

`(deleted_at, 정렬칼럼)` 순서는 `deleted_at=NULL` 구간 내에서 정렬칼럼이 이미 정렬되어 있으므로,
삭제 비율에 관계없이 **항상 20건만 스캔**한다.

`(정렬칼럼, deleted_at)` 순서는 전체 인덱스를 스캔하면서 `deleted_at` 필터링하므로,
삭제 비율이 높아지면 LIMIT 충족까지 더 많은 행을 스캔해야 한다.

---

## 권장 인덱스

```sql
CREATE INDEX idx_products_deleted_at_created_at ON products(deleted_at, created_at);
CREATE INDEX idx_products_deleted_at_price ON products(deleted_at, price);
CREATE INDEX idx_products_deleted_at_likes ON products(deleted_at, likes);
```

| 인덱스 | 쿼리 | 개선 |
|--------|------|------|
| `(deleted_at, created_at)` | 기본 정렬 (latest) | 53.3ms → 0.104ms (513x) |
| `(deleted_at, price)` | 가격순 정렬 | 57.4ms → 0.027ms (2,126x) |
| `(deleted_at, likes)` | 좋아요순 정렬 | 45.0ms → 0.036ms (1,250x) |

---

## 관련 파일

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductJpaRepository.kt`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/Product.kt`
