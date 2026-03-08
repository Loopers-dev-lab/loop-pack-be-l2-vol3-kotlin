package com.loopers.domain.stock

interface StockRepository {

    fun save(stock: Stock): Stock

    fun findByProductId(productId: Long): Stock?

    fun findStockWithLock(productId: Long): Stock?
}
