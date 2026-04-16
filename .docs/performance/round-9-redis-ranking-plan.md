# Round 9. Redis ZSET Realtime Ranking Plan

## 1. 목표

이번 Round 9의 목표는 기존 `product_metrics` 집계 파이프라인을 확장해서,

1. **Kafka Consumer -> Redis ZSET** 기반 실시간 랭킹 적재를 만들고
2. **랭킹 조회 API** 를 제공하고
3. **상품 상세 응답에 현재 순위 정보** 를 함께 노출하는 것이다.

Nice-to-have 로는 아래 2가지까지 확장 가능하도록 설계한다.

- **1시간 단위 초실시간 랭킹**
- **콜드 스타트 완화를 위한 carry-over scheduler**

---

## 2. 현재 구조 요약

현재 구조는 이미 랭킹 구현에 필요한 재료를 대부분 가지고 있다.

### 2.1 이벤트 발행 경로

`commerce-api` 에서는 상품/좋아요/주문 관련 사실을 outbox 를 통해 Kafka 로 전파하고 있다.

- 상품 상세 조회 시 `ProductViewed` 발행
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/product/ProductUseCase.kt`
- 좋아요 등록/취소 시 `ProductLiked`, `ProductUnliked` 발행
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/like/LikeUseCase.kt`
- 결제 완료 시 `OrderPaid` 발행
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/payment/PaymentUseCase.kt`

관련 설명 문서:

- `/.docs/design/09-event-flow-and-outbox.md`

즉, 랭킹에 필요한 이벤트 소스는 이미 Kafka 로 흘러가고 있다.

### 2.2 이벤트 소비 경로

`commerce-streamer` 는 이미 batch listener 로 catalog / order 이벤트를 소비 중이다.

- `apps/commerce-streamer/src/main/kotlin/com/loopers/interfaces/consumer/CatalogEventConsumer.kt`
- `apps/commerce-streamer/src/main/kotlin/com/loopers/interfaces/consumer/OrderEventConsumer.kt`

두 consumer 모두 아래 factory 를 사용한다.

- `modules/kafka/src/main/kotlin/com/loopers/config/kafka/KafkaConfig.kt`
  - `KafkaConfig.BATCH_LISTENER`

즉, 이번 과제의 Nice-to-have 인 **카프카 배치 리스너** 는 이미 구조적으로 적용되어 있다.

### 2.3 현재 집계 경로

현재 `commerce-streamer` 는 Kafka 이벤트를 받아 `product_metrics` 테이블을 업데이트한다.

- entity
  - `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/metrics/ProductMetricsEntity.kt`
- updater
  - `apps/commerce-streamer/src/main/kotlin/com/loopers/application/metrics/ProductMetricsUpdater.kt`

현재 집계 항목은 아래 3개다.

- `like_count`
- `view_count`
- `sales_count`

즉, 이번 랭킹은 완전히 새 파이프라인을 만드는 게 아니라,

> **기존 Kafka 집계 파이프라인에 Redis projection 을 하나 더 붙이는 작업**

으로 보는 것이 맞다.

### 2.4 Redis 사용 구조

Redis 설정과 접근 패턴도 이미 존재한다.

- Redis 설정
  - `modules/redis/src/main/kotlin/com/loopers/config/redis/RedisConfig.kt`
- Redis ZSET 사용 예시
  - `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/queue/QueueRedisSupport.kt`

특히 `QueueRedisSupport` 는 아래 연산을 이미 사용하고 있다.

- `opsForZSet().addIfAbsent(...)`
- `opsForZSet().rank(...)`
- `opsForZSet().range(...)`
- `opsForZSet().remove(...)`

즉, 이번 랭킹 구현도 같은 `RedisTemplate<String, String>` 패턴을 재사용하는 것이 자연스럽다.

---

## 3. Round 9 요구사항을 현재 구조에 매핑하면

### 3.1 Ranking Consumer

구현 목표:

- Kafka 이벤트를 소비한다.
- 이벤트 종류별로 score 를 계산한다.
- 날짜별 Redis ZSET key 에 누적 반영한다.

권장 key:

- 일간 전체 랭킹
  - `ranking:all:{yyyyMMdd}`

권장 TTL:

- **2일**

### 3.2 Ranking API

구현 목표:

- `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1`
- 단순 productId 목록이 아니라 **상품 정보 aggregation 결과** 반환
- 상품 상세 조회 시 해당 상품의 **현재 순위** 포함

### 3.3 Nice-to-have

- 시간 단위 랭킹 key 추가
  - `ranking:all:{yyyyMMddHH}`
- 23:50 carry-over scheduler 로 다음날 초기 랭킹판 생성

---

## 4. 구현 전략

## 4.1 핵심 원칙

이번 작업은 아래 원칙으로 가져간다.

1. **Kafka 이벤트는 source, Redis ZSET 은 조회 최적화 projection 으로 둔다**
2. **DB 집계(`product_metrics`) 와 Redis 집계(랭킹)를 분리한다**
3. **랭킹 API 는 Redis hit 기준으로 빠르게 응답하고, 상품 정보는 API 서버에서 aggregate 한다**
4. **Redis 장애 시 랭킹 기능만 degrade 되고 핵심 비즈니스 흐름은 유지되어야 한다**

즉:

> `product_metrics` 는 장기 집계용,
> `ranking:*` ZSET 은 빠른 조회용 projection 으로 역할을 나눈다.

---

## 4.2 Consumer 쪽 구현

### 추천 구조

`commerce-streamer` 에 아래 계층을 추가한다.

- `application/ranking/RankingScoreCalculator`
- `application/ranking/RankingUpdater`
- `infrastructure/ranking/RedisRankingStore`

역할은 아래처럼 나눈다.

#### RankingScoreCalculator

이벤트 타입별 점수 계산 담당.

예시 정책:

- 조회
  - weight = `0.1`
  - score = `1`
  - final = `0.1`
- 좋아요
  - weight = `0.2`
  - score = `1`
  - final = `0.2`
- 주문
  - weight = `0.6`
  - score = `price * amount` 또는 `quantity`
  - final = `0.6 * normalized(orderScore)`

초기 구현은 너무 복잡하게 가지 말고,

- `view = 0.1`
- `like = 0.2`
- `order = 1.0 * quantity`

정도로 단순 시작하는 편이 안전하다.

주문 점수는 가격까지 반영하면 비싼 상품 편향이 강해질 수 있으므로,
처음에는 `quantity` 기준 또는 `log(price * quantity)` 정규화 정도만 고려하는 것이 좋다.

#### RedisRankingStore

Redis ZSET 적재 전담.

필수 메서드 예시:

- `increaseDailyScore(productId, occurredAt, score)`
- `increaseHourlyScore(productId, occurredAt, score)`
- `getRankRange(date, offset, limit)`
- `getRank(productId, date)`

내부 구현 포인트:

- key 계산: `ranking:all:{yyyyMMdd}`
- member: `productId.toString()`
- score update: `ZINCRBY`
- TTL: write 시 `expire(key, 2 days)` 보장

### Consumer 연결 포인트

현재 consumer 변경 포인트는 아래 두 곳이다.

#### CatalogEventConsumer

파일:

- `apps/commerce-streamer/src/main/kotlin/com/loopers/interfaces/consumer/CatalogEventConsumer.kt`

현재는:

- `ProductLiked` -> `productMetricsUpdater.increaseLikeCount(...)`
- `ProductUnliked` -> `productMetricsUpdater.decreaseLikeCount(...)`
- `ProductViewed` -> `productMetricsUpdater.increaseViewCount(...)`

여기에 추가로:

- `rankingUpdater.applyLiked(...)`
- `rankingUpdater.applyUnliked(...)` 또는 unlike 는 점수 차감 여부 정책 결정
- `rankingUpdater.applyViewed(...)`

를 붙이면 된다.

#### OrderEventConsumer

파일:

- `apps/commerce-streamer/src/main/kotlin/com/loopers/interfaces/consumer/OrderEventConsumer.kt`

현재는 `OrderPaid` 발생 시 item 단위로 `sales_count` 만 증가시킨다.

여기에 추가로 각 item 별로:

- `rankingUpdater.applyOrdered(productId, quantity, price, occurredAt)`

를 수행하면 된다.

### unlike 처리 정책

여기서 한 가지 정책 결정을 먼저 해야 한다.

- **옵션 A**: 좋아요 취소 시 점수 차감
- **옵션 B**: 좋아요 취소는 랭킹 점수에 반영하지 않음

추천은 **옵션 A** 다.
이유는 현재 좋아요가 랭킹 signal 이라면, 취소 역시 signal 이기 때문이다.

다만 음수 점수 누적 가능성은 아래처럼 막는다.

- 단순 차감 허용
- API 레벨에서 음수도 허용
- 또는 필요 시 `0` 아래로 내려가지 않도록 보정 로직 추가

---

## 4.3 API 쪽 구현

### 추천 구조

`commerce-api` 에 아래 계층을 추가한다.

- `interfaces/api/ranking/RankingV1Controller`
- `interfaces/api/ranking/RankingV1Dto`
- `application/ranking/RankingUseCase`
- `application/ranking/RankingInfo`
- `infrastructure/ranking/RankingRedisReader`

### 랭킹 목록 API

목표 endpoint:

- `GET /api/v1/rankings?date=yyyyMMdd&size=20&page=1`

권장 흐름:

```text
RankingV1Controller
-> RankingUseCase.getPage(date, size, page)
-> RankingRedisReader.getRange(date, offset, limit)
-> Redis ZSET 에서 productId + score 조회
-> productReader 로 상품 조회
-> brandReader 로 브랜드 조회
-> 응답 DTO 조합
```

중요 포인트는,

> Redis 에는 productId 와 score 만 두고,
> 상품 상세 정보는 API 서버가 조합해서 내려주는 구조

로 가져가는 것이다.

이렇게 하면 Redis 데이터를 최소화할 수 있고,
상품 정보 변경 시 Redis 전체 재적재 부담도 줄어든다.

### 페이지네이션

Redis ZSET paging 은 offset 기반으로 처리한다.

- page = 1 이면 `offset = 0`
- size = 20 이면 `start = 0`, `end = 19`
- page = 2 이면 `start = 20`, `end = 39`

조회 명령은 descending 기준으로:

- `ZREVRANGE key start end WITHSCORES`

개념으로 구현한다.

### 상세 조회에 랭킹 포함

현재 상품 상세 흐름은 아래 파일에 있다.

- `apps/commerce-api/src/main/kotlin/com/loopers/application/product/ProductUseCase.kt`

현재 `getById()` 는 `ProductInfo.Detail` 을 만들고 반환한다.

변경 포인트:

- `ProductInfo.Detail` 에 `ranking: Long?` 추가
  - `apps/commerce-api/src/main/kotlin/com/loopers/application/product/ProductInfo.kt`
- `ProductV1Dto.DetailResponse` 에 `ranking: Long?` 추가
  - `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductV1Dto.kt`
- `ProductUseCase.getById()` 에서 `rankingRedisReader.getRank(today, productId)` 호출

순위 계산 규칙:

- Redis 에 rank 가 없으면 `null`
- Redis rank 는 0-based 이므로 응답은 `+1` 해서 1등부터 보이게 변환

### 캐시와의 관계

현재 상품 상세는 Redis 캐시를 사용한다.

- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/cache/RedisProductCacheStore.kt`

