package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
    private val productCacheRepository: ProductCacheRepository,
) {
    @Transactional
    fun execute(productId: Long) {
        val id = ProductId(productId)
        val product = productRepository.findById(id) ?: return
        if (product.isDeleted()) return
        product.delete()
        productRepository.save(product)
        productCacheRepository.evictProductDetail(id)
    }
}
