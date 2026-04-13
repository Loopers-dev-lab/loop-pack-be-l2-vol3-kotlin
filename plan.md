# Round 10 구현 계획 (plan.md)

> Spring Batch 기반 주간/월간 랭킹 + Materialized View 패턴
>
> 근거 문서: [`docs/requirements/round10-requirements-analysis.md`](docs/requirements/round10-requirements-analysis.md)
> 용어·규칙·설계 결정은 본 plan이 아니라 요구사항 문서를 단일 진실 원천으로 삼는다.

## 0. 실행 원칙 (Non-negotiable)

- **TDD 사이클**: 각 구현 항목은 `[RED] 테스트 → [GREEN] 구현` 쌍으로 진행한다. Fake Repository는 도메인/UseCase 단위 테스트에, JPA/Redis 경유 경로는
  `@SpringBootTest` 통합 테스트에 사용한다.
- **Tidy First**: 구조적 변경(파일 이동, 시그니처 확장, VO 추출 등)과 행위적 변경(로직 수정)을 **같은 커밋에 섞지 않는다**. 구조적 변경이 먼저.
- **R9 회귀 방지 최우선**: `ProductMetrics` 도메인, `GetRankingUseCase` DAILY 경로, `RankingV1Controller` 기존 E2E 테스트가 깨지면 즉시 중단한다.
- **완료 전 검증 필수**: `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test` 통과 후에만 커밋한다.
  commerce-batch / commerce-streamer 작업 시 해당 모듈도 함께 검증한다.
- **클래스 한정 테스트**: 각 Step의 `검증` 항목은 **방금 작성한 테스트 클래스만** `--tests "ClassName"` 옵션으로 실행한다. 모듈 전체(`./gradlew :module:test`)나 전체 빌드(`./gradlew test`)는 하단 "공통 검증 체크리스트" 1회만 실행한다.
- **명시적 승인 없이 커밋/푸시 금지**. 각 커밋 경계에서 개발자 확인을 받는다.

---

## Step 1 — `product_metrics_daily` 도메인/Infra 확보

설계 근거: 요구사항 §10.11, §Step 1.
세션 분할: **2개 커밋**으로 나눈다.

### Commit ①-a (구조적) — 신규 도메인/Repository/Infra 추가

변경 파일 (신규 6개). JpaRepository 내부 인터페이스는 기존 `ProductMetricsRepositoryImpl.kt` 패턴 그대로 **Impl 파일 안에 함께 선언**한다. Step 1은 전부 신규
파일이라 내부 의존성이 한 덩어리로 묶여 있어 "≤ 5 / 세션" 한도를 한 파일만 초과한다 — 하위 분할해도 부분 커밋의 의미가 없어 6개를 그대로 유지한다(요약표에도 "6 + 2"로 이미 반영).

1. `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/metrics/model/ProductMetricsDaily.kt`
2. `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/metrics/repository/ProductMetricsDailyRepository.kt`
3. `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/metrics/ProductMetricsDailyEntity.kt`
4. `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/metrics/ProductMetricsDailyRepositoryImpl.kt` — 한
   파일에 `interface ProductMetricsDailyJpaRepository : JpaRepository<...>`와
   `class ProductMetricsDailyRepositoryImpl(...) : ProductMetricsDailyRepository`를 함께 선언 (기존
   `ProductMetricsRepositoryImpl.kt` 관례 그대로)
5. `apps/commerce-streamer/src/test/kotlin/com/loopers/domain/metrics/FakeProductMetricsDailyRepository.kt`
6. `apps/commerce-streamer/src/test/kotlin/com/loopers/domain/metrics/ProductMetricsDailyTest.kt` — 도메인 단위 테스트 (체크리스트의
   RED 케이스가 담기는 파일)

작업 체크리스트:

- [x] [RED] `ProductMetricsDailyTest` — `productId <= 0`이면 예외
- [x] [RED] `incrementViewCount` / `incrementLikeCount` / `incrementSalesCount(quantity)` 누적 동작
- [x] [RED] `decrementLikeCount`가 0 미만으로 내려가지 않는다 (≥0 가드, `false` 반환)
- [x] [RED] `incrementSalesCount(0)` / 음수 quantity는 예외
- [x] [GREEN] `ProductMetricsDaily.kt` 구현 — 기존 `ProductMetrics.kt` 복제 + `metricDate: LocalDate` 필드 추가
- [x] [GREEN] `ProductMetricsDailyRepository` 인터페이스 — `findByDateAndProductId(metricDate, productId)`, `save`
- [x] [GREEN] `ProductMetricsDailyEntity` — `@Table(name = "product_metrics_daily")`
    - `BaseEntity` 기반 auto-increment `id`를 PK로 유지하고, `(metric_date, product_id)`는 **유니크 제약**으로 건다 (프로젝트 관례 준수)
    - 요구사항 §11.1 DDL은 지침일 뿐, 실제 엔티티 구조는 `ProductMetricsEntity` 패턴에 맞춘다
- [x] [GREEN] `ProductMetricsDailyJpaRepository` — `findByMetricDateAndProductId`
- [x] [GREEN] `ProductMetricsDailyRepositoryImpl` — 도메인 ↔ 엔티티 변환
- [x] [GREEN] `FakeProductMetricsDailyRepository` — 인메모리 `MutableMap<Pair<LocalDate, Long>, ProductMetricsDaily>`
- [x] 검증: `./gradlew :apps:commerce-streamer:test --tests "*.ProductMetricsDailyTest"`
- [x] 이 커밋만으로는 프로덕션 동작이 바뀌지 않는다는 점 확인 (아직 `UpdateProductMetricsUseCase`가 daily repo를 호출하지 않음)

