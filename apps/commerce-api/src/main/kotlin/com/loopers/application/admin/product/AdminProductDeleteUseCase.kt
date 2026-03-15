package com.loopers.application.admin.product

import com.loopers.application.event.product.ProductQueryChangedEvent
import com.loopers.application.event.product.ProductQueryChangedEventPublisher
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminProductDeleteUseCase(
    private val productQueryChangedEventPublisher: ProductQueryChangedEventPublisher,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
) {
    @Transactional
    fun delete(productId: Long, admin: String) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        productStockRepository.deleteByProductId(productId, admin)
        productRepository.delete(productId, admin)
        productQueryChangedEventPublisher.publish(
            ProductQueryChangedEvent(
                productIds = listOf(productId),
                brandIds = listOf(product.brandId),
            ),
        )
    }
}
