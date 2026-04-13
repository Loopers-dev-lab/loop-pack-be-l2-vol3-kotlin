# Round 10 — Spring Batch 기반 주간/월간 랭킹 통합 요구사항

## 배경

Round 9에서 구축한 랭킹 시스템은 Kafka Consumer가 이벤트를 소비할 때 Redis ZSET에 점수를 즉시 반영하고,
`GET /api/v1/rankings?date=yyyyMMdd` API가 해당 일자의 일간 랭킹을 조회하는 구조다.

이 구조는 "오늘의 인기 상품"처럼 실시간성이 중요한 읽기 모델에는 적합하지만,
주간/월간 랭킹처럼 긴 기간 집계를 요청 시점마다 계산하면 다음 문제가 생긴다.

- 조회 시점마다 긴 기간의 데이터를 합산해야 하므로 비용이 커진다.
- Redis 일간 키는 TTL 2일이라 주/월 집계의 근거 데이터로 쓰기 어렵다.
- 현재 코드베이스의 `product_metrics`는 `product_id` 기준 누적 스냅샷이라, 특정 주/월 범위만 정확히 재구성할 수 없다.

따라서 Round 10의 핵심은 다음 두 축이다.

1. 일간 랭킹은 기존 Redis 실시간 경로를 유지한다.
2. 주간/월간 랭킹은 Spring Batch로 유지하는 조회 전용 사전 집계 테이블에서 조회한다.

즉, 실시간 경로와 배치 경로를 역할별로 분리하되, 사용자에게는 단일 랭킹 API로 제공한다.

### 이 문서의 해석 기준

- 이 문서는 `request-10.md`, `req-10.md`의 과제 요구를 기준으로 하되, 현재 코드베이스 상태도 함께 반영한 구현 분석 문서다.
- 특히 원문 퀘스트는 `product_metrics`를 "일간 집계 정보"로 서술하지만, 현재 저장 구조가 실제로는 누적형이라 해석 충돌이 있다.
- 따라서 본 문서에서 제안하는 `product_metrics_daily`는 "원문 요구를 임의 변경"한 것이 아니라, **현재 코드베이스에서 원문 요구를 성립시키기 위한 보정 설계**로 본다.
- 반대로, 평가 기준이 "원문 퀘스트의 문면 자체"에 더 가깝다면 `product_metrics`를 그대로 일간 집계 테이블로 간주하는 해석도 가능하다.

#### 코드베이스 검증 결과 (2026-04-13)

"현재 `product_metrics`가 누적형"이라는 전제를 코드로 확인한 결과다. 이 전제가 바뀌면 §11·§Step 1 범위도 함께 바뀌므로, 스키마가 변경되면 이 박스를 갱신한다.

- **`apps/commerce-streamer/.../infrastructure/metrics/ProductMetricsEntity.kt`**: `product_metrics` 테이블은 `(product_id UNIQUE, view_count, like_count, sales_count)` 스키마다. **`metric_date` 컬럼이 존재하지 않으며** `product_id`가 유니크 제약이라 상품당 행이 1개뿐이다. 따라서 기간 재구성이 구조적으로 불가능하며, 본 문서의 `product_metrics_daily` 도입 판단이 정합성을 가진다.
- **`apps/commerce-streamer/.../application/metrics/UpdateProductMetricsUseCase.kt`**: 이벤트를 `@Transactional`로 받아 `findOrCreate(productId) + incrementXxx()` 방식으로 누적 갱신하고, `afterCommit` 훅에서 Redis 점수를 반영하며, 실패 시 `FailedScoreUpdate`로 재시도 큐에 넣는 3단 안정화 구조다. **R10의 `product_metrics_daily` 적재는 이 트랜잭션 안에 한 줄 추가**하는 편이 동일 `event_handled` 멱등 보호를 그대로 물려받을 수 있어 가장 안전하다.
- **컬럼명은 `sales_count`** — 도메인 모델 `ProductMetrics.incrementSalesCount(quantity)` 가 "판매 수량(=quantity)"을 누적한다. `sales_count`는 금액이 아닌 **수량 합산**이다.
- **가중치 상수명은 `RankingWeight.ORDER`** — 집계 컬럼(`sales`)과 점수 가중치(`order`)가 이원화되어 있지만 이는 기존 코드 컨벤션이다. 본 문서도 "데이터 = `sales_count`, 점수 가중치 표현 = `order`"의 분리를 그대로 계승한다. 네이밍 통일은 R10 범위 밖이다.
- **`GetRankingUseCase.execute(date, page, size)`**: 현재 R9 조회 경로는 `period` 파라미터가 없다. R10은 이 시그니처에 `period`를 추가하는 확장이며, **기존 0-based page·`date` 기본값 규칙은 그대로 유지한다**.
- **`rankingDate = LocalDate.now(clock)`**: R9는 이미 "Consumer 처리 시점의 KST 날짜"로 일별 키를 잡고 있다. R10 `product_metrics_daily`의 `metric_date`도 동일 기준을 사용하면 일자 경계 해석이 두 경로에서 자연스럽게 일치한다.
- **Flyway / `schema.sql` 없음**: `db/migration/` 디렉토리 자체가 없으며 JPA `ddl-auto` 기반으로 스키마가 관리된다. R10에서 추가하는 `product_metrics_daily` / `mv_product_rank_*` 는 Entity 선언으로 도입하되, 운영 환경 마이그레이션이 필요하면 별도 DDL 스크립트를 병행한다.

### 학습 목표

- Spring Batch의 Job / Step / Chunk-Oriented Processing을 실제 집계 문제에 적용한다.
- MySQL에는 네이티브 Materialized View가 없으므로, "별도 테이블 + 배치 적재" 방식의 MV 패턴을 구현한다.
- 배치 재실행 전략, JobParameter 설계, 멱등 적재 방식을 명확히 한다.
- 현재 코드베이스의 데이터 모델 한계를 발견하고, 배치 가능한 소스 구조를 설계한다.

### 키워드