### Commit ①-b (행위적) — `UpdateProductMetricsUseCase`에 일간 갱신 추가

변경 파일 (수정 2개):

1. `apps/commerce-streamer/src/main/kotlin/com/loopers/application/metrics/UpdateProductMetricsUseCase.kt`
2. `apps/commerce-streamer/src/test/kotlin/com/loopers/application/metrics/UpdateProductMetricsUseCaseTest.kt`

작업 체크리스트:

- [x] [RED] `handleCatalogEvent(PRODUCT_VIEWED)` 시 `product_metrics_daily`의 오늘 행 `viewCount`가 1 증가
- [x] [RED] 같은 eventId로 두 번 호출되면 daily도 증가하지 않는다 (멱등성, 기존 `event_handled` 기반)
- [x] [RED] `LIKE_ADDED` → daily `likeCount += 1`, `LIKE_REMOVED` → daily `likeCount -= 1` (0 미만 금지)
- [x] [RED] `handleOrderEvent(PAYMENT_COMPLETED, quantity = N)` 시 daily `salesCount += N`
- [x] [RED] 날짜가 바뀌면 새 행이 생기고 전일 행은 변경되지 않는다 (`Clock`을 advance시키는 테스트)
- [x] [RED] 기존 누적 `ProductMetrics` 갱신 동작은 이전과 동일하다 (회귀 방지)
- [x] [RED] Redis 반영 / `FailedScoreUpdate` 재시도 로직은 이전과 동일하다 (회귀 방지)
- [x] [GREEN] 생성자에 `productMetricsDailyRepository: ProductMetricsDailyRepository` 주입
- [x] [GREEN] `findOrCreateDaily(date: LocalDate, productId: Long): ProductMetricsDaily` private helper 추가
- [x] [GREEN] `handleCatalogEvent` 내부 `when` 분기에서 기존 `metrics.incrementXxx()` 호출 **직후**에 `daily.incrementXxx()` 호출 추가
- [x] [GREEN] `handleOrderEvent`도 동일하게 `daily.incrementSalesCount(quantity)` 추가
- [x] [GREEN] 두 Repository `save`는 기존 `productMetricsRepository.save(metrics)` 호출 **직후**에 호출
- [x] [GREEN] `rankingDate`와 `metricDate`는 **동일한 `LocalDate.now(clock)`** 지역 변수를 공유
- [x] 검증: `./gradlew :apps:commerce-streamer:test --tests "*.UpdateProductMetricsUseCaseTest"`

### Step 1 완료 조건

- `product_metrics_daily`가 실제로 적재되는 통합 테스트 통과
- 기존 누적 `product_metrics`, Redis ZSET, `FailedScoreUpdate` 동작 **완전 동일**
- ktlintCheck + test 통과

---

## Step 2 — `RankingPeriod` 도입 + `GetRankingUseCase` 시그니처 일괄 이행

설계 근거: 요구사항 §10.2, §10.10.
세션 분할: **2개 커밋** — (a) 구조적 시그니처 확장, (b) 회귀 방지 E2E 시나리오 확장. Step 2 전체 파일 수가 6개라 "≤ 5 / 세션" 규칙을 커밋 경계로 지키기 위해 E2E 확장을 b
커밋으로 분리한다.

**핵심 불변식**: 이 Step 완료 시점부터 `period=daily|weekly|monthly` 세 경로 모두 **200 정상 응답**을 반환한다. WEEKLY/MONTHLY의 본체 로직은 Step 4/5에서
구현되지만, 이 Step에서도 **빈 `PageResult` placeholder**를 돌려주어 "매 커밋마다 빌드/테스트/E2E 초록" 불변식을 유지한다. `TODO()` /
`NotImplementedError` / 500을 사용하지 않는다.

**periodKey 계산 책임 단일화**: UseCase 응답 타입을
`RankingPageResult(page: PageResult<RankingInfo>, period: RankingPeriod, periodKey: String)`로 확장해 **UseCase 내부에서만
periodKey를 계산**한다. Controller는 응답의 `periodKey`를 그대로 DTO에 전달하며 재계산하지 않는다. Step 2 단계에서는 각 분기 내부에 인라인 계산(`WeekFields.ISO` /
`YearMonth.from(...).toString()`)으로 두고, Step 4에서 `PeriodKeyCalculator` 유틸로 승격한다. `RankingPageResult`는
`GetRankingUseCase.kt` 내부 `data class`로 선언(별도 파일 없음).

### Commit ②-a (구조적) — 시그니처 확장 + placeholder 분기 (신규 1 + 수정 4 = 5개)

1. `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/RankingPeriod.kt` (신규)
2. `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/ranking/spec/RankingV1ApiSpec.kt` (수정) — **인터페이스 계약 갱신
   **. 프로젝트 규약(`interfaces/CLAUDE.md`)상 검증 어노테이션·파라미터 선언은 ApiSpec에만 한다
3. `apps/commerce-api/src/main/kotlin/com/loopers/application/ranking/GetRankingUseCase.kt`
4. `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/ranking/RankingV1Controller.kt`
5. `apps/commerce-api/src/test/kotlin/com/loopers/application/ranking/GetRankingUseCaseTest.kt`

작업 체크리스트:

- [x] [RED] `RankingPeriodTest` — `from("daily")` → DAILY, `"weekly"` → WEEKLY, `"monthly"` → MONTHLY
- [x] [RED] 대문자/혼합(`"Daily"`, `"WEEKLY"`, `"Monthly"`)은 `CoreException(BAD_REQUEST)`
- [x] [RED] 빈 문자열 / 알 수 없는 값은 `CoreException(BAD_REQUEST)`
- [x] [GREEN] `RankingPeriod` enum — `DAILY/WEEKLY/MONTHLY` +
  `companion object { fun from(value: String): RankingPeriod }`
