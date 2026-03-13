# Round 5 — 읽기 성능 최적화: 인덱스, 비정규화, 캐시

## 배경

Round 4에서 쿠폰 도메인과 동시성 제어를 완성한 아키텍처를 유지하면서,
**상품 조회 성능을 구조적으로 개선**한다.
10만+ 데이터 환경에서 인덱스 최적화를 검증하고, Redis 캐시를 도입하여 읽기 병목을 해소한다.

> 기능 요구사항(API 명세, 아키텍처)은 [Round 4 요구사항](round4-requirements-analysis.md)을 기준으로 한다.
> 본 문서는 Round 4 대비 **변경/추가된 사항**만 다룬다.

---

## 0. 변경 요약 (Delta from Round 4)

### 신규

- **Redis 캐시 레이어**: 상품 상세 / 인기 상품 목록에 캐시 적용
- **ProductCacheRepository**: Domain 인터페이스 + Infrastructure Redis 구현체 (DIP 유지)
- **10만 데이터 시딩**: 테스트 코드로 프로그래밍 방식 생성
- **EXPLAIN 분석 보고서**: 인덱스 최적화 전후 비교

### 변경

- 없음 (API 명세, 도메인 모델 변경 없음)

### 삭제

- 없음

### 유지

- 기존 아키텍처 (레이어, DIP, UseCase 패턴) 변경 없음
- 기존 도메인 (User, Catalog, Like, Order, Coupon) 구조 유지
- 기존 비관적 락 전략 유지
- 기존 API 명세 변경 없음

---

## 1. 문제 정의

### 핵심 목표

- 10만+ 상품 데이터에서 브랜드 필터 + 좋아요 순 정렬의 쿼리 성능을 EXPLAIN으로 검증한다
- 기존 비정규화(likeCount) 구조의 정합성을 동시성 테스트로 확인한다
- Redis 캐시를 도입하여 반복 조회의 응답 속도를 개선하고 DB 부하를 줄인다
- 캐시 미스 시에도 서비스가 정상 동작하도록 설계한다

### ① 상품 목록 조회 성능 개선

| 관점 | 문제 |
|------|------|
| 사용자 | 상품 목록 로딩이 느리면 이탈률이 증가한다. 브랜드 필터 + 정렬 조건에서 일관된 응답 속도를 보장해야 한다 |
| 비즈니스 | 상품 탐색은 구매 전환의 핵심 경로이다. 조회 성능 저하는 매출 감소로 직결된다 |
| 시스템 | 10만+ 데이터에서 OFFSET 페이지네이션 + 다양한 정렬 조건 시 인덱스가 제대로 활용되는지 검증해야 한다. `Using filesort`가 발생하면 인덱스 재설계가 필요하다 |

### ② 좋아요 수 정렬 구조 개선

| 관점 | 문제 |
|------|------|
| 사용자 | 인기순 정렬이 정확하고 빠르게 동작해야 한다 |
| 비즈니스 | 인기 상품 노출은 판매 촉진의 핵심이다 |
| 시스템 | JOIN + GROUP BY 집계 대신 비정규화된 likeCount로 빠른 정렬을 보장해야 한다. 좋아요 등록/취소 시 likeCount 동기화의 정합성을 검증해야 한다 |

### ③ 캐시 적용

| 관점 | 문제 |
|------|------|
| 사용자 | 동일 상품을 반복 조회할 때 빠른 응답을 기대한다 |
| 비즈니스 | DB 부하를 줄여 인프라 비용을 절감하고, 트래픽 급증 시에도 안정적으로 서비스해야 한다 |
| 시스템 | 자주 요청되지만 자주 바뀌지 않는 데이터에 캐시를 적용해야 한다. 캐시 정합성과 속도 사이의 균형을 설계해야 한다 |

---

## 2. 유비쿼터스 언어 (추가)

Round 4 유비쿼터스 언어에 다음 용어를 추가한다.

