# 상품 목록 조회 성능 최적화 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 상품 목록 API에서 `like_count` 정렬을 지원하고, 복합 인덱스로 100만~1000만 건 데이터 규모의 조회 성능을 O(log n)으로 최적화

**Architecture:**
- DB 레벨: (brand_id/status, 정렬컬럼) 복합 인덱스 6개 생성
- Repository 레벨: QueryDSL에 like_count 정렬 옵션 추가
- API 레벨: ProductSortOption enum으로 정렬 옵션 정규화

**Tech Stack:** Kotlin, Spring Data JPA, QueryDSL, MySQL (B-tree Index)

---

## Task 1: 데이터베이스 마이그레이션 - 인덱스 생성

**Files:**
- Create: `apps/commerce-api/src/main/resources/db/migration/V20260309__Add_product_composite_indexes.sql`

**Step 1: 마이그레이션 SQL 파일 생성**

```sql
-- V20260309__Add_product_composite_indexes.sql

-- Brand 필터링 용 복합 인덱스
CREATE INDEX idx_brand_like ON products(brand_id, like_count DESC);
CREATE INDEX idx_brand_created ON products(brand_id, created_at DESC);
CREATE INDEX idx_brand_price ON products(brand_id, price);

-- Status 필터링 용 복합 인덱스
CREATE INDEX idx_status_like ON products(status, like_count DESC);
CREATE INDEX idx_status_created ON products(status, created_at DESC);
CREATE INDEX idx_status_price ON products(status, price);
```

**Step 2: Flyway 마이그레이션 실행**

```bash
cd /Users/chuljoongkim/Documents/loopers/loop-pack-be-l2-vol3-kotlin
./gradlew :apps:commerce-api:flywayMigrate
```

Expected: Migration successful, 6개 인덱스 생성 완료

**Step 3: 인덱스 생성 확인**

```bash
# MySQL 접속
mysql -u root -p loopers_db

# 인덱스 확인
SHOW INDEX FROM products;
```

Expected:
- idx_brand_like, idx_brand_created, idx_brand_price
- idx_status_like, idx_status_created, idx_status_price
모두 존재해야 함

**Step 4: Commit**

```bash
git add apps/commerce-api/src/main/resources/db/migration/V20260309__Add_product_composite_indexes.sql
git commit -m "feat: add composite indexes for product search optimization

- Add idx_brand_* for brand-based filtering with multiple sort options
- Add idx_status_* for status-based filtering with multiple sort options
- Support like_count, created_at, price sorting efficiently"
```

---

## Task 2: ProductSortOption Enum 정의

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductSortOption.kt`

**Step 1: Enum 파일 생성**

```kotlin
// ProductSortOption.kt
package com.loopers.interfaces.api.product

enum class ProductSortOption {
    LIKE_COUNT,
    CREATED_AT,
    PRICE,
}
```

**Step 2: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/product/ProductSortOption.kt
git commit -m "feat: add ProductSortOption enum for sort parameter validation"
```

---

## Task 3: ProductRepositoryImpl에 like_count 정렬 지원

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt:39-49`
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt:79-89`

**Step 1: 정렬 옵션 매핑 로직 확인**

현재 코드 (ProductRepositoryImpl의 findWithPaging):
```kotlin
val orders = pageable.sort.mapNotNull { order ->
    val path = when (order.property) {
        "createdAt" -> qProduct.createdAt
        "price" -> qProduct.price
        else -> null
    }
    path?.let {
        val direction = if (order.isAscending) Order.ASC else Order.DESC
        OrderSpecifier(direction, it)
    }
}
```

**Step 2: like_count 정렬 옵션 추가 (findWithPaging)**

```kotlin
val orders = pageable.sort.mapNotNull { order ->
    val path = when (order.property) {
        "createdAt" -> qProduct.createdAt
        "price" -> qProduct.price
        "likeCount" -> qProduct.likeCount  // ← 추가
        else -> null
    }
    path?.let {
        val direction = if (order.isAscending) Order.ASC else Order.DESC
        OrderSpecifier(direction, it)
    }
}
```