이때 랭킹 값을 상품 상세 캐시에 같이 넣을지 결정해야 한다.

추천은 **초기에는 넣지 않는 것** 이다.

이유:

- 상품 기본 정보보다 랭킹 변동 주기가 훨씬 짧다.
- 기존 상세 캐시 TTL 과 랭킹 최신성이 충돌할 수 있다.

따라서 초기 구현은:

1. 상품 기본 detail 은 기존 캐시 사용
2. 랭킹 정보는 별도 Redis 조회

로 가져가는 편이 안전하다.

---

## 4.4 Redis key / TTL 전략

### Must-have

- daily key: `ranking:all:{yyyyMMdd}`
- TTL: `2 days`

이 방식이면 아래 요구사항을 만족한다.

- 오늘 랭킹 즉시 조회 가능
- 날짜가 바뀌어도 전날 랭킹 조회 가능
- 2일 지나면 자연 삭제

### Nice-to-have

- hourly key: `ranking:all:{yyyyMMddHH}`
- TTL: `48 hours` 또는 `25 hours`

시간 단위 key 는 하루 경계 전후 분석이나 초실시간 랭킹에 유리하다.

---

## 4.5 콜드 스타트 완화

문제:

- 날짜가 바뀐 직후 새 key 는 비어 있다.
- 00:00 직후 인기 상품 API 가 비어 보일 수 있다.