| 한글 | 영문 | 정의 |
|------|------|------|
| 캐시 | Cache | 자주 요청되는 데이터를 빠른 저장소(Redis)에 임시 보관하여 응답 속도를 향상시키는 구조 |
| 캐시 히트 | Cache Hit | 요청한 데이터가 캐시에 존재하여 DB 조회 없이 반환하는 경우 |
| 캐시 미스 | Cache Miss | 요청한 데이터가 캐시에 없어 DB에서 조회 후 캐시에 저장하는 경우 |
| TTL | Time-To-Live | 캐시 데이터의 유효 기간. 만료 시 자동 삭제 |
| 캐시 무효화 | Cache Eviction | 데이터 변경 시 캐시를 명시적으로 삭제하여 정합성을 유지하는 행위 |
| Write-Through | Write-Through | 데이터 쓰기 시 DB와 캐시를 동시에 갱신하는 전략 |
| 인덱스 | Index | DB 테이블의 특정 컬럼에 대한 정렬된 참조 구조. 조회 성능을 향상시킨다 |
| 복합 인덱스 | Composite Index | 여러 컬럼을 조합한 인덱스. 컬럼 순서가 쿼리 성능에 영향을 미친다 |
| 실행 계획 | EXPLAIN | 쿼리가 어떤 인덱스를 사용하고, 몇 행을 스캔하는지 보여주는 분석 도구 |
| 비정규화 | Denormalization | 조회 성능을 위해 의도적으로 데이터를 중복 저장하는 설계 기법 |
| 카디널리티 | Cardinality | 컬럼 값의 고유한 정도. 높을수록 인덱스 효과가 크다 |

---

## 3. 현재 코드 상태 분석

Round 5 과제의 상당 부분이 이전 라운드에서 이미 구현되어 있다. 과제 수행 전 현황을 명확히 한다.

### 3.1 이미 구현된 것

| 항목 | 상세 | 위치 |
|------|------|------|
| Product.likeCount 비정규화 | Domain Model + Entity에 필드 존재 | `domain/catalog/product/model/Product.kt` |
| likeCount 동기화 | AddLikeUseCase → `increaseLikeCount()`, RemoveLikeUseCase → `decreaseLikeCount()` | `application/like/` |
| 좋아요 순 정렬 | ProductSort.LIKES_DESC → QueryDSL `likeCount.desc()` | `infrastructure/catalog/product/ProductRepositoryImpl.kt` |
| 복합 인덱스 4개 | brandId, likeCount, createdAt, price 각각에 대한 복합 인덱스 | `infrastructure/catalog/product/ProductEntity.kt` |
| Redis 인프라 | Master-Replica 구성, RedisTemplate 빈 | `modules/redis/` |

### 3.2 구현 필요

| 항목 | 상세 |
|------|------|
| 10만 데이터 시딩 | 테스트 코드로 다양한 분포의 상품 데이터 생성 |
| EXPLAIN 분석 | 인덱스 활용 여부 검증 + 전후 비교 |
| Redis 캐시 로직 | 상품 상세 / 인기 상품 목록 캐시 적용 |
| 캐시 무효화 | 상세=Write-Through, 목록=TTL+수동 무효화 |
| ProductCacheRepository | Domain 인터페이스 + Infrastructure Redis 구현체 |

---

## 4. 과제별 상세 요구사항

### 4.1 상품 목록 조회 성능 개선 (인덱스)

**사전 조건:** 10만+ 상품 데이터가 준비되어야 한다.

**데이터 시딩 요구사항:**
- 테스트 코드에서 프로그래밍 방식으로 생성 (재현 가능, 분포 제어 가능)
- 브랜드별 상품 수가 다양하게 분포 (일부 브랜드는 수천 개, 일부는 수십 개)
- 가격 범위가 넓게 분포 (1,000원 ~ 1,000,000원)
- 좋아요 수가 다양하게 분포 (0 ~ 10,000)
- ProductStatus: ON_SALE 위주 + 일부 SOLD_OUT, HIDDEN

**EXPLAIN 분석 대상 쿼리:**

| 조건 | 기대 인덱스 | 검증 포인트 |
|------|------------|-----------|
| brandId 필터만 | `idx_products_ref_brand_id` | key 사용 여부 |
| brandId 필터 + price ASC 정렬 | `idx_products_active_price` | Using filesort 없음 |
| brandId 필터 + likeCount DESC 정렬 | `idx_products_active_like_count` | Using filesort 없음 |
| 정렬 없이 전체 조회 (OFFSET 깊은 페이지) | — | rows 수, OFFSET 성능 저하 확인 |

