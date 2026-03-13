# Brand Like Ranking Performance Practice

## 1. 개요

이번 성능 개선의 목표는 다음 3가지를 해결하는 것이었다.

1. **상품 목록 조회 성능 개선**
2. **좋아요 수 정렬 구조 개선**
3. **상품 상세/목록 Redis 캐시 적용**

특히 가장 중요한 hot path는 아래 시나리오로 정의했다.

- `brandId` 기반 상품 필터링
- `LIKES_DESC` 기준 인기 상품 정렬
- 상품 상세 조회 반복 호출

이 문서는 **원래 어떤 구조였는지**, **무엇을 어떻게 바꿨는지**, **지금은 어떤 구조로 동작하는지**를 중심으로 정리한다.

---

## 2. 설계 원칙

`brand-like-ranking-architecture-review.txt`를 기준으로 다음 원칙을 잡았다.

- **좋아요 원본 데이터는 `likes` 테이블이 유지한다**
- **조회 최적화용 집계값은 `products.like_count`로 분리한다**
- **핫패스는 `brandId + likes desc` 조합으로 본다**
- **Redis는 성능 최적화 계층으로만 사용하고, 실패 시 DB fallback이 가능해야 한다**

즉, 한 문장으로 요약하면:

> `likes`는 source of truth로 유지하고, 조회는 `products.like_count + 인덱스 + Redis 캐시`로 최적화했다.

---

## 3. AS-IS: 원래는 어떤 방식이었나

### 3.1 상품 상세 조회

기존 상세 조회는 아래 흐름이었다.

```text
상품 조회
→ 브랜드 조회
→ likes 테이블에서 productId 기준 count 집계
→ 응답 생성
```

즉, 상세 조회가 발생할 때마다 좋아요 수를 실시간으로 다시 계산했다.

---

### 3.2 상품 목록 조회

기존 목록 조회는 아래 흐름이었다.

```text
상품 목록 조회
→ 브랜드 목록 별도 조회
→ likes 테이블에서 상품별 좋아요 수 집계
→ 애플리케이션 메모리에서 좋아요순 정렬
→ 응답 생성
```

실제 로직은 아래와 같은 문제를 가지고 있었다.

- 상품 목록을 먼저 읽는다
- 상품별 좋아요 수를 별도로 다시 집계한다
- `LIKES_DESC` 정렬을 DB가 아니라 애플리케이션 메모리에서 수행한다

---

### 3.3 기존 구조의 문제

이 구조는 데이터가 작을 때는 동작하지만, 상품 수가 10만 건 이상으로 커지면 다음 문제가 발생한다.

- **상품 목록 수집 비용 증가**
- **좋아요 집계 비용 증가**
- **메모리 정렬 비용 증가**
- 정렬 조건이 DB 인덱스를 타지 못해 **읽기 병목** 발생
- Redis 설정은 있었지만 실제 **상품 상세/목록 API 캐시가 없음**

즉, 기존 구조는 다음처럼 볼 수 있다.

> 조회할 때마다 계산하고, 계산한 뒤 메모리에서 정렬하는 구조

---

## 4. TO-BE: 어떻게 바꿨나

### 4.1 좋아요 수 구조 개선: 비정규화 선택

좋아요순 정렬 성능 개선 방식으로는

- `Materialized View`
- `products.like_count` 비정규화

두 가지가 가능했는데, 이번에는 **비정규화**를 선택했다.

### 선택 이유

- MySQL 환경에서 운영 복잡도가 더 낮다
- 좋아요 등록/취소 시 **실시간 반영**이 쉽다
- 조회 쿼리를 단순하게 만들 수 있다

### 적용 방식

- `products.like_count` 컬럼 추가
- 좋아요 등록 시 `like_count + 1`
- 좋아요 취소 시 `like_count - 1`
- 같은 트랜잭션 안에서 동기화

즉, 지금 구조는:

```text
좋아요 쓰기
→ likes 저장/삭제
→ products.like_count 동기화
```

로 바뀌었다.

---

### 4.2 조회 구조 개선: 메모리 정렬 제거

기존에는 좋아요순 정렬을 메모리에서 수행했지만, 지금은 **QueryDSL로 DB 정렬**을 수행한다.

#### 변경 후 정렬 기준

- `LATEST` → `id desc`
- `PRICE_ASC` → `price asc, id desc`
- `LIKES_DESC` → `like_count desc, id desc`

즉, 지금의 목록 조회는:

