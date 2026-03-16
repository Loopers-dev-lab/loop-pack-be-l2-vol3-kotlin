# 캐시 전략 — 두 가지 캐시를 다르게 설계한 이유

## 문제 상황

상품 관련 API에서 두 가지 성능 병목이 있었다:

1. **상품 상세 조회** (`GET /v1/products/{id}`): `GetProductUseCase`가 매번 `productRepository.findById()` + `brandRepository.findById()`를 호출한다. 인기 상품은 수많은 사용자가 동시에 같은 상품을 반복 조회하는데, DB 결과는 매번 동일하다.
2. **상품 목록 조회** (`GET /v1/products?brandId=1&sort=likes_desc&page=0&size=20`): `GetProductsUseCase`가 매번 QueryDSL로 `products` 테이블을 조회한다. 같은 브랜드·정렬·페이지 조합이 반복 요청되는데, 상품 데이터는 어드민이 수정하기 전까지 바뀌지 않는다.

상품 등록/수정은 어드민만 하므로 변경 빈도가 낮고, 좋아요에 의한 `like_count` 변경도 조회 대비 압도적으로 적다. 그런데 매 요청마다 DB를 조회하니, 유저가 늘수록 DB 부하가 선형으로 증가하는 구조였다.

## 고려한 대안들

| 방식                                | 핵심 아이디어            | 장점                          | 단점                           |
|-----------------------------------|--------------------|-----------------------------|------------------------------|
| **A. 캐시 없음 (현행)**                 | 매번 DB 조회           | 항상 최신 데이터                   | DB 부하 증가, 응답 느림              |
| **B. @Cacheable (Spring 선언적 캐시)** | 어노테이션으로 자동 캐싱      | 도입 빠름, 코드 간결                | 캐시 흐름이 AOP 뒤에 숨음, 세밀한 제어 어려움 |
| **C. RedisTemplate 직접 제어**        | 코드에서 명시적으로 get/set | 캐시 흐름이 눈에 보임, TTL·무효화 세밀 제어 | 코드량 증가, 보일러플레이트              |
| **D. 로컬 캐시 (Caffeine 등)**         | JVM 메모리 캐시         | 네트워크 비용 없음, 매우 빠름           | 다중 인스턴스 시 정합성 문제, 메모리 제한     |
| **E. CDN / API Gateway 캐시**       | 인프라 레벨 캐시          | 애플리케이션 코드 무관                | 무효화 어려움, 개인화 불가              |

## 선택한 방법: B + C 혼합 전략

데이터 특성에 따라 캐시 방식을 다르게 적용했다.

| 대상        | 방식                          | 이유                                 |
|-----------|-----------------------------|------------------------------------|
| **상품 목록** | `@Cacheable` (Spring Cache) | 파라미터 조합이 키가 되는 단순 조회. 선언적 캐시가 딱 맞음 |
| **상품 상세** | `RedisTemplate` 직접 제어       | 변경 시점마다 즉시 갱신해야 하므로 명시적 제어 필요      |

### 1) 상품 목록 — @Cacheable

```kotlin
// GetProductsUseCase.kt
@Cacheable(
    cacheNames = ["product:list"],
    key = "(#brandId ?: 'all') + ':' + #sort + ':' + #page + ':' + #size",
)
@Transactional(readOnly = true)
fun execute(brandId: Long?, sort: String, page: Int, size: Int): PageResult<ProductInfo> {
    val domainSort = ProductSort.entries.find { it.name == sort.uppercase() }
        ?: throw CoreException(ErrorType.BAD_REQUEST, "잘못된 정렬 기준입니다.")
    return productRepository.findActiveProducts(
        brandId?.let { BrandId(it) }, domainSort, page, size,
    ).map { ProductInfo.from(it) }
}
```

- 캐시 키 예시: `product:list::1:LIKES_DESC:0:20` (brandId=1, 좋아요 정렬, 첫 페이지), `product:list::all:LATEST:0:20` (전체 브랜드, 최신순)
- TTL: **5분**. 목록은 약간의 지연을 허용해도 UX에 문제 없다. 어차피 `like_count` 변경은 상세 캐시에서 즉시 반영되고, 목록 정렬 순서가 1~2건 차이로 바뀌는 건 5분 후면 충분하다.
- 무효화: 상품 수정/삭제/복원 시 `evictProductList(brandId)`로 해당 브랜드 + 전체 목록(`all:*`) 캐시를 SCAN 기반으로 삭제.

```kotlin
// RedisConfig.kt
private val PRODUCT_LIST_TTL: Duration = Duration.ofMinutes(5)
// ...
val productListConfig = defaultConfig.entryTtl(PRODUCT_LIST_TTL)
return RedisCacheManager.builder(redisConnectionFactory)
    .withCacheConfiguration("product:list", productListConfig)
    .build()
```

### 2) 상품 상세 — RedisTemplate 직접 제어