**기존 인덱스 현황:**

```kotlin
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_ref_brand_id", columnList = "ref_brand_id"),
        Index(name = "idx_products_active_like_count", columnList = "deleted_at, status, like_count DESC"),
        Index(name = "idx_products_active_created_at", columnList = "deleted_at, status, created_at DESC"),
        Index(name = "idx_products_active_price", columnList = "deleted_at, status, price ASC"),
    ],
)
```

> **주의:** 현재 복합 인덱스에 `ref_brand_id`가 포함되어 있지 않다. 브랜드 필터 + 좋아요 순 정렬 시 인덱스 활용이 제한될 수 있으므로, EXPLAIN 결과에 따라 추가 인덱스 설계를 검토한다.

**산출물:**
- EXPLAIN 분석 결과 (인덱스 적용 전후 비교)
- 블로그에 AS-IS / TO-BE 비교 첨부

### 4.2 좋아요 수 정렬 구조 개선 (비정규화)

**현재 상태:** 이미 구현 완료. 검증만 수행한다.

**검증 항목:**

| 항목 | 검증 방식 |
|------|----------|
| likeCount 정합성 | N명 동시 좋아요 → likeCount == N 확인 (동시성 테스트) |
| 좋아요 취소 시 동기화 | 좋아요 취소 후 likeCount 감소 확인 |
| likeCount 음수 방지 | `decreaseLikeCount()`에서 0 이하로 내려가지 않음 |
| EXPLAIN 분석 | `ORDER BY like_count DESC` 시 인덱스 사용 확인 |

> **비정규화를 선택한 이유 (이미 결정됨):**
> - JOIN + GROUP BY 집계 방식은 10만+ 데이터에서 성능 저하가 심하다
> - likeCount를 Product에 직접 저장하면 단순 ORDER BY로 처리 가능
> - 쓰기 시 동시성 제어는 비관적 락으로 해결 (이미 구현)

### 4.3 캐시 적용 (Redis)

#### 적용 범위

| API | 캐시 방식 | 무효화 전략 | 근거 |
|-----|----------|-----------|------|
| 상품 상세 조회 | RedisTemplate 직접 사용 | **Write-Through** — 상품 수정/좋아요 시 캐시도 함께 갱신 | 단건 조회, 키가 명확 (`product:{id}`), 정합성 중요 |
| 인기 상품 목록 | @Cacheable AOP 방식 | **TTL + 수동 무효화** — TTL 기본 만료 + 상품 변경 시 관련 키 삭제 | 필터/정렬/페이지 조합이 다양, 특정 키 갱신이 어려움 |

> **두 방식을 모두 구현하는 이유:** 학습 목적. @Cacheable의 간결함과 RedisTemplate의 명시적 제어를 비교하여 각 방식의 trade-off를 이해한다.

#### 캐시 키 설계

| 대상 | 캐시 키 패턴 | 예시 |
|------|-----------|------|
| 상품 상세 | `product:detail:{productId}` | `product:detail:42` |
| 상품 목록 | `product:list:{brandId}:{sort}:{page}:{size}` | `product:list:null:LIKES_DESC:0:20` |

#### TTL 설정

| 대상 | TTL | 근거 |
|------|-----|------|
| 상품 상세 | Write-Through이므로 TTL 길게 (1시간) | 변경 시 즉시 갱신되므로 TTL은 안전망 역할 |
| 상품 목록 | 5~10분 | 목록은 변경 빈도가 높고 조합이 다양. 짧은 TTL로 정합성 유지 |

#### 캐시 무효화 시나리오

| 이벤트 | 무효화 대상 | 방식 |
|-------|-----------|------|
| 상품 정보 수정 (어드민) | `product:detail:{id}` + 해당 상품 포함 가능한 목록 키 | Write-Through (상세) + 수동 삭제 (목록) |
| 좋아요 등록/취소 | `product:detail:{id}` | Write-Through (갱신된 likeCount 반영) |
| 상품 삭제 (soft delete) | `product:detail:{id}` | 캐시 삭제 |
| 상품 등록 | 관련 목록 캐시 | 수동 삭제 또는 TTL 만료 대기 |

