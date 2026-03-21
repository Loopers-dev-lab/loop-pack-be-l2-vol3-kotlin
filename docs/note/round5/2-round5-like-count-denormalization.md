# 좋아요 수 정렬 성능 문제 — 비정규화 선택과 그 이유

## 문제 상황

대고객 상품 목록 API(`GET /v1/products?sort=likes_desc`)에서 좋아요 수 기준 정렬이 필요하다.
그런데 현재 스키마에서 `likes` 테이블(`ref_user_id`, `ref_product_id`)은 `products` 테이블과 분리되어 있고, `products`에는 좋아요 수 컬럼이 없다.

비정규화 없이 정렬하려면 매 요청마다 이런 쿼리가 필요하다:

```sql
-- products 테이블과 likes 테이블을 JOIN해서 좋아요 수를 집계
SELECT p.*, COUNT(l.id) AS like_count
FROM products p
         LEFT JOIN likes l ON p.id = l.ref_product_id
WHERE p.deleted_at IS NULL
  AND p.status != 'HIDDEN'
GROUP BY p.id
ORDER BY like_count DESC
LIMIT 20 OFFSET 0;
```

문제점:
- `likes` 테이블은 `(ref_user_id, ref_product_id)` unique 제약만 있고, `ref_product_id` 단독 인덱스가 없어서 JOIN 자체가 느리다
- GROUP BY + ORDER BY가 동시에 걸려서 인덱스로 커버 불가능, filesort 발생
- 상품 10만건, 좋아요 수십만건이면 매 요청마다 전체 집계를 수행하는 셈

## 고려한 대안들

| 방식                         | 핵심 아이디어               | 조회 성능 | 쓰기 복잡도 | 실시간성      | 구현 난이도 |
|----------------------------|-----------------------|-------|--------|-----------|--------|
| **A. JOIN + GROUP BY**     | 매번 집계 쿼리              | 느림    | 단순     | 완전 보장     | 낮음     |
| **B. 비정규화 (likeCount 컬럼)** | product 테이블에 count 유지 | 빠름    | 약간 증가  | 실시간       | 중간     |
| **C. 조회 전용 테이블 (CQRS)**    | 별도 집계 테이블 분리          | 빠름    | 높음     | 지연 발생     | 높음     |
| **D. 캐시 기반 정렬**            | Redis sorted set 등    | 매우 빠름 | 높음     | 설계에 따라 다름 | 높음     |

## 선택한 방법: B. 의도적 비정규화

`products` 테이블에 `like_count` 컬럼을 추가하고, 좋아요 등록/취소 시점에 함께 갱신하는 방식을 선택했다.

### 구현 코드

**1) 도메인 모델에 likeCount 필드 추가** (`Product.kt`)

```kotlin
class Product(
    // ...
    likeCount: Int = 0,
) {
    var likeCount: Int = likeCount
        private set

    fun increaseLikeCount() {
        this.likeCount++
    }
    fun decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--
    }
}
```

- likeCount는 도메인 모델이 직접 관리한다. 외부에서 setter로 임의 변경 불가.
- `decreaseLikeCount()`는 0 이하로 내려가지 않도록 방어 로직 포함.

**2) 좋아요 등록 시 product.likeCount 동시 갱신** (`AddLikeUseCase.kt`)

```kotlin
@Transactional
fun execute(userId: Long, productId: Long) {
    val product = productRepository.findByIdForUpdate(ProductId(productId))  // 비관적 락
        ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")

    likeRepository.save(Like(refUserId = UserId(userId), refProductId = ProductId(productId)))
    product.increaseLikeCount()
    productRepository.save(product)
}
```

- `findByIdForUpdate`로 비관적 락을 건 뒤 likeCount를 증가시킨다.
- likes 테이블 INSERT와 products 테이블 UPDATE가 같은 트랜잭션 안에서 실행된다.

**3) 정렬 시 단순 ORDER BY** (`ProductRepositoryImpl.kt`)

```kotlin
val orderSpecifier = when (sort) {
    ProductSort.LATEST -> product.createdAt.desc()
    ProductSort.PRICE_ASC -> product.price.asc()
    ProductSort.LIKES_DESC -> product.likeCount.desc()  // JOIN 없이 단순 정렬
}
```

- QueryDSL로 `likeCount` 컬럼 하나만 참조하면 끝. 집계 쿼리가 필요 없다.

**4) 복합 인덱스로 조회 최적화** (`ProductEntity.kt`)

```kotlin
@Table(
    indexes = [
        Index(name = "idx_products_active_like_count", columnList = "deleted_at, status, like_count DESC"),
    ]
)
```

- WHERE 조건(`deleted_at IS NULL`, `status != HIDDEN`)과 ORDER BY(`like_count DESC`)를 하나의 인덱스로 커버한다.

## 각 대안을 버린 이유

### A. JOIN + GROUP BY — 가장 빈번한 API에서 가장 비싼 쿼리

`GET /v1/products?sort=likes_desc`는 대고객 메인 화면에서 호출되는 가장 빈번한 API다.
이 API가 매번 `products LEFT JOIN likes ... GROUP BY ... ORDER BY`를 실행한다고 생각해보면:

