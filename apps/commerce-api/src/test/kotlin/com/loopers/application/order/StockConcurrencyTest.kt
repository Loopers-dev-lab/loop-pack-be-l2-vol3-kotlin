package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class StockConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(): Brand {
        return brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
    }

    private fun createProduct(stockQuantity: Int = 100): Product {
        val brand = createBrand()
        return productRepository.save(
            Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(10000L),
                likes = 0,
                stockQuantity = stockQuantity,
                brandId = brand.id,
            ),
        )
    }

    @DisplayName("재고 차감 동시성 제어")
    @Nested
    inner class StockDeductionConcurrency {

        @DisplayName("동시에 여러 주문이 같은 상품에 들어와도 재고가 정확히 차감된다.")
        @Test
        fun deductsStockCorrectly_whenConcurrentOrders() {
            // arrange
            val product = createProduct(stockQuantity = 100)
            val threadCount = 10
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // act
            repeat(threadCount) { i ->
                executorService.submit {
                    try {
                        orderFacade.placeOrder(
                            userId = i.toLong() + 1,
                            items = listOf(OrderPlaceCommand(productId = product.id, quantity = 1)),
                        )
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertAll(
                { assertThat(successCount.get()).isEqualTo(10) },
                { assertThat(updatedProduct?.stockQuantity).isEqualTo(90) },
            )
        }

        @DisplayName("재고보다 많은 동시 주문이 들어오면, 재고가 음수가 되지 않는다.")
        @Test
        fun doesNotGoNegative_whenConcurrentOrdersExceedStock() {
            // arrange
            val product = createProduct(stockQuantity = 5)
            val threadCount = 10
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            repeat(threadCount) { i ->
                executorService.submit {
                    try {
                        orderFacade.placeOrder(
                            userId = i.toLong() + 1,
                            items = listOf(OrderPlaceCommand(productId = product.id, quantity = 1)),
                        )
                        successCount.incrementAndGet()
                    } catch (_: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertAll(
                { assertThat(successCount.get()).isEqualTo(5) },
                { assertThat(failCount.get()).isEqualTo(5) },
                { assertThat(updatedProduct?.stockQuantity).isEqualTo(0) },
            )
        }
    }
}
