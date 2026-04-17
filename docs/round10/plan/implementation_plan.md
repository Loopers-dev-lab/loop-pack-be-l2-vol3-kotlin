# Round 10 구현 계획서 — Spring Batch 기반 주간/월간 랭킹 시스템

> 본 문서는 `docs/round10/quest/round_10_quest`, `docs/round10/quest/collect_stack_zip` 두 문서를 기준으로 작성되었다.
> 모든 Phase는 TDD(RED → GREEN → REFACTOR)로 진행하며, Phase 완료 시 사용자 승인 후 다음 Phase로 진입한다.

---

## 0. 요구사항 재진술 (Requirement Restatement)

### 0.1 퀘스트에서 요구하는 것

1. **Spring Batch Job** 으로 하루치 메트릭을 읽어 집계한다. Chunk-Oriented Processing 기반으로 구현하며, 파라미터 기반으로 동작해야 한다.
2. 주간/월간 TOP 100 을 **Materialized View** (조회 전용 테이블) 로 적재한다. 테이블명: `mv_product_rank_weekly`, `mv_product_rank_monthly`.
3. 기존 `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1` 를 확장하여 **일간/주간/월간** 랭킹을 조회 가능하게 한다. 기간별로 "조회해야 하는 형태에 따라 적절한 데이터"를 기반으로 제공한다 → 일간은 Redis ZSET, 주간/월간은 MV 테이블에서 조회한다.

### 0.2 Checklist (무조건 달성)

- [ ] **Spring Batch Job 을 작성하고, 파라미터 기반으로 동작시킬 수 있다.** → `--job.name=...` 지정 + `requestDate`(LocalDate) JobParameter 로 동작.
- [ ] **Chunk Oriented Processing (Reader/Processor/Writer or Tasklet) 기반의 배치 처리를 구현했다.** → 집계 Step 은 `JdbcPagingItemReader` + `ItemProcessor` + `JdbcBatchItemWriter` 의 Chunk 구성. (부가적 purge Step 만 Tasklet)
- [ ] **집계 결과를 저장할 Materialized View 의 구조를 설계하고 올바르게 적재했다.** → `mv_product_rank_weekly`, `mv_product_rank_monthly` JPA Entity 설계 + 배치가 TOP 100 레코드를 멱등하게 UPSERT.
- [ ] **API 가 일간, 주간, 월간 랭킹을 제공하며 조회해야 하는 형태에 따라 적절한 데이터를 기반으로 랭킹을 제공한다.** → `period=DAILY|WEEKLY|MONTHLY` 파라미터 추가. DAILY 는 Redis, WEEKLY/MONTHLY 는 MV 테이블 조회.

### 0.3 본 계획이 해결해야 할 핵심 설계 긴장점

| 긴장점 | 결정 | 근거 |
|--------|------|------|
| 현재 `product_metrics` 는 **누적** 테이블(UNIQUE(product_id))이라 "하루치"를 읽어 주/월간 집계할 수 없다 | **`product_metrics_daily` 일별 스냅샷 테이블을 신설**하여 streamer 가 이벤트 소비 시 cumulative 와 함께 daily 에도 upsert | 퀘스트의 "일간 집계정보를 기반으로" 문구와, 주/월간 집계에서 필요한 "일별 원천"을 확보하기 위함 |
| TOP 100 을 어떻게 Chunk 로 읽을지 | Reader 에서 **SQL 사전 집계 + `ORDER BY score DESC LIMIT 100`** 으로 이미 정렬·절단된 행만 내려받음. Processor 는 rank_position 부여, Writer 는 batch insert | Chunk 단위 트랜잭션이 보장되며, 메모리 부담 없음. "대량 데이터에서 TOP N" 을 Spring Batch 로 푸는 정석 패턴 |
| 재실행(idempotency) | 각 Job 의 첫 Step 은 해당 기간의 MV 행을 삭제하는 **purge Tasklet**, 이후 aggregate Chunk Step | `requestDate` 가 같으면 MV 결과가 동일해야 함 (멱등) |
| 가중치 정책 중복 | streamer 의 `RankingScorePolicy` 를 commerce-batch 로 복제. 추후 공용 모듈 승격은 YAGNI 원칙으로 미룸 | 프로젝트가 이미 `RankingKeyGenerator` 를 api/batch 에서 중복 정의한 선례가 있음 |
| API 의 date 포맷 통일 | 기존 `date=yyyyMMdd` 를 유지. WEEKLY 는 그 날짜가 속한 ISO 주(월~일), MONTHLY 는 그 날짜가 속한 월로 내부에서 변환 | 기존 호환성 유지 + 클라이언트 포맷 통일 |

---

## 1. 현황 분석 (Baseline Snapshot)

| 영역 | 현재 상태 | 본 Round 에서의 변경 |
|------|----------|---------------------|
| `commerce-api/event` (Outbox) | **R7 도입**. `OutboxEventModel`(`outbox_event` 테이블) + `CatalogEventOutboxAppender`(`@EventListener` 로 Like/Order/View 도메인 이벤트 → outbox INSERT, 비즈니스 TX 와 동일 트랜잭션) + `OutboxRelayPublisher`(`@Scheduled(1s)` 폴링 → Kafka send → published_at 마킹) | **변경 없음**. 이번 라운드의 신규 데이터 흐름도 기존 outbox 체인을 그대로 통과하므로 producer 측은 무수정 |
| `commerce-streamer` | Kafka 이벤트 → 누적 `product_metrics` 적재 + Redis ZSET(`ranking:all:{yyyyMMdd}`) 증가. `registerEvent(eventId)` + `isStale(version)` 두 멱등/순서 가드 보유 | **일별 스냅샷 테이블 `product_metrics_daily` 에 upsert 추가** (단일 `@Transactional` 안에서, 두 가드 통과 후) |
| `commerce-batch` | `demoJob`, `rankingCarryOverJob` 두 개(둘 다 Tasklet) 존재 | **주간/월간 집계 Job 2개 신설 (Chunk-Oriented)** |
| `commerce-api/ranking` | Redis ZSET 기반 일간 랭킹 Page API, 상품 상세에 순위 포함 | **`period` 파라미터 추가 + WEEKLY/MONTHLY 경로에서 MV 테이블 조회** |
| 스키마 관리 | `ddl-auto: create` (local/test), 운영은 수동 DDL 전제 | 신규 엔티티 추가 → local/test 는 자동, **운영 DDL 스크립트는 별도 산출물로 본 문서에 기재** |