**Step 3: like_count 정렬 옵션 추가 (findActiveProductsWithPaging)**

같은 방식으로 `findActiveProductsWithPaging` 메서드의 orders 로직에도 like_count 추가

**Step 4: 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "*ProductRepositoryImplTest*" -v
```

Expected: 기존 테스트 통과 (상호호환성)

**Step 5: Commit**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/product/ProductRepositoryImpl.kt
git commit -m "feat: add likeCount sort option to ProductRepositoryImpl

- Support like_count sorting in both findWithPaging and findActiveProductsWithPaging
- Composite indexes (brand_id/status, like_count) enable efficient O(log n) queries"
```

---

## Task 4: 단위 테스트 - ProductRepositoryImpl 정렬 검증

**Files:**
- Create: `apps/commerce-api/src/test/kotlin/com/loopers/infrastructure/product/ProductRepositoryImplTest.kt`

**Step 1: 테스트 클래스 생성**

```kotlin
// ProductRepositoryImplTest.kt
package com.loopers.infrastructure.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertEquals

@DataJpaTest
@Import(ProductRepositoryImpl::class)
@ActiveProfiles("test")
class ProductRepositoryImplTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired
    private lateinit var brandJpaRepository: BrandJpaRepository

    @Test
    fun `findWithPaging returns products sorted by likeCount descending`() {
        // Arrange
        val brand = createBrand("TestBrand")
        val product1 = createProduct(brand, "Product1", 100, 50)
        val product2 = createProduct(brand, "Product2", 200, 100)
        val product3 = createProduct(brand, "Product3", 150, 75)

        productJpaRepository.saveAll(listOf(product1, product2, product3))

        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "likeCount"))

        // Act
        val result = productRepository.findWithPaging(brand.id, pageable)

        // Assert
        assertEquals(3, result.totalElements)
        assertEquals(product2.id, result.content[0].id)  // 100 likes
        assertEquals(product3.id, result.content[1].id)  // 75 likes
        assertEquals(product1.id, result.content[2].id)  // 50 likes
    }

    @Test
    fun `findActiveProductsWithPaging returns active products sorted by likeCount`() {
        // Arrange
        val brand = createBrand("TestBrand")
        val activeProduct1 = createProduct(brand, "Active1", 100, 50, ProductStatus.ACTIVE)
        val activeProduct2 = createProduct(brand, "Active2", 200, 100, ProductStatus.ACTIVE)
        val inactiveProduct = createProduct(brand, "Inactive", 150, 75, ProductStatus.INACTIVE)

        productJpaRepository.saveAll(listOf(activeProduct1, activeProduct2, inactiveProduct))

        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "likeCount"))

        // Act
        val result = productRepository.findActiveProductsWithPaging(brand.id, pageable)

        // Assert
        assertEquals(2, result.totalElements)
        assertEquals(activeProduct2.id, result.content[0].id)
        assertEquals(activeProduct1.id, result.content[1].id)
    }

    @Test
    fun `findWithPaging supports multiple sort options`() {
        // Arrange
        val brand = createBrand("TestBrand")
        val product1 = createProduct(brand, "Product1", 100, 50)
        val product2 = createProduct(brand, "Product2", 50, 100)

        productJpaRepository.saveAll(listOf(product1, product2))

        // Act & Assert - createdAt 정렬
        val byCreatedAt = productRepository.findWithPaging(
            brand.id,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        )
        assertEquals(2, byCreatedAt.totalElements)

        // Act & Assert - price 정렬
        val byPrice = productRepository.findWithPaging(
            brand.id,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "price"))
        )
        assertEquals(2, byPrice.totalElements)
    }

    // Helper methods
    private fun createBrand(name: String): Brand {
        return Brand.create(name = name).apply {
            brandJpaRepository.save(this)
        }
    }

    private fun createProduct(
        brand: Brand,
        name: String,
        price: Int,
        likeCount: Int,
        status: ProductStatus = ProductStatus.ACTIVE
    ): Product {
        return Product.create(
            brand = brand,
            name = name,
            price = BigDecimal(price),
            status = status
        ).apply {
            this.likeCount = likeCount
            productJpaRepository.save(this)
        }
    }
}
```