#### 캐시 미스 처리

- 캐시에 데이터가 없으면 DB에서 조회 후 캐시에 저장 (Read-Through 패턴)
- Redis 장애 시에도 DB 직접 조회로 서비스 정상 동작 (캐시는 성능 최적화일 뿐, 필수 의존이 아님)
- Redis 연결 실패 시 예외를 삼키고 DB fallback (로그 기록)

#### 아키텍처 — 캐시 레이어 배치

DIP를 유지하여 Domain 레이어에 캐시 인터페이스를 정의하고, Infrastructure에서 Redis로 구현한다.

```
domain/catalog/product/
  └── ProductCacheRepository.kt        ← 인터페이스
infrastructure/catalog/product/
  └── ProductCacheRepositoryImpl.kt    ← RedisTemplate 사용 구현체
```

**ProductCacheRepository 인터페이스 (예상):**

```kotlin
interface ProductCacheRepository {
    fun findProductDetail(productId: ProductId): Product?
    fun saveProductDetail(product: Product)
    fun evictProductDetail(productId: ProductId)
    fun evictProductList(brandId: BrandId?)
}
```

UseCase는 `ProductCacheRepository` 인터페이스만 주입받아 사용한다. Redis 의존이 UseCase로 누출되지 않는다.

---

## 5. 도메인 규칙 (추가/변경)

### 5.1 캐시 관련 규칙

- 캐시는 **읽기 성능 최적화**이며, 비즈니스 로직의 정합성은 DB가 보장한다
- 캐시 미스 시 DB fallback은 반드시 보장한다
- Redis 장애 시 서비스 가용성을 우선한다 (캐시 예외 → 로그 + DB 직접 조회)
- 캐시에 저장되는 데이터는 Domain Model이 아닌 **직렬화 가능한 형태**(DTO 또는 JSON)이어야 한다

### 5.2 기존 규칙 유지

- 비관적 락 전략 (Product, Like, Coupon) 변경 없음
- 도메인 경계 변경 없음
- API 명세 변경 없음

---

## 6. API 변경사항

API 명세 변경 없음. 기존 API의 내부 구현(캐시 적용)만 변경된다.

| API | 변경 내용 |
|-----|----------|
| `GET /api/v1/products/{productId}` | 내부에 캐시 조회/저장 로직 추가 |
| `GET /api/v1/products` | 내부에 캐시 조회/저장 로직 추가 (특정 조건) |
| `PUT /api-admin/v1/products/{productId}` | 캐시 무효화 로직 추가 |
| `POST /api/v1/likes`, `DELETE /api/v1/likes` | 상품 상세 캐시 갱신 (Write-Through) |

---

## 7. 아키텍처 결정

### 7.1 패키지 구조 (추가분)

```
com.loopers/
├── domain/
│   └── catalog/product/
│       └── ProductCacheRepository.kt          ← 신규 (캐시 인터페이스)
└── infrastructure/
    └── catalog/product/
        └── ProductCacheRepositoryImpl.kt      ← 신규 (Redis 구현체)
```

### 7.2 캐시 방식을 API별로 다르게 적용하는 이유

| 기준 | 상품 상세 (RedisTemplate) | 상품 목록 (@Cacheable) |
|------|-------------------------|---------------------|
| 캐시 키 복잡도 | 단순 (`product:detail:{id}`) | 복잡 (4개 파라미터 조합) |
| 무효화 정밀도 | 높음 (특정 상품만 갱신) | 낮음 (어떤 목록이 영향받는지 특정 곤란) |
| 정합성 요구 | 높음 (상세 페이지 = 구매 결정 기준) | 중간 (목록은 약간의 지연 허용) |
| 적합한 전략 | Write-Through | TTL + 수동 무효화 |
| 학습 포인트 | 캐시 흐름이 명시적 | AOP 기반 간결함 |

### 7.3 @Cacheable과 RedisTemplate 비교

