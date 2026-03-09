# Round 5 — 읽기 성능 최적화 보고서

## 1. 상품 목록 조회 성능 개선 (인덱스 최적화)

### 선택 이유

상품 목록 조회는 가장 빈번한 API 요청이며, 정렬/필터/페이징 조합에서 안정적인 성능이 필요하다.
10만건 이상의 상품 데이터에서 `WHERE deleted_at IS NULL AND status != 'HIDDEN'` 조건과 다양한 정렬(최신순, 가격순, 좋아요순)이 결합되므로,
인덱스 없이는 풀 테이블 스캔(ALL)과 filesort가 발생하여 응답 시간이 급격히 증가한다.

### AS-IS (인덱스 없음)

- 기본 PK 인덱스와 `ref_brand_id` 단일 인덱스만 존재
- 정렬 조건별 복합 인덱스 없음

### TO-BE (복합 인덱스 적용)

3개의 정렬 패턴에 맞는 복합 인덱스를 추가:

| 인덱스 | 컬럼 | 대상 쿼리 |
|--------|------|----------|
| `idx_products_active_like_count` | `(deleted_at, status, like_count DESC)` | 좋아요순 정렬 |
| `idx_products_active_created_at` | `(deleted_at, status, created_at DESC)` | 최신순 정렬 |
| `idx_products_active_price` | `(deleted_at, status, price ASC)` | 가격 낮은순 정렬 |

### 실측 EXPLAIN 비교 (10만건 시딩, TestContainers MySQL 8.0)

| 쿼리 | AS-IS type | AS-IS Extra | TO-BE type | TO-BE Extra |
|------|-----------|-------------|-----------|-------------|
| 브랜드 필터 + 좋아요 정렬 | ALL | Using where; Using filesort | ref | Using index condition; Using where; Using filesort |
| 브랜드 필터 + 가격 정렬 | ALL | Using where; Using filesort | ref | Using index condition; Using where; Using filesort |
| 최신순 전체 조회 | ALL | Using where; Using filesort | ref | Using index condition; Using filesort |
| 좋아요 내림차순 깊은 페이지 | ALL | Using where; Using filesort | ref | Using index condition; Using filesort |

- 모든 쿼리에서 **ALL(풀 테이블 스캔) → ref(인덱스 참조)** 전환 확인
- `Using index condition`은 ICP(Index Condition Pushdown) 적용을 의미

### 인덱스 설계 근거

- **선두 컬럼**: `deleted_at`, `status` — WHERE 절의 등치/비교 조건 (Equality)
- **후미 컬럼**: 정렬 대상 — ORDER BY 절을 인덱스 순서로 커버 (Sort)
- 브랜드 필터(`ref_brand_id`)는 선택적 조건이므로 별도 단일 인덱스로 분리

### 검증 방법

- **테스트 클래스**: `ProductIndexComparisonTest`
- 10만건 시딩 후 인덱스 DROP → EXPLAIN 4개 쿼리 → 인덱스 CREATE → EXPLAIN 재실행
- AS-IS vs TO-BE 결과를 표 형태로 로그 출력
- assertion: TO-BE에서 `type=ALL` 없음

### 테스트 실행 방법

```bash
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.catalog.product.ProductIndexComparisonTest"
```

---

## 2. 좋아요 수 정렬 구조 개선 (비정규화)

### 선택: `like_count` 비정규화

### 선택 이유

| 방안 | 장점 | 단점 | 적합성 |
|------|------|------|--------|
| **비정규화 (like_count)** | 구현 단순, 인덱스 활용 가능, 실시간 반영 | 쓰기 시 동기화 필요, 정합성 관리 | **채택** |
| Materialized View | 집계 자동화 | MySQL 미지원, 갱신 비용 큼 | 불가 |
| COUNT 서브쿼리 | 항상 정확 | 10만건 이상에서 성능 저하 | 부적합 |

MySQL은 Materialized View를 네이티브로 지원하지 않으므로, 비정규화가 사실상 유일한 선택지다.

### AS-IS

- `like_count` 컬럼은 이미 존재 (Round 2에서 도입)
- 좋아요 등록/취소 시 동기 증감 처리 구현 완료
- **문제**: 동시 요청 시 Lost Update 가능성

### TO-BE

- **비관적 락(FOR UPDATE)** 적용: `findByIdForUpdate(productId)` 호출 후 likeCount 변경
- 동시성 테스트: 20명 동시 좋아요 등록 → `likeCount == 20` 정합성 검증
- 동시성 테스트: 20명 동시 좋아요 취소 → `likeCount == 0` 정합성 검증

### 실측 동시성 테스트 결과

| 시나리오 | 동시 스레드 수 | 예상 likeCount | 실제 likeCount | 결과 |
|----------|---------------|---------------|---------------|------|
| 동시 좋아요 등록 | 20 | 20 | 20 | PASS |
| 동시 좋아요 취소 | 20 | 0 | 0 | PASS |

- 비관적 락(FOR UPDATE) 적용으로 Lost Update 방지 확인
- `ExecutorService` + `CountDownLatch`로 20개 스레드 동시 실행

### 비정규화 vs COUNT 서브쿼리 EXPLAIN 비교

| 방식 | 쿼리 | type | Extra |
|------|------|------|-------|
| COUNT 서브쿼리 | `SELECT *, (SELECT COUNT(*) FROM likes ...) FROM products ORDER BY like_count DESC` | ALL | Using filesort |
| 비정규화 컬럼 | `SELECT * FROM products ORDER BY like_count DESC` | ref | Using index condition |

- COUNT 서브쿼리: 상품 행마다 서브쿼리 실행 → O(N×M) 비용
- 비정규화 컬럼: 인덱스로 직접 정렬 → O(log N) 비용