- Spring Batch
- Chunk-Oriented Processing
- ItemReader / ItemProcessor / ItemWriter
- JobParameters / JobRepository
- MV 패턴 (조회 전용 사전 집계 테이블)
- Period Key
- Idempotent Re-run
- KST 기준 기간 집계
- 실시간 처리 vs 배치 처리

### 우선순위

**Must-Have**

- 배치 입력으로 사용할 일자별 집계 구조 확보
- 주간/월간 조회 전용 사전 집계 테이블 적재 Job 구현
- 랭킹 API의 기간 확장 (`daily`, `weekly`, `monthly`)
- 배치 재실행 시 결과 멱등성 보장
- Technical Writing Quest는 별도 산출물 범위이며, 이 문서는 구현 요구 분석에 집중한다.

**Nice-to-Have**

- 스케줄링과 운영용 수동 트리거
- Spring Batch restart/skip/retry 정책
- 배치 처리 메트릭 및 실패 알림
- periodKey/집계 윈도우 노출 개선

---

## 1. 문제 정의

### 핵심 목표

- 일간 랭킹은 Redis 기반 실시간 모델을 유지한다.
- 주간/월간 랭킹은 조회 전용 사전 집계 테이블을 조회해 빠르게 응답한다.
- 기간별 랭킹이 동일한 가중치 규칙과 시간대 규칙을 공유하도록 한다.
- 같은 기간을 다시 집계해도 결과가 달라지지 않도록 재실행 전략을 설계한다.

### 현재 시스템의 한계

| 관점 | 현재 상태 | Round 10 요구 |
|------|----------|---------------|
| 일간 랭킹 | Redis ZSET 기반 실시간 조회 가능 | 유지 |
| 장기 기간 조회 | Redis TTL 2일로 주/월 직접 계산 불가 | 조회 전용 사전 집계 테이블 기반 조회 필요 |
| 집계 원천 데이터 | `product_metrics`는 누적형 | 기간별 재집계 가능한 일간 데이터 필요 |
| API 계약 | `date` 기반 일간 조회만 존재 | `period` 기반 확장 필요 |

### 왜 배치가 필요한가

| 관점 | 문제 |
|------|------|
| 사용자 | 주간/월간 인기 상품도 빠르게 조회되길 기대한다 |
| 비즈니스 | 마케팅/리포트/대시보드 용도는 실시간보다 안정적 집계가 중요하다 |
| 시스템 | 요청마다 7일/30일 집계를 수행하면 DB/Redis 부하와 응답 시간 모두 악화된다 |

### 왜 소스 데이터 보강이 필요한가

| 관점 | 문제 |
|------|------|
| 현재 `product_metrics` | `product_id` 단위 누적값이라 특정 주/월 합산 근거로 부적합하다 |
| 배치 입력 | 최소한 `metric_date` 단위의 일간 집계가 필요하다 |
| 설계 선택 | 기존 누적 테이블은 유지하고, 일자별 집계 테이블을 별도로 두는 편이 안전하다 |

---

## 2. 유비쿼터스 언어

| 한글 | 영문 | 정의 |
|------|------|------|
| 일간 랭킹 | Daily Ranking | KST 기준 하루 단위 실시간 랭킹 |
| 주간 랭킹 | Weekly Ranking | KST 기준 한 주의 사전 집계 랭킹 |
| 월간 랭킹 | Monthly Ranking | KST 기준 한 달의 사전 집계 랭킹 |
| 기간 | Period | HTTP 계약에서는 `daily`, `weekly`, `monthly` |
| 기간 키 | Period Key | 주/월 집계를 식별하는 값. 예: `2026-W15`, `2026-04` |
| 기준일 | Base Date / Anchor Date | 집계 대상 주/월을 결정하는 날짜 |
| 일간 메트릭 | Daily Product Metrics | 상품의 하루치 view/like/sales 집계 |
| 집계 윈도우 | Aggregation Window | 배치가 읽는 기간 범위 |
| 조회 전용 사전 집계 테이블 | Precomputed Ranking Table | 조회 성능을 위해 미리 계산해 둔 랭킹 테이블. MySQL의 네이티브 MV가 아니라 MV 패턴으로 운영한다 |
| 재집계 | Rebuild | 동일 기간의 사전 집계 결과를 다시 계산해 교체하는 작업 |

---

## 3. 액터 정의

| 액터 | 식별 방식 | 권한 및 역할 |
|------|----------|------------|
| 비인증 사용자 | 없음 | 기간별 랭킹 조회 |
| 인증된 사용자 | JWT 토큰 | 랭킹 조회 + 조회/좋아요/주문 이벤트 발생 |
| commerce-streamer | Kafka Consumer | 이벤트 소비, 일간 메트릭 적재, Redis 일간 랭킹 갱신 |
| commerce-batch | Spring Batch 애플리케이션 | 주간/월간 조회 전용 사전 집계 테이블 적재 Job 실행 |
| Scheduler / 운영자 | Cron, JobParameter, 내부 트리거 | 정기 실행 또는 특정 기간 재실행 |
| commerce-api | REST API | Redis 또는 조회 전용 사전 집계 테이블을 읽어 통합 랭킹 응답 생성 |
| MySQL | 영속 저장소 | 일간 메트릭, 사전 집계 테이블, Spring Batch 메타데이터 저장 |
| Redis | 읽기 모델 저장소 | 일간 실시간 랭킹 저장 |

---

## 4. 유저 시나리오

### 4.1 주간/월간 랭킹 배치 실행

**사전 조건**

- 일자 단위 메트릭 테이블에 집계 데이터가 존재해야 한다.
- 실행 대상 기간은 KST 기준으로 계산한다.

**흐름**

1. Scheduler 또는 운영자가 `baseDate`를 기준으로 Job을 실행한다.
2. Job은 `baseDate`로 periodKey와 집계 윈도우(startDate, endDate)를 계산한다.
3. Reader가 해당 기간의 일간 메트릭을 읽는다.
4. Processor가 상품별 기간 합계와 랭킹 점수를 계산한다.
5. Writer가 동일 periodKey의 기존 결과를 교체하고 Top 100만 적재한다.
6. 실행 결과는 Spring Batch 메타 테이블에 기록된다.

### 4.2 기간별 랭킹 조회

**일간**

