# 주간/월간 랭킹 배치 구현 정리

## 1. 목표

기존 일간 랭킹(Redis ZSET)만 제공하던 구조를 확장해서,
다음 3가지를 함께 만족하도록 구현했다.

1. `product_metrics` 일간 집계 데이터를 기반으로 주간/월간 랭킹 집계
2. Spring Batch Chunk-Oriented Processing 기반 배치 적재
3. `GET /api/v1/rankings` 에서 `DAILY`, `WEEKLY`, `MONTHLY` 기간별 조회 지원

---

## 2. 핵심 설계

### 2.1 `product_metrics` 를 일간 집계 소스로 정규화

주간/월간 집계를 만들려면 날짜 구간별 원천 집계가 필요하다.
그래서 `product_metrics` 를 아래 형태의 **일간 집계 테이블**로 정리했다.

- `metric_date`
- `product_id`
- `like_count`
- `view_count`
- `sales_count`
- `last_*_event_at`
- `created_at`, `updated_at`

제약/인덱스:

- unique: `(metric_date, product_id)`
- index: `(metric_date, product_id)`
- index: `(product_id)`

즉, 이제 `product_metrics` 는 "상품별 단일 누적 행"이 아니라
**날짜 + 상품 기준의 일간 집계 projection** 역할을 한다.

### 2.2 주간/월간 MV 테이블 추가

조회 전용 Materialized View 성격의 테이블을 2개 추가했다.

- `mv_product_rank_weekly`
- `mv_product_rank_monthly`

각 테이블에는 다음 정보를 저장한다.

- 기간 시작일 / 종료일
- `product_id`
- `ranking`
- `score`
- 집계에 사용된 `like_count`, `view_count`, `sales_count`

조회 API 는 이 MV 테이블에서 랭킹 순서와 점수만 읽고,
상품/브랜드 메타데이터는 기존 `ProductReader`, `BrandReader` 로 조합한다.

### 2.3 기간별 읽기 경로 분리

- `DAILY` → 기존 Redis 랭킹 조회 경로 사용
- `WEEKLY`, `MONTHLY` → MV 테이블 조회 경로 사용

이렇게 분리해서 일간 랭킹의 빠른 조회 특성은 유지하면서,
주간/월간 랭킹은 배치 기반 read model 로 안정적으로 제공한다.

---

## 3. 배치 구현

## 3.1 Job

추가한 Job 이름:

- `productRankingAggregationJob`

필수 Job Parameter:

- `targetDate=yyyyMMdd`

예시:

```bash
./gradlew :apps:commerce-batch:bootRun --args='--job.name=productRankingAggregationJob targetDate=20260416'
```

## 3.2 Step 구성

Job 은 2개의 chunk step 으로 구성된다.

1. `weeklyProductRankingMaterializeStep`
2. `monthlyProductRankingMaterializeStep`

각 step 은 아래 흐름으로 동작한다.

- Reader: `product_metrics` 에서 기간 구간 집계 TOP 100 조회
- Processor: pass-through
- Writer: 기존 기간 MV 삭제 후 새 랭킹 저장

즉, **Reader / Processor / Writer 기반의 Chunk-Oriented Processing** 을 사용했다.

## 3.3 기간 계산 규칙

- 주간: 기준일이 포함된 주의 월요일 ~ 일요일
- 월간: 기준일이 포함된 달의 1일 ~ 말일

API 조회도 동일한 규칙으로 기간 시작일을 계산해서 MV 를 읽는다.

## 3.4 점수 계산식

일간 Redis 랭킹과 같은 가중치를 유지하기 위해 공통 수식을 사용했다.

- 조회수: `0.1`
- 좋아요: `0.2`
- 판매량: `1.0`

공식:

```text
score = (view_count * 0.1) + (like_count * 0.2) + (sales_count * 1.0)
```

이를 위해 `RankingScoreFormula` 를 공통으로 두고,
실시간 랭킹 계산(`commerce-streamer`)과 배치 집계 SQL 이 같은 기준을 쓰도록 맞췄다.

---

## 4. API 변경

기존:

```http
GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1
```

변경:

```http
GET /api/v1/rankings?date=yyyyMMdd&period=DAILY|WEEKLY|MONTHLY&size=20&page=1
```

기본값:

- `period=DAILY`

예시:

```http
GET /api/v1/rankings?date=20260416&period=DAILY&size=20&page=1
GET /api/v1/rankings?date=20260416&period=WEEKLY&size=20&page=1
GET /api/v1/rankings?date=20260416&period=MONTHLY&size=20&page=1
```

### 참고

상품 상세의 `ranking` 필드는 기존처럼 **일간 랭킹** 기준을 유지했다.
이번 요구사항은 랭킹 목록 API 확장이 핵심이라 상세 응답 구조까지 넓히지는 않았다.

---

## 5. 주요 파일

### 공통/영속 계층

- `modules/jpa/.../metrics/ProductMetricsEntity.kt`
- `modules/jpa/.../metrics/ProductMetricsJpaRepository.kt`
- `modules/jpa/.../ranking/WeeklyProductRankingEntity.kt`
- `modules/jpa/.../ranking/WeeklyProductRankingJpaRepository.kt`
- `modules/jpa/.../ranking/MonthlyProductRankingEntity.kt`
- `modules/jpa/.../ranking/MonthlyProductRankingJpaRepository.kt`
- `modules/jpa/.../ranking/RankingPeriodDateRangeResolver.kt`
- `modules/jpa/.../ranking/RankingScoreFormula.kt`

