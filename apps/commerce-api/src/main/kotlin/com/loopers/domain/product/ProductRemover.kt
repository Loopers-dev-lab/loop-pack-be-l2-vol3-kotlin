package com.loopers.domain.product

import org.springframework.stereotype.Component

@Component
class ProductRemover(
    private val productReader: ProductReader,
    private val productRepository: ProductRepository,
) {

    fun remove(id: Long) {
        val product = productReader.getById(id)
        product.stopSelling()
        productRepository.save(product)
    }
}
