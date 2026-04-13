# Round 9 — Redis ZSET 기반 실시간 랭킹 시스템

## 배경

현재 유저 행동 이벤트(조회, 좋아요, 주문)는 R7에서 구축한 Kafka → commerce-streamer 파이프라인을 통해
`product_metrics` 테이블에 집계되고 있다.

그러나 이 데이터를 기반으로 "인기 상품"을 조회하려면 `GROUP BY + ORDER BY` 쿼리가 필요하고,
데이터가 쌓일수록 느려지며, 조회 빈도가 높은 랭킹 특성상 DB 과부하로 이어진다.

Redis Sorted Set을 활용하여 이벤트 소비 시점에 실시간으로 랭킹 점수를 갱신하고,
API는 ZSET을 직접 조회하여 Top-N 및 개별 상품 순위를 제공한다.

### 학습 목표

- Redis Sorted Set(ZSET)을 점수 기반 실시간 랭킹에 활용한다
- 이벤트별 가중치(Weighted Sum)를 설계하고 적용한다
- 일별 Key 전략과 TTL로 시간 윈도우를 관리한다
- 콜드 스타트 문제를 이해하고 완화 전략(Score Carry-Over)을 적용한다

### 키워드

- Redis Sorted Set (ZSET), ZINCRBY
- 가중치 합산 (Weighted Sum)
- Top-N 조회 (ZREVRANGE)
- 일별 Key 전략, TTL
- 콜드 스타트 (Cold Start), Score Carry-Over
- 시간의 양자화 (Time Quantization)
- Eventual Consistency, 교차 저장소 원자성
- 멱등성 (Idempotency)

### 우선순위

**Must-Have**

- Kafka Consumer에서 이벤트 소비 시 Redis ZSET에 가중치 기반 점수 갱신
- 일별 키 전략 (`ranking:all:{yyyyMMdd}`) + TTL 2일
- 랭킹 Page 조회 API (`GET /api/v1/rankings`)
- 상품 상세 조회 시 해당 상품의 순위 정보 포함

**Nice-to-Have**

- 콜드 스타트 완화를 위한 Score Carry-Over 스케줄러
- 시간 단위(초실시간) 랭킹
- 실시간 Weight 조절 기능
- Kafka 배치 리스너를 통한 스루풋 최적화

---

## 1. 문제 정의

### 핵심 목표

- 유저 행동 이벤트를 실시간으로 랭킹 점수에 반영하여 인기 상품을 효율적으로 노출
- 가중치 기반 점수 합산으로 서비스 전략에 맞는 랭킹 지표 설계
- 일별 키 분리로 시간 윈도우를 관리하여 롱테일 현상 방지
- 콜드 스타트 완화로 윈도우 전환 시에도 의미 있는 랭킹 제공

### 랭킹 시스템

| 관점 | 문제 |
|------|------|
| 사용자 | 인기 있는 상품을 빠르게 발견하고 싶음. 홈 화면, 인기순 정렬 등에서 랭킹 정보를 기대함 |
| 비즈니스 | 인기 상품 노출은 구매 전환율에 직결됨. 오래된 상품이 상위를 독식하면 신상품 노출 기회가 사라짐 (롱테일 문제) |
| 시스템 | DB 기반 집계(`GROUP BY + ORDER BY`)는 데이터 증가에 따라 성능이 저하됨. 조회 빈도가 높아 DB 과부하 위험 |

### 가중치 합산

| 관점 | 문제 |
|------|------|
| 사용자 | 단순 조회 수 기반 랭킹은 "많이 본 상품 = 인기 상품"이라는 왜곡을 유발함 |
| 비즈니스 | 조회/좋아요/주문은 스케일이 달라 단순 합산 시 조회 수가 지배함. 서비스 전략에 따라 지표 중요도 조절 필요 |
| 시스템 | 이벤트 타입별 점수를 분리 관리하면 ZSET을 여러 개 유지해야 하므로 단일 ZSET에 가중치 합산 점수 사용 |

### 콜드 스타트

| 관점 | 문제 |
|------|------|
| 사용자 | 하루가 바뀌면 랭킹이 텅 비어 있음. 어제 인기 있던 상품도 보고 싶음 |
| 비즈니스 | 랭킹이 비어 있으면 홈 화면에 노출할 컨텐츠가 없음. 클릭/구매가 발생하지 않는 악순환 |
| 시스템 | 새 키 생성 시 모든 상품 점수가 0이므로 ZREVRANGE 결과가 비어 있거나 무의미함 |