```kotlin
// GetProductUseCase.kt — Cache-Aside 패턴
@Transactional(readOnly = true)
fun execute(productId: Long): CatalogInfo {
    val id = ProductId(productId)
    val product = productCacheRepository.findProductDetail(id)      // 1. Redis에서 조회
        ?: productRepository.findById(id)?.also {                   // 2. 캐시 미스 → DB 조회
            productCacheRepository.saveProductDetail(it)            // 3. DB 결과를 Redis에 저장
        }
        ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    if (product.isDeleted() || !product.isActive()) {
        throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
    }
    val brand = brandRepository.findById(product.refBrandId)
        ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다.")
    return CatalogInfo.from(ProductDetail(product = product, brand = brand))
}
```

```kotlin
// ProductCacheRepositoryImpl.kt — Redis 저장 (Master 전용)
override fun saveProductDetail(product: Product) {
    try {
        val key = "$DETAIL_KEY_PREFIX${product.id.value}"  // "product:detail:42"
        val json = objectMapper.writeValueAsString(ProductCacheDto.fromDomain(product))
        redisTemplateMaster.opsForValue().set(key, json, DETAIL_TTL)  // TTL 1시간
    } catch (e: Exception) {
        log.warn("Redis 캐시 저장 실패 [productId={}]: {}", product.id.value, e.message)
    }
}
```

- 캐시 키 예시: `product:detail:42` (상품 ID 42번)
- TTL: **1시간**. 상세는 어드민 수정/좋아요 시점에 즉시 갱신하므로, TTL은 혹시 갱신이 누락된 경우의 안전망 역할.
- 무효화: 상품 변경이 발생하는 모든 시점에서 즉시 갱신하거나 삭제. 아래 무효화 전략 테이블 참고.

## 캐시 무효화 전략: 언제 캐시를 갱신/삭제하는가

캐시의 가장 어려운 문제는 "언제 버릴 것인가"다. 이 프로젝트에서는 **변경 주체(UseCase)가 캐시까지 책임지는 Write-Through 방식**을 채택했다.

| 이벤트 | 상세 캐시 | 목록 캐시 |
|--------|----------|----------|
| 상품 수정 (`UpdateProductUseCase`) | `saveProductDetail(saved)` — 즉시 갱신 | `evictProductList(saved.refBrandId)` — 해당 브랜드 + all 캐시 삭제 |
| 상품 삭제 (`DeleteProductUseCase`) | `evictProductDetail(id)` — 즉시 삭제 | — (삭제된 상품은 다음 조회에서 자연 제외) |
| 상품 복원 (`RestoreProductUseCase`) | `saveProductDetail(saved)` — 즉시 갱신 | `evictProductList(saved.refBrandId)` — 해당 브랜드 + all 캐시 삭제 |
| 좋아요 등록 (`AddLikeUseCase`) | `saveProductDetail(saved)` — 즉시 갱신 | TTL 만료 대기 (5분) |
| 좋아요 취소 (`RemoveLikeUseCase`) | `saveProductDetail(saved)` — 즉시 갱신 | TTL 만료 대기 (5분) |

**목록 캐시 삭제의 구현**: `KEYS` 명령어 대신 `SCAN`으로 패턴 매칭하여 삭제한다. `KEYS`는 Redis를 블로킹하므로 운영 환경에서 위험하다.

```kotlin
// ProductCacheRepositoryImpl.kt
override fun evictProductList(brandId: BrandId?) {
    if (brandId != null) {
        scanAndDelete("$LIST_KEY_PREFIX${brandId.value}:*")  // "product:list:1:*"
        scanAndDelete("${LIST_KEY_PREFIX}all:*")             // "product:list:all:*" (전체 목록도 무효화)
    } else {
        scanAndDelete("$LIST_KEY_PREFIX*")                   // 전체 삭제
    }
}

private fun scanAndDelete(pattern: String) {
    val options = ScanOptions.scanOptions().match(pattern).count(100).build()
    val keys = redisTemplateMaster.execute<Set<String>> { connection ->
        val result = mutableSetOf<String>()
        connection.scan(options).use { cursor ->
            while (cursor.hasNext()) { result.add(String(cursor.next())) }
        }
        result
    } ?: emptySet()
    if (keys.isNotEmpty()) { redisTemplateMaster.delete(keys) }
}
```

**좋아요 시 목록 캐시를 evict하지 않는 이유**: 좋아요는 빈도가 높다. 인기 상품에 초당 수십 건의 좋아요가 들어올 수 있는데, 매번 `SCAN + DELETE`로 수십~수백 개의 목록 캐시 키를 삭제하면 Redis에 불필요한 부하가 생긴다. 목록의 정렬 순서가 1~2건 차이로 즉시 바뀌어야 할 만큼 중요하지 않으므로, 5분 TTL 만료에 맡긴다.

## 각 대안을 버린 이유

### A. 캐시 없음 — DB 커넥션 풀이 병목

`GetProductUseCase`는 `productRepository.findById()` + `brandRepository.findById()`로 DB를 2회 호출한다. `GetProductsUseCase`는 QueryDSL로 count 쿼리까지 포함하면 2회 호출이다. 캐시 없이 동시 사용자 100명이 초당 10회 요청하면, 초당 2000~4000 DB 쿼리가 발생한다. HikariCP 커넥션 풀(기본 10개)이 바로 포화된다.