- `GET /api/v1/rankings?period=daily&date=20260413&size=20&page=0`
- 기존 Redis 일간 랭킹을 조회한다.

**주간**

- `GET /api/v1/rankings?period=weekly&date=20260413&size=20&page=0`
- `2026-04-13`이 속한 주의 `periodKey`로 주간 사전 집계 테이블을 조회한다.

**월간**

- `GET /api/v1/rankings?period=monthly&date=20260413&size=20&page=0`
- `2026-04-13`이 속한 달의 `periodKey`로 월간 사전 집계 테이블을 조회한다.

### 4.3 재실행 시나리오

- 소스 데이터 누락이나 배치 실패가 있었던 경우, 운영자는 동일 기간을 다시 집계할 수 있어야 한다.
- Spring Batch 실행 식별자는 `run.id`로 분리하되, 데이터 결과는 동일 periodKey 교체 전략으로 멱등성을 보장한다.

### 4.4 예외 흐름

| 조건 | 응답/처리 | 설명 |
|------|-----------|------|
| 잘못된 `period` | 400 BAD_REQUEST | 허용값은 `daily`, `weekly`, `monthly` |
| 잘못된 `date` 형식 | 400 BAD_REQUEST | `yyyyMMdd` 형식만 허용 |
| 해당 periodKey 데이터 없음 | 빈 목록 | 배치 미실행 또는 데이터 없음 |
| `page`/`size` 범위 오류 | 400 BAD_REQUEST | 기존 페이지 정책 유지 |
| `page * size > 100` | 빈 페이지 | 주/월 사전 집계 테이블은 Top 100만 저장 |
| `baseDate` 누락 | Job 실패 | JobParameter 검증 단계에서 차단 |

### 4.5 원문 퀘스트와의 차이 명시

- 원문 퀘스트는 `product_metrics`를 배치 대상 테이블로 직접 지목한다.
- 본 문서는 현재 코드 구조를 근거로, 그 지시를 그대로 구현하기보다 `product_metrics_daily`를 추가하는 해석을 택한다.
- 따라서 이 문서는 "과제 원문 요약"이 아니라 "과제 원문 + 현재 저장 구조를 함께 고려한 구현 분석"이다.

---

## 5. 도메인 규칙

### 기간 규칙

- 서비스 시간대는 KST(`Asia/Seoul`) 기준이다.
- `daily`: 전달된 `date` 하루를 의미한다.
- `weekly`: 전달된 `date`가 포함된 ISO 주간(월요일 시작, 일요일 종료)이다.
- `monthly`: 전달된 `date`가 포함된 달 전체다.

### 점수 규칙

- 기간별 랭킹 점수는 R9와 동일한 가중치를 사용한다.
- `score = 0.1 × viewCount + 0.2 × likeCount + 0.7 × salesCount`
- **이벤트가 전혀 없는 상품(`score == 0`)은 사전 집계 테이블에 적재하지 않는다.** R9 실시간 경로는 ZINCRBY로 음수 점수가 누적될 수 있지만(like_removed 어뷰징 방지), R10 소스인 `product_metrics_daily`는 non-negative 카운터이므로 재집계 점수는 항상 0 이상이다. 따라서 이 규칙은 **"어뷰징 방지"가 아니라 "이벤트 0건 상품 배제"** 이며, R9의 음수 점수 정책과는 충돌하지 않는다 — 층위가 다르다.
- 디버깅과 운영 확인을 위해 `viewCount`, `likeCount`, `salesCount` 원본 합계도 사전 집계 테이블에 저장한다.
- 동점일 경우 `productId` 오름차순으로 타이 브레이킹한다. 이 규칙은 `(period_key, rank_no)` 유니크 인덱스가 충돌 없이 1~100까지 유일하게 채워지도록 보장한다.

### 소스 데이터 규칙

- 현재 코드 기준으로 `product_metrics`는 누적형(상품당 1행, `metric_date` 없음)이므로 주/월 재집계 소스로 직접 사용하지 않는다.
- 배치 입력은 신규 `product_metrics_daily`를 기준으로 한다. PK는 `(metric_date, product_id)`, 적재 방식은 **upsert**(동일 일자의 같은 상품에 이벤트가 여러 번 오면 카운터 누적).
- `metric_date`는 Consumer 처리 시점의 **KST 기준 `LocalDate.now(clock)`**을 사용한다. 이 기준은 R9의 `UpdateProductMetricsUseCase`가 `rankingDate`를 잡을 때 이미 사용 중이며, 이벤트 payload에 `occurredAt`이 없는 현실을 그대로 계승한다. 두 경로의 "오늘" 정의가 항상 일치한다.
- commerce-streamer는 기존 누적 `product_metrics` 갱신과 **동일 `@Transactional` 경계 안에서** 일간 집계 테이블도 함께 저장한다. 이렇게 해야 두 테이블이 같은 `event_handled` 멱등 키로 보호된다.
- `sales_count`는 **수량 합산**이다(금액 아님). 가중치 상수 `RankingWeight.ORDER`가 `sales_count`에 곱해져 점수가 만들어진다는 점을 주의한다.
- 만약 교육용 평가가 원문 퀘스트 문면을 우선한다면, `product_metrics` 자체를 일간 집계 테이블로 간주하는 단순 해석도 가능하다. 그 경우 본 절의 `product_metrics_daily` 관련 설계는 모두 무효화된다.

### 사전 집계 테이블 적재 규칙

- 각 periodKey마다 상위 100건만 저장한다.
- 적재 전략은 "해당 periodKey 전체 교체"를 기본으로 한다.
- 동일 기간 재실행 시 결과가 누적되지 않아야 한다.

### 조회 규칙

- `daily`는 Redis, `weekly/monthly`는 조회 전용 사전 집계 테이블을 조회한다.
- **비활성/삭제 상품은 응답 시점에 필터링한다** (배치 적재 시점에 사전 제외하지 않는다). 근거:
  - R9 daily 경로가 이미 `GetRankingUseCase.fetchVisibleRankings`에서 응답 시점 필터링을 채택하고 있어 두 경로의 정책을 일치시키면 혼란이 없다.
  - 배치 적재 시점 제외 방식은 배치 주기(최대 24시간) 안에 상품이 비활성화되면 여전히 stale 데이터를 노출한다.
  - 응답 필터링으로 인해 페이지 크기가 모자라도 **그대로 허용한다** (over-fetch나 재정렬 없이 자연 축소). daily·weekly·monthly 모두 동일 정책.