---

## 2. 유비쿼터스 언어

| 한글 | 영문 | 정의 |
|------|------|------|
| 랭킹 | Ranking | 가중치 점수 기준으로 상품을 정렬한 읽기 모델 |
| 랭킹 키 | Ranking Key | 특정 기간의 랭킹을 저장하는 Redis 키. 예: `ranking:all:20260404` |
| 랭킹 점수 | Ranking Score | 이벤트별 가중치를 곱해 합산한 단일 스코어 (ZINCRBY로 누적) |
| 일간 랭킹 | Daily Ranking | KST(`Asia/Seoul`) 기준 하루 단위로 분리한 랭킹 |
| 가중치 | Weight | 이벤트 타입별 중요도를 나타내는 계수. 총합 1.0 |
| 시간 윈도우 | Time Window | 랭킹 점수가 유효한 시간 범위 (이번 구현에서는 1일) |
| 콜드 스타트 | Cold Start | 새 시간 윈도우 시작 시 점수가 없어 랭킹이 비어 있는 문제 |
| 스코어 캐리오버 | Score Carry-Over | 전일 점수의 일부(예: 10%)를 새 키에 복사하여 콜드 스타트를 완화하는 기법 |
| Top-N | Top-N | 점수 기준 상위 N개 상품 목록 (ZREVRANGE로 조회) |
| 비활성 상품 보정 | Inactive Product Filtering | 랭킹 ZSET에는 남아있으나 삭제/비활성 상태인 상품을 API 응답에서 제외하는 처리 |

---

## 3. 액터 정의

| 액터 | 식별 방식 | 권한 및 역할 |
|------|----------|------------|
| 비인증 사용자 | 없음 | 랭킹 조회 (Top-N, 상품별 순위) |
| 인증된 사용자 | JWT 토큰 (userId) | 랭킹 조회 + 유저 행동(조회, 좋아요, 주문)으로 이벤트 발행 |
| commerce-streamer | Kafka Consumer | 이벤트 소비 → ZSET 점수 갱신 + product_metrics 적재 (기존 R7) |
| 캐리오버 스케줄러 | 내부 컴포넌트 (Spring @Scheduled) | 일별 키 전환 시 전일 점수 일부를 복사 (Nice-to-Have) |
| Redis | 외부 인프라 | ZSET 기반 랭킹 데이터 저장소 |

---

## 4. 유저 시나리오

### 4.1 랭킹 조회 (모든 사용자)

**사전 조건:** 이벤트가 발생하여 ZSET에 점수가 존재함.

**Top-N 조회:**
- `GET /api/v1/rankings?date=20260404&size=20&page=0`
- Redis ZREVRANGE로 해당 날짜의 상위 N개 상품 ID를 조회
- 상품 ID 목록으로 상품 정보를 조회하여 Aggregation 후 반환

**상품 상세 조회 시 순위 포함:**
- 기존 `GET /api/v1/products/{productId}` 응답에 순위 정보 추가
- ZREVRANK로 해당 상품의 오늘 랭킹 순위를 조회
- 랭킹에 없으면 null 반환

**예외 흐름:**

| 조건 | 응답 | 설명 |
|------|------|------|
| 해당 날짜에 랭킹 데이터 없음 | 빈 목록 반환 | 콜드 스타트 또는 TTL 만료 |
| date 파라미터 누락 | 오늘 날짜 기본값 | 미지정 시 당일 랭킹 조회 |
| 유효하지 않은 date 형식 | 400 BAD_REQUEST | yyyyMMdd 형식이 아닌 경우 |

### 4.2 이벤트 기반 점수 갱신 (commerce-streamer)

**사전 조건:** 유저가 상품 조회/좋아요/주문을 수행하여 Kafka에 이벤트가 발행됨.

**점수 갱신:**
- CatalogEventConsumer / OrderEventConsumer가 이벤트를 소비
- 이벤트 타입에 따라 가중치를 적용한 점수를 계산
- `ZINCRBY ranking:all:{yyyyMMdd} {score} {productId}`로 해당 상품의 점수를 누적
- 기존 product_metrics 적재 로직은 그대로 유지

