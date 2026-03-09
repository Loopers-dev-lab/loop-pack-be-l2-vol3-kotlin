package com.loopers.concurrency

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("재고 동시성 테스트")
@SpringBootTest
class StockConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("재고 5개인 상품에 10개 스레드가 동시에 1개씩 주문하면, 정확히 5개만 성공하고 재고가 0이 된다.")
    @Test
    fun maintainsStockIntegrity_whenConcurrentOrders() {
        // arrange
        val threadCount = 10
        val initialStock = 5
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = initialStock,
                description = null,
                imageUrl = null,
            ),
        )

        val latch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    latch.await()
                    val criteria = listOf(OrderItemCriteria(productId = product.id, quantity = 1))
                    orderFacade.createOrder(userId = index.toLong() + 1, criteria = criteria)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)

        // assert
        val updatedProduct = productJpaRepository.findById(product.id).get()
        assertAll(
            { assertThat(successCount.get()).isEqualTo(initialStock) },
            { assertThat(failCount.get()).isEqualTo(threadCount - initialStock) },
            { assertThat(updatedProduct.stock).isEqualTo(0) },
        )
    }
}
