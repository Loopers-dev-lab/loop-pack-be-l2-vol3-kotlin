# 복합 인덱스 성능 분석

## 환경

- MySQL 8.0
- products 테이블: 100,000건 / brands 테이블: 100건 (멱급수 분포로 상품 할당)
- orders 테이블: 100,000건 / order_items 테이블: 170,008건 (user_id: 1~10,000 멱급수 분포)

---

## 1. products 테이블

### 대상 쿼리

```sql
SELECT * FROM products WHERE brand_id = 1 ORDER BY likes;
```

### 비교 대상 인덱스

다음 5가지 인덱스 전략의 성능을 비교한다.

| # | 인덱스 전략 | 생성 구문 |
|---|------------|----------|
| 1 | 인덱스 없음 | - |
| 2 | 단일 인덱스 (brand_id) | `CREATE INDEX idx_products_brand_id ON products (brand_id)` |
| 3 | 단일 인덱스 (likes) | `CREATE INDEX idx_products_likes ON products (likes)` |
| 4 | 복합 인덱스 정순 (brand_id, likes) | `CREATE INDEX idx_products_brand_id_likes ON products (brand_id, likes)` |
| 5 | 복합 인덱스 역순 (likes, brand_id) | `CREATE INDEX idx_products_likes_brand_id ON products (likes, brand_id)` |

### EXPLAIN 결과 비교

#### 인덱스 없음

```
| type | possible_keys | key  | rows  | filtered | Extra                       |
|------|---------------|------|-------|----------|-----------------------------|
| ALL  | NULL          | NULL | 98671 |    10.00 | Using where; Using filesort |
```

#### 단일 인덱스: `(brand_id)`

```
| type | possible_keys         | key                   | rows  | filtered | Extra          |
|------|-----------------------|-----------------------|-------|----------|----------------|
| ref  | idx_products_brand_id | idx_products_brand_id | 18336 |   100.00 | Using filesort |
```

#### 단일 인덱스: `(likes)`

```
| type | possible_keys | key  | rows  | filtered | Extra                       |
|------|---------------|------|-------|----------|-----------------------------|
| ALL  | NULL          | NULL | 98671 |     1.00 | Using where; Using filesort |
```

#### 복합 인덱스 정순: `(brand_id, likes)`

```
| type | possible_keys               | key                         | rows  | filtered | Extra |
|------|-----------------------------|-----------------------------|-------|----------|-------|
| ref  | idx_products_brand_id_likes | idx_products_brand_id_likes | 18816 |   100.00 | NULL  |
```

#### 복합 인덱스 역순: `(likes, brand_id)`

```
| type | possible_keys | key  | rows  | filtered | Extra                       |
|------|---------------|------|-------|----------|-----------------------------|
| ALL  | NULL          | NULL | 98671 |     1.00 | Using where; Using filesort |
```

> FORCE INDEX로 강제 사용 시에도 `type: index`(풀 인덱스 스캔, 98,671건)로 동작

### 분석

| 항목 | 인덱스 없음 | 단일 (brand_id) | 단일 (likes) | 복합 정순 (brand_id, likes) | 복합 역순 (likes, brand_id) |
|------|-------------|-----------------|--------------|-----------------------------|-----------------------------|
| type | ALL | **ref** | ALL | **ref** | ALL |
| key | NULL | idx_products_brand_id | NULL (사용 거부) | idx_products_brand_id_likes | NULL (사용 거부) |
| rows | 98,671 | **18,336** | 98,671 | **18,816** | 98,671 |
| filtered | 10% | **100%** | 1% | **100%** | 1% |
| Extra | Using where; Using filesort | **Using filesort** | Using where; Using filesort | **NULL** | Using where; Using filesort |

#### 단일 인덱스 `(brand_id)` — WHERE만 해결

- `WHERE brand_id = 1` 조건으로 B-Tree 탐색이 가능하여 스캔 행 수를 18,336건으로 줄임
- 그러나 likes 순서 정보가 없어 **filesort가 여전히 발생**
- 대상 행이 적으면 filesort 비용이 낮지만, 행이 많아질수록 정렬 비용이 급증한다

```
(brand_id) 인덱스의 B-Tree 구조:

brand_id=1 → row_ptr  ← WHERE로 이 범위를 특정
brand_id=1 → row_ptr     하지만 likes 순서는 모름
brand_id=1 → row_ptr     → 18,336건을 읽은 뒤 filesort 필요
...
brand_id=2 → row_ptr
```

#### 단일 인덱스 `(likes)` — ORDER BY만 해결 가능하나 사용 거부