### 1.1 대안 비교 — 왜 streamer 핸들러에 동기 upsert 인가

이번 라운드의 daily 적재 위치/방식에 대해 검토한 대안은 다음과 같다.

| 대안 | 정합성 | 적시성 | 추가 인프라 | 채택 |
|------|--------|--------|-------------|------|
| **(A) streamer 컨슈머에 동기 upsert (현 안)** | ✅ 단일 JPA TX 로 cumulative ↔ daily 가 원자 커밋 | ✅ 즉시 | 없음 | ✅ |
| (B) streamer 가 자체 outbox 에 "DailyAggregationNeeded" 적재 → 별도 컨슈머가 daily 처리 | △ eventually consistent | △ 폴링 지연 | streamer 측 outbox 테이블·릴레이·전용 컨슈머 필요 | ❌ YAGNI (현재 부하·요구 미발생) |
| (C) commerce-api 가 "DailyMetricEvent" 별도 토픽으로도 발행 | ✅ outbox 활용 | △ 컨슈머 추가 | 토픽·컨슈머 추가 | ❌ 동일 정보를 두 토픽으로 발행 → 중복 |
| (D) 별도 일배치가 Redis 또는 다른 소스에서 daily 를 재구성 | ✅ | ✗ "전일 분만" 가능 → weekly 항상 ≥1일 지연 | 추가 Job | ❌ 적시성 손실 |

**결론**: 기존 outbox(producer 측)가 commerce-api → Kafka 의 "잃지 않는 전송"을 이미 보장하므로, streamer 안에서는 단일 트랜잭션으로 cumulative 와 daily 를 함께 쓰는 것이 가장 단순·정합·적시. 자체 outbox 도입은 부하나 분산 트랜잭션 요구가 실제로 발생할 때까지 보류.

---

## 2. 전체 아키텍처

> 굵은 화살표(◀── R10 NEW) 만 본 라운드의 신규 변경. 나머지는 기존 인프라이며 **무수정 활용**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ [commerce-api: 비즈니스 트랜잭션]                          (기존, R7 도입분) │
│   ├─ DB: 도메인 상태 변경 (Like / Order / ProductView)                       │
│   └─ Spring publishEvent → @EventListener                                   │
│       └─ outbox_event INSERT  ◀── 동일 트랜잭션 내                          │
└──────────────────────────┬─────────────────────────────────────────────────┘
                           │ (커밋)
                           ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ [OutboxRelayPublisher  @Scheduled(fixedDelay=1s)]          (기존)           │
│   SELECT * FROM outbox_event WHERE published_at IS NULL LIMIT 100           │
│   → kafkaTemplate.send(topic, partitionKey, payload)                        │
│   → outboxEvent.markPublished()                                             │
└──────────────────────────┬─────────────────────────────────────────────────┘
                           ▼
                       [Kafka]
                           │ (at-least-once)
                           ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ [commerce-streamer  @Transactional ProductMetricsEventHandler.handle]       │
│   1. registerEvent(eventId)         ─ 멱등 가드        (기존)               │
│   2. isStale(event)                 ─ 버전 역전 가드   (기존)               │
│   3. product_metrics 누적 save      ─▶ MySQL          (기존)               │
│   4. product_metrics_daily upsert   ─▶ MySQL          ◀── R10 NEW (Phase 1)│
│   5. Redis ZSET ZINCRBY             ─▶ Redis          (기존)               │
└──────────────────────────┬─────────────────────────────────────────────────┘
                           │
                           ▼
                   [MySQL: product_metrics_daily]   ◀── R10 NEW
                           │
                           ▼
