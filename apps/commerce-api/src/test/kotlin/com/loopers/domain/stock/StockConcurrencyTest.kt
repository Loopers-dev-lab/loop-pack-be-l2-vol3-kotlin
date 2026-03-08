package com.loopers.domain.stock

import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
class StockConcurrencyTest @Autowired constructor(
    private val stockService: StockService,
    private val stockJpaRepository: StockJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("여러 상품의 재고 감소가 원자적으로 처리된다 (모두 성공 또는 모두 실패)")
    fun testDecreaseAllStocksAtomicity() {
        // Arrange
        val stock1 = stockJpaRepository.save(Stock.create(productId = 1L, quantity = 100))
        val stock2 = stockJpaRepository.save(Stock.create(productId = 2L, quantity = 50))
        val stock3 = stockJpaRepository.save(Stock.create(productId = 3L, quantity = 30))

        val commands = listOf(
            StockDecreaseCommand(productId = 1L, quantity = 10),
            StockDecreaseCommand(productId = 2L, quantity = 20),
            StockDecreaseCommand(productId = 3L, quantity = 30),
        )

        // Act
        stockService.decreaseAllStocks(commands)

        // Assert
        val stocks = listOf(
            stockJpaRepository.findByProductId(1L),
            stockJpaRepository.findByProductId(2L),
            stockJpaRepository.findByProductId(3L),
        )

        assertThat(stocks[0]?.quantity).isEqualTo(90)
        assertThat(stocks[1]?.quantity).isEqualTo(30)
        assertThat(stocks[2]?.quantity).isEqualTo(0)
    }

    @Test
    @DisplayName("여러 상품 중 하나라도 재고 부족이면 전체 실패")
    fun testDecreaseAllStocksRollbackOnPartialFailure() {
        // Arrange
        val stock1 = stockJpaRepository.save(Stock.create(productId = 1L, quantity = 100))
        val stock2 = stockJpaRepository.save(Stock.create(productId = 2L, quantity = 10)) // 부족

        val commands = listOf(
            StockDecreaseCommand(productId = 1L, quantity = 50),
            StockDecreaseCommand(productId = 2L, quantity = 20),
        )

        // Act & Assert
        assertThatThrownBy {
            stockService.decreaseAllStocks(commands)
        }.isInstanceOf(CoreException::class.java)

        // 부분 성공 없음 - 트랜잭션 롤백
        val stock1After = stockJpaRepository.findByProductId(1L)
        val stock2After = stockJpaRepository.findByProductId(2L)

        assertThat(stock1After?.quantity).isEqualTo(100)
        assertThat(stock2After?.quantity).isEqualTo(10)
    }

    @Test
    @DisplayName("동시에 여러 스레드가 decreaseAllStocks를 호출하면 일부는 실패")
    fun testConcurrentDecreaseAllStocks() {
        // Arrange
        val stock = stockJpaRepository.save(Stock.create(productId = 1L, quantity = 10))

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val results = Collections.synchronizedList(mutableListOf<StockResult>())

        // Act: 10개 스레드가 동시에 2개씩 차감 시도 (단 1번의 REQUIRES_NEW 트랜잭션)
        val tasks = (1..threadCount).map { threadId ->
            executor.submit {
                latch.countDown()
                latch.await()

                try {
                    val commands = listOf(
                        StockDecreaseCommand(productId = stock.productId, quantity = 2),
                    )
                    stockService.decreaseAllStocks(commands)
                    results.add(StockResult.Success)
                } catch (e: Exception) {
                    results.add(StockResult.Failure(e::class.simpleName))
                }
            }
        }

        tasks.forEach { task ->
            try {
                task.get(5, TimeUnit.SECONDS)
            } catch (e: java.util.concurrent.TimeoutException) {
                throw AssertionError("Task timeout after 5 seconds")
            }
        }
        executor.shutdown()
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            executor.shutdownNow()
            throw AssertionError("Executor did not terminate within 10 seconds")
        }

        // Assert
        val successCount = results.count { it is StockResult.Success }

        // 재고 10개, 각 2개씩 = 최대 5개까지만 성공
        assertThat(successCount).isLessThanOrEqualTo(5)

        val stockAfter = stockJpaRepository.findByProductId(stock.productId)
        assertThat(stockAfter?.quantity).isGreaterThanOrEqualTo(0)
    }
}

sealed class StockResult {
    object Success : StockResult()
    data class Failure(val reason: String?) : StockResult()
}