해결 전략:

- 매일 `23:50` 에 scheduler 실행
- 오늘 일간 ZSET 상위 N개를 읽는다.
- 내일 key 에 carry-over score 를 미리 적재한다.

추천 구현 위치:

- 신규 `RankingWarmupScheduler`
  - `commerce-streamer` 또는 `commerce-api` 중 Redis write 책임이 있는 쪽

개인적으로는 **Redis write projection 을 담당하는 `commerce-streamer` 쪽** 이 더 자연스럽다.

carry-over 예시:

- 오늘 score 의 20~30% 만 복사
- 또는 순위만 유지하도록 작은 seed score 부여

초기에는 너무 공격적으로 가지 말고,

- top 20 상품
- `score * 0.2`

정도로 단순 시작하면 된다.

---

## 5. 구현 순서

### Step 1. Redis ranking 저장소 추가

대상:

- `commerce-streamer` 신규 `RedisRankingStore`

할 일:

- key 계산기 구현
- `ZINCRBY` 기반 점수 누적
- TTL 2일 보장
- daily rank / hourly rank 조회용 메서드 추가

### Step 2. score calculator 추가

대상:

- `commerce-streamer` 신규 `RankingScoreCalculator`

할 일:

- view / like / unlike / order 점수 계산 정책 분리
- 정책을 상수 또는 properties 로 분리

