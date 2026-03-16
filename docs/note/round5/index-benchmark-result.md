# Soft Delete 인덱스 전략 벤치마크

## 배경

Soft Delete(`deleted_at IS NULL`) 조건과 정렬(`ORDER BY likes_count DESC LIMIT 20`)이 결합된 쿼리에서,
`deleted_at`을 인덱스에 포함해야 하는지에 대한 실험.

기존 분석글에서는 EXPLAIN의 `rows` 추정치만 보고 "B/D가 압도적"이라 결론 내렸으나,
**EXPLAIN `rows`는 `type`에 따라 계산 방식이 다르기 때문에 직접 비교가 불가능**하다는 의문에서 출발.

## 실험 환경

- MySQL 8.0 (Docker Container)
- 테이블: `test_product` (id, name, likes_count, deleted_at, created_at)
- 데이터: 100,000건
- `likes_count`: 0~9,999 랜덤
- 쿼리: `SELECT * FROM test_product WHERE deleted_at IS NULL ORDER BY likes_count DESC LIMIT 20`

## 비교 전략

| 전략 | 인덱스 구성 | 설명 |
|------|------------|------|
| A | 없음 (PRIMARY만) | 풀 테이블 스캔 |
| B | `(likes_count DESC)` | 정렬 컬럼만 |
| C | `(deleted_at, likes_count DESC)` | 필터 선두 + 정렬 |
| D | `(likes_count DESC, deleted_at)` | 정렬 선두 + 필터 |

## EXPLAIN vs EXPLAIN ANALYZE 비교 (5% 삭제율)

EXPLAIN(추정치)과 EXPLAIN ANALYZE(실측치)의 차이를 먼저 확인한다.

### EXPLAIN (추정치)

| 전략 | type | rows (추정) | Extra |
|------|------|------------|-------|
| A | ALL | 99,509 | Using where; Using filesort |
| B | index | 20 | Using where |
| C | ref | 49,754 | Using index condition |
| D | index | 20 | Using where |

여기서 C가 `rows: 49,754`로 나와 B/D 대비 2,500배 비효율적으로 보인다.

### EXPLAIN ANALYZE (실측치)

| 전략 | actual time | actual rows scanned | actual rows returned |
|------|------------|--------------------|--------------------|
| A | 22ms | 100,000 | 20 |
| B | 0.10ms | 20 | 20 |
| C | 0.42ms | **20** | 20 |
| D | 0.05ms | 20 | 20 |

**C도 실제로는 20건만 읽고 조기 종료한다.** `rows: 49,754`는 LIMIT 반영 전 파티션 크기 추정치일 뿐이다.

### 왜 이런 차이가 생기는가?

- `type: ref` (C전략) → 옵티마이저가 **LIMIT을 반영하지 않은** 전체 파티션 크기를 `rows`에 표시
- `type: index` (B/D전략) → 옵티마이저가 **LIMIT을 감안한** 예측치를 `rows`에 표시
- 같은 `rows` 컬럼인데 **계산 방식이 달라서 직접 비교 불가능**

## 벤치마크 결과 (100회 반복 평균)

| 전략 | 5% 삭제 (μs) | 70% 삭제 (μs) | 95% 삭제 (μs) |
|------|-------------|--------------|--------------|
| B `(likes_count)` | 46 | 65 | **340** |
| C `(deleted_at, likes_count)` | 43 | 73 | **79** |
| D `(likes_count, deleted_at)` | 47 | 70 | **396** |

### 삭제율별 동작 방식

**B/D 전략 (정렬 컬럼 선두):**
- 인덱스를 likes_count 순서로 스캔하면서 `deleted_at IS NULL`인 행 20개를 수집
- 삭제율이 높을수록 더 많은 행을 스캔해야 함: `20 / active_ratio`
- 95% 삭제 시: 약 400행 스캔 필요 (EXPLAIN ANALYZE에서 353~363행 확인)

**C 전략 (필터 컬럼 선두):**
- B-Tree에서 `deleted_at IS NULL` 리프로 점프 (O(log N))
- 해당 파티션 내에서 likes_count DESC로 이미 정렬되어 있음
- 정확히 20건만 읽고 종료 — **삭제율과 무관하게 항상 동일**

### 95% 삭제율에서의 역전

```
B: ████████████████████████████████████ 340μs (4.3x slower than C)
C: ████████ 79μs (baseline)
D: ████████████████████████████████████████ 396μs (5.0x slower than C)
```

## 결론

1. **EXPLAIN `rows`만으로 인덱스 전략을 비교하면 안 된다** — `type`에 따라 계산 방식이 다르다. 반드시 `EXPLAIN ANALYZE`로 실측 확인 필요.

2. **일반적인 삭제율(5~70%)에서는 세 전략 모두 성능 차이 무의미** — 43~73μs 범위.

3. **삭제율이 높아지면 C전략이 안정적** — B/D는 선형 열화, C는 항상 일정.

4. **`(deleted_at, likes_count DESC)` 복합 인덱스가 가장 안정적인 선택** — 일반 상황에서 손해 없이, 극단 상황에서 4~5배 이점.