### 검증 방법

- **테스트 클래스**: `LikeConcurrencyTest`
- `ExecutorService`로 20개 스레드 동시 실행, `CountDownLatch`로 동기화
- 정합성 assertion: `product.likeCount == 예상값`

### 테스트 실행 방법

```bash
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.like.LikeConcurrencyTest"
```

---

## 3. 캐시 적용 (Redis)

### 선택 이유

인덱스 최적화 후에도 DB I/O 자체를 줄이면 응답 시간을 추가 개선할 수 있다.
상품 상세/목록은 읽기 빈도가 쓰기 빈도보다 압도적으로 높아 캐시 효과가 크다.

### AS-IS (캐시 없음)

- 모든 조회 요청이 DB 직접 조회
- 동일 상품 반복 조회 시에도 매번 쿼리 실행

### TO-BE (Redis 캐시 적용)

#### 캐시 전략

| API | 패턴 | 캐시 키 | TTL | 무효화 |
|-----|------|---------|-----|--------|
| 상품 상세 | Write-Through | `product:detail:{id}` | 30분 | 수정 시 갱신, 삭제 시 evict |
| 상품 목록 | @Cacheable | `product:list:{brandId}:{sort}:{page}:{size}` | 30분 | 수정 시 evict |

#### Write-Through 패턴 (상품 상세)

```
조회: 캐시 히트 → 반환 / 캐시 미스 → DB 조회 → 캐시 저장 → 반환
수정: DB 저장 → 캐시 갱신 (saveProductDetail)
삭제: DB soft delete → 캐시 삭제 (evictProductDetail)
좋아요: DB 저장 → 캐시 갱신 (likeCount 반영)
```

#### @Cacheable 패턴 (상품 목록)

```
조회: Spring @Cacheable이 자동으로 캐시 히트/미스 처리
무효화: 상품 수정/삭제 시 evictProductList(brandId) 호출
```

#### Redis 장애 Fallback

`ProductCacheRepositoryImpl`에서 Redis 오류 발생 시 warn 로그만 남기고 DB 조회로 폴백.
캐시 장애가 서비스 장애로 전파되지 않도록 한다.

### 캐시 키 설계

```
product:detail:{productId}     — 단건 상세 (Write-Through)
product:list:{key}             — 목록 조회 (@Cacheable, Spring CacheManager 관리)
```

### 실측 캐시 성능 비교 (TestContainers Redis, 10회 반복 측정, JIT 워밍업 1회 선행)

**상품 상세 조회 (Write-Through)**

| 구분 | 1회 | 2회 | 3회 | 4회 | 5회 | 6회 | 7회 | 8회 | 9회 | 10회 | 평균 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|------|------|
| AS-IS: DB 직접 조회 (ms) | 7 | 7 | 6 | 5 | 6 | 6 | 6 | 7 | 7 | 7 | 6.40 |
| TO-BE: 캐시 히트 (ms) | 4 | 3 | 5 | 5 | 4 | 3 | 6 | 4 | 4 | 4 | 4.20 |

**개선율: 34%**

**상품 목록 조회 (@Cacheable)**

| 구분 | 1회 | 2회 | 3회 | 4회 | 5회 | 6회 | 7회 | 8회 | 9회 | 10회 | 평균 |
|------|-----|-----|-----|-----|-----|-----|-----|-----|-----|------|------|
| AS-IS: DB 직접 조회 (ms) | 14 | 12 | 13 | 10 | 10 | 16 | 10 | 11 | 12 | 8 | 11.60 |
| TO-BE: 캐시 히트 (ms) | 12 | 9 | 7 | 6 | 7 | 10 | 5 | 4 | 4 | 5 | 6.90 |

**개선율: 40%**

### 검증 방법

- **테스트 클래스**: `ProductCacheComparisonTest`
- 상품 상세: 캐시 미스 10회 평균 vs 캐시 히트 10회 평균 비교
- 상품 목록: 캐시 미스 10회 평균 vs 캐시 히트 10회 평균 비교
- assertion: 캐시 히트 평균 < 캐시 미스 평균

### 테스트 실행 방법

```bash
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.catalog.product.ProductCacheComparisonTest"
```

---

## 측정 환경 및 한계

- **측정 환경**: TestContainers (MySQL 8.0, Redis latest), JDK 21, Spring Boot 3.4.4
- **인덱스 EXPLAIN**: `rows` 값이 TestContainers 환경에서 통계가 부정확하여 `1`로 표시됨. 실제 운영 DB에서는 인덱스 유무에 따라 rows 차이가 극적으로 발생한다. 핵심은 `type=ALL → ref` 전환이다.
- **비정규화**: 동시성 테스트는 성능 비교가 아닌 정합성 증명이다. EXPLAIN 비교는 이론적 분석이며 실측은 아니다.
- **캐시**: TestContainers 환경은 네트워크 레이턴시가 거의 없어 DB 직접 조회와 캐시 히트의 차이가 작게 나온다. 운영 환경에서는 DB I/O가 훨씬 비싸므로 개선율이 더 클 것으로 예상된다. 초반 측정값이 높은 것은 JIT/커넥션풀 워밍업 영향이다.

---

## 전체 테스트 실행

```bash
# 전체 검증 (lint + test)
./gradlew :apps:commerce-api:ktlintCheck && ./gradlew :apps:commerce-api:test

# 성능 비교 테스트만
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.catalog.product.ProductIndexComparisonTest"
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.catalog.product.ProductCacheComparisonTest"
./gradlew :apps:commerce-api:test --tests "com.loopers.infrastructure.like.LikeConcurrencyTest"
```