### Step 3. Kafka consumer 에 ranking projection 연결

대상:

- `CatalogEventConsumer.kt`
- `OrderEventConsumer.kt`

할 일:

- 기존 `productMetricsUpdater` 호출은 유지
- 추가로 `rankingUpdater` 호출 연결

### Step 4. Ranking API 추가

대상:

- 신규 `RankingV1Controller`
- 신규 `RankingUseCase`
- 신규 `RankingRedisReader`

할 일:

- 날짜/페이지 파라미터 검증
- Redis ZSET 에서 목록 조회
- 상품/브랜드 aggregate
- 응답 DTO 구성

### Step 5. 상품 상세에 순위 추가

대상:

- `ProductInfo.kt`
- `ProductV1Dto.kt`
- `ProductUseCase.kt`

할 일:

- rank nullable 필드 추가
- 상세 조회 시 오늘 기준 rank 조회

### Step 6. Nice-to-have 확장

후순위:

- hourly ranking
- carry-over scheduler
- weight properties 외부화

---

## 6. 검증 포인트

### 6.0 현재 구현 상태 (volume-9)

- `commerce-api` 는 상품 조회/좋아요/주문 완료 시 **Kafka 로 직접 이벤트를 발행**한다.
- `commerce-streamer` 는 Kafka consumer 에서 `product_metrics` 와 Redis ranking ZSET 을 함께 갱신한다.
- `commerce-api` 는 Redis ranking 을 읽어 랭킹 API 와 상품 상세 rank 를 제공한다.
- `commerce-api` 는 `23:50` carry-over scheduler 로 다음날 랭킹 seed 를 미리 적재한다.
- 이번 구현에서는 **outbox 는 제거**했고, 랭킹은 eventually consistent 한 read model 로 취급한다.

이번 과제 체크리스트를 그대로 코드 검증 항목으로 바꾸면 아래와 같다.

### 6.1 Ranking Consumer

