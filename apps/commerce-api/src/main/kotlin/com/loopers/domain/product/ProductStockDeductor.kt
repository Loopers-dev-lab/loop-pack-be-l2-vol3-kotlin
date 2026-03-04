package com.loopers.domain.product

import org.springframework.stereotype.Component

@Component
class ProductStockDeductor(
    private val productReader: ProductReader,
    private val productRepository: ProductRepository,
) {

    fun deductStock(productId: Long, quantity: Int): Product {
        val product = productReader.getSellingById(productId)
        product.deductStock(quantity)
        return productRepository.save(product)
    }

    fun restoreStock(productId: Long, quantity: Int) {
        val product = productReader.getById(productId)
        product.restoreStock(quantity)
        productRepository.save(product)
    }
}
