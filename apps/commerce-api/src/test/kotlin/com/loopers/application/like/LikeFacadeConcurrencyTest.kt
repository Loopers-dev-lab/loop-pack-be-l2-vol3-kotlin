package com.loopers.application.like

import com.loopers.infrastructure.catalog.brand.BrandEntity
import com.loopers.infrastructure.catalog.brand.BrandJpaRepository
import com.loopers.infrastructure.catalog.product.ProductEntity
import com.loopers.infrastructure.catalog.product.ProductJpaRepository
import com.loopers.infrastructure.like.LikeJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.loopers.domain.catalog.product.ProductStatus

@SpringBootTest
@ActiveProfiles("test")
class LikeFacadeConcurrencyTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val likeJpaRepository: LikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    @Test
    fun `addLike() - 동시에 50명이 좋아요를 눌러도 likeCount가 정확히 50이어야 한다`() {
        // Arrange
        val brand = brandJpaRepository.save(BrandEntity(name = "TestBrand", description = "desc"))
        val product = productJpaRepository.save(
            ProductEntity(
                brandId = brand.id,
                name = "TestProduct",
                description = "desc",
                price = 10000,
                status = ProductStatus.ACTIVE,
            )
        )

        val threadCount = 50
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // Act: distinct userId per thread → no UNIQUE constraint violation
        repeat(threadCount) { i ->
            executor.submit {
                try {
                    likeFacade.addLike(userId = (i + 1).toLong(), productId = product.id)
                } catch (e: Exception) {
                    // ignore — test asserts final count
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        // Assert
        val finalProduct = productJpaRepository.findById(product.id).get()
        assertThat(finalProduct.likeCount).isEqualTo(50)
    }

    @Test
    fun `addLike() - 동시에 같은 사용자가 같은 상품에 좋아요를 눌러도 likeCount는 1이어야 한다`() {
        // Arrange
        val brand = brandJpaRepository.save(BrandEntity(name = "TestBrand", description = "desc"))
        val product = productJpaRepository.save(
            ProductEntity(
                brandId = brand.id,
                name = "TestProduct",
                description = "desc",
                price = 10000,
                status = ProductStatus.ACTIVE,
            )
        )

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // Act: same userId, same productId → UNIQUE constraint prevents duplicate
        repeat(threadCount) {
            executor.submit {
                try {
                    likeFacade.addLike(userId = 1L, productId = product.id)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        // Assert: only 1 should succeed, likeCount must be exactly 1
        val finalProduct = productJpaRepository.findById(product.id).get()
        val likeRows = likeJpaRepository.findAllByProductId(product.id)

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(failCount.get()).isEqualTo(9)
        assertThat(finalProduct.likeCount).isEqualTo(1)
        assertThat(likeRows).hasSize(1)
    }
}
