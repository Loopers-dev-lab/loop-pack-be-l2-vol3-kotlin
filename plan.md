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

- [ ] `OrderItemPayload`에 `unitPrice: Long` 필드 추가 (modules/kafka)
- [ ] commerce-api 이벤트 발행 코드에서 `unitPrice` 포함하도록 수정

### 1. 공통 — Redis 키 정의

- [ ] `RedisKeys`에 `rankingKey(date: String): String` 추가 (`ranking:all:{yyyyMMdd}`)

### 2. commerce-streamer — 랭킹 ZSET 적재

#### 2-1. 도메인

- [ ] `RankingRepository` 인터페이스 정의: `incrementScore(date: LocalDate, productId: Long, score: Double)`
- [ ] `RankingService` 구현: 이벤트 타입별 점수 계산 + `RankingRepository` 호출
  - [ ] VIEWED 이벤트 시 해당 상품 점수 0.1 증가
  - [ ] LIKED 이벤트 시 해당 상품 점수 0.2 증가
  - [ ] UNLIKED 이벤트 시 해당 상품 점수 0.2 차감
  - [ ] ORDER_COMPLETED 이벤트 시 상품별 점수 `0.7 × log(unitPrice × quantity)` 증가

#### 2-2. 인프라

- [ ] `RankingRedisRepository` 구현: ZINCRBY + Lua 스크립트 (TTL 없으면 2일 설정)
  - [ ] ZINCRBY로 점수가 정상 증가한다
  - [ ] 키가 없으면 자동 생성되고 TTL 2일이 설정된다
  - [ ] 이미 TTL이 있는 키에 ZINCRBY 시 TTL이 변경되지 않는다

#### 2-3. Processor 수정

- [ ] `CatalogEventProcessor.process()`에 `RankingService` 호출 추가 (VIEWED, LIKED, UNLIKED)
- [ ] `OrderEventProcessor.process()`에 `RankingService` 호출 추가 (ORDER_COMPLETED, 상품별)

### 3. commerce-api — 랭킹 API

#### 3-1. 도메인

- [ ] `RankingRepository` 인터페이스 정의
  - `getTopRankings(date: LocalDate, offset: Long, count: Long): List<RankingEntry>`
  - `getRank(date: LocalDate, productId: Long): Long?`
- [ ] `RankingEntry` 데이터 클래스: `productId: Long, score: Double`
- [ ] `RankingService` 구현: 페이지네이션 계산 + Repository 호출
  - [ ] page, size → ZREVRANGE offset 계산 (0-based, inclusive)
  - [ ] 특정 상품 순위 조회 시 null 허용 (랭킹 미진입)

#### 3-2. 인프라

- [ ] `RankingRedisRepository` 구현
  - [ ] ZREVRANGE WITHSCORES로 상위 N개 점수 내림차순 조회
  - [ ] 페이지네이션 정상 동작 (page=2, size=20 → offset 20~39)
  - [ ] ZREVRANK로 특정 상품 순위 조회 (0-based)
  - [ ] 존재하지 않는 상품 순위 조회 시 null 반환

#### 3-3. Application

- [ ] `RankingInfo` DTO 정의 (productId, rank, score, 상품 정보)
- [ ] `RankingFacade` 구현: RankingService + ProductCacheManager 조합
  - [ ] 랭킹 조회 시 상품 정보(이름, 가격 등)가 함께 반환된다
  - [ ] ZSET 순서대로 상품 정보가 매핑된다 (순서 보장)

#### 3-4. Interfaces

- [ ] `RankingDto` 정의 (RankingResponse, RankingItemResponse)
- [ ] `RankingApiSpec` Swagger 스펙 정의
- [ ] `RankingController` 구현: `GET /api/v1/rankings?date={yyyyMMdd}&page=1&size=20`
  - [ ] date 파라미터 없으면 오늘 날짜 기본값
  - [ ] page, size 파라미터로 페이지네이션
- [ ] 상품 상세 API 응답에 `rank: Long?` 필드 추가
  - [ ] 랭킹 진입 상품은 순위 반환 (1-based로 변환)
  - [ ] 랭킹 미진입 상품은 null 반환

#### 3-5. 스케줄러

- [ ] `RankingCarryOverScheduler` 구현 (매일 23:50 실행)
  - [ ] `RankingRepository`에 `carryOver(fromDate, toDate, weight)` 메서드 추가
  - [ ] ZUNIONSTORE로 전날 점수 × 0.1 복사
  - [ ] 생성된 키에 TTL 2일 설정
  - [ ] 스케줄러 정상 실행 시 다음날 키가 생성된다

### 4. E2E 검증

- [ ] 이벤트 발행 → ZSET 점수 반영 → API 조회 전체 흐름 정상 동작
- [ ] 일자 변경 시 이전 날짜 랭킹 조회 정상 동작
- [ ] 가중치 확인: 주문 1건의 점수 > 좋아요 3건의 점수
- [ ] `http/ranking-v1.http` 파일 작성

### 5. commerce-batch — 랭킹 점수 배치 보정 (Reconciliation)

Redis 장애 등으로 유실된 랭킹 점수를 DB의 `product_metrics` 집계값 기반으로 재계산하여 Redis ZSET을 보정한다.

#### 설계 결정

| 항목 | 결정 | 근거 |
|------|------|------|
| 점수 공식 | `view×0.1 + like×0.2 + sales×0.7` | DB에 개별 주문금액이 없으므로 salesCount × 고정 가중치로 단순화 |
| 실행 방식 | Spring Batch Job (Reader→Processor→Writer) | StockReconciliationJob과 동일 패턴 |
| 보정 대상 키 | `ranking:all:{오늘}` | 당일 랭킹만 보정 (이전 날짜는 이미 적재 완료) |
| 보정 전략 | Redis ZADD로 덮어쓰기 | ZINCRBY가 아닌 ZADD로 DB 기준 점수를 절대값으로 설정 |

#### 5-1. 데이터 모델

- [ ] `RankingScore` 데이터 클래스 정의 (productId, dbScore)

#### 5-2. Reader

- [ ] `product_metrics` + `products` JOIN으로 viewCount, likeCount, salesCount 조회 (JdbcPagingItemReader)
- [ ] `deleted_at IS NULL` 조건으로 삭제된 상품 제외

#### 5-3. Processor

- [ ] DB 집계값 기반 점수 계산: `view × 0.1 + like × 0.2 + sales × 0.7`
- [ ] Redis 현재 점수와 비교하여 불일치 항목만 통과 (일치하면 null 반환)

#### 5-4. Writer

- [ ] Redis ZADD로 `ranking:all:{오늘}` 키에 보정 점수 덮어쓰기
- [ ] 키가 없으면 생성하고 TTL 2일 설정

#### 5-5. JobConfig

- [ ] `RankingReconciliationJobConfig` 구성 (Job → Step, chunk size 100)

#### 5-6. 테스트

- [ ] DB-Redis 점수 불일치 시 DB 기준으로 Redis가 보정된다
- [ ] Redis에 키가 없는 상품도 DB 기준으로 점수가 생성된다
- [ ] DB-Redis 점수가 일치하면 Redis를 갱신하지 않는다