- [x] [RED] `GetRankingUseCaseTest` — 기존 DAILY 테스트에 `period = RankingPeriod.DAILY`를 기계적으로 추가한 후 전부 통과 (회귀 방지)
- [x] [RED] `execute(period = DAILY)` 반환의 `periodKey`가 요청 `date`의 ISO 포맷(없으면 오늘 날짜)과 일치
- [x] [RED] `execute(period = WEEKLY)` 호출 시 **빈 `RankingPageResult`를 반환한다** (`page.content = emptyList()`,
  `page.totalElements = 0`, `periodKey`는 date 기반 `YYYY-Www` 포맷)
- [x] [RED] `execute(period = MONTHLY)` 호출 시 **빈 `RankingPageResult`를 반환한다** (동일 원칙, `periodKey`는 date 기반 `YYYY-MM` 포맷)
- [x] [GREEN] `GetRankingUseCase.kt` 내부에
  `data class RankingPageResult(val page: PageResult<RankingInfo>, val period: RankingPeriod, val periodKey: String)` 선언
- [x] [GREEN] `GetRankingUseCase.execute` 시그니처를
  `(date: LocalDate?, period: RankingPeriod, page: Int, size: Int): RankingPageResult`로 확장
    - `period`에 **기본값 두지 않는다** (§10.10)
    - 내부: `when (period) { DAILY -> executeDaily(...); WEEKLY -> executeWeekly(...); MONTHLY -> executeMonthly(...) }`
    - 각 분기는 `RankingPageResult`를 반환하며 자기 periodKey를 직접 계산해 담는다 (인라인 계산, Step 4에서 유틸로 승격)
    - `executeDaily`는 기존 `execute` 본문을 그대로 추출한 뒤 마지막에 `RankingPageResult(pageResult, DAILY, dateIso)`로 감싼다
    - `executeWeekly`, `executeMonthly`는 **빈 `PageResult(emptyList(), 0, page, size)` + 계산된 periodKey**로 placeholder
      반환 (Step 4/5에서 pageResult만 실제 조회 결과로 교체)
- [x] [GREEN] `RankingV1ApiSpec`에 `@RequestParam(required = false) period: String?` 파라미터 추가 — 프로젝트 규약상 모든
  `@RequestParam` 선언은 ApiSpec에만 둔다
- [x] [GREEN] `RankingV1Controller`는 ApiSpec의 override로 `period` 인자를 받아 `RankingPeriod.from(period ?: "daily")`로 정규화한 뒤
  UseCase에 전달. Controller 본체에는 검증 어노테이션을 넣지 않는다
- [x] [GREEN] Controller는 `RankingPageResult`에서 `period`/`periodKey`를 **그대로 DTO에 주입**한다 — periodKey 재계산 금지 (Step 6에서도 동일
  원칙)
- [x] [GREEN] `GetRankingUseCaseTest`의 모든 `execute(date = ..., page = ..., size = ...)` 호출에
  `period = RankingPeriod.DAILY` 명시 추가 (named argument라 기계적). 반환 비교는 `result.page.content` 등으로 갱신
- [x] 검증: `./gradlew :apps:commerce-api:test --tests "*.RankingPeriodTest" --tests "*.GetRankingUseCaseTest"`

### Commit ②-b (회귀 방지 E2E) — 세 period 동선 검증 (수정 1개)

1. `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/ranking/RankingApiE2ETest.kt`

작업 체크리스트:

- [x] [RED] E2E — `GET /api/v1/rankings?period=weekly` → **200 빈 목록** (placeholder, 아직 DTO에 periodKey 필드 없음)
- [x] [RED] E2E — `GET /api/v1/rankings?period=monthly` → **200 빈 목록** (placeholder)
- [x] [RED] E2E — `period=Daily` (대문자 혼합) → 400 BAD_REQUEST
- [x] [RED] E2E — `period` 생략 시 기존 DAILY 동선과 완전히 동일 (회귀 방지)
- [x] `RankingApiE2ETest` 기존 시나리오 전부 통과 (회귀 방지)
- [x] 검증: `./gradlew :apps:commerce-api:test --tests "*.RankingApiE2ETest"`

### Step 2 완료 조건

- `period` 파라미터 생략 시 → `daily`로 해석되어 R9 동작 그대로
- `period=daily` 명시 → R9와 동일 동작
- `period=weekly` / `period=monthly` → **200 빈 목록 응답** (Step 4/5에서 실제 MV 조회로 교체 예정)
- `period=DAILY` / `Daily` (대문자 혼합) → 400 BAD_REQUEST
- `period=unknown` 등 알 수 없는 값 → 400 BAD_REQUEST
- **이 Step 완료 시점에도 빌드/테스트/E2E 모두 초록** — 브랜치 내부 중간 커밋도 안전한 상태

---

## Step 3 — 주간 MV 테이블 + 조회 Repository (commerce-api)

설계 근거: 요구사항 §11.2, §Step 3.
세션 분할: **1개 커밋**.

변경 파일 (신규 5개):

1. `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/model/WeeklyProductRank.kt` — 조회 전용 VO
2. `apps/commerce-api/src/main/kotlin/com/loopers/domain/ranking/repository/WeeklyRankingRepository.kt` — 인터페이스
3. `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/ranking/MvProductRankWeeklyEntity.kt`
4. `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/ranking/WeeklyRankingRepositoryImpl.kt` (JpaRepository
   내부 선언 포함)