- likes 순서로 정렬되어 있지만, `WHERE brand_id = 1` 조건을 인덱스로 해결할 수 없음
- 옵티마이저가 인덱스 사용을 거부하여 **인덱스 없는 것과 동일한 풀 테이블 스캔**
- 역순 복합 인덱스 `(likes, brand_id)`와 실질적으로 동일한 성능 — 선두 컬럼이 WHERE 조건과 무관하면 복합이든 단일이든 차이가 없다

```
(likes) 인덱스의 B-Tree 구조:

likes=0 → row_ptr (brand_id=3)    ← brand_id=1을 찾으려면
likes=0 → row_ptr (brand_id=15)      모든 likes 값을 순회해야 함
likes=0 → row_ptr (brand_id=42)      → 풀 스캔과 다를 바 없음
likes=1 → row_ptr (brand_id=1)
likes=1 → row_ptr (brand_id=7)
...
```

#### 복합 인덱스 정순 `(brand_id, likes)` — WHERE + ORDER BY 모두 해결

- brand_id로 탐색 범위를 좁힌 뒤, 같은 brand_id 내에서 likes가 이미 정렬되어 있음
- **스캔 행 수 감소 + filesort 제거** — 두 가지 이점을 동시에 달성
- 단일 인덱스 두 개를 합친 것이 아닌, 하나의 복합 인덱스가 두 역할을 동시에 수행한다

```
(brand_id, likes) 인덱스의 B-Tree 구조:

brand_id=1, likes=0    ← WHERE로 이 범위를 특정
brand_id=1, likes=3       likes가 이미 정렬되어 있으므로
brand_id=1, likes=15      filesort 불필요
...
brand_id=2, likes=0
```

#### 복합 인덱스 역순 `(likes, brand_id)` — 사용 거부

- 옵티마이저가 사용을 거부. 인덱스 없는 것과 동일한 성능

```
(likes, brand_id) 인덱스의 B-Tree 구조:

likes=0, brand_id=3
likes=0, brand_id=15
likes=0, brand_id=42
likes=1, brand_id=1    ← brand_id=1이 likes 값마다 흩어져 있음
likes=1, brand_id=7
likes=1, brand_id=22
likes=2, brand_id=1    ← 여기도
...
```

- `likes`가 선두이므로 likes 순서로 정렬되어 있지만, `brand_id=1`은 모든 likes 값에 걸쳐 흩어져 있다
- `WHERE brand_id = 1`을 만족하는 행을 찾으려면 인덱스 전체를 스캔해야 함
- 옵티마이저는 인덱스 풀 스캔보다 테이블 풀 스캔이 낫다고 판단하여 인덱스 사용을 거부함

> 옵티마이저도 이 인덱스가 비효율적임을 인지하여, 정순/역순 인덱스가 동시에 존재할 때 `(brand_id, likes)` 인덱스를 자동 선택한다.

### 결론

> 단일 인덱스는 WHERE 또는 ORDER BY **한쪽만** 최적화할 수 있다. 두 조건을 동시에 최적화하려면 복합 인덱스가 필요하다.

---

## 2. orders 테이블

### 대상 쿼리

```sql
SELECT * FROM orders WHERE user_id = 1964 AND created_at BETWEEN '2024-03-11' AND '2026-03-11';
```

### 비교 대상 인덱스

다음 5가지 인덱스 전략의 성능을 비교한다.

| # | 인덱스 전략 | 생성 구문 |
|---|------------|----------|
| 1 | 인덱스 없음 | - |
| 2 | 단일 인덱스 (user_id) | `CREATE INDEX idx_orders_user_id ON orders (user_id)` |
| 3 | 단일 인덱스 (created_at) | `CREATE INDEX idx_orders_created_at ON orders (created_at)` |
| 4 | 복합 인덱스 정순 (user_id, created_at) | `CREATE INDEX idx_orders_user_created ON orders (user_id, created_at)` |
| 5 | 복합 인덱스 역순 (created_at, user_id) | `CREATE INDEX idx_orders_created_user ON orders (created_at, user_id)` |

### EXPLAIN 결과 비교

#### 인덱스 없음

```
| type | possible_keys | key  | rows  | filtered | Extra       |
|------|---------------|------|-------|----------|-------------|
| ALL  | NULL          | NULL | 96772 |     0.00 | Using where |
```

#### 단일 인덱스: `(user_id)`

```
| type | possible_keys      | key                | rows | filtered | Extra       |
|------|--------------------|--------------------| -----|----------|-------------|
| ref  | idx_orders_user_id | idx_orders_user_id |    6 |    11.11 | Using where |
```

#### 단일 인덱스: `(created_at)`

