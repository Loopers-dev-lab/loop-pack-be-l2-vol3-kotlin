package com.loopers.domain.stock

import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class StockRepositoryTest @Autowired constructor(
    private val stockRepository: StockRepository,
    private val stockJpaRepository: StockJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("Stock을 저장한다")
    fun testSaveStock() {
        val stock = Stock.create(productId = 1L, quantity = 100)
        val savedStock = stockRepository.save(stock)

        assertThat(savedStock.id).isNotZero
        assertThat(savedStock.productId).isEqualTo(1L)
        assertThat(savedStock.quantity).isEqualTo(100)
    }

    @Test
    @DisplayName("productId로 Stock을 찾는다")
    fun testFindByProductId() {
        val stock = stockRepository.save(Stock.create(productId = 1L, quantity = 100))

        val foundStock = stockRepository.findByProductId(1L)
        assertThat(foundStock).isNotNull
        assertThat(foundStock?.id).isEqualTo(stock.id)
    }

    @Test
    @DisplayName("SELECT FOR UPDATE로 Stock을 찾는다")
    fun testFindStockWithLock() {
        val stock = stockRepository.save(Stock.create(productId = 1L, quantity = 100))

        val lockedStock = stockRepository.findStockWithLock(1L)
        assertThat(lockedStock).isNotNull
        assertThat(lockedStock?.id).isEqualTo(stock.id)
    }

    @Test
    @DisplayName("존재하지 않는 productId로 찾으면 null을 반환한다")
    fun testFindByProductIdNotFound() {
        val foundStock = stockRepository.findByProductId(999L)
        assertThat(foundStock).isNull()
    }

    @Test
    @DisplayName("SELECT FOR UPDATE에서도 null을 반환한다")
    fun testFindStockWithLockNotFound() {
        val lockedStock = stockRepository.findStockWithLock(999L)
        assertThat(lockedStock).isNull()
    }
}
