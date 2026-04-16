# Spring Batch 기반 주간/월간 랭킹 시스템

## 개요

Round 9의 Redis ZSET 실시간 일간 랭킹 위에, Spring Batch로 주간/월간 랭킹을 집계하여
Materialized View(MV)에 적재하고, API에서 period 파라미터로 일간/주간/월간을 분기 조회한다.

### 설계 결정 (학습 세션 확정)

| 항목 | 결정 | 근거 |
|------|------|------|
| 기간 경계 | ISO Week 고정 경계 (B-style) | archive 가능, 과거 주차 조회 단일 쿼리 |
| 배치 주기 | 하루 1회 (새벽 3시) | 기간 정의 ≠ 실행 주기, 매일 누적 갱신 |
| Reader | JdbcPagingItemReader (GROUP BY) | 영속성 컨텍스트 불필요, 집계 투영은 Entity 아님 |
| Processor | Java 가중치 계산 | DB에 비즈니스 로직 침투 방지, 가중치 외부화 |
| Writer | JdbcBatchItemWriter + ON DUPLICATE KEY UPDATE | 벌크 INSERT, rewriteBatchedStatements=true |
| 멱등성 | RunIdIncrementer + staging swap + 결정론적 계산 | 재실행 허용, 탈락 row 정리 |
| MV PK | 복합 PK (year_week, product_id) | 조회 전용, clustered index 최적화 |
| daily PK | id AUTO_INCREMENT + unique (metric_date, product_id) | 운영 테이블, 마이그레이션 용이 |
| API 분기 | Strategy 패턴 + Repository 분리 | LSP 위반 제거, 테스트 격리 |
| API 파라미터 | date + period (서버 변환) | 클라이언트 실수 방지 |
| 응답 | resolvedWeek + periodStart/periodEnd | ISO Week 몰라도 구간으로 이해 |

### 데이터 흐름

```
[일간] Kafka → commerce-streamer → product_metrics_daily (일별 row upsert)
[주간/월간 배치] commerce-batch (새벽 3시)
  → Reader: product_metrics_daily GROUP BY product_id (해당 주/월 범위)
  → Processor: Java 가중치 계산 (view×0.1 + like×0.2 + sales×0.7)
  → Writer: staging 테이블 upsert
  → Final Step: staging → MV swap (DELETE + INSERT SELECT, 단일 트랜잭션)
[조회] commerce-api
  → GET /api/v1/rankings?date=20260415&period=WEEKLY&page=1&size=20
  → Controller → Facade → strategies[period] 위임
    → DAILY: Redis ZSET (기존)
    → WEEKLY: mv_product_rank_weekly
    → MONTHLY: mv_product_rank_monthly
```

---

## 구현 계획

### 1. 스키마 — product_metrics_daily

- [x] product_metrics_daily 테이블 생성 (id AUTO_INCREMENT, metric_date, product_id, view_count, like_count, sales_count, unique (metric_date, product_id))
- [x] commerce-streamer의 ProductMetrics Entity에 metricDate 필드 추가 및 product_metrics_daily 매핑
- [x] ProductMetricsRepository 인터페이스에 일별 upsert 메서드 추가
- [x] ProductMetricsRepositoryImpl에서 일별 upsert 구현 (ON DUPLICATE KEY UPDATE)
- [x] CatalogEventProcessor에서 product_metrics_daily에 일별 적재하도록 수정
- [x] OrderEventProcessor에서 product_metrics_daily에 일별 적재하도록 수정

### 2. 스키마 — MV 테이블 + Staging 테이블

- [x] mv_product_rank_weekly 테이블 생성 (PK: (year_week, product_id), rank, score, view_count, like_count, sales_count, updated_at)
- [x] mv_product_rank_monthly 테이블 생성 (PK: (year_month, product_id), rank, score, view_count, like_count, sales_count, updated_at)
- [x] staging_product_rank_weekly 테이블 생성 (MV와 동일 구조)
- [x] staging_product_rank_monthly 테이블 생성 (MV와 동일 구조)

### 3. 도메인 — VO