```
| type | possible_keys         | key  | rows  | filtered | Extra       |
|------|-----------------------|------|-------|----------|-------------|
| ALL  | idx_orders_created_at | NULL | 96772 |     0.00 | Using where |
```

#### 복합 인덱스 정순: `(user_id, created_at)`

```
| type  | possible_keys           | key                     | rows | filtered | Extra                 |
|-------|-------------------------|-------------------------|------|----------|-----------------------|
| range | idx_orders_user_created | idx_orders_user_created |    6 |   100.00 | Using index condition |
```

#### 복합 인덱스 역순: `(created_at, user_id)`

```
| type | possible_keys           | key  | rows  | filtered | Extra       |
|------|-------------------------|------|-------|----------|-------------|
| ALL  | idx_orders_created_user | NULL | 96772 |     0.00 | Using where |
```

> FORCE INDEX로 강제 사용 시 `type: range`, `filtered: 5%`로 동작하지만 실제로는 created_at 범위 전체를 스캔 후 user_id를 필터링

### 분석

| 항목 | 인덱스 없음 | 단일 (user_id) | 단일 (created_at) | 복합 정순 (user_id, created_at) | 복합 역순 (created_at, user_id) |
|------|-------------|----------------|--------------------|---------------------------------|---------------------------------|
| type | ALL | **ref** | ALL | **range** | ALL |
| key | NULL | idx_orders_user_id | NULL (사용 거부) | idx_orders_user_created | NULL (사용 거부) |
| rows | 96,772 | **6** | 96,772 | **6** | 96,772 |
| filtered | 0% | **11.11%** | 0% | **100%** | 0% |
| Extra | Using where | **Using where** | Using where | **Using index condition** | Using where |

#### 단일 인덱스 `(user_id)` — 동등 조건만 해결

- `WHERE user_id = 1964` 조건으로 B-Tree 탐색이 가능하여 스캔 행 수를 6건으로 줄임
- 그러나 created_at 조건은 인덱스가 아닌 **테이블 데이터에서 필터링** (filtered: 11.11%)
- 6건을 찾은 뒤 각 행의 테이블 데이터를 읽어 created_at 조건을 추가 검증해야 함
- 이 쿼리에서는 대상이 6건뿐이라 성능 차이가 작지만, 한 유저의 주문이 수만 건이면 불필요한 행 접근이 급증한다

```
(user_id) 인덱스의 B-Tree 구조:

user_id=1964 → row_ptr  ← WHERE로 이 범위를 특정
user_id=1964 → row_ptr     하지만 created_at 범위는 모름
user_id=1964 → row_ptr     → 6건을 읽은 뒤 테이블에서 created_at 필터링
...
user_id=1965 → row_ptr
```

#### 단일 인덱스 `(created_at)` — 범위가 넓어 사용 거부

- `BETWEEN '2024-03-11' AND '2026-03-11'`은 2년 범위로 대부분의 데이터를 포함
- 옵티마이저가 인덱스를 사용해도 이점이 없다고 판단하여 **사용을 거부. 풀 테이블 스캔**

```
(created_at) 인덱스의 B-Tree 구조:

2024-03-11 → row_ptr (user_id=42)      ← BETWEEN 범위가 거의 전체 데이터를 포함
2024-03-11 → row_ptr (user_id=1964)       인덱스를 써도 대부분의 행을 읽어야 함
2024-03-12 → row_ptr (user_id=156)        → 풀 테이블 스캔이 더 효율적
...
2026-03-10 → row_ptr (user_id=1964)
```

#### 복합 인덱스 정순 `(user_id, created_at)` — 동등 + 범위 조건 모두 해결

- user_id로 B-Tree 노드를 특정한 뒤, created_at 범위를 인덱스 레벨에서 직접 스캔
- **filtered 100%** — 인덱스에서 읽은 행이 모두 최종 결과. 불필요한 테이블 접근 없음
- ICP(Index Condition Pushdown) 적용으로 스토리지 엔진 레벨에서 조건 평가

```
(user_id, created_at) 인덱스의 B-Tree 구조:

user_id=1964, 2023-05-20   ← user_id로 범위 특정 후
user_id=1964, 2024-08-11      created_at BETWEEN으로
user_id=1964, 2025-01-03      인덱스 범위 스캔
user_id=1964, 2025-06-22      → 두 조건 모두 인덱스에서 해결
...
user_id=1965, 2024-01-15
```

#### 복합 인덱스 역순 `(created_at, user_id)` — 사용 거부

- 옵티마이저가 사용을 거부. 인덱스 없는 것과 동일한 성능

