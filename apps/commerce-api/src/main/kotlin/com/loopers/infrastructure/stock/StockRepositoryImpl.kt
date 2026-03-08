package com.loopers.infrastructure.stock

import com.loopers.domain.stock.Stock
import com.loopers.domain.stock.StockRepository
import org.springframework.stereotype.Component

@Component
class StockRepositoryImpl(
    private val stockJpaRepository: StockJpaRepository,
) : StockRepository {

    override fun save(stock: Stock): Stock = stockJpaRepository.save(stock)

    override fun findByProductId(productId: Long): Stock? =
        stockJpaRepository.findByProductId(productId)

    override fun findStockWithLock(productId: Long): Stock? =
        stockJpaRepository.findStockWithLockByProductId(productId)
}