- [x] 랭킹 ZSET key 가 `ranking:all:{yyyyMMdd}` 형식으로 적재된다.
- [x] TTL 이 2일로 걸린다.
- [x] 날짜별 key 계산이 `occurredAt` 기준으로 올바르게 동작한다.
- [x] 조회/좋아요/주문 이벤트 후 ZSET score 가 의도대로 증가한다.
- [x] 주문 1건 > 좋아요 3건 같은 우선순위가 weight 에 의해 보장된다.

### 6.2 Ranking API

- [x] `GET /api/v1/rankings` 가 page/size/date 기준으로 정상 응답한다.
- [x] 응답이 productId 목록이 아니라 상품 정보 aggregate 결과를 포함한다.
- [x] 상품 상세 조회 시 순위가 포함된다.
- [x] 순위가 없으면 `null` 로 반환된다.

### 6.3 E2E

- [x] 이벤트 발행 -> Kafka consume -> Redis ZSET 반영 -> API 조회 흐름이 동작한다.
- [x] 날짜가 바뀌어도 이전 날짜 key 조회가 가능하다.
- [x] Redis 에 score 누적 후 랭킹 순서가 의도대로 정렬된다.

### 6.4 남은 TODO

- [ ] Kafka publish 실패 시 보정 전략 문서화
- [ ] 중복/역순 이벤트에 대한 복구 전략 정리
- [x] carry-over scheduler 구현 (`23:50`, top 20, `score * 0.2`)
- [ ] hourly ranking 구현 여부 결정
- [x] 실제 E2E 시나리오 추가

---

## 7. 테스트 전략

### 단위 테스트

- `RankingScoreCalculatorTest`
  - 이벤트별 점수 계산 검증
- `RankingKeyGeneratorTest`
  - `yyyyMMdd`, `yyyyMMddHH` key 계산 검증

### 통합 테스트

- Redis integration test
  - `ZINCRBY`, `ZREVRANGE`, `ZREVRANK`, TTL 검증
- Ranking API integration test
  - page 조회 / aggregate 응답 / 상세 순위 검증

### E2E 테스트

시나리오:

1. 상품 생성
2. 조회 이벤트 발행
3. 좋아요 이벤트 발행
4. 주문 이벤트 발행
5. `commerce-streamer` consumer 반영
6. 랭킹 API 조회
7. 상품 상세 조회

이 흐름에서 아래를 확인한다.

- score 누적값
- 정렬 순위
- 상세 API 의 rank 표시

---

## 8. 리스크와 대응

### 리스크 1. 주문 점수 편향

고가 상품이 지나치게 상위권을 독식할 수 있다.

대응:

- 초기에는 `quantity` 중심 점수
- 이후 필요 시 `log(price * quantity)` 로 완화

### 리스크 2. 상세 캐시와 랭킹 최신성 충돌

상품 상세 캐시에 rank 까지 같이 넣으면 최신 순위 반영이 늦어질 수 있다.

대응:

- 랭킹은 상세 캐시와 분리 조회

### 리스크 3. Redis projection 실패

랭킹 적재 실패가 전체 소비 실패로 번지면 안 된다.

대응:

- consumer 에서 dead letter 전략은 유지
- ranking projection 예외 처리 정책을 명확히 정리
- 필요 시 metrics DB 적재와 ranking Redis 적재를 분리 로깅

### 리스크 4. page 조회 시 상품 다건 조회 비용

랭킹 API 는 Redis 에서 productId 만 가져오므로 DB aggregate 비용이 생긴다.

대응:

- size 를 제한한다. 예: max 100
- productId 일괄 조회 + brand 일괄 조회로 N+1 방지

---

## 9. 최종 설계 한 줄 요약

이번 Round 9는

> **기존 Kafka -> product_metrics 집계 흐름에 Redis ZSET 기반 ranking projection 을 추가하고,**
> **API 서버는 Redis 에서 순위를 읽고 상품 정보를 aggregate 해서 응답하는 구조**

로 구현하는 것이 가장 자연스럽다.

이 방식이면:

- outbox 없이도 Kafka 기반 집계 흐름을 유지할 수 있고
- batch listener 구조도 유지할 수 있고
- Redis 는 조회 최적화 계층으로 명확히 분리되며
- Nice-to-have 인 hourly ranking / carry-over 도 자연스럽게 확장 가능하다.