| 구분 | @Cacheable | RedisTemplate |
|------|-----------|---------------|
| 도입 속도 | 빠름 (어노테이션 1줄) | 느림 (직접 구현) |
| 코드 간결성 | 매우 간결 | 직접 처리 필요 |
| 캐시 흐름 이해 | AOP로 감춰짐 | 명확히 보임 |
| 복잡한 캐시 구조 | 어려움 | 세밀한 제어 가능 |
| 무효화 제어 | @CacheEvict | 키 직접 삭제 |
| 프로젝트 적합성 | 목록 캐시 (간단 적용) | 상세 캐시 (Write-Through 필요) |

---

## 8. 테스트 전략

### 8.1 인덱스 검증 테스트

| 테스트 | 시나리오 | 검증 포인트 |
|--------|---------|-----------|
| EXPLAIN 분석 | 10만 데이터에서 각 정렬/필터 조합 쿼리 실행 | key 사용 여부, type (range/index/ALL), rows 수, Extra (Using filesort 없음) |
| OFFSET 깊은 페이지 | page=1000 등 깊은 페이지 조회 | rows 수 증가 패턴 확인, 성능 저하 정도 |

### 8.2 비정규화 검증 테스트

| 테스트 | 시나리오 | 검증 포인트 |
|--------|---------|-----------|
| likeCount 동시성 | N명 동시 좋아요 → likeCount == N | 비관적 락으로 Lost Update 방지 |
| 좋아요 취소 동기화 | 좋아요 취소 후 likeCount 감소 | 정확한 감소, 음수 방지 |

### 8.3 캐시 테스트

| 테스트 | 시나리오 | 검증 포인트 |
|--------|---------|-----------|
| 캐시 히트 | 동일 상품 2회 조회 | 2번째 조회 시 DB 호출 없음 |
| 캐시 미스 | 캐시에 없는 상품 조회 | DB 조회 후 캐시 저장 확인 |
| Write-Through | 상품 수정 후 상세 조회 | 캐시에 최신 데이터 반영 |
| TTL 만료 | TTL 경과 후 조회 | 캐시 미스 → DB 재조회 |
| 수동 무효화 | 상품 변경 후 목록 캐시 | 관련 목록 캐시 삭제 확인 |
| Redis 장애 시 fallback | Redis 연결 실패 상태에서 조회 | DB 직접 조회로 정상 응답 |

### 8.4 데이터 시딩

테스트 코드에서 프로그래밍 방식으로 10만+ 상품 데이터를 생성한다.

**분포 요구사항:**
- 브랜드: 50~100개, 브랜드별 상품 수 편차 (10 ~ 5,000개)
- 가격: 1,000 ~ 1,000,000원 범위 (균등 분포 아닌 실제 가격대 분포)
- 좋아요 수: 0 ~ 10,000 (멱법칙 분포 — 소수의 인기 상품에 좋아요 집중)
- 상태: ON_SALE 90%, SOLD_OUT 8%, HIDDEN 2%

---

## 9. 잠재 리스크

### 기존 리스크 상태 업데이트

| 리스크 (Round 4) | 상태 | Round 5 변경사항 |
|-----------------|------|----------------|
| 주문 트랜잭션에 3개 도메인 참여 | **유지** | 변경 없음 |
| 쿠폰 만료 처리 | **유지** | 변경 없음 |
| Fake Repository 유지 비용 | **변경** | ProductCacheRepository Fake 1개 추가 |

### 신규 리스크

| 리스크 | 영향 | 현재 대응 | 향후 대응 |
|--------|------|----------|----------|
| 캐시-DB 정합성 불일치 | Write-Through 실패 시 캐시에 오래된 데이터 잔존 | TTL을 안전망으로 설정 (1시간). Redis 쓰기 실패 시 로그 기록 | 이벤트 기반 캐시 갱신 (비동기) |
| 캐시 키 폭발 | 목록 캐시의 파라미터 조합이 많아 Redis 메모리 증가 | TTL 짧게 설정 (5~10분)으로 자연 정리 | 캐시 대상 조건을 인기 조합으로 제한, maxmemory-policy 설정 |
| Redis 장애 시 DB 과부하 | 캐시 없이 모든 요청이 DB로 향함 | try-catch로 Redis 예외 삼키고 DB fallback | 서킷 브레이커 도입 (Round 6 주제) |
| OFFSET 페이지네이션 성능 저하 | 깊은 페이지(page=1000+)에서 성능 급락 | 현재 규모에서는 허용 | Cursor 기반 페이지네이션으로 전환 검토 |
| 캐시 웜업 | 서버 재시작 시 모든 캐시가 비어 Cold Start 발생 | Read-Through로 점진적 적재 | 필요 시 사전 웜업 배치 검토 |