### streamer

- `apps/commerce-streamer/.../ProductMetricsUpdater.kt`
- `apps/commerce-streamer/.../RankingScoreCalculator.kt`

### batch

- `apps/commerce-batch/.../ProductRankingAggregationJobConfig.kt`
- `apps/commerce-batch/.../ProductRankingAggregationJobParameterValidator.kt`
- `apps/commerce-batch/.../WeeklyProductRankingItemWriter.kt`
- `apps/commerce-batch/.../MonthlyProductRankingItemWriter.kt`

### api

- `apps/commerce-api/.../RankingPeriod.kt`
- `apps/commerce-api/.../RankingUseCase.kt`
- `apps/commerce-api/.../RankingMaterializedViewReader.kt`
- `apps/commerce-api/.../RankingV1Controller.kt`
- `apps/commerce-api/.../RankingV1ApiSpec.kt`

---

## 6. 테스트/검증

### 통과한 검증

아래는 로컬에서 실제로 통과시킨 검증이다.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew \
  :apps:commerce-streamer:test \
  --tests 'com.loopers.application.metrics.ProductMetricsUpdaterTest' \
  --tests 'com.loopers.application.ranking.RankingScoreCalculatorTest'

./gradlew \
  :apps:commerce-api:test \
  --tests 'com.loopers.application.ranking.RankingUseCaseTest' \
  --tests 'com.loopers.application.product.ProductUseCaseTest' \
  --tests 'com.loopers.infrastructure.ranking.RankingRedisReaderTest' \
  --tests 'com.loopers.infrastructure.ranking.RankingMaterializedViewReaderTest'

./gradlew \
  :apps:commerce-batch:test \
  --tests 'com.loopers.batch.job.ranking.ProductRankingAggregationJobParameterValidatorTest' \
  --tests 'com.loopers.batch.job.ranking.WeeklyProductRankingItemWriterTest' \
  --tests 'com.loopers.batch.job.ranking.MonthlyProductRankingItemWriterTest'

./gradlew :apps:commerce-api:ktlintCheck :apps:commerce-streamer:ktlintCheck :apps:commerce-batch:ktlintCheck
```

### 추가로 작성한 통합 테스트

- `RankingFlowE2ETest`
  - 일간 Redis 랭킹 흐름 검증
  - 주간 MV 조회 검증
  - 월간 MV 조회 검증
- `ProductRankingAggregationJobE2ETest`
  - Job parameter 검증
  - 주간/월간 MV 동시 갱신 검증

### 로컬 환경 주의사항

이 저장소의 일부 통합 테스트는 Testcontainers(MySQL/Redis/Kafka)에 의존한다.
현재 세션에서는 Docker 런타임 초기화가 되지 않아 해당 통합 테스트 전체 실행은 막혔다.
따라서 배치/조회 핵심 로직은 단위 테스트 + 컴파일 + ktlint 로 우선 검증했고,
Testcontainers 기반 통합 테스트는 Docker 가능한 환경에서 추가 실행하면 된다.

---

## 7. 체크리스트 대응

### Spring Batch

- [x] Spring Batch Job 작성 및 파라미터 기반 실행
- [x] Chunk-Oriented Processing 구현
- [x] MV 적재 구조 설계 및 저장

### Ranking API

- [x] `DAILY`, `WEEKLY`, `MONTHLY` 기간별 랭킹 조회 지원
- [x] 조회 형태에 맞는 저장소 분리
  - daily → Redis
  - weekly/monthly → MV 테이블

---

## 8. 리뷰 포인트

### 좋았던 점

- 일간 실시간 랭킹과 주간/월간 배치 랭킹의 읽기 경로를 분리해서 책임이 명확하다.
- MV 테이블은 product/brand 정보를 중복 저장하지 않고 랭킹 팩트만 저장해서 diff 와 유지비를 줄였다.
- 점수 계산식을 공통화해서 실시간/배치 랭킹 드리프트를 줄였다.

### 남은 리스크

1. `product_metrics` 의 이벤트 반영은 아직 JPA read-modify-save 기반이라 동시성 경쟁이 심하면 업서트 최적화 여지가 있다.
2. Testcontainers 기반 전체 통합 검증은 Docker 가능한 환경에서 한 번 더 확인하는 것이 안전하다.
3. 상품 상세의 `ranking` 은 여전히 일간 기준이라, 상세에서도 주간/월간이 필요하면 별도 API 설계가 더 필요하다.

---

## 9. 결론

이번 구현으로,

- **일간 실시간 랭킹**은 기존 Redis 기반으로 유지하고
- **주간/월간 랭킹**은 `product_metrics` 일간 집계 → Spring Batch → MV 적재 → API 조회

흐름으로 확장했다.

즉, 요구사항의 핵심인

- Spring Batch
- Batch Processing
- Materialized View (Statistics)
- Ranking API 확장

을 한 번에 연결하는 구조를 완성했다.