```text
상품 목록 요청
→ DB에서 brandId / sortType 기준 정렬 조회
→ 결과 응답
```

방식으로 바뀌었다.

---

### 4.3 인덱스 최적화

유즈케이스별로 인덱스를 분리해서 적용했다.

#### products

- `products(brand_id, like_count desc, id desc)`
  - 브랜드 필터 + 좋아요순 정렬
- `products(like_count desc, id desc)`
  - 전체 인기 상품 조회
- `products(brand_id, id desc)`
  - 브랜드 필터 + 최신순 조회
- `products(brand_id, price, id desc)`
  - 브랜드 필터 + 가격순 조회

#### likes

- `likes(product_id)`
  - 원본 집계
  - backfill
  - 검증 쿼리 최적화

즉, 인덱스 전략은

> “필터 조건 + 정렬 조건” 단위로 설계

했다.

---

### 4.4 Redis 캐시 적용

상품 상세 API와 상품 목록 API에 Redis 캐시를 적용했다.

#### TTL

- 상품 상세: **10분**
- 상품 목록: **3분**

#### 캐시 키

- 상세
  - `products:detail:v:{version}:product:{productId}`
- 목록
  - `products:list:gv:{globalVersion}:bv:{brandVersion}:sort:{sortType}:brand:{brandId|all}`

#### 무효화 전략

다음 이벤트 발생 시 캐시를 무효화한다.

- 좋아요 등록/취소
- 상품 등록/수정/삭제
- 브랜드명 변경/비활성화

#### 장애 대응

- Redis 조회 실패 → DB fallback
- Redis 저장 실패 → 요청은 정상 처리
- 캐시 미스 → DB 조회 후 캐시 저장

즉, Redis는 **성능 최적화 계층**이지, 서비스 필수 계층은 아니다.

---

## 5. 지금 구조는 어떤 방식으로 동작하나

현재 구조는 아래처럼 정리할 수 있다.

### 5.1 좋아요 등록

```text
LikeUseCase.register()
→ likes row 저장
→ products.like_count +1
→ 상품 상세 캐시 삭제
→ 상품 목록 캐시 무효화
```

### 5.2 좋아요 취소

```text
LikeUseCase.remove()
→ likes row 삭제
→ products.like_count -1
→ 상품 상세 캐시 삭제
→ 상품 목록 캐시 무효화
```

### 5.3 상품 상세 조회

```text
ProductUseCase.getById()
→ Redis 상세 캐시 조회
→ miss면 DB 조회
→ product.likeCount 사용
→ 응답 후 캐시 저장
```

### 5.4 상품 목록 조회

```text
ProductUseCase.getAll()
→ Redis 목록 캐시 조회
→ miss면 QueryDSL 조회
→ DB에서 brandId + sortType 기준 정렬
→ product.likeCount 사용
→ 응답 후 캐시 저장
```

즉, 지금 구조는:

> 쓰기 시점에 집계값을 맞춰두고, 읽기 시점에는 최대한 빠르게 꺼내서 반환하는 구조

이다.

---

## 6. 성능 비교

10만 건 이상 시드 데이터를 기준으로 `EXPLAIN ANALYZE`를 수행했다.

---

### 6.1 브랜드 필터 + 좋아요순 정렬

#### AS-IS

`likes` 집계 + `GROUP BY` + 정렬

- 실행 시간: **84.2ms**

#### TO-BE

`products.like_count` + 복합 인덱스

- 실행 시간: **2.56ms**

#### 개선 효과

- **약 32.9배 개선**
- 지연시간 약 **96.9% 감소**

---

### 6.2 전체 인기 상품 정렬

#### AS-IS

`likes` 그룹 집계

- 실행 시간: **129ms**

#### TO-BE

`products.like_count` 인덱스 스캔

- 실행 시간: **11.4ms**

#### 개선 효과

- **약 11.3배 개선**
- 지연시간 약 **91.2% 감소**

---

## 7. 부하 테스트 결과

`k6/scripts/product-ranking-hot-path.js`로 아래 시나리오를 실행했다.

- 상품 목록: `700 req/s`
- 상품 상세: `300 req/s`

### 1차 실행 결과

- 총 요청 수: **53,055**
- 실제 처리량: **881.5 req/s**
- 실패율: **0.0019%**
- dropped iterations: **6,947**

### 응답 시간

- 상품 목록
  - avg: **254.85ms**
  - p95: **809.46ms**
- 상품 상세
  - avg: **270.52ms**
  - p95: **916.16ms**

### 해석