- 현재 `likes` 테이블에는 `(ref_user_id, ref_product_id)` unique 제약만 있다. `ref_product_id` 단독 인덱스가 없어서 JOIN 시 likes 테이블을 풀 스캔한다.
- GROUP BY 결과로 만들어진 `like_count`는 가상 컬럼이라 인덱스를 탈 수 없다. ORDER BY에서 filesort가 불가피하다.
- 상품 10만건 × 좋아요 수십만건이면 매 요청마다 수십만 row를 집계하는 셈이다. 동시 접속 100명이면 초당 수천만 row 처리.

### C. 조회 전용 테이블 (CQRS) — 현재 인프라에서 과한 설계

`product_like_summary(product_id, like_count)` 같은 별도 테이블을 두고, 좋아요 변경 시 이벤트로 동기화하는 방식이다.

이 프로젝트에서 선택하지 않은 이유:
- 현재 이벤트 인프라가 없다. Kafka 모듈(`modules/kafka`)은 있지만 아직 좋아요 이벤트 발행 구조가 없다. CQRS를 위해 이벤트 파이프라인을 새로 구축하는 건 과한 투자다.
- 배치로 동기화하면 실시간성이 떨어진다. `AddLikeUseCase`에서 좋아요를 눌렀는데 목록에 즉시 반영되지 않으면 UX가 어색하다.
- 비정규화(`like_count` 컬럼)로 같은 효과를 훨씬 단순하게 달성할 수 있다.

### D. 캐시 기반 정렬 (Redis Sorted Set) — 이미 캐시는 다른 용도로 쓰고 있다

Redis Sorted Set에 `(productId, likeCount)`를 넣고 `ZREVRANGE`로 정렬하는 방식이다.

이 프로젝트에서 선택하지 않은 이유:
- 이미 `ProductCacheRepository`에서 상품 상세 캐시(RedisTemplate)와 목록 캐시(`@Cacheable`)를 운영하고 있다. 여기에 Sorted Set까지 추가하면 캐시 간 정합성 관리가 3중이 된다.
- 좋아요 등록/취소 시 `AddLikeUseCase`와 `RemoveLikeUseCase`에서 Sorted Set도 함께 갱신해야 하는데, 트랜잭션과 Redis 조작이 원자적이지 않아서 불일치 위험이 있다.
- 브랜드 필터(`brandId`)가 있으면 Sorted Set을 브랜드별로 분리하거나 필터링 로직을 따로 구현해야 한다. 단순 정렬이 아닌 "필터 + 정렬 + 페이징"을 Redis로 구현하는 건 복잡도가 급증한다.

## B안을 선택한 이유

- **조회가 극도로 단순해진다.** JOIN 없이 `ORDER BY like_count DESC` 한 줄이면 끝난다.
- **인덱스 활용이 가능하다.** `(deleted_at, status, like_count DESC)` 복합 인덱스로 WHERE + ORDER BY를 모두 커버한다.
- **쓰기 비용 증가는 감당 가능하다.** 좋아요는 조회 대비 빈도가 훨씬 낮다. 쓰기 1회 추가 비용으로 읽기 수만 회를 절약하는 트레이드오프다.
- **동시성 문제는 비관적 락으로 해결했다.** `findByIdForUpdate`로 product 행에 `SELECT ... FOR UPDATE`를 걸어서, 동시에 좋아요가 들어와도 likeCount가 정확하게
  유지된다.

## 동시성 제어: 왜 비관적 락인가

비정규화의 가장 큰 위험은 동시성 문제다. 두 사용자가 동시에 좋아요를 누르면 likeCount가 1만 증가할 수 있다 (lost update).

```kotlin
// 비관적 락으로 product 행을 잠근 뒤 likeCount 증가
val product = productRepository.findByIdForUpdate(ProductId(productId))
product.increaseLikeCount()
productRepository.save(product)
```

낙관적 락(`@Version`)이 아닌 비관적 락을 선택한 이유:

- 좋아요는 인기 상품에 동시 요청이 몰릴 가능성이 높다.
- 낙관적 락은 충돌 시 재시도 로직이 필요한데, 좋아요처럼 짧은 트랜잭션에서는 비관적 락의 대기 시간이 거의 무시할 수 있는 수준이다.
- 재시도 로직 없이 한 번에 정합성을 보장할 수 있다.

## 트레이드오프 요약

| 선택                  | 얻은 것                    | 지불한 것                      |
|---------------------|-------------------------|----------------------------|
| 비정규화 (likeCount 컬럼) | 조회 O(1) 정렬, 인덱스 활용      | 쓰기 시 product 테이블 추가 UPDATE |
| 비관적 락               | 정합성 100% 보장             | 동시 요청 시 짧은 대기              |
| 복합 인덱스              | WHERE + ORDER BY 인덱스 스캔 | 인덱스 저장 공간, 쓰기 시 인덱스 갱신 비용  |

핵심 판단 기준: **"읽기가 쓰기보다 압도적으로 많은가?"** — 그렇다면 쓰기 쪽에 약간의 비용을 추가해서 읽기를 극적으로 개선하는 비정규화가 합리적이다.