**이벤트-점수 매핑:**

| 이벤트 타입 | 가중치(Weight) | Score | 최종 점수 |
|------------|--------------|-------|----------|
| PRODUCT_VIEWED | 0.1 | 1 | +0.1 |
| LIKE_ADDED | 0.2 | 1 | +0.2 |
| LIKE_REMOVED | 0.2 | -1 | -0.2 |
| PAYMENT_COMPLETED | 0.7 | quantity | +0.7 × quantity |

### 4.3 Score Carry-Over (Nice-to-Have)

**사전 조건:** 자정 전(예: 23:50) 스케줄러 실행.

**캐리오버:**
- `ZUNIONSTORE ranking:all:{내일날짜} 1 ranking:all:{오늘날짜} WEIGHTS 0.1`
- 오늘의 점수를 10%만 내일 키로 복사
- 내일 키의 TTL을 2일로 설정

---

## 5. 도메인 규칙 (Business Rules)

### 점수 계산 규칙

- 랭킹 점수는 가중치 합산(Weighted Sum) 방식으로 계산한다.
- `Score(p) = W(view) × Count(view) + W(like) × Count(like) + W(order) × Count(order)`
- 기본 가중치: view=0.1, like=0.2, order=0.7 (합계 1.0)
- ZINCRBY를 사용하여 이벤트 발생마다 해당 가중치 점수를 원자적으로 누적한다.
- LIKE_REMOVED 이벤트는 `-0.2`로 점수를 차감한다. 좋아요 취소는 명확한 관심 철회 시그널이며, 차감하지 않으면 좋아요→취소 반복으로 점수를 무한히 올릴 수 있다.
- 점수는 실수(Double)로 저장한다.

### 키 전략 규칙

- 키 패턴: `ranking:all:{yyyyMMdd}` (일별 분리)
- 비즈니스 날짜는 KST(`Asia/Seoul`) 기준으로 계산한다.
- 현재 이벤트 스키마에 `occurredAt`이 없으므로, 집계 기준은 Consumer 처리 시점의 날짜다.
- TTL: 2일 (172,800초). 어제와 오늘의 랭킹을 조회할 수 있도록 보장.
- 키가 처음 생성될 때 TTL을 설정한다 (ZINCRBY 첫 호출 시).

### 조회 규칙

- Top-N 조회는 ZREVRANGE로 score 내림차순 정렬된 상위 N개를 반환한다.
- 개별 상품 순위는 ZREVRANK로 조회하며, 0-based를 1-based로 변환하여 반환한다.
- 랭킹 조회 시 상품 ID뿐 아니라 상품 상세 정보(이름, 가격, 이미지 등)를 함께 제공한다.
- date 파라미터가 없으면 오늘 날짜로 기본 조회한다.

### 상품 노출 규칙

- 삭제되었거나 비활성 상태인 상품은 ZSET에 남아있어도 랭킹 API 응답에서 제외한다.
- 필터링으로 인해 페이지 크기가 부족해질 수 있으므로 over-fetch 여부는 구현 시 판단이 필요하다.

### 멱등성 및 교차 저장소 원자성

- 동일 Kafka 이벤트는 랭킹 점수에 중복 반영되면 안 된다.
- 기존 `event_handled` 테이블을 재사용하되, RDB(`event_handled` 기록)와 Redis(`ZINCRBY` 반영)는 하나의 트랜잭션으로 묶이지 않으므로 교차 저장소 원자성 문제가 존재한다.
- 순서만으로는 안전해지지 않는다: Redis 먼저 → RDB 실패 시 재처리로 중복 반영, RDB 먼저 → Redis 실패 시 점수 유실. 둘 다 trade-off가 있으며 별도 dedup/보정 전략이 필요하다.
- 현재 코드(`UpdateProductMetricsUseCase`)는 RDB 트랜잭션 안에서만 멱등성을 관리하고 있어, Redis를 끼우면 동일한 문제가 발생한다.

### 캐리오버 규칙 (Nice-to-Have)

- 매일 23:50에 스케줄러가 다음 날 키를 미리 생성한다.
- 전일 점수의 10%를 ZUNIONSTORE로 복사한다.
- 캐리오버 가중치는 오늘의 점수가 빠르게 상위를 차지할 수 있도록 낮게 유지한다.