DB 쿼리 자체는 크게 개선되었지만, 현재 목록 API는 페이지네이션 없이 많은 데이터를 한 번에 내려주고 있다.

그래서 현재 시점의 병목은 완전히 DB가 아니라,

- 응답 payload 크기
- JSON 직렬화 비용
- 네트워크 전송 비용

으로 이동한 상태다.

즉,

> 이번 개선으로 DB 병목은 크게 완화되었고, 다음 개선 포인트는 pagination/cursor 도입이다.

### 2차 재실행 결과

같은 시나리오로 다시 한 번 부하 테스트를 수행했다.

- 총 요청 수: **58,591**
- 실제 처리량: **973.75 req/s**
- 실패율: **0%**
- dropped iterations: **1,411**

#### 응답 시간

- 상품 상세
  - avg: **132.37ms**
  - p95: **465.59ms**
- 상품 목록
  - avg: **138.34ms**
  - p95: **469.52ms**
- 전체 HTTP
  - avg: **136.52ms**
  - p95: **468.92ms**

#### 재실행 해석

1차 실행보다 처리량이 증가했고, p95 latency도 크게 낮아졌다.

- 처리량: **881.5 req/s → 973.75 req/s**
- 상품 목록 p95: **809.46ms → 469.52ms**
- 상품 상세 p95: **916.16ms → 465.59ms**

이는 Redis 캐시, JVM warm-up, DB buffer cache가 안정화된 이후의 결과로 해석할 수 있다.

즉, 현재 구조는 warm 상태에서 **거의 1000 rps 수준까지 안정적으로 처리** 가능하다고 볼 수 있다.

---

## 8. 체크리스트

### 🔖 Index

- [x] 상품 목록 API에서 `brandId` 기반 검색과 좋아요순 정렬 처리
- [x] 조회 필터, 정렬 조건별 유즈케이스를 분석하여 인덱스 적용
- [x] `EXPLAIN ANALYZE` 기반 전/후 비교 수행

### ❤️ Structure

- [x] 상품 목록/상세 조회 시 좋아요 수 조회 가능
- [x] 좋아요순 정렬 가능
- [x] 좋아요 적용/해제 시 상품 좋아요 수 정상 동기화

### ⚡ Cache

- [x] Redis 캐시 적용
- [x] TTL 적용
- [x] 캐시 키 설계 적용
- [x] 무효화 전략 적용
- [x] 캐시 미스/Redis 장애 시 fallback 처리

---

## 9. 결론

이번 성능 개선은 다음과 같이 요약할 수 있다.

### 원래는

- 좋아요 수를 매 조회마다 집계했고
- 상품 목록은 메모리에서 정렬했고
- 캐시가 없어서 반복 조회 비용이 컸다

### 바꾼 뒤에는

- `products.like_count` 비정규화로 읽기 모델을 만들고
- 인덱스로 `brandId + likes desc`를 최적화하고
- 상세/목록에 Redis 캐시를 적용했다

### 그래서 지금은

- 좋아요 원본은 `likes`
- 조회는 `products.like_count`
- 정렬은 DB
- 반복 조회는 Redis

구조로 동작한다.

즉,

> 원래는 “조회할 때마다 계산하는 구조”였다면, 지금은 “쓰기 때 맞춰두고 읽을 때 빠르게 꺼내는 구조”로 바뀌었다.

---

## 10. 재현 방법

### 1. 인프라 실행

```bash
docker-compose -f ./docker/infra-compose.yml up -d
```

### 2. 10만 건 데이터 적재

```bash
docker exec -i docker-mysql-1 mysql -uapplication -papplication loopers < scripts/performance/seed_brand_like_ranking.sql
```

### 3. EXPLAIN ANALYZE 실행

```bash
docker exec -i docker-mysql-1 mysql -uapplication -papplication loopers < scripts/performance/explain_brand_like_ranking.sql
```

### 4. 부하 테스트 실행

```bash
k6 run k6/scripts/product-ranking-hot-path.js \
  -e BASE_URL=http://localhost:8080 \
  -e HOT_BRAND_IDS=1,2,3,4,5 \
  -e HOT_PRODUCT_IDS=1,2,3,4,5,6,7,8,9,10
```

---

## 11. 다음 단계

- 현재 목록 API는 전체 반환 구조라서 **cursor pagination** 도입이 다음 우선순위다.
- Materialized View는 top-N 랭킹이 매우 고정적이고 read-heavy한 상황에서 추가 검토할 수 있다.
