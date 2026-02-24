package com.loopers.application.catalog.product

import com.loopers.domain.PageResult
import com.loopers.domain.catalog.product.repository.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductsAdminUseCase(private val productRepository: ProductRepository) {
    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<ProductInfo> {
        return productRepository.findAll(page, size).map { ProductInfo.from(it) }
    }
}