┌────────────────────────────────────────────────────────────────────────────┐
│ [commerce-batch]                                          ◀── R10 NEW       │
│   weeklyRankingAggregationJob                                               │
│     Step1(Tasklet: purge by period_start)                                   │
│     Step2(Chunk: JdbcPagingItemReader → Processor → JpaItemWriter, size=50) │
│       ─▶ mv_product_rank_weekly (TOP 100)                                   │
│   monthlyRankingAggregationJob                                              │
│     Step1(Tasklet: purge by year_month_val)                                 │
│     Step2(Chunk: 동일 구조)                                                 │
│       ─▶ mv_product_rank_monthly (TOP 100)                                  │
└────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────┐
│ [commerce-api: GET /api/v1/rankings?period=...&date=yyyyMMdd]               │
│   period=DAILY    ─▶ Redis ZSET                          (기존 경로)        │
│   period=WEEKLY   ─▶ mv_product_rank_weekly              ◀── R10 NEW (Phase 5)│
│   period=MONTHLY  ─▶ mv_product_rank_monthly             ◀── R10 NEW         │
└────────────────────────────────────────────────────────────────────────────┘
```

### 계층 책임

- **Domain**: `RankingPeriod` 열거형, `WeeklyRanking` / `MonthlyRanking` Entity, `ProductMetricsDaily` Entity, 각 Repository 인터페이스.
- **Application**: `RankingFacade` 에 `period` 분기 추가. 배치 쪽은 `WeeklyRankingAggregationService` / `MonthlyRankingAggregationService` 도메인 서비스.
- **Infrastructure**: JPA Repository 구현 + 배치 Reader/Writer 구성.
- **Interfaces**: `RankingV1Controller` 에 `period` 파라미터 추가.

---

## 3. Phase 구성 (총 6 Phase)

> 각 Phase 는 **TDD (RED → GREEN → REFACTOR)** 로 진행하며, 완료 시 보고 후 **사용자 승인 대기**. 승인 전까지 다음 Phase 진입 금지.

---

### Phase 1. 일별 메트릭 스냅샷 테이블 신설 (`product_metrics_daily`)

**목표**: 주/월간 집계의 원천이 될 일별 메트릭 테이블을 만들고 streamer 에서 이벤트 소비 시 upsert 한다.

#### 1.0 컨텍스트 — 기존 인프라 활용 / 변경 없는 범위

본 Phase 의 변경은 **commerce-streamer 컨슈머 측에 한정** 된다. 다음 컴포넌트는 그대로 활용하며 절대 수정하지 않는다.

| 컴포넌트 | 위치 | 본 Phase 에서의 역할 |
|----------|------|----------------------|
| `CatalogEventOutboxAppender` | `commerce-api/application/event/` | commerce-api 도메인 이벤트(좋아요/주문/조회) → `outbox_event` INSERT (비즈니스 TX 와 동일) |
| `OutboxRelayPublisher` | `commerce-api/application/event/` | `@Scheduled(1s)` 폴링 → Kafka 발행 → `published_at` 마킹 |
| `ProductMetricsEventHandler.registerEvent` | `commerce-streamer/application/metrics/` | eventId UNIQUE 위반으로 중복 이벤트 차단 (멱등) |
| `ProductMetricsModel.isStale` | `commerce-streamer/domain/metrics/` | event.version < lastEventVersion 시 스킵 (역전 가드) |
| Redis ZSET 일간 랭킹 적재 | `commerce-streamer/infrastructure/ranking/` | R9 그대로 |

**결과적으로 Kafka 가 at-least-once 로 동일 이벤트를 재전송하더라도**:
- `registerEvent` 가 핸들러 진입 직후(트랜잭션 시작 직후)에 동작 → daily upsert 도 자동으로 중복 차단됨
- 즉, daily 쪽에 추가 멱등 키나 락이 필요 없음

#### 1.1 변경 파일

| 파일 | 내용 |
|------|------|
| `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/metrics/ProductMetricsDailyModel.kt` (신규) | JPA Entity |
| `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/metrics/ProductMetricsDailyJpaRepository.kt` (신규) | Repository |
| `apps/commerce-streamer/src/main/kotlin/com/loopers/application/metrics/ProductMetricsEventHandler.kt` | daily upsert 호출 추가 |
| `apps/commerce-streamer/src/test/kotlin/com/loopers/application/metrics/ProductMetricsEventHandlerTest.kt` | RED: daily 반영 검증 테스트 추가 |

#### 1.2 스키마 설계 (`product_metrics_daily`)

```sql
CREATE TABLE product_metrics_daily (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    product_id      BIGINT       NOT NULL,
    metric_date     DATE         NOT NULL,             -- Asia/Seoul 기준 이벤트 발생일
    likes_count     BIGINT       NOT NULL DEFAULT 0,
    views_count     BIGINT       NOT NULL DEFAULT 0,
    sales_count     BIGINT       NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pmd_product_date (product_id, metric_date),
    KEY idx_pmd_metric_date (metric_date)              -- 배치 range scan 용
);
```

**필드 근거**
- `likes_count / views_count / sales_count`: `CatalogEventType` 와 1:1. Round 9 `product_metrics` 와 명명 일치.
- `metric_date`: event.occurredAt.atZone(Asia/Seoul).toLocalDate(). UTC 저장 정책과 무관하게 "한국 기준 하루" 로 집계해야 주/월 경계가 직관적이다.
- `UNIQUE (product_id, metric_date)`: upsert 키.
- `INDEX (metric_date)`: 주간(7일) 범위 스캔 성능 확보.

#### 1.3 upsert 전략

`registerEvent` + `isStale` 가드를 통과한 이벤트만 daily 에 도달하므로, daily upsert 자체는 **단순 누적 합산** 으로 충분하다 (재전송·역전 방어는 상위 가드가 책임). JPA 표준만으로 MySQL `INSERT ... ON DUPLICATE KEY UPDATE` 를 쓰려면 raw JDBC 또는 `@Modifying @Query(nativeQuery = true)`. 본 프로젝트는 후자를 선택한다.

```kotlin
// ProductMetricsDailyJpaRepository.kt
@Modifying
@Query(
    value = """
        INSERT INTO product_metrics_daily
            (product_id, metric_date, likes_count, views_count, sales_count, created_at, updated_at)
        VALUES
            (:productId, :metricDate, :likesDelta, :viewsDelta, :salesDelta, NOW(6), NOW(6))
        ON DUPLICATE KEY UPDATE
            likes_count = likes_count + VALUES(likes_count),
            views_count = views_count + VALUES(views_count),
            sales_count = sales_count + VALUES(sales_count),
            updated_at  = NOW(6)
    """,
    nativeQuery = true,
)
fun upsert(
    productId: Long,
    metricDate: LocalDate,
    likesDelta: Long,
    viewsDelta: Long,
    salesDelta: Long,
): Int
```

`ProductMetricsEventHandler` 는 기존 cumulative 저장 뒤에 daily upsert 를 호출한다. event 의 eventType 에 따라 3개 컬럼 중 하나만 delta 값이 들어가고 나머지는 0.

#### 1.4 TDD 시나리오

| 단계 | 테스트 | 설명 |
|------|--------|------|
| RED | `productMetricsDaily 가 이벤트 일자별로 누적된다` | 동일 상품·동일 날짜 2회 이벤트 → likes_count 합산 |
| RED | `날짜가 다르면 별도 행이 생성된다` | 같은 상품, 다른 날짜 → 행 2개 |
| RED | `중복 eventId 는 daily 에도 반영되지 않는다` | 기존 멱등 정책 회귀 검증 |
| GREEN | 최소 구현 | upsert 네이티브 쿼리 + handler 호출 |
| REFACTOR | import 정리, 명명 일관화 | - |

#### 1.5 완료 조건

- streamer 통합 테스트에서 단일 이벤트 처리 후 `product_metrics`, `product_metrics_daily` 모두 반영됨을 확인.
- ktlintCheck 통과.

---

### Phase 2. Materialized View 엔티티 및 Repository 설계

**목표**: 배치가 적재할 주간/월간 MV 테이블을 JPA Entity 로 정의한다. 이 Phase 에서는 스키마 확정과 엔티티·Repository만 추가하며, 쓰기는 Phase 3/4, 읽기는 Phase 5 에서 한다.

#### 2.1 변경 파일

배치와 API 모두에서 읽기/쓰기가 필요하므로 엔티티는 **각 모듈에 위치 + 필요한 책임만 노출** 하는 방식으로 시작한다 (프로젝트가 supports/* 같은 공용 도메인 모듈을 두지 않는 원칙).

- `apps/commerce-batch/src/main/kotlin/com/loopers/domain/mv/WeeklyProductRankModel.kt` (쓰기 전용)
- `apps/commerce-batch/src/main/kotlin/com/loopers/domain/mv/MonthlyProductRankModel.kt` (쓰기 전용)
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/mv/WeeklyProductRankModel.kt` (읽기 전용, 동일 테이블 매핑)
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/mv/MonthlyProductRankModel.kt` (읽기 전용, 동일 테이블 매핑)
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/PeriodicRankingRepository.kt` (읽기 인터페이스)
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/ranking/mv/PeriodicRankingRepositoryImpl.kt` (JPA 구현)

> 동일 테이블을 두 모듈에서 각각 엔티티로 매핑하는 것은 Spring Boot 멀티 모듈에서 자주 쓰이는 방식이다. API 측 엔티티는 `@Immutable` + 필요한 프로젝션 필드만 유지한다. (GraalVM / Lombok 없이도 안전.)

#### 2.2 `mv_product_rank_weekly` 스키마

```sql
CREATE TABLE mv_product_rank_weekly (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    period_start    DATE         NOT NULL,             -- ISO 주 월요일
    period_end      DATE         NOT NULL,             -- 일요일
    rank_position   INT          NOT NULL,             -- 1..100
    product_id      BIGINT       NOT NULL,
    likes_count     BIGINT       NOT NULL,
    views_count     BIGINT       NOT NULL,
    sales_count     BIGINT       NOT NULL,
    score           DOUBLE       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mpw_start_product (period_start, product_id),
    UNIQUE KEY uk_mpw_start_rank    (period_start, rank_position),
    KEY idx_mpw_start_rank (period_start, rank_position)  -- 조회 인덱스
);
```

#### 2.3 `mv_product_rank_monthly` 스키마

```sql
CREATE TABLE mv_product_rank_monthly (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    year_month_val  CHAR(7)      NOT NULL,             -- 'YYYY-MM' (예: '2026-04')
    period_start    DATE         NOT NULL,             -- 해당 월 1일
    period_end      DATE         NOT NULL,             -- 해당 월 말일
    rank_position   INT          NOT NULL,
    product_id      BIGINT       NOT NULL,
    likes_count     BIGINT       NOT NULL,
    views_count     BIGINT       NOT NULL,
    sales_count     BIGINT       NOT NULL,
    score           DOUBLE       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mpm_month_product (year_month_val, product_id),
    UNIQUE KEY uk_mpm_month_rank    (year_month_val, rank_position),
    KEY idx_mpm_month_rank (year_month_val, rank_position)
);
```

> `year_month` 는 MySQL 예약어로 혼동 우려가 있으므로 컬럼명은 `year_month_val` 로 둔다.

#### 2.4 설계 의사결정 정리

- **제약 `UNIQUE (period_start, rank_position)`**: TOP 100 에서 rank 중복 방지. 재실행 시 purge 이후 재삽입 순서가 깨지지 않도록 함.
- **score 를 그대로 저장**: 디버깅/검증 용이 (API 에서 그대로 노출 가능). 상수 가중치가 바뀌면 재배치 실행으로 덮어씀.
- **likes/views/sales 를 저장**: 차후 "주간 랭킹에 왜 이 상품이 1위인가"의 설명 가능성(Explainability)을 위해.

#### 2.5 TDD 시나리오 (Repository Read 계약)

| 단계 | 테스트 | 설명 |
|------|--------|------|
| RED | `주어진 period_start 로 rank asc 로 정렬된 N개 조회` | `findTopByPeriodStart(periodStart, limit)` |
| RED | `page/size 기반 offset 조회` | Pageable 없이 rank 범위로 slice |
| RED | `데이터 없음일 때 빈 리스트` | - |
| GREEN | JPA derived query 또는 `@Query` | - |
| REFACTOR | DTO projection | - |

#### 2.6 완료 조건

- local/test 프로파일에서 `ddl-auto: create` 로 테이블 자동 생성 확인.
- Repository 단위 테스트(@DataJpaTest 또는 경량 통합) 모두 통과.

---

### Phase 3. 주간 랭킹 집계 Batch Job (Chunk-Oriented)

**목표**: `weeklyRankingAggregationJob` 을 만들고, `product_metrics_daily` 에서 ISO 주 기준 7일치를 읽어 `mv_product_rank_weekly` 로 TOP 100 을 적재한다.

#### 3.1 Job 구조

```
weeklyRankingAggregationJob
 ├─ Step 1: weeklyRankingPurgeStep   (Tasklet)   — 해당 주의 기존 MV 행 삭제
 └─ Step 2: weeklyRankingAggregateStep (Chunk)    — Reader → Processor → Writer (TOP 100)
