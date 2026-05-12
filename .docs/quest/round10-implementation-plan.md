# Round10 구현 계획

## 목표

Round10에서는 기존 Redis 기반 rolling 랭킹은 유지하면서,
Spring Batch를 이용해 **마감형(fixed) 주간/월간 랭킹**을 생성한다.

핵심 방향은 다음과 같다.

- Redis 랭킹은 운영형 rolling 랭킹으로 유지한다.
- Batch는 마감 통계형 랭킹을 생성한다.
- 배치 입력 원천은 `product_metrics` 일별 집계 테이블로 재설계한다.
- 배치 결과는 조회 전용 MV 성격의 테이블에 기간별 스냅샷으로 저장한다.

## 랭킹 의미 정리

### Rolling 랭킹
기존 Redis 기반 조회용 랭킹은 그대로 유지한다.

- `realtime`: 최근 60분 rolling
- `daily`: 최근 24시간 rolling
- `weekly`: 최근 7일 rolling
- `monthly`: 최근 30일 rolling

### Fixed 랭킹
Batch로 생성하는 마감 통계형 랭킹은 별도 type으로 분리한다.

- `day-fixed`: 특정 날짜 확정 일간
- `week-fixed`: 월~일 기준 주간 마감본
- `month-fixed`: 달력 월 기준 월간 마감본

즉, `weekly/monthly`는 rolling이고
`week-fixed/month-fixed`는 batch MV 기반 fixed ranking이다.

## 핵심 설계

### 1. 일별 `product_metrics`
`product_metrics`는 `(product_id, metrics_date)`를 키로 가지는 일별 집계 테이블로 재설계한다.

컬럼:
- `product_id`
- `metrics_date`
- `view_count`
- `like_count`
- `sales_count`
- `ranking_score`
- `last_event_at`
- BaseEntity 공통 컬럼

### 2. Atomic metrics upsert
이벤트를 소비할 때 해당 일자의 row를 MySQL native upsert로 갱신한다.

- 키: `(product_id, metrics_date)`
- 구현: `INSERT ... ON DUPLICATE KEY UPDATE`
- count와 `ranking_score`를 함께 누적
- `last_event_at`는 가장 최신 이벤트 시각 유지
- `like_count`는 음수로 내려가지 않도록 보정

### 3. 점수 정책 통일
일별 `ranking_score`는 Redis 랭킹 적재와 동일한 정책을 사용한다.

- 조회: `+0.1`
- 좋아요: `+0.2`
- 좋아요 취소: `-0.2`
- 주문: `+0.6 * log1p(unitPrice * quantity)`

### 4. Fixed MV 테이블
새로운 영속 스냅샷 테이블:
- `tb_mv_product_rank_weekly`
- `tb_mv_product_rank_monthly`

컬럼:
- `period_start_date`
- `period_end_date`
- `rank_position`
- `product_id`
- `score`
- BaseEntity 공통 컬럼

저장 정책:
- 각 기간별 TOP 100 저장
- 최신 결과만 덮어쓰지 않고 기간별 스냅샷 누적 보관

## Batch 구조

### Job
- `fixedRankingMaterializeJob`

### Job 파라미터
- `scope=week-fixed|month-fixed|all`
- `targetDate=yyyyMMdd` (선택)

### 처리 흐름
1. 대상 기간 계산
2. 해당 기간의 기존 MV 데이터 삭제
3. 상품별 집계 점수 읽기
4. rank 부여 후 TOP 100 스냅샷 저장

### Step 설계
- `prepareWeekFixedStep`: tasklet
- `materializeWeekFixedStep`: chunk step
- `prepareMonthFixedStep`: tasklet
- `materializeMonthFixedStep`: chunk step

### Chunk 채택 이유
Batch의 핵심 집계 step은 Chunk-Oriented Processing으로 구현한다.

- DB가 먼저 `SUM(ranking_score)`를 `product_id` 기준으로 집계
- Batch는 그 집계 결과를 chunk 단위로 읽음
- writer가 rank를 부여해 MV에 저장

## API 확장

`GET /api/v1/rankings`에 다음을 추가한다.

- `type=week-fixed`
- `type=month-fixed`
- `weekStartDate=yyyyMMdd`
- `yearMonth=yyyyMM`

조회 정책:
- 파라미터가 없으면 최신 fixed snapshot 조회
- 파라미터가 있으면 해당 주/월의 fixed snapshot 조회

기존 rolling ranking type은 유지한다.

## 구현 순서
1. 설계/트레이드오프 문서 작성
2. daily metrics 모델/리포지토리를 shared JPA 모듈로 이동
3. native upsert 기반 일별 metrics 반영 추가
4. streamer 소비 로직을 delta 기반 일별 적재로 변경
5. fixed ranking MV 엔티티와 리포지토리 추가
6. batch job, tasklet, reader, writer 구현
7. ranking API와 query service에 fixed 랭킹 조회 추가
8. metrics, batch, API 테스트 보강