```
(created_at, user_id) 인덱스의 B-Tree 구조:

2024-03-11, user_id=42
2024-03-11, user_id=1964   ← 날짜 범위 내 user_id가 흩어져 있음
2024-03-11, user_id=7823
2024-03-12, user_id=156
2024-03-12, user_id=1964   ← 여기도
...
2026-03-10, user_id=1964   ← 여기도
```

- `created_at`이 선두이므로 날짜 범위(2년)에 해당하는 인덱스 엔트리를 넓게 스캔해야 함
- 그 안에서 `user_id=1964`는 여기저기 흩어져 있어, 후행 컬럼으로 B-Tree 탐색 범위를 좁힐 수 없음
- 옵티마이저는 범위가 너무 넓어 인덱스 사용 비용이 테이블 풀 스캔보다 크다고 판단하여 사용을 거부함

### 결론

> 단일 인덱스는 두 개의 WHERE 조건 중 **하나만** 인덱스로 평가할 수 있다. 나머지 조건은 서버에서 후처리하므로 filtered가 100%에 미치지 못한다. 복합 인덱스는 두 조건을 모두 인덱스에서 평가하여 불필요한 행 접근을 완전히 제거한다.

---

## 3. 종합 비교

> 성능이 나쁜 순서(풀 스캔) → 좋은 순서(최적)로 정렬

### products: `WHERE brand_id = 1 ORDER BY likes`

| 인덱스 전략 | type | rows | filesort | 평가 |
|---|---|---|---|---|
| 인덱스 없음 | ALL | 98,671 | 발생 | 최악 |
| 단일 (likes) | ALL | 98,671 | 발생 | WHERE 조건 불일치로 미사용 |
| 복합 역순 (likes, brand_id) | ALL | 98,671 | 발생 | 옵티마이저 사용 거부 |
| 단일 (brand_id) | ref | 18,336 | **발생** | WHERE만 해결, 정렬은 미해결 |
| **복합 정순 (brand_id, likes)** | **ref** | **18,816** | **없음** | **WHERE + ORDER BY 모두 해결** |

### orders: `WHERE user_id = 1964 AND created_at BETWEEN ...`

| 인덱스 전략 | type | rows | filtered | 평가 |
|---|---|---|---|---|
| 인덱스 없음 | ALL | 96,772 | 0% | 최악 |
| 단일 (created_at) | ALL | 96,772 | 0% | 범위가 넓어 미사용 |
| 복합 역순 (created_at, user_id) | ALL | 96,772 | 0% | 옵티마이저 사용 거부 |
| 단일 (user_id) | ref | 6 | **11.11%** | 동등 조건만 해결, 범위는 테이블에서 필터링 |
| **복합 정순 (user_id, created_at)** | **range** | **6** | **100%** | **동등 + 범위 조건 모두 인덱스에서 해결** |

### 핵심 차이: 단일 인덱스 vs 복합 인덱스

단일 인덱스(동등 조건 컬럼)는 탐색 범위를 좁히는 데까지는 성공하지만, 나머지 조건은 테이블 데이터에서 처리해야 한다.

| | 단일 인덱스 (동등 조건 컬럼) | 복합 인덱스 (동등 + 범위/정렬) |
|---|---|---|
| WHERE 동등 조건 | 인덱스로 해결 | 인덱스로 해결 |
| WHERE 범위 조건 | 테이블에서 필터링 | **인덱스에서 해결** |
| ORDER BY 정렬 | filesort 발생 | **인덱스 순서로 해결** |
| filtered | 낮음 (추가 필터링 필요) | **100%** (불필요한 행 없음) |

---

## 4. 복합 인덱스 설계 원칙

복합 인덱스의 컬럼 순서는 쿼리 패턴에 따라 결정한다.

```
(동등 조건 컬럼, 범위 조건 컬럼 또는 정렬 컬럼)
 ↑ WHERE =          ↑ BETWEEN / ORDER BY
```

- **선두 컬럼**: WHERE 절의 동등 조건(=)에 사용되는 컬럼 → B-Tree에서 탐색 범위를 좁힘
- **후행 컬럼**: 범위 조건(BETWEEN, >, <) 또는 ORDER BY에 사용되는 컬럼 → 이미 정렬된 상태로 저장
- **범위 조건 이후 컬럼**: 범위 조건 뒤에 배치된 컬럼은 인덱스 탐색에 활용되지 못함 (필터링만 가능)
- **순서를 뒤집으면**: 옵티마이저가 인덱스 사용을 거부하고 풀 테이블 스캔으로 퇴화할 수 있음
- **단일 인덱스의 한계**: 동등 조건 컬럼만 단일 인덱스로 만들면 탐색은 가능하나, 정렬이나 범위 조건은 테이블에서 추가 처리 필요