```

`@ConditionalOnProperty(name = "spring.batch.job.name", havingValue = JOB_NAME)` 패턴을 기존 Job(`DemoJobConfig`, `RankingCarryOverJobConfig`)과 동일하게 적용하여, `--job.name=weeklyRankingAggregationJob` 으로만 해당 Job 이 활성화되게 한다.

#### 3.2 JobParameters

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `requestDate` | `LocalDate` (yyyyMMdd) | ✅ | 이 날짜가 속한 **ISO 주(월요일 시작)** 를 집계 대상으로 삼음 |
| `topN` | `Long` | ⭕ (기본 100) | 상위 N개 (향후 확장 여지) |

**파라미터 기반 동작 증명**: `requestDate` 미지정 시 `@Value("#{jobParameters['requestDate']}")` 바인딩 실패로 Job 실행이 `ExitStatus.FAILED` 로 종료되어야 한다 (DemoJobE2ETest 와 동일한 패턴).

#### 3.3 Step 1 — Purge Tasklet

`WeeklyRankingPurgeTasklet`:
- `requestDate` → `periodStart` (월요일) 계산
- `WeeklyProductRankJpaRepository.deleteByPeriodStart(periodStart)` 를 호출하여 idempotent 재실행 지원
- `ResourcelessTransactionManager` 가 아닌 **실제 `PlatformTransactionManager`** 를 사용해야 실제 DELETE 가 트랜잭션 보호 하에 실행됨

#### 3.4 Step 2 — Chunk-Oriented Aggregation

**Chunk size**: 50 (TOP 100 → 2 chunks)

##### Reader: `JdbcPagingItemReader<WeeklyAggregationRow>`

```sql
SELECT
    product_id,
    SUM(likes_count)                                           AS total_likes,
    SUM(views_count)                                           AS total_views,
    SUM(sales_count)                                           AS total_sales,
    (SUM(likes_count) * :likeWeight
   + SUM(views_count) * :viewWeight
   + SUM(sales_count) * :orderWeight)                          AS score
