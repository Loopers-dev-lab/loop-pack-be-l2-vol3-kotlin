package com.loopers.domain.stock

import com.loopers.support.error.CoreException
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class StockServiceTest @Autowired constructor(
    private val stockService: StockService,
    private val stockRepository: StockRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("여러 상품의 재고를 감소시킨다")
    fun testDecreaseAllStocks() {
        val stock1 = stockRepository.save(Stock.create(productId = 1L, quantity = 100))
        val stock2 = stockRepository.save(Stock.create(productId = 2L, quantity = 50))

        val commands = listOf(
            StockDecreaseCommand(productId = 1L, quantity = 30),
            StockDecreaseCommand(productId = 2L, quantity = 20),
        )
        stockService.decreaseAllStocks(commands)

        val updatedStock1 = stockRepository.findByProductId(1L)
        val updatedStock2 = stockRepository.findByProductId(2L)

        assertThat(updatedStock1?.quantity).isEqualTo(70)
        assertThat(updatedStock2?.quantity).isEqualTo(30)
    }

    @Test
    @DisplayName("재고 부족 시 전체 트랜잭션이 롤백된다")
    fun testDecreaseAllStocksRollbackOnInsufficientStock() {
        val stock1 = stockRepository.save(Stock.create(productId = 1L, quantity = 100))
        val stock2 = stockRepository.save(Stock.create(productId = 2L, quantity = 10))

        val commands = listOf(
            StockDecreaseCommand(productId = 1L, quantity = 50),
            StockDecreaseCommand(productId = 2L, quantity = 20),
        )

        assertThatThrownBy {
            stockService.decreaseAllStocks(commands)
        }.isInstanceOf(CoreException::class.java)

        // 부분 성공 없음 - 모두 원래 값으로 복구
        val updatedStock1 = stockRepository.findByProductId(1L)
        val updatedStock2 = stockRepository.findByProductId(2L)

        assertThat(updatedStock1?.quantity).isEqualTo(100)
        assertThat(updatedStock2?.quantity).isEqualTo(10)
    }

    @Test
    @DisplayName("Stock이 없으면 예외를 발생시킨다")
    fun testDecreaseAllStocksNotFound() {
        val commands = listOf(
            StockDecreaseCommand(productId = 999L, quantity = 10),
        )

        assertThatThrownBy {
            stockService.decreaseAllStocks(commands)
        }.isInstanceOf(CoreException::class.java)
    }

    @Test
    @DisplayName("여러 상품의 재고를 증가시킨다")
    fun testIncreaseAllStocks() {
        val stock1 = stockRepository.save(Stock.create(productId = 1L, quantity = 100))
        val stock2 = stockRepository.save(Stock.create(productId = 2L, quantity = 50))

        val commands = listOf(
            StockIncreaseCommand(productId = 1L, quantity = 50),
            StockIncreaseCommand(productId = 2L, quantity = 30),
        )
        stockService.increaseAllStocks(commands)

        val updatedStock1 = stockRepository.findByProductId(1L)
        val updatedStock2 = stockRepository.findByProductId(2L)

        assertThat(updatedStock1?.quantity).isEqualTo(150)
        assertThat(updatedStock2?.quantity).isEqualTo(80)
    }

    @Test
    @DisplayName("재고 증가 시 Stock이 없으면 예외를 발생시킨다")
    fun testIncreaseAllStocksNotFound() {
        val commands = listOf(
            StockIncreaseCommand(productId = 999L, quantity = 10),
        )

        assertThatThrownBy {
            stockService.increaseAllStocks(commands)
        }.isInstanceOf(CoreException::class.java)
    }
}
