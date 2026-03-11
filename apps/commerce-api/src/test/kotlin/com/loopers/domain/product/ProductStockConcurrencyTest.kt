package com.loopers.domain.product

import com.loopers.infrastructure.product.ProductEntity
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class ProductStockConcurrencyTest @Autowired constructor(
    private val productStockDeductor: ProductStockDeductor,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `동시에_100개의_주문이_같은_상품의_재고를_차감해도_정확한_재고가_유지된다`() {
        // arrange
        val product = createAndSaveProduct(stock = 100)
        val threadCount = 100
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    productStockDeductor.deductStock(product.id!!, 1)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executorService.shutdown()

        // assert
        val updatedProduct = productJpaRepository.findById(product.id!!).get()
        assertThat(successCount.get()).isEqualTo(100)
        assertThat(failCount.get()).isEqualTo(0)
        assertThat(updatedProduct.stock).isEqualTo(0)
    }

    @Test
    fun `재고보다_많은_동시_주문이_들어오면_일부만_성공한다`() {
        // arrange
        val product = createAndSaveProduct(stock = 10)
        val threadCount = 100
        val executorService = Executors.newFixedThreadPool(32)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    productStockDeductor.deductStock(product.id!!, 1)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executorService.shutdown()

        // assert
        val updatedProduct = productJpaRepository.findById(product.id!!).get()
        assertThat(successCount.get()).isEqualTo(10)
        assertThat(failCount.get()).isEqualTo(90)
        assertThat(updatedProduct.stock).isEqualTo(0)
    }

    private fun createAndSaveProduct(stock: Int = 100): ProductEntity {
        return productJpaRepository.save(
            ProductEntity(
                brandId = 1L,
                name = "테스트상품",
                price = 10000L,
                description = "테스트 상품 설명",
                stock = stock,
                status = ProductStatus.SELLING.name,
            ),
        )
    }
}