5. `apps/commerce-api/src/test/kotlin/com/loopers/domain/ranking/FakeWeeklyRankingRepository.kt`

작업 체크리스트:

- [x] [RED] `FakeWeeklyRankingRepository` 단위 테스트 — `findAllByPeriodKey(periodKey)`가 rank_no 오름차순으로 최대 100건 반환
- [x] [RED] 존재하지 않는 periodKey는 빈 목록
- [x] [RED] 동일 periodKey에 100건 이하만 저장되어 있으면 그 수만큼 반환
- [x] [GREEN] `WeeklyProductRank` VO —
  `rank, productId, score, viewCount, likeCount, salesCount, periodKey, periodStartDate, periodEndDate`
- [x] [GREEN] `WeeklyRankingRepository` 인터페이스 — **`findAllByPeriodKey(periodKey: String): List<WeeklyProductRank>` 단일
  메서드**
    - `count*` 메서드는 두지 않는다. `totalElements` 계산은 UseCase가 active 필터 이후에 수행 (Step 4, daily 경로와 일관)
    - Top 100 상한이 고정이므로 전체 로드 비용이 ≤ 100건으로 경계되어 있다
    - JPQL/NativeQuery 금지, 메서드명 쿼리(`findTop100ByPeriodKeyAndDeletedAtIsNullOrderByRankNoAsc`) 사용
- [x] [GREEN] `MvProductRankWeeklyEntity` — `BaseEntity` + `(period_key, product_id)` 유니크 + `(period_key, rank_no)` 유니크
- [x] [GREEN] `WeeklyRankingRepositoryImpl` — 도메인 ↔ 엔티티 변환
- [x] JPA 경로 스모크 통합 테스트 1개 (periodKey별 100건 저장 → `findAllByPeriodKey` 정상 반환)
- [x] 검증: `./gradlew :apps:commerce-api:test --tests "*.WeeklyRankingRepositoryTest"`

### Step 3 완료 조건

- 조회 경로만 완성. 적재 경로는 Step 8에서 배치가 담당
- Repository 인터페이스는 `findAllByPeriodKey` 하나만 공개 — active 필터링은 UseCase 책임
- Fake 기반 단위 테스트 통과

---

## Step 4 — 주간 조회 분기 연결 (`executeWeekly`)

변경 파일 (신규 1 + 수정 2 = 3개):

1. `apps/commerce-api/src/main/kotlin/com/loopers/application/ranking/PeriodKeyCalculator.kt` (신규) — `weekly(date)` +
   `monthly(date)` 두 메서드를 함께 선언. Step 5에서 그대로 재사용한다
2. `apps/commerce-api/src/main/kotlin/com/loopers/application/ranking/GetRankingUseCase.kt`
3. `apps/commerce-api/src/test/kotlin/com/loopers/application/ranking/GetRankingUseCaseTest.kt`

**핵심 원칙**: daily 경로(`scanTotalVisibleCount` + `fetchVisibleRankings`)와 동일한 구조로 **active 필터 후 count**를 직접 계산한다.
Repository의 단순 count를 사용하지 않는다 — 요구사항 §6.1 `totalElements` 의미("MV 저장 행 중 active 상품 건수")를 UseCase가 보장한다.

작업 체크리스트:

- [ ] [RED] `period = WEEKLY` 호출 시 `date`가 속한 주의 `periodKey`로 MV 전체를 로드 후 active 상품만 반환
- [ ] [RED] 비활성/삭제 상품은 응답에서 제외되며 over-fetch 하지 않는다 (§5) — 필터링으로 페이지가 부족해져도 자연 축소
- [ ] [RED] `periodKey`는 `java.time.WeekFields.ISO`로 계산된 `YYYY-Www` 포맷
- [ ] [RED] `totalElements`는 "active 필터링 후 남은 행 수"와 정확히 일치 (daily의 `scanTotalVisibleCount` 의미와 일관)
- [ ] [RED] MV에 100건이 있고 30건이 비활성 상품이면 `totalElements == 70`
- [ ] [RED] ISO Week 경계 테스트: `2025-12-29` → `2026-W01`, `2026-04-13` → `2026-W15`
- [ ] [RED] `date`를 오늘이 아닌 과거로 주면 해당 주의 periodKey로 조회된다
- [ ] [GREEN] `PeriodKeyCalculator.kt` 신규 — `weekly(date)` + `monthly(date)` 두 메서드 동시 선언 (Step 5 재사용 대비)
- [ ] [GREEN] Step 2의 `executeWeekly` / `executeMonthly`에 들어 있던 인라인 periodKey 계산을 `PeriodKeyCalculator` 호출로 **일괄 대체** (
  구조적 리팩토링). executeMonthly 본체 로직은 Step 5에서 교체되지만 유틸 호출부만 여기서 먼저 정리한다
- [ ] [GREEN] `GetRankingUseCase` 생성자에 `WeeklyRankingRepository` 주입
- [ ] [GREEN] `executeWeekly(date, page, size)` 구현:
  ```
  1. val periodKey = PeriodKeyCalculator.weekly(date ?: LocalDate.now(clock))
  2. val allRows: List<WeeklyProductRank> = weeklyRankingRepository.findAllByPeriodKey(periodKey)  // ≤ 100
  3. val productIds = allRows.map { ProductId(it.productId) }
  4. val activeProducts = readOnlyTxTemplate.execute { productRepository.findAllByIds(productIds).filter { it.isActive() }.associateBy { it.id.value } }
  5. val visibleRows = allRows.filter { it.productId in activeProducts.keys }
  6. val totalElements = visibleRows.size.toLong()
  7. val pageSlice = visibleRows.drop(page * size).take(size)
  8. val content = pageSlice.map { row -> RankingInfo(rank = row.rank, ..., score = row.score) }
  9. return RankingPageResult(PageResult(content, totalElements, page, size), RankingPeriod.WEEKLY, periodKey)
  ```
    - 핵심: **MV 전체 로드(Top 100 상한이라 최대 100건) → active 필터 → count → 페이지 슬라이스** 순서
    - `rank` 필드는 MV에 사전 계산된 `rank_no` 그대로 사용 (daily처럼 오프셋 기반이 아님)
