package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ProductStockDeductor(
    private val productReader: ProductReader,
    private val productRepository: ProductRepository,
) {

    fun deductStock(productId: Long, quantity: Int): Product {
        val affected = productRepository.deductStock(productId, quantity)
        if (affected == 0) {
            throw CoreException(ErrorType.INSUFFICIENT_STOCK)
        }
        return productReader.getSellingById(productId)
    }

    fun restoreStock(productId: Long, quantity: Int) {
        productRepository.restoreStock(productId, quantity)
    }
}