- 기존 상품 상세 API의 `rank`는 Round 10 범위에서는 일간 랭킹만 유지한다.

---

## 6. API 명세

### 6.1 랭킹 조회

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | /api/v1/rankings | 불필요 | 일간/주간/월간 랭킹 조회 |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| period | String | N | `daily` | `daily` \| `weekly` \| `monthly`. **소문자만 허용**, 그 외 값은 400 BAD_REQUEST |
| date | String | N | 오늘(KST) | `yyyyMMdd` 포맷(BASIC_ISO_DATE). `weekly/monthly`는 이 날짜가 속한 주/월로 해석 |
| size | Int | N | 20 | 페이지당 항목 수. `@Positive @Max(100)` |
| page | Int | N | 0 | 0-based 페이지 번호. `@PositiveOrZero` |

**응답 예시**

```json
{
  "meta": {
    "result": "SUCCESS",
    "errorCode": null,
    "message": null
  },
  "data": {
    "content": [
      {
        "rank": 1,
        "productId": 101,
        "productName": "인기 상품 A",
        "price": 29900,
        "score": 342.7
      }
    ],
    "period": "weekly",
    "periodKey": "2026-W15",
    "page": 0,
    "size": 20,
    "totalElements": 100
  }
}
```

**응답 필드 — 신규/변경 사항**

| 필드 | 타입 | 설명 |
|------|------|------|
| `data.period` | String | 해석된 기간 유형. 요청에서 생략해도 응답에는 항상 포함한다 |
| `data.periodKey` | String | 기간 식별자. period별 포맷이 다르다 (아래 표) |
| `data.totalElements` | Long | period별 계산 경로는 다르지만 "페이지네이션의 전체 크기"라는 의미는 동일 (아래 설명) |
| `content[].rank` | Int | daily는 페이지 오프셋 기반(1-based), weekly/monthly는 MV에 사전 계산된 `rank_no` |

`RankingV1Dto.RankingPageResponse`에 `period`, `periodKey` 필드를 신규 추가한다. `RankingV1Dto.RankingResponse`(content 아이템)는 **변경하지 않는다** — 기존 클라이언트 호환성을 최대한 보존하고, period별로 중복되는 메타 값을 아이템마다 돌려보내는 낭비를 피하기 위함이다.

**`periodKey` 포맷**

| period | 포맷 | 예시 |
|--------|------|------|
| `daily` | `yyyy-MM-dd` | `2026-04-13` |
| `weekly` | `YYYY-Www` | `2026-W15` |
| `monthly` | `YYYY-MM` | `2026-04` |

`periodKey`는 요청 `date`가 속한 주/월로 해석하며, daily의 경우 해당 `date`의 ISO 포맷을 그대로 돌려준다. `date`를 생략하면 오늘 날짜(KST)를 기준으로 동일 규칙이 적용된다.

**`totalElements` 의미 (period별)**

- **daily**: R9 `GetRankingUseCase.computeTotalVisibleCount` 로직을 그대로 유지한다. 즉 **"해당 날짜 ZSET의 `score > 0` 항목 중 active 상품 건수"** 이며 30초 캐시가 적용된다. Top 100 고정값이 아니다.
- **weekly / monthly**: 해당 `periodKey`의 MV 저장 행 중 **active 상품 건수**. MV가 Top 100까지만 저장하므로 상한은 100이지만, 비활성/삭제 상품 필터링으로 그보다 작아질 수 있다.
- period별 계산 경로는 다르지만, 클라이언트가 사용하는 "전체 페이지 수 계산"이라는 의미는 동일하다.

**daily 응답 예시**

`GET /api/v1/rankings?period=daily&date=20260413`

```json
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": {
    "content": [
      { "rank": 1, "productId": 101, "productName": "인기 상품 A", "price": 29900, "score": 1.8 }
    ],
    "period": "daily",
    "periodKey": "2026-04-13",
    "page": 0,
    "size": 20,
    "totalElements": 37
  }
}
```

### 6.2 호환성 규칙

- `period`를 생략하면 기존과 동일하게 일간 랭킹을 조회한다.
- 기존 `date/page/size`와 0-based page 규칙은 유지한다.
- 원문 예시에는 `page=1`이 나오지만, 현재 프로젝트 API 관례를 따라 본 문서는 0-based page를 기준으로 해석한다.
- 상품 상세 API는 일간 랭킹만 유지해 범위를 통제한다.

### 6.3 운영용 트리거

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| POST | /internal/batch/ranking/weekly | 내부 전용 | `baseDate` 기준 주간 랭킹 Job 실행 |
| POST | /internal/batch/ranking/monthly | 내부 전용 | `baseDate` 기준 월간 랭킹 Job 실행 |

이 항목은 Nice-to-Have다. `commerce-batch` 모듈 내부에 두는 것을 권장한다.

---

## 7. 인증/인가