### D. 로컬 캐시 (Caffeine) — `UpdateProductUseCase`의 변경이 다른 인스턴스에 반영 안 됨

서버가 2대 이상일 때, A 서버에서 `UpdateProductUseCase`로 상품 가격을 수정해도 B 서버의 Caffeine 캐시에는 반영되지 않는다. B 서버의 사용자는 TTL이 만료될 때까지 이전 가격을 본다. Redis는 중앙 저장소이므로, `saveProductDetail(saved)`을 호출하면 모든 서버가 즉시 갱신된 데이터를 읽는다.

### E. CDN/API Gateway — 어드민 수정 즉시 반영 불가

`UpdateProductUseCase`로 상품 정보를 수정한 뒤 `GET /v1/products/42`에 즉시 반영되어야 한다. CDN 캐시는 애플리케이션에서 `saveProductDetail()`처럼 세밀하게 무효화할 수 없다. Purge API가 있더라도 전파 지연이 있고, 브랜드별 목록 캐시를 패턴 매칭으로 삭제하는 것도 불가능하다.

### @Cacheable만 쓰지 않은 이유

상품 상세에 `@Cacheable`을 쓰면 캐시 갱신이 어렵다. 좋아요 등록/취소, 상품 수정 등 여러 UseCase에서 캐시를 즉시 갱신해야 하는데, `@CacheEvict`나 `@CachePut`으로는 다른
UseCase에서 발생한 변경을 유연하게 처리하기 어렵다. RedisTemplate으로 직접 제어하면 "어디서든 `saveProductDetail(product)`을 호출하면 캐시가 갱신된다"는 단순한 규칙이
만들어진다.

### RedisTemplate만 쓰지 않은 이유

상품 목록은 파라미터 조합이 캐시 키가 되는 전형적인 `@Cacheable` 적합 케이스다. 굳이 RedisTemplate으로 키 생성, 직렬화, TTL 설정을 직접 할 이유가 없다. `@Cacheable`이 정확히
이 용도를 위해 존재한다.

## Redis 인프라 설계: Master-Replica 분리

캐시 읽기/쓰기를 분리하여 Redis 부하도 분산했다.

```kotlin
// 읽기: Replica 우선 (defaultRedisTemplate)
private val redisTemplate: RedisTemplate<String, String>       // ReadFrom.REPLICA_PREFERRED

// 쓰기: Master 전용 (masterRedisTemplate)
private val redisTemplateMaster: RedisTemplate<String, String> // ReadFrom.MASTER
```

- 읽기는 Replica에서 처리하여 Master 부하를 줄인다.
- 쓰기는 반드시 Master에서 수행하여 데이터 정합성을 보장한다.
- Replica 지연(replication lag)은 밀리초 수준이므로 캐시 용도로는 문제 없다.

## 캐시 실패 시 안전장치: Fail-Silent

```kotlin
// ProductCacheRepositoryImpl.kt — 조회 (Replica 우선)
override fun findProductDetail(productId: ProductId): Product? {
    return try {
        val key = "$DETAIL_KEY_PREFIX${productId.value}"
        val json = redisTemplate.opsForValue().get(key) ?: return null
        objectMapper.readValue(json, ProductCacheDto::class.java).toDomain()
    } catch (e: Exception) {
        log.warn("Redis 캐시 조회 실패 [key={}]: {}", "$DETAIL_KEY_PREFIX${productId.value}", e.message)
        null  // 캐시 실패 → GetProductUseCase에서 DB 폴백
    }
}
```

`findProductDetail()`이 `null`을 반환하면 `GetProductUseCase`의 엘비스 연산자(`?:`)에 의해 자연스럽게 DB 조회로 넘어간다. Redis가 죽어도 서비스는 정상 동작한다. 캐시는 성능 최적화 수단이지 필수 의존성이 아니다.

## 트레이드오프 요약

| 선택                      | 얻은 것                | 지불한 것                  |
|-------------------------|---------------------|------------------------|
| 상세: RedisTemplate 직접 제어 | 변경 즉시 캐시 갱신, 흐름 투명  | 코드량 증가, DTO 변환 필요      |
| 목록: @Cacheable 선언적 캐시   | 코드 간결, 도입 빠름        | 세밀한 제어 어려움 (TTL 만료 의존) |
| TTL 차등 (상세 1시간, 목록 5분)  | 데이터 특성에 맞는 신선도      | TTL 설계에 대한 판단 필요       |
| Master-Replica 분리       | Redis 부하 분산         | 인프라 설정 복잡도 증가          |
| Fail-Silent             | Redis 장애 시에도 서비스 정상 | 장애 시 DB 부하 일시 증가       |

핵심 판단 기준: **"캐시 방식은 데이터의 변경 빈도와 무효화 요구사항에 따라 달라야 한다."** 단순 조회에는 `@Cacheable`, 능동적 갱신이 필요한 곳에는 `RedisTemplate`. 한 가지 방식만
고집하지 않고 상황에 맞게 혼합했다.
