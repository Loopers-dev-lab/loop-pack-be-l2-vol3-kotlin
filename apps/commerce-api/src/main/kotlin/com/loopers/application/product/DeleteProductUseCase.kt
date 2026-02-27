package com.loopers.application.product

import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ProductErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
) {

    @Transactional
    fun execute(productId: Long) {
        val product = productRepository.findActiveByIdOrNull(productId)
            ?: throw CoreException(ProductErrorCode.PRODUCT_NOT_FOUND)
        product.delete()
    }
}