---

## 10. 설계 결정 사항

### 캐시 레이어를 Infrastructure에 배치하는 이유

- **결정**: Domain에 `ProductCacheRepository` 인터페이스, Infrastructure에 Redis 구현체를 둔다
- **근거**: 프로젝트 아키텍처(`Application → Domain ← Infrastructure`)의 DIP 원칙 일관 유지. UseCase가 Redis에 직접 의존하면 아키텍처 위반이다. Fake 구현체로 단위 테스트도 가능하다

### 캐시 전략을 API별로 다르게 적용하는 이유

- **결정**: 상품 상세는 Write-Through (RedisTemplate), 상품 목록은 TTL + 수동 무효화 (@Cacheable)
- **근거**: 상세는 캐시 키가 단순하고 정합성이 중요하므로 Write-Through가 적합. 목록은 파라미터 조합이 다양하여 특정 키 갱신이 어렵고, 약간의 지연이 허용되므로 TTL 기반이 합리적

### @Cacheable과 RedisTemplate을 모두 사용하는 이유

- **결정**: 두 가지 방식을 각각 다른 API에 적용하여 비교 학습한다
- **근거**: 과제의 학습 목표가 "캐시 흐름 이해"이다. @Cacheable의 간결함과 RedisTemplate의 명시적 제어를 직접 경험하여 trade-off를 이해한다

### 기존 인덱스를 재설계하지 않고 검증하는 이유

- **결정**: 기존 4개 복합 인덱스를 EXPLAIN으로 검증하고, 필요 시에만 추가 인덱스를 설계한다
- **근거**: 이미 합리적인 인덱스가 설계되어 있다. 과제 목표는 "인덱스가 실제로 효과가 있는지 데이터로 증명"하는 것이다. 단, `brandId + likeCount` 복합 조건에서 인덱스 미활용이 확인되면 추가 인덱스를 검토한다

### 데이터 시딩을 테스트 코드로 수행하는 이유

- **결정**: SQL 스크립트가 아닌 테스트 코드에서 프로그래밍 방식으로 10만 데이터를 생성한다
- **근거**: 재현 가능하고, 도메인 불변식을 자동 준수하며, 브랜드·가격·좋아요 수 분포를 코드로 정밀 제어할 수 있다. 스키마 변경 시 컴파일 타임에 잡힌다

---

## 체크리스트

### Index

- [ ] 상품 목록 API에서 brandId 기반 검색, 좋아요 순 정렬 등을 처리했다
- [ ] 10만+ 상품 데이터를 준비하고 다양한 분포로 시딩했다
- [ ] 조회 필터, 정렬 조건별 유즈케이스를 분석하여 EXPLAIN 분석을 수행했다
- [ ] 인덱스 적용 전후 성능 비교를 진행했다

### Structure (비정규화)

- [ ] 상품 목록/상세 조회 시 좋아요 수를 조회 및 좋아요 순 정렬이 가능하도록 구조가 구현되어 있다
- [ ] 좋아요 적용/해제 진행 시 상품 좋아요 수가 정상적으로 동기화된다
- [ ] 동시성 테스트로 likeCount 정합성을 검증했다

### Cache

- [ ] 상품 상세 API에 Redis 캐시를 적용했다 (RedisTemplate, Write-Through)
- [ ] 상품 목록 API에 Redis 캐시를 적용했다 (@Cacheable, TTL + 수동 무효화)
- [ ] TTL 설정이 적용되어 있다
- [ ] 캐시 키 설계가 명확하다
- [ ] 캐시 무효화 전략이 적용되어 있다 (상품 수정/좋아요 시)
- [ ] 캐시 미스 상황에서도 서비스가 정상 동작한다
- [ ] Redis 장애 시에도 DB fallback으로 서비스가 정상 동작한다