**Step 2: 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "ProductRepositoryImplTest" -v
```

Expected: 3개 테스트 모두 PASS

**Step 3: Commit**

```bash
git add apps/commerce-api/src/test/kotlin/com/loopers/infrastructure/product/ProductRepositoryImplTest.kt
git commit -m "test: add ProductRepositoryImpl tests for like_count sorting

- Verify likeCount DESC sorting works correctly
- Verify status-based filtering with likeCount sorting
- Verify multiple sort options (createdAt, price, likeCount)"
```

---

## Task 5: E2E 테스트 - API 엔드포인트 검증

**Files:**
- Modify: `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/product/ProductV1ApiE2ETest.kt`

**Step 1: 테스트 메서드 추가**

기존 ProductV1ApiE2ETest에 다음 테스트 추가:

```kotlin
@Test
fun `getProducts returns products sorted by likeCount DESC`() {
    // Arrange
    val brand = createAndSaveBrand("Nike")
    val product1 = createAndSaveProduct(brand, "Product1", 100, 50)
    val product2 = createAndSaveProduct(brand, "Product2", 200, 100)
    val product3 = createAndSaveProduct(brand, "Product3", 150, 75)

    // Act & Assert
    mockMvc.perform(
        get("/api/v1/products")
            .param("brandId", brand.id.toString())
            .param("page", "0")
            .param("size", "10")
            .param("sort", "likeCount,desc")
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.data.content", hasSize<Any>(3)))
        .andExpect(jsonPath("$.data.content[0].id").value(product2.id))  // 100 likes
        .andExpect(jsonPath("$.data.content[1].id").value(product3.id))  // 75 likes
        .andExpect(jsonPath("$.data.content[2].id").value(product1.id))  // 50 likes
}

@Test
fun `getProducts supports multiple sort options`() {
    // Arrange
    val brand = createAndSaveBrand("Adidas")
    createAndSaveProduct(brand, "Expensive", 1000, 10)
    createAndSaveProduct(brand, "Cheap", 100, 50)

    // Act & Assert - price ASC
    mockMvc.perform(
        get("/api/v1/products")
            .param("brandId", brand.id.toString())
            .param("sort", "price,asc")
    )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.data.content[0].price").value(100))
        .andExpect(jsonPath("$.data.content[1].price").value(1000))
}
```

**Step 2: 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "ProductV1ApiE2ETest" -v
```

Expected: 모든 E2E 테스트 PASS (신규 포함)

**Step 3: Commit**

```bash
git add apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/product/ProductV1ApiE2ETest.kt
git commit -m "test: add E2E tests for like_count sorting in getProducts API

- Verify likeCount DESC sorting returns products in correct order
- Verify multiple sort options work via API endpoint
- Confirm composite indexes enable fast response"
```

---

## Task 6: 성능 테스트 - 100만 건 데이터 기준

**Files:**
- Create: `apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/product/ProductSearchPerformanceTest.kt`

**Step 1: 성능 테스트 클래스 생성**

```kotlin
// ProductSearchPerformanceTest.kt
package com.loopers.interfaces.api.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.infrastructure.product.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.test.assertTrue

@DataJpaTest
@Import(ProductRepositoryImpl::class)
@ActiveProfiles("test")
class ProductSearchPerformanceTest {

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var productJpaRepository: ProductJpaRepository

    @Autowired
    private lateinit var brandJpaRepository: BrandJpaRepository

    private lateinit var testBrand: Brand

    @BeforeEach
    fun setup() {
        testBrand = Brand.create("TestBrand")
        brandJpaRepository.save(testBrand)
    }

    @Test
    fun `findWithPaging with like_count sort should respond within 100ms for 10k records`() {
        // Arrange - 10,000 상품 생성 (단위 테스트용)
        val products = (1..10000).map { i ->
            Product.create(
                brand = testBrand,
                name = "Product$i",
                price = BigDecimal(i),
                status = ProductStatus.ACTIVE
            ).apply {
                this.likeCount = (Math.random() * 100).toInt()
            }
        }
        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "likeCount"))

        // Act
        val startTime = System.currentTimeMillis()
        val result = productRepository.findWithPaging(testBrand.id, pageable)
        val endTime = System.currentTimeMillis()

        val elapsedMs = endTime - startTime

        // Assert
        assertTrue(elapsedMs < 100, "Query took ${elapsedMs}ms, expected < 100ms")
        assertTrue(result.content.isNotEmpty(), "Should return results")
    }

    @Test
    fun `findActiveProductsWithPaging with created_at sort should respond within 100ms`() {
        // Arrange - 10,000 활성 상품
        val now = LocalDateTime.now()
        val products = (1..10000).map { i ->
            Product.create(
                brand = testBrand,
                name = "Active$i",
                price = BigDecimal(i),
                status = ProductStatus.ACTIVE
            )
        }
        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

        // Act
        val startTime = System.currentTimeMillis()
        val result = productRepository.findActiveProductsWithPaging(testBrand.id, pageable)
        val endTime = System.currentTimeMillis()

        val elapsedMs = endTime - startTime

        // Assert
        assertTrue(elapsedMs < 100, "Query took ${elapsedMs}ms, expected < 100ms")
        assertTrue(result.totalElements == 10000L)
    }

    @Test
    fun `multiple sorts should be efficient with composite indexes`() {
        // Arrange
        val products = (1..5000).map { i ->
            Product.create(
                brand = testBrand,
                name = "Product$i",
                price = BigDecimal((Math.random() * 1000000).toInt()),
                status = ProductStatus.ACTIVE
            ).apply {
                this.likeCount = (Math.random() * 100).toInt()
            }
        }
        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        // Act & Assert - 각 정렬 옵션별 성능 측정
        val sortOptions = listOf(
            Sort.by(Sort.Direction.DESC, "likeCount"),
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Sort.by(Sort.Direction.ASC, "price")
        )

        for (sort in sortOptions) {
            val startTime = System.currentTimeMillis()
            val result = productRepository.findWithPaging(
                testBrand.id,
                PageRequest.of(0, 20, sort)
            )
            val elapsedMs = System.currentTimeMillis() - startTime

            assertTrue(elapsedMs < 100, "Sort $sort took ${elapsedMs}ms")
            assertTrue(result.content.isNotEmpty())
        }
    }
}
```

**Step 2: 테스트 실행**

```bash
./gradlew :apps:commerce-api:test --tests "ProductSearchPerformanceTest" -v
```

Expected: 모든 성능 테스트 PASS (각 쿼리 < 100ms)

**Step 3: Commit**

```bash
git add apps/commerce-api/src/test/kotlin/com/loopers/interfaces/api/product/ProductSearchPerformanceTest.kt
git commit -m "test: add performance tests for product search with composite indexes

- Verify likeCount sort completes within 100ms for 10k records
- Verify createdAt sort performance with active product filter
- Confirm all sort options are efficient with indexes"
```

---

## Task 7: 통합 테스트 - EXPLAIN 분석

**Files:**
- Create: `PERFORMANCE_ANALYSIS.md` (임시 문서)

**Step 1: MySQL EXPLAIN 분석**

```bash
# 로컬 MySQL 접속
mysql -u root -p loopers_db

# 쿼리 1: brand_id + like_count 정렬
EXPLAIN SELECT * FROM products
WHERE brand_id = 1
ORDER BY like_count DESC
LIMIT 20;

# 예상 결과:
# - type: ref (인덱스 범위 스캔)
# - key: idx_brand_like
# - Extra: NO filesort ✅

# 쿼리 2: status + created_at 정렬
EXPLAIN SELECT * FROM products
WHERE status = 'ACTIVE'
ORDER BY created_at DESC
LIMIT 20;

# 예상 결과:
# - type: ref
# - key: idx_status_created
# - Extra: NO filesort ✅

# 쿼리 3: price 정렬
EXPLAIN SELECT * FROM products
WHERE brand_id = 1
ORDER BY price ASC
LIMIT 20;

# 예상 결과:
# - type: ref
# - key: idx_brand_price
# - Extra: NO filesort ✅
```

**Step 2: 검증 체크리스트**

```
✅ filesort 없음 (Extra 컬럼에 "filesort" 미포함)
✅ 모든 쿼리가 적절한 인덱스 사용 (key 컬럼)
✅ type = ref or range (효율적인 인덱스 사용)
✅ rows < 전체 테이블 행 수 (인덱스로 범위 축소됨)
```

**Step 3: Commit (분석 결과 문서화)**

```bash
git add PERFORMANCE_ANALYSIS.md
git commit -m "docs: document EXPLAIN analysis for composite indexes

- Verify all queries use appropriate composite indexes
- Confirm no filesort operations
- Document query execution plans for monitoring"
```

---

## Task 8: 최종 통합 테스트 및 검증

**Files:**
- None (전체 테스트 스위트 실행)

**Step 1: 전체 테스트 실행**

```bash
./gradlew :apps:commerce-api:test -v
```

Expected: 모든 테스트 PASS

**Step 2: Kotlin Lint 검사**

```bash
./gradlew :apps:commerce-api:ktlintCheck
```

Expected: PASS (스타일 위반 없음)

**Step 3: 빌드 검증**

```bash
./gradlew :apps:commerce-api:build
```

Expected: BUILD SUCCESSFUL

**Step 4: 최종 커밋**

```bash
git log --oneline | head -8
# 확인: 총 8개 커밋
# - Task 1: 인덱스 생성
# - Task 2: ProductSortOption
# - Task 3: ProductRepositoryImpl 수정
# - Task 4: 단위 테스트
# - Task 5: E2E 테스트
# - Task 6: 성능 테스트
# - Task 7: 분석 문서
# - Task 8: 최종 검증
```

---

## 마이그레이션 검증

### 데이터베이스 마이그레이션 확인

```bash
# 1. Flyway 히스토리 확인
SELECT * FROM flyway_schema_history WHERE script = 'V20260309__Add_product_composite_indexes.sql';

# 2. 인덱스 통계 확인
SHOW INDEX FROM products WHERE Key_name LIKE 'idx_%';

# Expected: 6개 새로운 인덱스가 생성되어야 함
```

### 역롤백 계획 (문제 발생 시)

```sql
-- 만약 인덱스 삭제가 필요하면:
DROP INDEX idx_brand_like ON products;
DROP INDEX idx_brand_created ON products;
DROP INDEX idx_brand_price ON products;
DROP INDEX idx_status_like ON products;
DROP INDEX idx_status_created ON products;
DROP INDEX idx_status_price ON products;
```

---

## 배포 체크리스트

- [ ] Task 1-8 모두 완료
- [ ] 로컬 테스트 100% PASS
- [ ] EXPLAIN 분석 문서화
- [ ] 커밋 메시지 명확함
- [ ] 기존 코드 호환성 검증
- [ ] CI/CD 파이프라인 통과
- [ ] Code Review 승인 (PR)
- [ ] 프로덕션 배포

---

## 참고 자료

- 설계 문서: `docs/plans/2026-03-09-product-search-index-design.md`
- MySQL B-tree 인덱스: https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html
- QueryDSL Sorting: https://querydsl.com/static/querydsl/latest/reference/html_single/
- Flyway 마이그레이션: https://flywaydb.org/documentation/