- [ ] [GREEN] `when(period)` 분기에서 Step 2의 빈 `RankingPageResult(empty, WEEKLY, periodKey)` placeholder를 `executeWeekly`
  호출로 **교체**
- [ ] 검증: `./gradlew :apps:commerce-api:test --tests "*.GetRankingUseCaseTest"`

### Step 4 완료 조건

- `GET /api/v1/rankings?period=weekly&date=20260413` → 200, 빈 목록 허용 (아직 배치 미실행)
- MV에 데이터가 있을 때 `totalElements`가 active 필터링을 정확히 반영
- ISO Week 경계(연말/연초) 정상 처리

---

## Step 5 — 월간 MV Repository + `executeMonthly` 분기

세션 분할: **2개 커밋**. Step 3/4의 주간 구조를 복제하되 periodKey 계산만 `YearMonth` 기반으로 교체한다.

### Commit ⑤-a — 월간 MV Repository 추가 (신규 5개)

1. `apps/commerce-api/.../domain/ranking/model/MonthlyProductRank.kt`
2. `apps/commerce-api/.../domain/ranking/repository/MonthlyRankingRepository.kt`
3. `apps/commerce-api/.../infrastructure/ranking/MvProductRankMonthlyEntity.kt`
4. `apps/commerce-api/.../infrastructure/ranking/MonthlyRankingRepositoryImpl.kt` (JpaRepository 내부 선언 포함)
5. `apps/commerce-api/src/test/.../domain/ranking/FakeMonthlyRankingRepository.kt`

작업 체크리스트:

- [ ] [RED] `FakeMonthlyRankingRepository.findAllByPeriodKey(periodKey)`가 rank_no 오름차순 최대 100건 반환
- [ ] [RED] 존재하지 않는 periodKey는 빈 목록
- [ ] [GREEN] `MonthlyProductRank` VO (Weekly와 동일 필드 구성)
- [ ] [GREEN] `MonthlyRankingRepository` 인터페이스 — **`findAllByPeriodKey(periodKey: String): List<MonthlyProductRank>` 단일
  메서드**. `count*` 메서드 없음 (Step 3과 동일 원칙)
- [ ] [GREEN] `MvProductRankMonthlyEntity` — PK `(period_key, product_id)` 유니크 + `(period_key, rank_no)` 유니크
- [ ] [GREEN] `MonthlyRankingRepositoryImpl` — 메서드명 쿼리, 도메인 ↔ 엔티티 변환
- [ ] JPA 스모크 통합 테스트 1개
- [ ] 검증: `./gradlew :apps:commerce-api:test --tests "*.MonthlyRankingRepositoryTest"`

### Commit ⑤-b — `executeMonthly` 분기 연결 (수정 2개)

1. `apps/commerce-api/src/main/kotlin/com/loopers/application/ranking/GetRankingUseCase.kt`
2. `apps/commerce-api/src/test/kotlin/com/loopers/application/ranking/GetRankingUseCaseTest.kt`

작업 체크리스트:

- [ ] [RED] `period = MONTHLY` 호출 시 `date`가 속한 달의 periodKey로 조회 후 active 상품만 반환
- [ ] [RED] YearMonth 경계: 윤년 `2024-02-29`, 31일 월, 30일 월, 28일 2월 모두 정상
- [ ] [RED] `periodKey` 포맷은 `YYYY-MM` (`YearMonth.toString()`과 일치)
- [ ] [RED] `totalElements`는 active 필터링 후 남은 행 수 (Step 4와 동일 원칙)
- [ ] [RED] MV에 100건이 있고 40건이 비활성 상품이면 `totalElements == 60`
- [ ] [GREEN] `GetRankingUseCase` 생성자에 `MonthlyRankingRepository` 주입
- [ ] [GREEN] `executeMonthly(date, page, size)` 구현 — Step 4의 `executeWeekly`와 동일 순서(MV 전체 로드 → active 필터 → count → 페이지
  슬라이스). `rank` 필드는 MV에 사전 계산된 `rank_no` 사용. `PeriodKeyCalculator.monthly`는 Step 4에서 이미 도입된 유틸을 재사용한다
- [ ] [GREEN] 반환 타입은 `RankingPageResult(PageResult(...), RankingPeriod.MONTHLY, periodKey)`
- [ ] [GREEN] `when(period)` 분기에서 Step 2의 빈 `RankingPageResult(empty, MONTHLY, periodKey)` placeholder를 `executeMonthly`
  호출로 **교체**
- [ ] 검증: `./gradlew :apps:commerce-api:test --tests "*.GetRankingUseCaseTest"`

### Step 5 완료 조건

- `GET /api/v1/rankings?period=monthly&date=20260413` → 200 (아직 배치 미실행 시 빈 목록)
- daily / weekly / monthly 세 경로 모두 회귀 없음
- `totalElements`가 active 필터링을 정확히 반영

---

## Step 6 — API 응답 DTO 확장 (`period`, `periodKey`)