FROM product_metrics_daily
WHERE metric_date BETWEEN :periodStart AND :periodEnd
  AND deleted_at IS NULL
GROUP BY product_id
ORDER BY score DESC, product_id ASC
LIMIT :topN
```

- Paging 은 `MySqlPagingQueryProvider` 사용.
- `pageSize = 50` (chunk size 와 동일) → 2 page = 100 row.
- `ORDER BY` 에 `product_id ASC` 를 보조 키로 두어 동점 시 정렬 안정성 확보.

##### Processor: `WeeklyRankingProcessor`

- `@StepScope` + `AtomicInteger` 로 rank_position 1부터 부여.
- `WeeklyAggregationRow` → `WeeklyProductRankModel` 변환.
- 0점이거나 null score 는 `null` 반환하여 Writer 에서 스킵.

##### Writer: `JpaItemWriter<WeeklyProductRankModel>` 또는 `JdbcBatchItemWriter`

- 간단성 기준 `JpaItemWriter` 채택. 단 TOP 100 기준이라 성능 병목은 없음.
- Hibernate batch_size 는 이미 100 (jpa.yml `default_batch_fetch_size`), 추가로 `hibernate.jdbc.batch_size=50` 을 선택적으로 설정 (옵션).

#### 3.5 쓰기용 도메인 및 Repository

| 클래스 | 위치 | 책임 |
|--------|------|------|
| `WeeklyProductRankModel` | `apps/commerce-batch/.../domain/mv/` | `@Entity(name = "mv_product_rank_weekly")` |
| `WeeklyProductRankJpaRepository` | `apps/commerce-batch/.../infrastructure/mv/` | `deleteByPeriodStart(periodStart: LocalDate): Long` |
| `WeeklyPeriodResolver` | `apps/commerce-batch/.../domain/mv/` | `LocalDate → Pair<LocalDate, LocalDate>` (월요일, 일요일) |
| `BatchRankingScorePolicy` | `apps/commerce-batch/.../domain/ranking/` | 가중치 상수를 JdbcPagingItemReader 파라미터로 노출 |

> `WeeklyPeriodResolver.of(date)`:
> ```
> val monday = date.with(WeekFields.ISO.dayOfWeek(), 1)
> val sunday = monday.plusDays(6)
> ```
> `WeekFields.ISO` 로 한국식 주(월~일) 를 명확히 강제한다.

#### 3.6 Job Config (핵심 골격)

```kotlin
@ConditionalOnProperty(name = ["spring.batch.job.name"], havingValue = WeeklyRankingAggregationJobConfig.JOB_NAME)
@Configuration
class WeeklyRankingAggregationJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val jobListener: JobListener,
    private val stepMonitorListener: StepMonitorListener,
    private val chunkListener: ChunkListener,
    private val purgeTasklet: WeeklyRankingPurgeTasklet,
    private val reader: ItemReader<WeeklyAggregationRow>,
    private val processor: ItemProcessor<WeeklyAggregationRow, WeeklyProductRankModel>,
    private val writer: ItemWriter<WeeklyProductRankModel>,
) {
    companion object {
        const val JOB_NAME = "weeklyRankingAggregationJob"
        private const val PURGE_STEP = "weeklyRankingPurgeStep"
        private const val AGGREGATE_STEP = "weeklyRankingAggregateStep"
        private const val CHUNK_SIZE = 50
    }

    @Bean(JOB_NAME)
    fun job(): Job = JobBuilder(JOB_NAME, jobRepository)
        .incrementer(RunIdIncrementer())
        .start(purgeStep())
        .next(aggregateStep())
        .listener(jobListener)
        .build()

    @JobScope
    @Bean(PURGE_STEP)
    fun purgeStep(): Step = StepBuilder(PURGE_STEP, jobRepository)
        .tasklet(purgeTasklet, txManager)
        .listener(stepMonitorListener)
        .build()

    @JobScope
    @Bean(AGGREGATE_STEP)
    fun aggregateStep(): Step = StepBuilder(AGGREGATE_STEP, jobRepository)
        .chunk<WeeklyAggregationRow, WeeklyProductRankModel>(CHUNK_SIZE, txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .listener(stepMonitorListener)
        .listener(chunkListener)
        .build()
}
```

#### 3.7 TDD 시나리오

| 단계 | 테스트 | 설명 |
|------|--------|------|
| RED (Unit) | `WeeklyPeriodResolver 는 ISO 주(월~일) 경계를 반환한다` | 수/목/일요일 입력 → 올바른 범위 |
| RED (Unit) | `BatchRankingScorePolicy 의 가중치가 설정과 일치한다` | - |
| RED (E2E @SpringBootTest + @SpringBatchTest) | `requestDate 없이 실행하면 Job 이 FAILED 로 종료된다` | 파라미터 기반 동작 검증 |
| RED (E2E) | `일별 메트릭을 주간 TOP100 으로 올바르게 집계한다` | `product_metrics_daily` 시드 → Job 실행 → MV 검증 |
| RED (E2E) | `같은 requestDate 로 재실행해도 결과가 동일하다 (idempotent)` | purge Step 검증 |
| RED (E2E) | `주간 경계 외 데이터는 집계되지 않는다` | 지난 주 / 다음 주 데이터 삽입 후 무시 확인 |
| RED (E2E) | `동점 상품은 product_id ASC 로 안정 정렬된다` | - |
| GREEN | Job/Step/Reader/Processor/Writer 구현 | 최소 구현 |
| REFACTOR | listener, 로그, 상수 추출 | - |

#### 3.8 완료 조건

- 위 E2E 테스트 전부 통과.
- `./gradlew :apps:commerce-batch:test` 성공.
- `--job.name=weeklyRankingAggregationJob --requestDate=20260413` 으로 로컬 실행 시 MV 적재 확인.

---

### Phase 4. 월간 랭킹 집계 Batch Job (Chunk-Oriented)

**목표**: Phase 3 의 구조를 그대로 따라 월간 Job 을 구현한다. 날짜 경계·저장 테이블·키 컬럼만 다름.

#### 4.1 차이점 요약

| 항목 | 주간 (Phase 3) | 월간 (Phase 4) |
|------|----------------|----------------|
| Job 이름 | `weeklyRankingAggregationJob` | `monthlyRankingAggregationJob` |
| 기간 결정 | ISO 주(월~일) | 해당 월 1일~말일 |
| 구분자 컬럼 | `period_start` (Monday) | `year_month_val` ('YYYY-MM') + `period_start` (월 1일) |
| MV 테이블 | `mv_product_rank_weekly` | `mv_product_rank_monthly` |
| Reader WHERE | `metric_date BETWEEN :periodStart AND :periodEnd` | 동일 |
| purge 기준 | `period_start = :monday` | `year_month_val = :yearMonth` |

#### 4.2 신규 파일

- `apps/commerce-batch/.../batch/job/ranking/MonthlyRankingAggregationJobConfig.kt`
- `apps/commerce-batch/.../batch/job/ranking/step/MonthlyRankingPurgeTasklet.kt`
- `apps/commerce-batch/.../batch/job/ranking/step/MonthlyRankingItemReader.kt` (Configuration 의 @Bean 으로 가능)
- `apps/commerce-batch/.../batch/job/ranking/step/MonthlyRankingProcessor.kt`
- `apps/commerce-batch/.../domain/mv/MonthlyProductRankModel.kt`
- `apps/commerce-batch/.../infrastructure/mv/MonthlyProductRankJpaRepository.kt`
- `apps/commerce-batch/.../domain/mv/MonthlyPeriodResolver.kt`

#### 4.3 공통화 여부

`WeeklyRankingItemReader` 와 `MonthlyRankingItemReader` 는 WHERE / ORDER / LIMIT 골격이 거의 같다. 그러나 리팩터링은 **두 Job 이 모두 초록 상태가 된 뒤** 진행한다 (성급한 추상화 방지). 본 계획에선 "공통화 여지 있음"만 명시하고 실제 리팩터링은 Phase 6 에 배치.

#### 4.4 TDD 시나리오

Phase 3 과 동일 항목을 월간 기준으로 재작성. 추가로:

- `MonthlyPeriodResolver 는 윤년 2월(29일)을 올바르게 처리한다`
- `MonthlyPeriodResolver 는 해가 바뀌는 케이스(12월 → 1월)를 다루지 않는다 (단일 월 내)`

#### 4.5 완료 조건

- 월간 E2E 테스트 통과.
- `--job.name=monthlyRankingAggregationJob --requestDate=20260415` 실행 시 `mv_product_rank_monthly` 에 TOP 100 적재.

---

### Phase 5. Ranking API 확장 — `period` 파라미터 도입

**목표**: `GET /api/v1/rankings` 에 `period=DAILY|WEEKLY|MONTHLY` 파라미터를 추가하고, DAILY 는 기존 Redis ZSET 을, WEEKLY/MONTHLY 는 MV 테이블을 조회한다.

#### 5.1 API 스펙

```
GET /api/v1/rankings?period=DAILY&date=20260413&size=20&page=1
GET /api/v1/rankings?period=WEEKLY&date=20260413&size=20&page=1
GET /api/v1/rankings?period=MONTHLY&date=20260413&size=20&page=1
```

- `period` 기본값 `DAILY` (기존 호환성 유지).
- `date` 는 변함없이 `yyyyMMdd`. 내부에서 WEEKLY 는 ISO 주, MONTHLY 는 해당 월로 변환.
- 응답 포맷은 기존 `ApiResponse<RankingPageInfo>` 그대로.

#### 5.2 코드 구조

```
interfaces/api/ranking/
  └─ RankingV1Controller.kt               (period RequestParam 추가)
  └─ RankingPeriodRequest.kt              (Enum: DAILY/WEEKLY/MONTHLY, 파싱 로직)
application/ranking/
  └─ RankingFacade.kt                     (period 기반 위임)
domain/ranking/
  └─ RankingPeriod.kt                     (Enum)
  └─ RankingService.kt                    (getTopRankings(period, date, page, size))
  └─ PeriodicRankingRepository.kt         (신규, WEEKLY/MONTHLY 조회 인터페이스)
domain/ranking/mv/
  └─ WeeklyProductRankModel.kt            (API 측 읽기 전용 엔티티)
  └─ MonthlyProductRankModel.kt
infrastructure/ranking/mv/
  └─ PeriodicRankingRepositoryImpl.kt     (JPA 구현)
```

#### 5.3 `RankingPeriod` 도메인

```kotlin
enum class RankingPeriod { DAILY, WEEKLY, MONTHLY }
```

각 period 에 따라 `RankingService` 가 분기:

- **DAILY**: 기존 `RankingRepository` (Redis) 사용.
- **WEEKLY**: `date` → ISO Monday 계산 → `PeriodicRankingRepository.findTopWeekly(monday, offset, size)` + `countWeekly(monday)`.
- **MONTHLY**: `date` → 해당 월 첫날 + yearMonth 문자열 → `findTopMonthly(yearMonth, offset, size)` + `countMonthly(yearMonth)`.

`RankingService` 가 세 경로의 반환을 동일한 `RankingPage` 로 통일. 결과적으로 `RankingFacade` 는 변경 최소화.

#### 5.4 `PeriodicRankingRepository` 인터페이스

```kotlin
interface PeriodicRankingRepository {
    fun findTopWeekly(periodStart: LocalDate, offset: Long, limit: Long): List<RankedProduct>
    fun countWeekly(periodStart: LocalDate): Long

    fun findTopMonthly(yearMonth: String, offset: Long, limit: Long): List<RankedProduct>
    fun countMonthly(yearMonth: String): Long
}
```

구현은 Spring Data JPA Derived Query + projection.

#### 5.5 예외 처리

| 케이스 | 동작 |
|--------|------|
| 존재하지 않는 period 값 | `BAD_REQUEST` (`ApiResponse` FAIL) |
| 해당 기간의 MV 가 아직 적재되지 않음 | 빈 페이지 반환 (기존 DAILY 동작과 동일) |
| WEEKLY 에 대해 미래 날짜를 전달 | 빈 페이지 반환 (배치가 아직 실행 안 됨) |

#### 5.6 TDD 시나리오

| 단계 | 테스트 | 설명 |
|------|--------|------|
| RED (Controller @WebMvcTest) | `period=DAILY` 기본값 적용 | 파라미터 미전달 시 DAILY 경로 호출 |
| RED (Controller) | `period=WEEKLY` 는 MV 주간 조회를 호출한다` | Facade mock |
| RED (Controller) | `잘못된 period 값은 400 으로 응답한다` | - |
| RED (Facade/Service) | `WEEKLY 조회 시 ISO Monday 로 정규화된다` | 수/일요일 date 입력 → 같은 월요일로 조회 |
| RED (Repository @DataJpaTest) | `주간 TOP 결과가 rank_position asc 로 반환된다` | MV seed → 조회 |
| RED (Repository) | `monthlyRankingAggregationJob 미실행 시 빈 결과` | - |
| RED (E2E @SpringBootTest) | `실제 Redis + MySQL 에서 세 경로가 모두 동작한다` | Testcontainers |
| GREEN | 각 레이어 구현 | - |
| REFACTOR | DTO 공통화, null 처리 | - |

#### 5.7 완료 조건

- 위 테스트 전체 통과.
- 수동 검증: 아래 `.http` 세 건 200 응답.

---

### Phase 6. `.http` 파일 정리 · 수동 E2E 검증 · 공통화 리팩터링

**목표**: 완성된 API/Job 에 대한 `.http` 파일 갱신 및 Phase 3/4 의 공통 코드 추출 기회를 정리한다.

#### 6.1 `.http/ranking/getRankings.http` 추가/수정

```http
### 일간 랭킹 (기본: period 미지정)
GET http://localhost:8080/api/v1/rankings?date=20260413&size=20&page=1

### 일간 랭킹 (명시)
GET http://localhost:8080/api/v1/rankings?period=DAILY&date=20260413&size=20&page=1

### 주간 랭킹
GET http://localhost:8080/api/v1/rankings?period=WEEKLY&date=20260413&size=20&page=1

### 월간 랭킹
GET http://localhost:8080/api/v1/rankings?period=MONTHLY&date=20260413&size=20&page=1

### 주간 랭킹 2페이지
GET http://localhost:8080/api/v1/rankings?period=WEEKLY&date=20260413&size=20&page=2
```

#### 6.2 수동 E2E 검증 체크리스트

1. streamer 에 view/like/order 이벤트 수십 건 발행 → `product_metrics_daily` 에 행이 누적되는지 확인.
2. `java -jar commerce-batch.jar --job.name=weeklyRankingAggregationJob --requestDate=20260413` 실행 → `mv_product_rank_weekly` 테이블에 TOP 100 레코드 존재, UNIQUE 제약 위반 없음.
3. 같은 명령어 재실행 → 결과 동일 (멱등).
4. `GET /api/v1/rankings?period=WEEKLY&date=20260413` 200, content size == min(20, 실적재 수).
5. 월간 Job 동일 검증.

#### 6.3 선택적 공통화 리팩터링

Phase 3/4 의 `*PeriodResolver`, `*ItemReader`, `*PurgeTasklet`, `*Processor` 를 비교하고 진짜 중복(구조적 동일 + 변경 사유 동일)만 추출.

- 후보: `BatchRankingScorePolicy`, `WeeklyAggregationRow`/`MonthlyAggregationRow` 가 동일 구조라면 `PeriodicAggregationRow` 로 통합.
- 후보: `AbstractRankingAggregationJobConfig` 는 **도입하지 않음** (OCP 보다 YAGNI 우선, 향후 분기 시 변경 비용 오히려 증가).

#### 6.4 완료 조건

- `.http` 4개 경로 200.
- 전체 `./gradlew build` 통과.
- ktlintCheck 통과.

---

## 4. 의존 관계 및 순서

```
Phase 1 ─┐
         ├─▶ Phase 3 ─┐
Phase 2 ─┤            ├─▶ Phase 5 ─▶ Phase 6
         └─▶ Phase 4 ─┘
```

- Phase 1(원천 데이터) 과 Phase 2(적재 목적지 스키마) 가 Phase 3/4 의 선행 조건.
- Phase 3 과 Phase 4 는 상호 독립 (동일 패턴 적용). 안정성 위해 **Phase 3 먼저, Phase 4 는 그 구조를 복제**.
- Phase 5 는 MV 테이블이 존재해야 의미 있는 테스트가 가능하므로 Phase 3/4 완료 후 진행.

---

## 5. 리스크 및 대응

| 리스크 | 수준 | 대응 |
|--------|------|------|
| streamer 의 이벤트 시각 타임존 처리 오차로 daily 경계가 어긋남 | HIGH | `event.occurredAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate()` 로 명시적 KST 변환. ProductMetricsEventHandler 테스트에서 자정 경계 케이스를 시나리오로 포함 |
| `ddl-auto: create` 는 local/test 전용 — 운영은 별도 DDL 필요 | MEDIUM | 본 문서에 DDL 전문을 기재하고, PR 설명에 "prod DDL 수동 집행 필요" 라벨 명시 |
| MV 테이블 purge/insert 가 큰 트랜잭션이 될 우려 | LOW | TOP 100 이 상한이라 영향 미미. chunk size 50 로 분할 |
| 배치 실행 중간 실패 시 MV 불완전 상태 | MEDIUM | purge + aggregate 를 단일 Step 이 아닌 2-Step 으로 구성. purge 실패 시 재실행 safe. aggregate 실패 시 기존 데이터는 이미 삭제된 상태이므로 재실행 필요 → 로그/알람에 의존 (운영 전략) |
| 동시 실행 (Job 중복 기동) | LOW | Spring Batch 의 기본 `JobRepository` 가 동일 JobParameters 재실행 방지. `RunIdIncrementer` 사용 시 파라미터가 달라지므로 운영에서 `--requestDate` 를 키로 고정 관리 |
| commerce-api 와 commerce-batch 가 동일 MV 테이블을 각각 엔티티로 매핑할 때 불일치 | MEDIUM | 컬럼명/타입을 본 문서의 DDL 표를 Single Source Of Truth 로 삼아 양쪽 엔티티의 `@Column` 매핑을 동일하게 유지. Phase 2 완료 시 체크리스트로 크로스 검증 |
| 가중치(`like=0.2, view=0.1, order=0.7`) 가 streamer 와 batch 에서 값이 어긋남 | MEDIUM | 각 모듈 `application.yml` 의 `ranking.weight.*` 프로퍼티명을 동일하게 유지하고 본 문서에 프로퍼티 규격을 명시. 추후 공용 모듈 승격 시 일원화 가능 |
| Kafka at-least-once 재전송으로 daily 가 중복 적재될 가능성 | LOW | 기존 `registerEvent(eventId)` 가드가 핸들러 진입 직후 (단일 `@Transactional`) 동작 → daily upsert 도 자동 차단. 추가 가드 불필요. Phase 1 TDD 시나리오 "중복 eventId 는 daily 에도 반영되지 않는다" 로 회귀 검증 |
| Outbox Relay 와 streamer 핸들러 사이 Kafka 장애로 일시적 daily 누락 | LOW | outbox_event 행은 `published_at IS NULL` 로 보존 → Kafka 복구 후 자동 재발행. streamer 측은 여전히 동일 가드로 중복 없이 처리. 본 라운드에서 추가 작업 불필요 |

---

## 6. Checklist 달성 근거 (체크리스트 ↔ 구현 매핑)

| Checklist 항목 | 달성 Phase | 구현물 |
|----------------|-----------|--------|
| Spring Batch Job 을 작성하고, 파라미터 기반으로 동작시킬 수 있다 | Phase 3, 4 | `weeklyRankingAggregationJob`, `monthlyRankingAggregationJob` + `@Value("#{jobParameters['requestDate']}")`, `requestDate` 미지정 시 실패 E2E 테스트 |
| Chunk Oriented Processing (Reader/Processor/Writer or Tasklet) 기반의 배치 처리를 구현했다 | Phase 3, 4 | `JdbcPagingItemReader` → `ItemProcessor` → `JpaItemWriter`, chunk size 50. 보조 Step 은 Tasklet |
| 집계 결과를 저장할 Materialized View 의 구조를 설계하고 올바르게 적재했다 | Phase 2, 3, 4 | `mv_product_rank_weekly`, `mv_product_rank_monthly` 스키마 + JPA Entity + E2E 적재 검증 테스트 |
| API 가 일간, 주간, 월간 랭킹을 제공하며 조회해야 하는 형태에 따라 적절한 데이터를 기반으로 랭킹을 제공한다 | Phase 5 | `period` 파라미터 + DAILY(Redis) / WEEKLY/MONTHLY(MV) 분기. 각 경로 E2E 검증 |

---

## 7. 설정 변경 사항 요약

### 7.1 `apps/commerce-batch/src/main/resources/application.yml` 추가 프로퍼티

```yaml
ranking:
  weight:
    view: 0.1
    like: 0.2
    order: 0.7
  aggregation:
    chunk-size: 50
    top-n: 100
```

### 7.2 `apps/commerce-streamer/src/main/resources/application.yml`

변경 없음. 기존 `ranking.weight.*` 프로퍼티를 그대로 사용.

### 7.3 `apps/commerce-api/src/main/resources/application.yml`

변경 없음.

---

## 8. 산출물 목록 (Phase 별)

### Phase 1
- `ProductMetricsDailyModel.kt`, `ProductMetricsDailyJpaRepository.kt`, `ProductMetricsEventHandler.kt` 수정, 테스트

### Phase 2
- `WeeklyProductRankModel.kt` × 2 (batch/api), `MonthlyProductRankModel.kt` × 2 (batch/api), `WeeklyProductRankJpaRepository.kt`, `MonthlyProductRankJpaRepository.kt`, 읽기 Repository 인터페이스

### Phase 3
- `WeeklyRankingAggregationJobConfig.kt`, `WeeklyRankingPurgeTasklet.kt`, `WeeklyRankingProcessor.kt`, `WeeklyRankingItemReaderConfig.kt`, `WeeklyPeriodResolver.kt`, `BatchRankingScorePolicy.kt`, E2E 테스트

### Phase 4
- Phase 3 의 월간 대응 파일 세트 + E2E 테스트

### Phase 5
- `RankingPeriod.kt`, `RankingV1Controller.kt` 수정, `RankingFacade.kt` 수정, `RankingService.kt` 수정, `PeriodicRankingRepository.kt`, `PeriodicRankingRepositoryImpl.kt`, API/Repository/E2E 테스트

### Phase 6
- `.http/ranking/getRankings.http` 업데이트, 공통화 리팩터링(있을 시), 수동 검증 스크린샷/로그 (PR 첨부)

---

## 9. 예상 복잡도

| Phase | 복잡도 | 추정 소요 |
|-------|--------|---------|
| Phase 1 | Low-Medium | 1-2h |
| Phase 2 | Low | 1h |
| Phase 3 | **High** (배치 E2E 인프라가 Heaviest) | 3-4h |
| Phase 4 | Medium (Phase 3 복제) | 1-2h |
| Phase 5 | Medium | 2-3h |
| Phase 6 | Low | 30m-1h |
| **합계** | **Medium-High** | **8-13h** |

---

## 10. 다음 액션

**CONFIRM 대기**: 위 계획대로 진행해도 되는지 사용자 승인 후, **Phase 1 부터 TDD 로 진입** 한다.
- 수정 필요 항목이 있다면 "Phase N 의 X 부분을 Y 로 바꿔줘" 형태로 피드백 부탁.
- "proceed" / "yes" / "go" 입력 시 Phase 1 RED 단계부터 시작.