---

## 6. API 명세

### 6.1 랭킹

| METHOD | URI | 인증 | 설명 |
|--------|-----|------|------|
| GET | /api/v1/rankings | 불필요 | 랭킹 Page 조회. date/size/page 파라미터 |

**Query Parameters:**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| date | String | N | 오늘 (yyyyMMdd) | 조회할 랭킹 날짜 |
| size | Int | N | 20 | 페이지당 항목 수 |
| page | Int | N | 0 | 페이지 번호 (0-based, 기존 API 관례와 동일) |

**응답 예시:**

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
        "score": 85.3
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150
  }
}
```

### 6.2 기존 API 변경사항

- `GET /api/v1/products/{productId}`: 응답에 `rank` 필드 추가 (순위에 없으면 null)

---

## 7. 인증/인가

랭킹 조회는 공개 API로, 인증 없이 접근 가능하다.

| 경로 | 인증 요구 |
|------|----------|
| GET /api/v1/rankings | 불필요 |
| GET /api/v1/products/{productId} (순위 포함) | 기존과 동일 |

---

## 8. 기존 시스템과의 관계

### 기존 완료 (재사용)

- **이벤트 파이프라인 (R7)**: commerce-api → Kafka → commerce-streamer. 이벤트 소비 및 product_metrics 적재
- **CatalogEventConsumer**: `catalog-events` 토픽 소비. PRODUCT_VIEWED, LIKE_ADDED 이벤트 처리
- **OrderEventConsumer**: `order-events` 토픽 소비. PAYMENT_COMPLETED 이벤트 처리
- **UpdateProductMetricsUseCase**: 이벤트 멱등성 처리(EventHandled) + product_metrics 갱신
- **modules/redis**: Redis 인프라 모듈
- **상품 조회 API**: GetProductUseCase, GetProductsUseCase

### 신규 구현

- **랭킹 점수 갱신**: commerce-streamer의 이벤트 소비 흐름에 ZINCRBY 기반 ZSET 점수 갱신 추가
- **랭킹 조회 API**: `GET /api/v1/rankings` (commerce-api)
- **상품 상세 순위 포함**: GetProductUseCase 응답에 ZREVRANK 기반 순위 추가
- **랭킹 도메인**: ZSET 키 생성, 점수 계산, 가중치 관리

### 추후 확장

- Score Carry-Over 스케줄러 (Nice-to-Have)
- 시간 단위 랭킹 — `ranking:all:{yyyyMMddHH}` (Nice-to-Have)
- 실시간 Weight 조절 기능 (Nice-to-Have)
- Kafka 배치 리스너로 스루풋 최적화 (Nice-to-Have)
- 주간/월간 집계 (ZUNIONSTORE 활용) — 차주 예고

---

## 9. 잠재 리스크

| 리스크 | 영향 | 현재 대응 | 향후 대응 |
|--------|------|----------|----------|
| 콜드 스타트 (윈도우 전환) | 자정 이후 랭킹이 비어 있거나 무의미함 | 빈 목록 반환 허용 | Score Carry-Over 스케줄러 (Nice-to-Have) |
| 롱테일 (누적 랭킹) | 오래된 상품이 상위 독식, 신상품 노출 불가 | 일별 키 분리로 매일 리셋 | 주간/월간 별도 집계 |
| ZSET 메모리 사용 | 상품 수가 많으면 메모리 증가 | TTL 2일로 자동 만료 | 메모리 모니터링, 필요 시 키 분할 검토 (ZREMRANGEBYRANK는 개별 순위 조회 요구사항과 충돌하므로 사용 불가) |
| 가중치 편향 | 특정 이벤트가 점수를 지배하여 왜곡 | view=0.1, like=0.2, order=0.7 (합계 1.0) | 모니터링 기반 가중치 튜닝 |
| Redis 장애 | 랭킹 조회 불가 | 빈 목록 또는 에러 반환 | Fallback (DB 기반 조회 또는 캐시) |
| 이벤트 유실/지연 | 랭킹 점수가 실제와 불일치 | Kafka 기본 재시도 + 멱등성 처리 | 주기적 보정 배치 |
| 동시 이벤트 대량 발생 | ZINCRBY 호출 폭주 | Redis 단일 스레드로 원자성 보장 | 배치 리스너로 묶어서 처리 |
| 교차 저장소 원자성 부재 | event_handled(RDB)와 ZINCRBY(Redis) 간 중복/누락 가능 | 설계 단계에서 명시적 인지, 반영 순서 고정 | Redis dedup key 또는 보정 배치 |
| 비활성 상품 잔존 | 삭제/비활성 상품이 랭킹에 노출 | 응답 시 필터링 | 주기적 ZREM 정리 작업 |
| 처리 날짜 ≠ 이벤트 발생 날짜 | 일자 경계 근처 랭킹 왜곡 | Consumer 처리 시점 기준 (현재 이벤트에 occurredAt 없음) | 이벤트 payload에 occurredAt 추가 |
| TTL 만료 후 과거 조회 불가 | 이전 날짜 랭킹 히스토리 손실 | 2일 유지 | 장기 보존 필요 시 RDB 적재 |

---

## 10. 설계 결정 사항

### 점수 갱신 위치: commerce-streamer (Q1)

- 랭킹 점수 갱신은 commerce-streamer의 기존 Consumer에서 수행한다.
- 이유: 이미 이벤트 소비 및 product_metrics 적재 흐름이 존재함. 동일 소비 지점에서 ZSET 갱신을 추가하면 이벤트를 중복 소비하지 않고 일관된 처리 가능.
- 트레이드오프: commerce-streamer에 Redis 의존성 추가 필요.

### 주문 점수 산정: 건수 기반 vs 금액 기반 (Q2)

- **결정 필요**: 과제에서 `score = price * amount`를 예시로 제시했으나, 현재 OrderEventConsumer는 `quantity`만 전달하고 `price` 정보가 없음.
- **기본안: `0.7 × quantity`** — 현재 payload에 quantity가 존재하며, product_metrics도 수량 기반 집계를 사용 중이므로 일관됨.
- 선택지 B: 금액 기반 (`0.7 × price × quantity`) — 이벤트 페이로드에 price 추가 필요, 스케일 차이로 log 정규화 고려.
- 매출 기반 랭킹이 필요해지면 이벤트 스키마 확장이 선행되어야 한다.

### 가중치 하드코딩 vs 설정 외부화 (Q3)

- Must-Have에서는 코드 내 상수로 관리한다.
- 이유: 가중치 변경 빈도가 낮고, 실시간 조절은 Nice-to-Have 범위. 초기 구현 복잡도를 낮춘다.
- 향후: application.yml 또는 Redis 기반 설정으로 외부화 가능.

### LIKE_REMOVED 점수 차감 (Q4)

- 좋아요 취소 시 `-0.2`로 점수를 차감한다.
- 이유: 좋아요 취소는 명확한 "관심 철회" 시그널이다. 차감하지 않으면 좋아요→취소 반복으로 점수를 무한히 올릴 수 있어 어뷰징에 취약하다.
- 참고: ZINCRBY는 음수도 지원하므로 구현상 추가 비용 없음.

### 비즈니스 날짜는 KST 기준 (Q5)

- 랭킹 키의 날짜는 `Asia/Seoul` 기준으로 계산한다.
- 이유: 서비스 사용자와 운영 문맥이 KST 중심이며, "오늘의 인기 상품"은 한국 시간 기준이 자연스럽다.
- 현재 이벤트에 `occurredAt`이 없으므로 Consumer 처리 시점의 KST 날짜를 사용한다.

### 페이지 번호 0-based 유지 (Q6)

- 과제 원문은 `page=1`(1-based) 예시이지만, 기존 프로젝트 API가 0-based이므로 랭킹도 0-based로 통일한다.
- 이유: API 일관성. 클라이언트가 엔드포인트마다 다른 페이지 관례를 기억해야 하면 혼란 초래.

### 랭킹 조회 인증 여부 (Q7)

- 랭킹 조회는 인증 불필요 (공개 API).
- 이유: 인기 상품 목록은 비로그인 유저에게도 노출되어야 구매 전환에 유리함.

### 랭킹 API 위치: commerce-api (Q8)

- 랭킹 조회 API는 commerce-api에 구현한다.
- 이유: 랭킹 조회는 상품 정보와 Aggregation이 필요하므로 상품 도메인이 있는 commerce-api가 적합. commerce-streamer는 이벤트 소비 전용.

---

## Redis 키 설계

| 키 패턴 | 자료구조 | 용도 | TTL |
|---------|---------|------|-----|
| `ranking:all:{yyyyMMdd}` | Sorted Set | 일간 랭킹. score=가중치 합산 점수, member=productId | 2일 (172,800초) |

### 확장 시 고려 가능한 키

| 키 패턴 | 자료구조 | 용도 |
|---------|---------|------|
| `ranking:hourly:{yyyyMMddHH}` | Sorted Set | 시간 단위 실시간 랭킹 (Nice-to-Have) |
| `ranking:carry-over:lock:{yyyyMMdd}` | String | Carry-Over 중복 실행 방지 (Nice-to-Have) |

### 핵심 Redis 명령어

| 명령어 | 용도 |
|--------|------|
| `ZINCRBY ranking:all:{yyyyMMdd} {score} {productId}` | 이벤트별 가중치 점수 누적 |
| `ZREVRANGE ranking:all:{yyyyMMdd} {start} {stop} WITHSCORES` | Top-N 조회 (score 내림차순) |
| `ZREVRANK ranking:all:{yyyyMMdd} {productId}` | 특정 상품의 순위 조회 (0-based) |
| `ZSCORE ranking:all:{yyyyMMdd} {productId}` | 특정 상품의 점수 조회 |
| `ZCARD ranking:all:{yyyyMMdd}` | 랭킹에 진입한 전체 상품 수 |
| `EXPIRE ranking:all:{yyyyMMdd} 172800` | TTL 설정 (2일) |
| `ZUNIONSTORE ranking:all:{내일} 1 ranking:all:{오늘} WEIGHTS 0.1` | Score Carry-Over (Nice-to-Have) |

---

## Step별 구현 범위

### Step 1 — ZSET 점수 갱신 (commerce-streamer)

- [ ] 일별 키 생성 유틸 구현 (`ranking:all:{yyyyMMdd}`)
- [ ] 가중치 상수 정의 (view=0.1, like=0.2, order=0.7)
- [ ] CatalogEventConsumer → PRODUCT_VIEWED 이벤트 시 ZINCRBY 0.1
- [ ] CatalogEventConsumer → LIKE_ADDED 이벤트 시 ZINCRBY +0.2
- [ ] CatalogEventConsumer → LIKE_REMOVED 이벤트 시 ZINCRBY -0.2
- [ ] OrderEventConsumer → PAYMENT_COMPLETED 이벤트 시 ZINCRBY +(0.7 × quantity)
- [ ] 키 최초 생성 시 TTL 2일 설정
- [ ] 날짜 계산은 KST(`Asia/Seoul`) 기준
- [ ] 기존 product_metrics 적재 로직은 그대로 유지
- [ ] 멱등성 처리 방식 점검 (event_handled와 Redis 반영 순서)

### Step 2 — 랭킹 조회 API (commerce-api)

- [ ] `GET /api/v1/rankings` API 구현 (date, size, page 파라미터)
- [ ] ZREVRANGE로 상위 N개 상품 ID 조회
- [ ] 상품 ID 목록으로 상품 상세 정보 Aggregation
- [ ] 페이지네이션 처리 (offset 기반)
- [ ] date 미지정 시 오늘 날짜 기본값
- [ ] 비활성/삭제 상품 필터링

### Step 3 — 상품 상세에 순위 포함 (commerce-api)

- [ ] 상품 상세 조회 시 ZREVRANK로 해당 상품의 오늘 랭킹 순위 조회
- [ ] 순위 존재 시 1-based로 변환하여 응답에 포함
- [ ] 순위에 없으면 null 반환

### 검증

- [ ] 이벤트 발행 → ZSET 점수 반영 → API 조회까지 E2E 흐름 확인
- [ ] 일자가 변경되어도 이전 날짜의 랭킹 조회가 정상 동작하는지 확인
- [ ] 가중치 적용이 의도대로 랭킹 순서에 반영되는지 확인 (e.g. 주문 1건 > 좋아요 3건)
- [ ] TTL 만료 후 키가 삭제되는지 확인
- [ ] 동일 상품에 여러 이벤트가 발생하면 점수가 정확히 누적되는지 확인