설계 근거: 요구사항 §6.1, §10.2.
세션 분할: **1개 커밋**.

**periodKey 계산 책임**: UseCase가 Step 2/4/5에서 이미 `RankingPageResult`에 period/periodKey를 담아 반환하도록 확정돼 있다. 이 Step은 **DTO 필드
노출과 Controller의 단순 전달**만 수행한다 — Controller는 periodKey를 재계산하지 않는다.

변경 파일 (수정 3개):

1. `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/ranking/dto/RankingV1Dto.kt`
2. `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/ranking/RankingV1Controller.kt`
3. `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/ranking/RankingApiE2ETest.kt`

작업 체크리스트:

- [ ] [RED] E2E — `period=daily` 응답에 `"period": "daily"`, `"periodKey": "2026-04-13"` 포함
- [ ] [RED] E2E — `period=weekly` 응답에 `"periodKey": "2026-W15"` 포함
- [ ] [RED] E2E — `period=monthly` 응답에 `"periodKey": "2026-04"` 포함
- [ ] [RED] E2E — `period` 생략 시 응답은 `"period": "daily"` + ISO date periodKey
- [ ] [RED] E2E — `period=Daily`(대문자 혼합) → 400
- [ ] [GREEN] `RankingV1Dto.RankingPageResponse`에 `period: String`, `periodKey: String` 필드 추가
    - `RankingV1Dto.RankingResponse`(content 아이템)는 **손대지 않는다** (§10.2 최소 침습)
    - 팩토리 시그니처는 `from(rankingPageResult: RankingPageResult)` — 내부에서 `page`, `period.name.lowercase()`, `periodKey`를 직접
      꺼내 매핑
- [ ] [GREEN] Controller는 `useCase.execute(...)`가 반환한 `RankingPageResult`를 **계산 없이** DTO 팩토리로 넘긴다 — periodKey 재계산 금지 (
  UseCase 단일 책임)
- [ ] 검증: `./gradlew :apps:commerce-api:test --tests "*.RankingApiE2ETest"`

### Step 6 완료 조건

- 세 period 모두 응답 계약 충족
- 기존 R9 클라이언트 호환 (`period`, `periodKey` 필드 무시해도 깨지지 않음)

---

## Step 7 — `commerce-batch` 모듈 Spring Batch 인프라 구성

설계 근거: 요구사항 §12, §Step 1.
세션 분할: **탐색 선행 → 1개 커밋**.

### Pre-task — 현재 상태 파악

- [ ] `apps/commerce-batch` 디렉토리 구조 확인 (build.gradle, main/resources, main/kotlin)
- [ ] `@SpringBootApplication`, DataSource 설정 존재 여부
- [ ] `modules/jpa` 재사용 가능 여부
- [ ] `spring-boot-starter-batch` 의존성 유무
- [ ] Spring Batch 메타 스키마 자동 초기화 설정 여부 (`spring.batch.jdbc.initialize-schema`)
- [ ] 기존 Job이 이미 등록되어 있는지 확인

변경 파일 (탐색 결과에 따라 1~3개):

1. `apps/commerce-batch/build.gradle.kts` — starter-batch 추가 (필요 시)
2. `apps/commerce-batch/src/main/resources/application.yml` — Batch 스키마 자동 초기화
3. `apps/commerce-batch/src/main/kotlin/com/loopers/config/BatchConfig.kt` — 필요 시 (Spring Boot 3에서는
   `@EnableBatchProcessing` 자동)

작업 체크리스트:

- [ ] 탐색 결과 요약 후 변경 파일이 5개를 넘을 것 같으면 여기서 멈추고 재분할
- [ ] 스모크 테스트: 빈 Job 하나를 등록하고 `@SpringBootTest`로 `JobLauncher` 주입 확인
- [ ] 검증: `./gradlew :apps:commerce-batch:test --tests "*.BatchInfraTest"`

### Step 7 완료 조건

- commerce-batch 모듈에서 `JobLauncher`, `JobRepository`, `TransactionManager` 정상 주입
- Batch 메타 테이블(`BATCH_JOB_INSTANCE`, ...)이 DB에 존재

---

## Step 8 — `WeeklyRankingJob` 구현 (단일 Tasklet Step)

설계 근거: 요구사항 §10.8, §12 업데이트 반영.
세션 분할: **2개 커밋**.

### 구조 결정 (왜 Chunk가 아니라 Tasklet인가)

- **Chunk-Oriented는 이 Job에 맞지 않는다**: Writer가 청크마다 호출되므로 "전체 집계 → Top 100 선별 → 단일 트랜잭션 replace"를 한 Step으로 구현하려면
  ExecutionContext 또는 `@JobScope` 상태 공유가 필요하여 복잡도가 폭발한다.
- **Top 100은 고정 상한**이라 DB에서 `LIMIT 100` 한 번으로 해결되며 메모리 부담이 0에 수렴한다.
- 따라서 **DB-level GROUP BY + 단일 Tasklet**으로 구성한다. 요구사항 §10.8이 이미 "Chunk 기본, Tasklet 허용"으로 명시되어 있어 정책 변경은 필요 없다.
- 트랜잭션 경계는 **Tasklet 1개 = 1 트랜잭션 = SELECT GROUP BY + DELETE + INSERT**가 모두 원자적으로 수행되는 구조.

### Commit ⑧-a — Job 뼈대 + JobParameter 검증 (신규 3)

1. `apps/commerce-batch/.../ranking/WeeklyRankingJobConfig.kt` — `@Configuration`, Job 등록, Tasklet Step placeholder
2. `apps/commerce-batch/.../ranking/WeeklyRankingJobParameterValidator.kt` — `JobParametersValidator` 구현
3. `apps/commerce-batch/src/test/.../WeeklyRankingJobParameterValidatorTest.kt`

