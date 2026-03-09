package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun execute(productId: Long) {
        val id = ProductId(productId)
        val product = productRepository.findById(id) ?: return
        if (product.isDeleted()) return
        product.delete()
        productRepository.save(product)
        eventPublisher.publishEvent(ProductCacheEvent.DetailEvicted(id, product.refBrandId))
    }
}