- [x] YearWeek value class 생성 (from(LocalDate) 팩토리, toString = "2026-W15", startDate/endDate 계산)
- [x] YearWeek.from() ISO Week 변환 정확성 테스트 (일반 케이스)
- [x] YearWeek.from() 엣지 케이스 테스트 (연초/연말 경계, 예: 1월 1일이 전년도 주차에 속하는 경우)

### 4. 배치 — 주간 랭킹 Job

#### 4-1. Reader
- [x] ProductAggregateDto 정의 (productId, viewCount, likeCount, salesCount)
- [x] JdbcPagingItemReader 구성: product_metrics_daily에서 targetDate 기준 해당 주차 범위 GROUP BY product_id 페이징 조회

#### 4-2. Processor
- [x] WeightScoreProcessor 구현: ProductAggregateDto → ProductRankRow 변환 (가중치 Java 계산)
- [x] 가중치를 @ConfigurationProperties로 외부화 (view=0.1, like=0.2, sales=0.7)
- [x] 가중치 계산 정확성 단위 테스트

#### 4-3. Writer
- [x] JdbcBatchItemWriter 구성: staging_product_rank_weekly에 ON DUPLICATE KEY UPDATE upsert

#### 4-4. Swap Step
- [x] Tasklet Step: staging → MV swap (DELETE mv WHERE year_week=? + INSERT mv SELECT FROM staging, 단일 트랜잭션)
- [x] swap 후 staging 테이블 TRUNCATE

#### 4-5. Job 구성
- [x] WeeklyRankingJobConfig 구성 (Job → Step1: Chunk + Step2: Swap)
- [x] JobParameters: targetDate + RunIdIncrementer
- [x] 같은 targetDate 재실행 시 동일 결과 (멱등성 테스트)
- [x] 실패 후 재실행 시 MV 데이터 정합성 유지 테스트

### 5. 배치 — 월간 랭킹 Job

- [x] MonthlyRankingJobConfig 구성 (주간과 동일 패턴, year_month 기준)
- [x] Reader: targetDate 기준 해당 월 범위 GROUP BY product_id
- [x] Writer: staging_product_rank_monthly → mv_product_rank_monthly swap
- [x] 월간 배치 멱등성 테스트

### 6. API — Repository 분리

- [x] DailyRankingRepository 인터페이스 정의 (기존 RankingRepository에서 분리)
- [x] WeeklyRankingRepository 인터페이스 정의 (findTopRankings(yearWeek, offset, count))
- [x] MonthlyRankingRepository 인터페이스 정의 (findTopRankings(yearMonth, offset, count))
- [x] 기존 RankingRedisRepository를 DailyRankingRepository 구현으로 전환
- [x] WeeklyRankingJdbcRepository 구현 (mv_product_rank_weekly 조회)
- [x] MonthlyRankingJdbcRepository 구현 (mv_product_rank_monthly 조회)

### 7. API — Strategy 패턴

- [x] Period enum 정의 (DAILY, WEEKLY, MONTHLY)
- [x] RankingStrategy 인터페이스 정의 (getRankings(date, page, size): List<RankingInfo>)
- [x] DailyRankingStrategy 구현 (기존 Redis + DB fallback 로직 이관)
- [x] WeeklyRankingStrategy 구현 (LocalDate → YearWeek 변환 + MV 조회)
- [x] MonthlyRankingStrategy 구현 (LocalDate → YearMonth 변환 + MV 조회)
- [x] RankingFacade 수정: Map<Period, RankingStrategy> 주입, strategies[period] 위임

### 8. API — Controller/DTO 수정

- [x] RankingDto.Request에 period 파라미터 추가 (기본값: DAILY)
- [x] RankingDto.Response에 resolvedPeriod, periodStart, periodEnd 필드 추가
- [x] RankingController 수정: period 파라미터 수신 → Facade 전달
- [x] RankingApiSpec 업데이트

### 9. E2E 검증

- [x] 일간 랭킹 조회 기존 동작 유지 (period=DAILY 또는 미지정)
- [x] 주간 랭킹 배치 실행 → API 조회 전체 흐름
- [x] 월간 랭킹 배치 실행 → API 조회 전체 흐름
- [x] period별 서로 다른 데이터 소스에서 조회되는지 검증
- [x] http/ranking-v1.http 파일 업데이트 (period 파라미터 추가)