- [ ] [RED] `baseDate` 누락 시 `JobParametersInvalidException`
- [ ] [RED] `baseDate` 형식 오류(`yyyyMMdd`가 아님) 시 예외
- [ ] [RED] 정상 `baseDate`에 대해 `WeekFields.ISO`로 `(periodKey, startDate, endDate)`가 계산된다
- [ ] [GREEN] Validator 구현 (입력 검증 + 윈도우 계산 유틸 분리)
- [ ] [GREEN] `WeeklyRankingJob`을 빈 Tasklet Step으로 등록 (이 커밋에서는 Tasklet 내부는 no-op)
- [ ] 검증: `./gradlew :apps:commerce-batch:test --tests "*.WeeklyRankingJobParameterValidatorTest"`

### Commit ⑧-b — Tasklet 본체 구현 (Top 100 replace)

1. `apps/commerce-batch/.../ranking/WeeklyRankingTasklet.kt` — Tasklet 본체
2. `apps/commerce-batch/.../ranking/WeeklyRankingQueryDao.kt` — QueryDSL 또는 Spring Data 기반 Top 100 조회 + replace DAO
3. `apps/commerce-batch/src/test/.../WeeklyRankingTaskletTest.kt` — 통합 테스트(`@SpringBootTest`)
4. `WeeklyRankingJobConfig.kt` — Tasklet 연결 (수정)

Tasklet 내부 로직(의사코드):

```kotlin
@Component
@StepScope
class WeeklyRankingTasklet(
    private val queryDao: WeeklyRankingQueryDao,
    @Value("#{jobParameters['baseDate']}") private val baseDate: String,
) : Tasklet {
    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val (periodKey, startDate, endDate) = WeeklyWindow.from(baseDate)
        val top100 = queryDao.selectTop100Aggregate(startDate, endDate, periodKey)
        // Tasklet 자체가 하나의 트랜잭션으로 수행됨
        queryDao.deleteByPeriodKey(periodKey)
        queryDao.bulkInsert(periodKey, top100)
        return RepeatStatus.FINISHED
    }
}
```

작업 체크리스트:

- [ ] [RED] Tasklet이 없는 periodKey에 대해 호출되면 → MV에 정확히 Top 100건이 insert된다 (소스가 100건 이상일 때)
- [ ] [RED] 소스 데이터가 50건이면 MV에는 50건만 insert된다 (상한만 적용)
- [ ] [RED] 가중치 계산이 `0.1·Σview + 0.2·Σlike + 0.7·Σsales`로 정확히 수행되며 `score == 0`인 상품은 제외된다
- [ ] [RED] 같은 점수일 때 `productId` 오름차순으로 `rank_no`가 부여되고 `(period_key, rank_no)` 유니크 제약이 충돌하지 않는다
- [ ] [RED] 이미 해당 periodKey에 이전 Top 100이 존재하면 → 재실행 시 **전체 교체**되며 이전에 있던 상품 중 이번 Top 100에 없는 상품은 MV에서 사라진다
- [ ] [RED] Tasklet 내부 예외(예: 중간에 Insert 실패) 발생 시 → **기존 Top 100이 그대로 유지**된다 (원자성)
- [ ] [GREEN] `WeeklyRankingQueryDao.selectTop100Aggregate`: DB-level GROUP BY 쿼리
    - 의사 SQL:
      `SELECT product_id, SUM(view_count), SUM(like_count), SUM(sales_count) FROM product_metrics_daily WHERE metric_date BETWEEN :start AND :end GROUP BY product_id HAVING 가중치 > 0 ORDER BY 가중치 DESC, product_id ASC LIMIT 100`
    - 구현: **QueryDSL로 고정**. 메서드명 쿼리는 `GROUP BY + HAVING + ORDER BY (가중치) + LIMIT 100` 조합을 표현할 수 없다. JPQL/NativeQuery는
      금지(CLAUDE.md)
    - 집계 결과 Projection 타입은 `WeeklyRankRow` Value Object
- [ ] [GREEN] `deleteByPeriodKey(periodKey)`: 해당 periodKey의 기존 행 전체 삭제
- [ ] [GREEN] `bulkInsert(periodKey, rows)`: Top 100을 `rank_no = 1..100`으로 부여하여 insert (JPA `saveAll` 또는 배치 insert)
- [ ] [GREEN] `WeeklyRankingTasklet` 구현 및 `WeeklyRankingJobConfig`에 연결
- [ ] [GREEN] Tasklet의 트랜잭션 경계는 Spring Batch가 기본 제공 (Tasklet당 1 트랜잭션). 별도 `@Transactional` 추가 불필요
- [ ] 검증: `./gradlew :apps:commerce-batch:test --tests "*.WeeklyRankingTaskletTest"`

### Step 8 완료 조건

- 주간 배치가 실제 `mv_product_rank_weekly`에 Top 100 적재
- 재실행(동일 `baseDate + run.id` 증가) 시 결과가 동일 — delete-insert 전체 교체
- 중간 실패 시 기존 Top 100이 그대로 유지 (Tasklet 원자성)
- DB-level GROUP BY로 처리하므로 일간 행이 수만 건이어도 메모리 부담 없음

---

## Step 9 — `MonthlyRankingJob` 구현 (단일 Tasklet Step)

세션 분할: **1개 커밋**. Step 8의 `WeeklyRankingJob` 구조를 복제하되 윈도우 계산만 `YearMonth` 기반으로 교체한다.

변경 파일 (신규 3~4개):