| 경로 | 인증 요구 |
|------|----------|
| GET /api/v1/rankings | 불필요 |
| POST /internal/batch/ranking/** | 내부 전용 |

---

## 8. 기존 시스템과의 관계

### 기존 완료 (재사용)

- **Redis 일간 랭킹**: `ranking:all:{yyyyMMdd}` 기반 일간 실시간 랭킹
- **랭킹 가중치 정책**: view=0.1, like=0.2, order=0.7
- **이벤트 소비 흐름**: Kafka Consumer → `UpdateProductMetricsUseCase`
- **일간 랭킹 안정화 로직**: after-commit Redis 반영 + 실패 적재(`FailedScoreUpdate`)
- **commerce-batch 모듈**: 별도 배치 애플리케이션 존재

### 현재 한계

- **`product_metrics` 누적 구조**: 기간 재구성이 안 된다
- **랭킹 Repository 추상화**: 현재는 일간 Redis 조회 중심이다
- **Redis TTL**: 2일 보존이라 주/월 집계 소스로 부적합하다

### 신규 구현

- **일간 집계 소스**: `product_metrics_daily`
- **주간/월간 사전 집계 테이블**: `mv_product_rank_weekly`, `mv_product_rank_monthly`
- **배치 Job**: `weeklyRankingJob`, `monthlyRankingJob`
- **기간 도메인**: `RankingPeriod`, periodKey 계산 규칙
- **조회 분기**: `daily`는 Redis, `weekly/monthly`는 사전 집계 테이블

---

## 9. 잠재 리스크

| 리스크 | 영향 | 현재 대응 | 향후 대응 |
|--------|------|----------|----------|
| 소스 데이터가 누적형뿐임 | 주/월 집계 정확도 보장 불가 | `product_metrics_daily` 도입 | 일간 집계 검증 테스트 |
| 배치 재실행 시 중복 적재 | 랭킹 왜곡 | periodKey 단위 교체 전략 | 결과 diff 검증 |
| JobParameters 충돌 | 복구 불가 | `baseDate` + `run.id` 사용 | 운영 트리거 자동화 |
| 청크 크기 오설정 | 메모리/성능 저하 | 기본값 설정 후 측정 | 운영 지표 기반 튜닝 |
| 주간 경계 정의 불명확 | 결과 분쟁 | ISO Week + KST 고정 | periodKey 응답 노출 |
| 비활성 상품 포함 | 조회 결과 오염 | 응답 시점 필터링 (daily/weekly/monthly 공통) | 필요 시 주기적 MV 청소 배치 |
| 사전 집계 테이블 미생성 상태 조회 | 빈 결과 | 빈 목록 허용 | 배치 상태 모니터링 |
| Top 100 제한 | 101위 이후 조회 불가 | 스펙으로 명시 | 컷오프 설정화 검토 |
| 일간과 주간/월간 값 차이 | 사용자 혼동 | 동일 가중치 체계 사용 | 교차 검증 배치 |
| Spring Batch 메타 테이블 증가 | 운영 부담 | 메타 테이블 사용 | 오래된 실행 이력 정리 |

---

## 10. 설계 결정 사항

### 10.1 소스 데이터는 `product_metrics_daily`

- 현재 코드베이스 정합성을 기준으로, 주/월 배치 소스는 신규 `product_metrics_daily`로 잡는다.
- 기존 `product_metrics`는 누적형이므로 유지하되, 배치 입력으로는 사용하지 않는다.

### 10.2 API는 단일 엔드포인트 유지

- `GET /api/v1/rankings`에 `period`를 추가한다.
- 엔드포인트를 분리하지 않아도 하위 호환성과 클라이언트 단순성을 동시에 얻을 수 있다.
- 원문에 `period` 값 형식이 고정되어 있지 않으므로 본 문서는 다음 규약을 채택한다.
  - **URL 파라미터 값**: 소문자 `daily` / `weekly` / `monthly` 고정. 대소문자 섞인 값(`Daily`, `WEEKLY` 등)은 400 BAD_REQUEST. 명확성 우선.
  - **내부 enum**: `RankingPeriod.DAILY / WEEKLY / MONTHLY` (Kotlin 관례상 대문자). URL 값과는 `companion object { fun from(value: String): RankingPeriod }` 팩토리로 매핑.
  - **파싱 책임**: Controller가 `period: String?`을 받아 `RankingPeriod.from(...)` 또는 `DAILY` 기본값으로 정규화한 뒤 UseCase에 넘긴다. UseCase는 이미 정규화된 enum만 받는다 (순수성 확보).
  - **기본값**: 파라미터 생략 시 `daily`. 기존 R9 호출자는 자동으로 daily로 해석되어 하위 호환 유지.
  - **응답 필드 위치**: `data.period`, `data.periodKey`를 **상위 레벨**에 항상 포함. content 아이템에는 넣지 않는다(중복 회피).
- DTO 확장:
  - `RankingV1Dto.RankingPageResponse`에 `period: String`, `periodKey: String` 필드 추가.
  - `RankingV1Dto.RankingResponse`(content 아이템)는 **손대지 않는다** — 기존 클라이언트가 응답 스키마 차이로 깨지지 않도록 최소 침습 확장.
- UseCase 시그니처 확장:
  - `GetRankingUseCase.execute(date: LocalDate?, period: RankingPeriod, page: Int, size: Int): PageResult<RankingInfo>`
  - 내부에서 `period`로 분기: `DAILY`는 기존 Redis 경로 유지, `WEEKLY/MONTHLY`는 MV Repository 경로.
  - `PageResult<RankingInfo>` 자체의 구조는 변경하지 않고, 컨트롤러 DTO 변환 시점에 `period`/`periodKey`를 주입한다.

### 10.3 주간 기준은 ISO Week

- 주간은 월요일 시작, 일요일 종료로 고정한다.
- periodKey 예시는 `2026-W15`다.

### 10.4 월간 기준은 YearMonth

- 월간은 KST 기준 1일~말일이다.
- periodKey 예시는 `2026-04`다.

### 10.5 `daily`는 Redis, `weekly/monthly`는 사전 집계 테이블

- 실시간 경로와 배치 경로를 명확히 분리한다.
- 일간까지 배치화하지 않는다.

### 10.6 사전 집계 테이블 적재는 replace 전략

- 동일 periodKey를 먼저 삭제한 뒤 새 Top 100을 삽입한다.
- 재집계 후 Top 100에서 탈락한 상품이 자연스럽게 제거되는 장점이 있다.

### 10.7 재실행 식별자는 `baseDate` + `run.id`

- `baseDate`는 비즈니스 식별자다.
- `run.id`는 동일 기간 재실행을 위한 실행 식별자다.
- Spring Batch의 중복 실행 제약과 데이터 멱등성을 분리해서 다룬다.

### 10.8 이 Job은 단일 Tasklet Step으로 구현한다 (Chunk 비채택)

- 원문 체크리스트는 `Reader/Processor/Writer or Tasklet`을 허용한다.
- 본 Job의 요구는 "전체 집계 → Top 100 선별 → periodKey 전체 교체" 이며, 이는 **Chunk-Oriented 모델과 구조적으로 맞지 않다**:
  - Chunk Writer는 청크마다 호출되므로 "전체 집계 결과"를 알 수 없다 → 청크별 부분 Top 100만 쓰거나 첫 청크에서 delete가 튀는 현상 발생
  - `ExecutionContext` 또는 `@JobScope` 빈으로 상태를 공유하면 되지만, 복잡도가 폭발하며 Spring Batch의 stateless 원칙과도 어긋난다
- 대신 **DB-level GROUP BY + 단일 Tasklet Step**을 채택한다:
  - `SELECT product_id, SUM(view_count), SUM(like_count), SUM(sales_count) FROM product_metrics_daily WHERE metric_date BETWEEN :start AND :end GROUP BY product_id HAVING 가중치 > 0 ORDER BY 가중치 DESC, product_id ASC LIMIT 100` — DB가 집계·정렬·상한을 모두 처리
  - Tasklet 내부 순서: 위 쿼리로 Top 100 로드 → `DELETE FROM mv WHERE period_key = :pk` → Top 100 bulk insert
  - Tasklet 1개 = 1 트랜잭션이므로 delete-insert가 **원자적으로 수행**되며, 실패 시 기존 Top 100이 그대로 유지된다
- Top 100이 고정 상한이므로 애플리케이션 메모리 부담은 0에 수렴하며, 일간 행이 수만 건이어도 DB 인덱스와 `LIMIT`로 처리된다.
- Chunk-Oriented 학습 요소는 R10의 본질 목표가 아니므로, 다른 Job(일간 집계 / 데이터 마이그레이션 등)이 추가될 때 학습하면 된다. 이 결정은 요구사항 원문 체크리스트의 "or Tasklet" 선택지를 그대로 사용하는 것이지 범위 이탈이 아니다.

### 10.9 상품 상세 `rank`는 DAILY 유지

- 상품 상세에 주/월 순위를 넣으면 범위가 급격히 커진다.
- Round 10은 랭킹 API 확장에 집중한다.

### 10.10 `GetRankingUseCase` 시그니처 일괄 이행 (오버로드 금지)

- R9 `GetRankingUseCase.execute(date, page, size)` 시그니처는 R10에서 `execute(date: LocalDate?, period: RankingPeriod, page: Int, size: Int)` 로 확장한다.
- **오버로드를 만들지 않는다. UseCase 레벨의 기본값도 두지 않는다.** `period`는 **필수 파라미터**.
- **기본값 책임은 Controller에만** 둔다: `RankingPeriod.from(period ?: "daily")` 로 정규화해 UseCase에 넘긴다. UseCase는 항상 정규화된 enum만 받아 순수성을 유지한다.
- 기존 `GetRankingUseCaseTest`는 named argument 관례(`execute(date = today, page = 0, size = 10)`)를 쓰고 있어, `period = RankingPeriod.DAILY`를 기계적으로 추가하면 된다. 프로덕션 호출부는 `apps/commerce-api/.../interfaces/api/ranking/RankingV1Controller.kt:33` **한 줄뿐**이다 — R9 "상품 상세에 rank 포함" 기능은 현재 코드베이스에 `GetRankingUseCase`를 호출하지 않는다.
- **Tidy First 분리**: 시그니처 변경은 구조적 변경이므로 WEEKLY/MONTHLY 분기 구현(행위적 변경)과 **커밋을 분리**한다.
  - 커밋 ①: `RankingPeriod` enum 추가 + `execute`에 `period` 파라미터 추가 + 내부는 `when(period)` 분기만 만들고 DAILY 케이스는 기존 로직 그대로, **WEEKLY/MONTHLY는 빈 `PageResult(emptyList(), 0, page, size)` placeholder를 반환**한다 (200 빈 목록). `TODO()` / `throw NotImplementedError`는 사용하지 않는다 — "매 커밋마다 빌드/테스트/E2E 초록" 불변식을 유지해야 하기 때문이다. 기존 호출부/테스트를 `period = RankingPeriod.DAILY` 명시로 일괄 수정.
  - 커밋 ② 이후: WEEKLY/MONTHLY 분기 로직을 TDD로 추가.
- **오버로드가 아니라 일괄 이행을 택한 이유**: 오버로드는 같은 로직에 두 진입점을 만들어 "요청되지 않은 유연성"에 해당하며, CLAUDE.md behavior.md §4 단순함 원칙과 충돌한다. 일괄 이행의 비용(테스트 파일 1~2개의 `execute` 호출에 `period = RankingPeriod.DAILY` 추가)은 기계적이며, 구조 유지비용보다 작다.

### 10.11 `ProductMetricsDaily` 별도 도메인 모델 + UseCase 직접 오케스트레이션

- 일간 집계는 **기존 `ProductMetrics`와 별개의 도메인 모델**로 추가한다. 이름은 `ProductMetricsDaily`, PK는 `(metric_date, product_id)`, 카운터 필드는 기존과 동일(`view_count/like_count/sales_count`).
- 기존 `ProductMetrics` 도메인/Repository/Entity/테스트는 **한 줄도 건드리지 않는다.** R9 회귀 방지가 최우선이다.
- **카운터 증감 메서드는 기존 `ProductMetrics`에서 복제한다** (`incrementViewCount`, `incrementLikeCount`, `decrementLikeCount`(>=0 가드 포함), `incrementSalesCount(quantity)`). 코드 중복은 인정하되 유지비용이 낮다 — 카운터 규칙은 R9에서 이미 안정화되어 변경 빈도가 낮다.
- **공유 VO(`MetricsCounters`) 추출은 R10 이후 별도 PR로 미룬다.** Tidy First 원칙상 구조적 리팩토링(VO 추출)과 행위적 변경(일간 테이블 추가)을 한 커밋에 섞지 않는다.
- **`UpdateProductMetricsUseCase`가 두 Repository를 직접 오케스트레이션**한다. Domain Service로 묶지 않는다.
  - 근거: `apps/commerce-api/.../application/CLAUDE.md` — "UseCase가 Repository를 직접 호출하는 것이 기본. Domain Service는 여러 Entity 간 원자적 얽힘이 있을 때만". 지금의 얽힘(두 테이블 동시 갱신)은 기존 `@Transactional` 경계 하나로 자동 보호되므로 Domain Service 추가는 오버엔지니어링이다.
  - `findOrCreateDaily(metricDate, productId)` private helper를 추가하여 기존 `findOrCreate(productId)` 패턴과 대칭을 맞춘다.
- **세션/커밋 분할**: 신규 파일 6개(도메인 모델/도메인 Repository 인터페이스/JPA Entity/JPA Repository/Repository Impl/Fake Repository) + 수정 2개(`UpdateProductMetricsUseCase`/`UpdateProductMetricsUseCaseTest`) = **총 8개**. CLAUDE.md "한 세션 5개 초과 시 분할" 규칙에 걸리므로 다음과 같이 나눈다.
  - **커밋 ① (구조적)**: 신규 파일 6개 + 도메인 단위 테스트(`ProductMetricsDailyTest`). 이 커밋만으로는 프로덕션 동작이 바뀌지 않는다 — 아직 `UpdateProductMetricsUseCase`가 daily repo를 호출하지 않기 때문.
  - **커밋 ② (행위적)**: `UpdateProductMetricsUseCase` 생성자에 `ProductMetricsDailyRepository` 주입 + `handleCatalogEvent` / `handleOrderEvent` 내부에 daily 갱신 추가 + `UpdateProductMetricsUseCaseTest`에 "daily 테이블도 함께 증가한다" 성격의 assertions 추가.
- **검증 불변식**: `product_metrics_daily`는 non-negative 카운터다. `decrementLikeCount`는 0 미만으로 내려가지 않도록 기존 `ProductMetrics.decrementLikeCount` 방식(`likeCount == 0L`이면 `false` 반환)을 그대로 계승한다. 이 불변식이 R10 MV 집계에서 "재집계 점수 `>= 0`"을 보장하는 근거이며, §5 점수 규칙의 `score == 0` 배제 정책과 연결된다.

---

## 11. 제안 테이블 설계

> **DDL은 개념 설계, 실제 엔티티는 프로젝트 관례**: 아래 `CREATE TABLE` 문은 스키마 설계 의도(키·인덱스·제약)를 설명하는 개념 수준 표기다. 실제 JPA 엔티티 구현은 프로젝트 관례(`BaseEntity` auto-increment `id`를 PK로 두고, 도메인 식별 키는 `UNIQUE` 제약으로 표현)를 따른다. 예를 들어 `product_metrics_daily`의 `PRIMARY KEY (metric_date, product_id)`는 엔티티에서 `id` PK + `(metric_date, product_id)` 유니크 제약으로 구현된다 (`ProductMetricsEntity` 패턴과 동일). `mv_product_rank_weekly` / `mv_product_rank_monthly`도 같은 원칙: `(period_key, product_id)`와 `(period_key, rank_no)`는 유니크 제약으로 건다. 이 차이는 "복합 PK를 쓰지 않는다"는 코드 관례와 "개념 설계에서는 논리 PK를 그대로 기술한다"는 문서 관례의 차이이며, 충돌이 아니라 표현 계층의 차이다.

### 11.1 일간 메트릭 테이블

```sql
CREATE TABLE product_metrics_daily (
  metric_date DATE NOT NULL,
  product_id BIGINT NOT NULL,
  view_count BIGINT NOT NULL DEFAULT 0,
  like_count BIGINT NOT NULL DEFAULT 0,
  sales_count BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (metric_date, product_id),
  INDEX idx_product_metrics_daily_product_id (product_id)
);
```

### 11.2 주간 랭킹 사전 집계 테이블

```sql
CREATE TABLE mv_product_rank_weekly (
  period_key VARCHAR(10) NOT NULL,
  rank_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  score DECIMAL(18,4) NOT NULL,
  view_count BIGINT NOT NULL,
  like_count BIGINT NOT NULL,
  sales_count BIGINT NOT NULL,
  period_start_date DATE NOT NULL,
  period_end_date DATE NOT NULL,
  aggregated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (period_key, product_id),
  UNIQUE KEY uk_mv_product_rank_weekly_rank (period_key, rank_no),
  INDEX idx_mv_product_rank_weekly_page (period_key, rank_no)
);
```

### 11.3 월간 랭킹 사전 집계 테이블

```sql
CREATE TABLE mv_product_rank_monthly (
  period_key VARCHAR(7) NOT NULL,
  rank_no INT NOT NULL,
  product_id BIGINT NOT NULL,
  score DECIMAL(18,4) NOT NULL,
  view_count BIGINT NOT NULL,
  like_count BIGINT NOT NULL,
  sales_count BIGINT NOT NULL,
  period_start_date DATE NOT NULL,
  period_end_date DATE NOT NULL,
  aggregated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (period_key, product_id),
  UNIQUE KEY uk_mv_product_rank_monthly_rank (period_key, rank_no),
  INDEX idx_mv_product_rank_monthly_page (period_key, rank_no)
);
```

### 11.4 Spring Batch 메타 테이블

- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_STEP_EXECUTION`
- 그 외 Spring Batch 기본 메타 스키마

기존 마이그레이션 체계에 맞춰 함께 관리한다.

---

## 12. Batch Job 설계 개요

### WeeklyRankingJob — 단일 Tasklet Step

```text
Job: weeklyRankingJob
  Parameters:
    - baseDate (yyyyMMdd, identifying)
    - run.id  (Long, identifying)     ← 재실행 식별자

  Step 1: weeklyReplaceStep (Tasklet)
    Transaction: 1 Tasklet = 1 Transaction (DB-level)
    Logic:
      1. Validate JobParameters (JobParametersInvalidException on failure)
      2. (periodKey, startDate, endDate) = WeeklyWindow.from(baseDate)      // WeekFields.ISO
      3. top100 = queryDao.selectTop100Aggregate(startDate, endDate)
         // SELECT product_id, SUM(view_count), SUM(like_count), SUM(sales_count)
         // FROM product_metrics_daily
         // WHERE metric_date BETWEEN :start AND :end
         // GROUP BY product_id
         // HAVING (0.1*SUM(view)+0.2*SUM(like)+0.7*SUM(sales)) > 0
         // ORDER BY (가중치 점수) DESC, product_id ASC
         // LIMIT 100
      4. queryDao.deleteByPeriodKey(periodKey)
      5. queryDao.bulkInsert(periodKey, top100, startDate, endDate)
      6. return RepeatStatus.FINISHED
```

### MonthlyRankingJob — 단일 Tasklet Step

- 구조는 동일하다. `WeeklyWindow.from` 대신 `MonthlyWindow.from`을 사용하여 `YearMonth.atDay(1)` ~ `atEndOfMonth()` 범위를 계산한다.
- periodKey 포맷은 `YYYY-MM`.
- Tasklet/QueryDao/Config 파일을 **복제**한다. 공통 추출은 Rule of Three(중복 3건) 기준으로 R10 이후로 미룬다.

### 구현 원칙

- **Step은 단일 Tasklet**으로 구성한다 (§10.8 참조). Chunk-Oriented는 이 Job의 구조와 맞지 않아 채택하지 않는다.
- **트랜잭션 경계**는 Tasklet 기본 제공(Spring Batch가 Step마다 1개 트랜잭션)을 사용한다. `@Transactional`을 중복해서 달지 않는다.
- **집계 쿼리는 DB에서 처리**한다. **QueryDSL로 구현을 고정**한다 — `GROUP BY + HAVING + ORDER BY (가중치 점수) + LIMIT 100` 조합은 Spring Data 메서드명 쿼리로는 표현이 불가능하기 때문이다. JPQL/NativeQuery 사용은 금지한다(프로젝트 CLAUDE.md).
- **Top 100은 DB `LIMIT`으로 상한**이 걸리므로 애플리케이션 메모리에 로드되는 건 최대 100건이다.
- **delete-insert는 Tasklet 내부에서 순차 수행**된다. Tasklet 전체가 하나의 트랜잭션이므로 외부에는 원자적으로 보인다 — 읽기 쪽이 "삭제된 빈 상태"를 잠깐이라도 관찰하지 않는다.
- **재실행 멱등성**: `run.id`를 증가시켜 Spring Batch의 중복 실행 제약을 풀고, Tasklet의 delete-insert가 데이터 레벨 멱등성을 보장한다(§10.7과 §10.6을 함께 참조).
- **실패 시 롤백**: Tasklet 중간에 예외가 나면 트랜잭션이 롤백되어 기존 Top 100이 그대로 유지된다.
- **메트릭**: Tasklet 시작/종료 시각, 처리 상품 수, 실패 여부를 Micrometer로 노출(Nice-to-Have §10).

---

## 13. Step별 구현 범위

### Step 1 — 일간 집계 원천 데이터 확보

- [ ] `product_metrics_daily` Entity 추가 (PK `(metric_date, product_id)`, 컬럼 `view_count/like_count/sales_count`)
- [ ] `ProductMetricsDailyRepository` 인터페이스 + Fake 구현
- [ ] [RED] 같은 날짜 · 같은 상품에 이벤트가 여러 번 오면 카운터가 누적된다는 테스트
- [ ] [RED] 날짜가 넘어가면 새 행이 생기고 전일 행은 변경되지 않는다는 테스트
- [ ] [RED] `event_handled`에 이미 처리된 eventId면 `product_metrics_daily`도 갱신되지 않는다는 테스트 (멱등성)
- [ ] [GREEN] `UpdateProductMetricsUseCase`의 기존 `@Transactional` 안에서 `product_metrics`와 `product_metrics_daily`를 함께 upsert
- [ ] `metric_date`는 기존 `rankingDate`와 동일하게 `LocalDate.now(clock)` 사용 (KST 기준)
- [ ] 기존 누적 `product_metrics` 갱신 경로는 시그니처·동작 모두 변경하지 않는다 (R9 회귀 방지)
- [ ] Redis 반영(after-commit) 및 `FailedScoreUpdate` 재시도 로직도 그대로 유지한다

### Step 2 — 배치 Job 구현

- [ ] `weeklyRankingJob` 구현
- [ ] `monthlyRankingJob` 구현
- [ ] `baseDate`, `run.id` JobParameter 검증
- [ ] Chunk 기반 Reader/Processor/Writer 구성
- [ ] periodKey 단위 교체 적재

### Step 3 — 사전 집계 테이블 조회 경로 구현

- [ ] 주간/월간 사전 집계 테이블 Repository 구현
- [ ] periodKey 계산 유틸 구현
- [ ] Top 100 범위 페이지네이션 처리

### Step 4 — 랭킹 API 확장

- [ ] `period` 파라미터 추가
- [ ] `daily`는 기존 Redis 조회 재사용
- [ ] `weekly/monthly`는 사전 집계 테이블 조회로 분기
- [ ] `period/date/page/size` 검증

### Step 5 — 검증

- [ ] 일간 메트릭 적재 후 주간/월간 Job 실행 검증
- [ ] 재실행 시 중복이 아닌 교체 동작 검증
- [ ] `daily|weekly|monthly`가 각기 올바른 소스를 읽는지 검증
- [ ] ISO 주간 경계 테스트
- [ ] 비활성 상품 제외 테스트

---

## 14. 이 통합 문서가 취한 기준

- 클로드 문서의 장점: Spring Batch 운영 관점, JobParameter/재실행/메타 테이블/periodKey 설계의 밀도
- 내 문서의 장점: 현재 코드베이스와의 정합성 검토, `product_metrics` 누적형 한계, `product_metrics_daily` 필요성

## 15. 원문 요구에서 의도적으로 제외한 항목

- `req-10.md`의 Technical Writing Quest / Retrospective 요구는 구현 요구 분석 문서 범위 밖으로 두었다.
- 필요하다면 별도 "round10-writing-guide.md" 또는 제출 체크리스트 문서로 분리하는 편이 더 명확하다.

최종 기준안은 "운영 설계는 풍부하게 가져가되, 실제 구현 출발점은 현재 코드베이스에 맞춘다"이다.
