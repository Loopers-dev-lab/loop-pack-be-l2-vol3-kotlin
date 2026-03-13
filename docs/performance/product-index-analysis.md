# 상품 인덱스 EXPLAIN 분석

## 대상 환경
- MySQL 8.0 (Docker, 로컬)
- product **5,000,000건** / 브랜드 20개 (균등 분포, 브랜드당 ~250,000건)
- like_count: 전체 0 (배치 집계 전)

## 인덱스 변경 사항

### 기존 인덱스 (AS-IS)
| 인덱스명 | 컬럼 |
|----------|------|
| idx_product_brand_id | (brand_id) |
| idx_product_status_price | (status, price ASC, id DESC) |
| idx_product_status_like_count | (status, like_count DESC, id DESC) |

### 추가 인덱스 (TO-BE)
| 인덱스명 | 컬럼 |
|----------|------|
| idx_product_brand_status_like | (brand_id, status, like_count DESC, id DESC) |
| idx_product_brand_status_price | (brand_id, status, price ASC, id DESC) |

### 제거 인덱스
- `idx_product_brand_id` → 복합 인덱스의 leftmost prefix로 대체

---

## 쿼리별 EXPLAIN 분석 (실측)

### Q1: 브랜드 필터 + 좋아요 순

```sql
SELECT * FROM product
WHERE status = 'ACTIVE' AND brand_id = 5
ORDER BY like_count DESC, id DESC
LIMIT 20;
```

#### EXPLAIN 구조 비교

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| type | range | **ref** |
| key | idx_product_status_like_count | **idx_product_brand_status_like** |
| ref | NULL | **const, const** |
| rows | 2,400,000 | **499,000** |
| filtered | 9% | **100%** |
| Extra | Using index condition; Using where | **NULL** |

#### EXPLAIN ANALYZE 실행 시간

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| actual time | **1.21ms** | **1.03ms** |
| 스캔 방식 | status+like_count 인덱스 range scan → brand_id 필터 (396행 스캔) | brand_id+status 직접 lookup (20행만) |

> **분석**: AS-IS는 status+like_count 인덱스를 range scan하면서 brand_id를 후필터링. like_count가 모두 0인 현재 데이터에서는 early termination이 빠르게 발생하여 시간 차이가 적음. **like_count 분포가 다양해지면 AS-IS는 수천~수만 행을 스캔해야 하므로 성능 격차가 크게 벌어질 것으로 예상.**

---

### Q2: 브랜드 필터 + 가격 순

```sql
SELECT * FROM product
WHERE status = 'ACTIVE' AND brand_id = 5
ORDER BY price ASC, id DESC
LIMIT 20;
```

#### EXPLAIN 구조 비교

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| type | range | **ref** |
| key | idx_product_status_price | **idx_product_brand_status_price** |
| ref | NULL | **const, const** |
| rows | 2,400,000 | **499,000** |
| filtered | 9% | **100%** |
| Extra | Using index condition; Using where | **NULL** |

#### EXPLAIN ANALYZE 실행 시간

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| actual time | **13.4ms** | **0.76ms** |
| 스캔 방식 | status+price 인덱스 range scan → brand_id 필터 (380행 스캔) | brand_id+status 직접 lookup (20행만) |

> **핵심 개선: 17.6x 성능 향상.** price가 균등 분포이므로 가장 저렴한 상품 중 brand_id=5인 것을 찾으려면 AS-IS에서는 380행을 스캔해야 했음. TO-BE는 brand_id+status로 바로 진입하여 price 순서대로 20행만 읽음.

---

### Q3: 전체 좋아요 순 (브랜드 필터 없음)

```sql
SELECT * FROM product
WHERE status = 'ACTIVE'
ORDER BY like_count DESC, id DESC
LIMIT 20;
```

#### EXPLAIN 구조

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| type | ref | ref (동일) |
| key | idx_product_status_like_count | idx_product_status_like_count (동일) |
| rows | 2,400,000 | 2,400,000 (동일) |
| filtered | 100% | 100% (동일) |
| Extra | NULL | NULL (동일) |

#### EXPLAIN ANALYZE 실행 시간

| 항목 | AS-IS | TO-BE |
|------|-------|-------|
| actual time | **0.03ms** | **0.05ms** |

> **변경 없음** — 기존 `idx_product_status_like_count`로 충분. 인덱스 순서대로 20행만 읽으므로 최적.

---

## 요약

| 쿼리 | AS-IS | TO-BE | 개선 |
|------|-------|-------|------|
| Q1: 브랜드 + 좋아요 순 | 1.21ms (range + filter) | 1.03ms (ref lookup) | 구조 개선 (like_count 분포 다양화 시 효과 극대화) |
| Q2: 브랜드 + 가격 순 | 13.4ms (range + filter) | **0.76ms** (ref lookup) | **17.6x 향상** |
| Q3: 전체 + 좋아요 순 | 0.03ms | 0.05ms | 변경 없음 |

## 구조적 개선 포인트

1. **type: range → ref**: 인덱스 전체를 스캔하며 필터링하는 방식에서, 복합 키로 직접 lookup하는 방식으로 전환
2. **filtered: 9% → 100%**: 후필터링 제거. 인덱스에서 읽은 모든 행이 조건에 부합
3. **Extra: "Using index condition; Using where" → NULL**: 추가적인 WHERE 필터링 불필요
4. **idx_product_brand_id 제거**: 복합 인덱스 leftmost prefix로 대체, 불필요한 인덱스 유지보수 비용 제거

## 트레이드오프

- 인덱스 2개 추가 → 쓰기(INSERT/UPDATE) 시 인덱스 유지보수 비용 약간 증가
- 커머스 특성상 **읽기 >> 쓰기** 이므로 합리적 트레이드오프
- 5M 데이터 기준 인덱스 생성 시간: 약 30~60초 (1회성)
