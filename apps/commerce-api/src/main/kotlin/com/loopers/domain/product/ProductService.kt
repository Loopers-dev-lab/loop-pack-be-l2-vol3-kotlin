package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductService(
    private val productRepository: ProductRepository,
) {

    fun getProducts(brandId: Long?, pageable: Pageable): Page<Product> {
        return productRepository.findAll(brandId, pageable)
    }
}