1. `apps/commerce-batch/.../ranking/MonthlyRankingJobConfig.kt`
2. `apps/commerce-batch/.../ranking/MonthlyRankingTasklet.kt`
3. `apps/commerce-batch/.../ranking/MonthlyRankingQueryDao.kt`
4. `apps/commerce-batch/src/test/.../MonthlyRankingTaskletTest.kt`

작업 체크리스트:

- [ ] [RED] `baseDate`가 월 중간일 때도 해당 월 전체(1일~말일)가 윈도우가 된다
- [ ] [RED] YearMonth 경계 케이스: 윤년 2월(2024-02-29), 31일 월(1/3/5/7/8/10/12), 30일 월(4/6/9/11), 28일 2월
- [ ] [RED] `periodKey` 포맷은 `YearMonth.toString()` 결과와 동일 (`YYYY-MM`)
- [ ] [RED] 월 범위로 DB-level GROUP BY가 정상 동작하고 Top 100이 적재된다
- [ ] [RED] 재실행 시 전체 교체(멱등성)
- [ ] [GREEN] `MonthlyWindow.from(baseDate)` 유틸 (`YearMonth.from(LocalDate).atDay(1)` / `atEndOfMonth()`)
- [ ] [GREEN] `MonthlyRankingQueryDao` — `selectTop100Aggregate`, `deleteByPeriodKey`, `bulkInsert` (MV 테이블만
  `mv_product_rank_monthly`로 교체)
- [ ] [GREEN] `MonthlyRankingTasklet` — Weekly와 동일 구조, 주입받는 DAO와 Window만 교체
- [ ] [GREEN] `MonthlyRankingJobConfig` — Job/Step 등록
- [ ] **Rule of Three**: 공통 추출은 중복이 3번째 발생할 때 수행한다. weekly/monthly 두 개면 복제를 허용한다. R10 범위에서는 공통 Tasklet 추상화를 하지 않는다 —
  Tidy First 원칙
- [ ] 검증: `./gradlew :apps:commerce-batch:test --tests "*.MonthlyRankingTaskletTest"`

### Step 9 완료 조건

- 월간 배치가 실제 `mv_product_rank_monthly`에 Top 100 적재
- 재실행 시 전체 교체(멱등성) 보장
- 중간 실패 시 기존 Top 100 유지 (Tasklet 원자성)

---

## Step 10 (Nice-to-Have) — 스케줄링 & 운영 편의

- [ ] `@Scheduled(cron = "0 30 1 * * *", zone = "Asia/Seoul")` 또는 K8s CronJob 문서화
- [ ] `/internal/batch/ranking/weekly` / `/internal/batch/ranking/monthly` 수동 트리거 (내부 전용)
- [ ] Micrometer 메트릭: 처리 건수, 소요 시간, 실패 여부
- [ ] 스케줄링은 테스트 환경에서 **자동 비활성화** (`@ConditionalOnProperty` 또는 프로파일 가드)

---

## 공통 검증 체크리스트 (모든 Step 완료 후 1회)

- [ ] `./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test`
- [ ] `./gradlew :apps:commerce-streamer:ktlintCheck && ./gradlew :apps:commerce-streamer:test`
- [ ] `./gradlew :apps:commerce-batch:ktlintCheck && ./gradlew :apps:commerce-batch:test`
- [ ] E2E: `period=daily` 요청이 R9와 동일 동작 (회귀 방지)
- [ ] E2E: `period=weekly` + 배치 실행 후 Top 100 응답
- [ ] E2E: `period=monthly` + 배치 실행 후 Top 100 응답
- [ ] `product_metrics_daily` → 배치 → MV → API 전체 흐름 스모크
- [ ] ISO Week 경계 수동 확인: `2025-12-29` → `2026-W01`

---

## 세션/커밋 경계 요약

| Step | 커밋 수 | 파일 수  | 권장 세션               |
|------|------|-------|---------------------|
| 1    | 2    | 6 + 2 | 세션 A                |
| 2    | 2    | 5 + 1 | 세션 A 말미 → 세션 B 초    |
| 3    | 1    | 5     | 세션 B                |
| 4    | 1    | 1 + 2 | 세션 B                |
| 5    | 2    | 5 + 2 | 세션 C                |
| 6    | 1    | 3     | 세션 C                |
| 7    | 1    | 1~3   | 세션 D (탐색 선행)        |
| 8    | 2    | 3 + 4 | 세션 D~E              |
| 9    | 1    | 3~4   | 세션 E                |
| 10   | 1    | 2~3   | 세션 F (Nice-to-Have) |

- 각 세션은 **ktlint + test 통과 + 1~2회 커밋** 후 종료
- 세션 간 전환 시 `.claude/handoff.md`를 남기거나 `/handoff` 스킬 사용
- **서브에이전트 위임이 유리한 Step**: 3, 5, 8-b, 9 (Repository / Infrastructure 보일러플레이트 복제가 많음). executor 에이전트에 파일 경로 + 템플릿을 전달해 병렬
  처리 가능

---

## 의도적으로 제외한 범위

- Technical Writing Quest / Retrospective (요구사항 §15)
- 상품 상세 API `rank`의 주/월 확장 (§10.9)
- `MetricsCounters` 공유 VO 추출 (§10.11 — R10 이후 별도 PR)
- 가중치 외부 설정화, Redis 기반 가중치 조절
- 주/월 배치 결과의 장기 보존/롤업 (분기, 연간)
- 배치 실패 Slack/Webhook 알림

새로 추가하고 싶으면 먼저 요구사항 문서에 반영한 뒤 plan.md에 Step을 추가하는 순서를 따른다.
