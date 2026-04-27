# Redis ZSET 기반 실시간 랭킹 시스템

## 개요

Round 7의 Kafka → commerce-streamer 파이프라인이 수집한 유저 행동 이벤트(조회, 좋아요, 주문)를 기반으로 Redis ZSET에 랭킹 점수를 실시간 갱신하고, commerce-api에서 Top-N 랭킹 조회 및 개별 상품 순위 API를 제공한다.

### 설계 결정

| 항목 | 결정 | 근거 |
|------|------|------|
| ZSET Key | `ranking:all:{yyyyMMdd}` | 일별 키로 롱테일 방지 + 히스토리 보존 |
| TTL | 2일 (키 생성 시 1회 설정) | Score Carry-Over 의존 + 장애 대응 여유 |
| Score 가중치 | view=0.1, like=0.2, order=0.7×log(price×qty) | 읽기:쓰기 비율 고려, 금액 차이 log로 완화 |
| 랭킹 적재 위치 | commerce-streamer Processor | Application 계층 오케스트레이터 역할 |
| Carry-Over 위치 | commerce-api 스케줄러 | 랭킹판 생성/관리는 API, 점수 갱신은 streamer |
| 상품 정보 조회 | ProductCacheManager | 로컬캐시 + Redis 캐시로 DB 부하 방지 |
| UNLIKED 처리 | ZINCRBY -0.2 | 좋아요 취소 시 점수 차감 |

### 데이터 흐름

```
[이벤트 발행] commerce-api → Kafka
    → commerce-streamer (Consumer → Processor)
        → ProductMetricsRepository.increment() — DB 집계 (기존)
        → RankingService.updateScore() — Redis ZSET ZINCRBY (신규)

[랭킹 조회] commerce-api
    → GET /api/v1/rankings — ZREVRANGE + ProductCacheManager
    → GET /api/v1/products/{id} — ZREVRANK로 현재 순위 추가

[Carry-Over] commerce-api 스케줄러 (23:50)
    → ZUNIONSTORE ranking:all:{내일} 1 ranking:all:{오늘} WEIGHTS 0.1
    → EXPIRE ranking:all:{내일} 2일
```

---

## 구현 계획

### 0. 사전 작업 — 이벤트 페이로드 수정

- [x] `OrderItemPayload`에 `unitPrice: Long` 필드 추가 (modules/kafka)
- [x] commerce-api 이벤트 발행 코드에서 `unitPrice` 포함하도록 수정

### 1. 공통 — Redis 키 정의

- [x] `RedisKeys`에 `rankingKey(date: String): String` 추가 (`ranking:all:{yyyyMMdd}`)

### 2. commerce-streamer — 랭킹 ZSET 적재

#### 2-1. 도메인

- [x] `RankingRepository` 인터페이스 정의: `incrementScore(date: LocalDate, productId: Long, score: Double)`
- [x] `RankingService` 구현: 이벤트 타입별 점수 계산 + `RankingRepository` 호출
    - [x] VIEWED 이벤트 시 해당 상품 점수 0.1 증가
    - [x] LIKED 이벤트 시 해당 상품 점수 0.2 증가
    - [x] UNLIKED 이벤트 시 해당 상품 점수 0.2 차감
    - [x] ORDER_COMPLETED 이벤트 시 상품별 점수 `0.7 × log(unitPrice × quantity)` 증가

#### 2-2. 인프라

- [x] `RankingRedisRepository` 구현: ZINCRBY + Lua 스크립트 (TTL 없으면 2일 설정)
    - [x] ZINCRBY로 점수가 정상 증가한다
    - [x] 키가 없으면 자동 생성되고 TTL 2일이 설정된다
    - [x] 이미 TTL이 있는 키에 ZINCRBY 시 TTL이 변경되지 않는다

#### 2-3. Processor 수정

- [x] `CatalogEventProcessor.process()`에 `RankingService` 호출 추가 (VIEWED, LIKED, UNLIKED)
- [x] `OrderEventProcessor.process()`에 `RankingService` 호출 추가 (ORDER_COMPLETED, 상품별)

### 3. commerce-api — 랭킹 API

#### 3-1. 도메인

- [x] `RankingRepository` 인터페이스 정의
    - `getTopRankings(date: LocalDate, offset: Long, count: Long): List<RankingEntry>`
    - `getRank(date: LocalDate, productId: Long): Long?`
- [x] `RankingEntry` 데이터 클래스: `productId: Long, score: Double`
- [x] `RankingService` 구현: 페이지네이션 계산 + Repository 호출
    - [x] page, size → ZREVRANGE offset 계산 (0-based, inclusive)
    - [x] 특정 상품 순위 조회 시 null 허용 (랭킹 미진입)

#### 3-2. 인프라

- [x] `RankingRedisRepository` 구현
    - [x] ZREVRANGE WITHSCORES로 상위 N개 점수 내림차순 조회
    - [x] 페이지네이션 정상 동작 (page=2, size=20 → offset 20~39)
    - [x] ZREVRANK로 특정 상품 순위 조회 (0-based)
    - [x] 존재하지 않는 상품 순위 조회 시 null 반환

#### 3-3. Application

- [x] `RankingInfo` DTO 정의 (productId, rank, score, 상품 정보)
- [x] `RankingFacade` 구현: RankingService + ProductCacheManager 조합
    - [x] 랭킹 조회 시 상품 정보(이름, 가격 등)가 함께 반환된다
    - [x] ZSET 순서대로 상품 정보가 매핑된다 (순서 보장)

#### 3-4. Interfaces

- [x] `RankingDto` 정의 (RankingResponse, RankingItemResponse)
- [x] `RankingApiSpec` Swagger 스펙 정의
- [x] `RankingController` 구현: `GET /api/v1/rankings?date={yyyyMMdd}&page=1&size=20`
    - [x] date 파라미터 없으면 오늘 날짜 기본값
    - [x] page, size 파라미터로 페이지네이션
- [x] 상품 상세 API 응답에 `rank: Long?` 필드 추가
    - [x] 랭킹 진입 상품은 순위 반환 (1-based로 변환)
    - [x] 랭킹 미진입 상품은 null 반환

#### 3-5. 스케줄러

- [x] `RankingCarryOverScheduler` 구현 (매일 23:50 실행)
    - [x] `RankingRepository`에 `carryOver(fromDate, toDate, weight)` 메서드 추가
    - [x] ZUNIONSTORE로 전날 점수 × 0.1 복사
    - [x] 생성된 키에 TTL 2일 설정
    - [x] 스케줄러 정상 실행 시 다음날 키가 생성된다

### 4. E2E 검증

- [x] 이벤트 발행 → ZSET 점수 반영 → API 조회 전체 흐름 정상 동작
- [x] 일자 변경 시 이전 날짜 랭킹 조회 정상 동작
- [x] 가중치 확인: 주문 1건의 점수 > 좋아요 3건의 점수
- [x] `http/ranking-v1.http` 파일 작성
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
