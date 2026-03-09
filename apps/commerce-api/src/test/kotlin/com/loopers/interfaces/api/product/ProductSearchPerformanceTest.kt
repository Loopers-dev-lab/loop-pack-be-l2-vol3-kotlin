package com.loopers.interfaces.api.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.math.abs

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProductSearchPerformanceTest")
class ProductSearchPerformanceTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    private lateinit var testBrand: Brand

    @BeforeEach
    fun setUp() {
        testBrand = createBrand()
    }

    @AfterEach
    fun cleanup() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("findWithPaging with like_count sort should respond efficiently for 10k records")
    @Test
    fun findWithPaging_sortsByLikeCount_performanceTest() {
        // Arrange
        val productCount = 10_000
        val products = (1..productCount).map { index ->
            val product = Product.create(
                brand = testBrand,
                name = "Performance Test Product $index",
                price = BigDecimal("${1000 + (index % 100_000)}"),
            )
            // Set likeCount through reflection
            product::class.java.getDeclaredField("likeCount").apply {
                isAccessible = true
                setInt(product, abs(index * 7 % 10_000)) // Generate varied likeCount
            }
            product
        }

        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        val pageable = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("likeCount")))

        // Act
        val startTime = System.currentTimeMillis()
        val result = productRepository.findWithPaging(null, pageable)
        val elapsedMs = System.currentTimeMillis() - startTime

        // Assert
        println("🔍 Performance Test 1 - findWithPaging (likeCount DESC, 10k records)")
        println("   ⏱️  Elapsed time: ${elapsedMs}ms")
        println("   📦 Result size: ${result.content.size}")
        println("   📊 Total records: ${result.totalElements}")

        assertThat(result.content).hasSize(100)
        assertThat(result.totalElements).isEqualTo(productCount.toLong())
        assertThat(elapsedMs).isLessThan(1000)
            .withFailMessage("Performance threshold exceeded: ${elapsedMs}ms > 1000ms")
    }

    @DisplayName("findActiveProductsWithPaging with created_at sort should respond efficiently")
    @Test
    fun findActiveProductsWithPaging_sortsByCreatedAt_performanceTest() {
        // Arrange
        val productCount = 10_000
        val products = (1..productCount).map { index ->
            Product.create(
                brand = testBrand,
                name = "Active Product $index",
                price = BigDecimal("${2000 + (index % 50_000)}"),
                status = ProductStatus.ACTIVE,
            )
        }

        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        val pageable = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("createdAt")))

        // Act
        val startTime = System.currentTimeMillis()
        val result = productRepository.findActiveProductsWithPaging(null, pageable)
        val elapsedMs = System.currentTimeMillis() - startTime

        // Assert
        println("🔍 Performance Test 2 - findActiveProductsWithPaging (createdAt DESC, 10k records)")
        println("   ⏱️  Elapsed time: ${elapsedMs}ms")
        println("   📦 Result size: ${result.content.size}")
        println("   📊 Total records: ${result.totalElements}")

        assertThat(result.content).hasSize(100)
        assertThat(result.totalElements).isEqualTo(productCount.toLong())
        assertThat(result.content).allMatch { it.status == ProductStatus.ACTIVE }
        assertThat(elapsedMs).isLessThan(1000)
            .withFailMessage("Performance threshold exceeded: ${elapsedMs}ms > 1000ms")
    }

    @DisplayName("multiple sorts should be efficient with composite indexes")
    @Test
    fun multipleSorts_shouldBeEfficientWithCompositeIndexes() {
        // Arrange
        val productCount = 5_000
        val products = (1..productCount).map { index ->
            val product = Product.create(
                brand = testBrand,
                name = "Multi-Sort Product $index",
                price = BigDecimal("${1500 + (index % 50_000)}"),
            )
            // Set likeCount through reflection
            product::class.java.getDeclaredField("likeCount").apply {
                isAccessible = true
                setInt(product, abs((index * 13) % 5_000))
            }
            product
        }

        productJpaRepository.saveAll(products)
        productJpaRepository.flush()

        println("\n🔍 Performance Test 3 - Multiple Sorts with Composite Indexes (5k records)")

        // Test 1: Sort by likeCount
        val pageableLikeCount = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("likeCount")))
        val startLikeCount = System.currentTimeMillis()
        val resultLikeCount = productRepository.findWithPaging(null, pageableLikeCount)
        val elapsedLikeCount = System.currentTimeMillis() - startLikeCount

        println("   Sort by likeCount DESC:")
        println("      ⏱️  Elapsed time: ${elapsedLikeCount}ms")
        println("      📦 Result size: ${resultLikeCount.content.size}")

        // Test 2: Sort by createdAt
        val pageableCreatedAt = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("createdAt")))
        val startCreatedAt = System.currentTimeMillis()
        val resultCreatedAt = productRepository.findWithPaging(null, pageableCreatedAt)
        val elapsedCreatedAt = System.currentTimeMillis() - startCreatedAt

        println("   Sort by createdAt DESC:")
        println("      ⏱️  Elapsed time: ${elapsedCreatedAt}ms")
        println("      📦 Result size: ${resultCreatedAt.content.size}")

        // Test 3: Sort by price
        val pageablePrice = PageRequest.of(0, 100, Sort.by(Sort.Order.desc("price")))
        val startPrice = System.currentTimeMillis()
        val resultPrice = productRepository.findWithPaging(null, pageablePrice)
        val elapsedPrice = System.currentTimeMillis() - startPrice

        println("   Sort by price DESC:")
        println("      ⏱️  Elapsed time: ${elapsedPrice}ms")
        println("      📦 Result size: ${resultPrice.content.size}")

        // Assert - All should be efficient (under 1000ms)
        assertThat(resultLikeCount.content).hasSize(100)
        assertThat(resultCreatedAt.content).hasSize(100)
        assertThat(resultPrice.content).hasSize(100)

        assertThat(elapsedLikeCount).isLessThan(1000)
            .withFailMessage("likeCount sort exceeded threshold: ${elapsedLikeCount}ms > 1000ms")
        assertThat(elapsedCreatedAt).isLessThan(1000)
            .withFailMessage("createdAt sort exceeded threshold: ${elapsedCreatedAt}ms > 1000ms")
        assertThat(elapsedPrice).isLessThan(1000)
            .withFailMessage("price sort exceeded threshold: ${elapsedPrice}ms > 1000ms")

        println("   ✅ All sorts completed within 1000ms threshold")
    }

    private fun createBrand(): Brand {
        return brandRepository.save(
            Brand.create(
                name = "Performance Test Brand ${System.nanoTime()}",
                description = "Performance Test Brand Description",
            ),
        )
    }
}
