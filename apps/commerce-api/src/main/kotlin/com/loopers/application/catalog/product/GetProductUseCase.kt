package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetProductUseCase(private val productRepository: ProductRepository) {
    @Transactional(readOnly = true)
    fun executeAdmin(productId: Long): ProductInfo {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        return ProductInfo.from(product)
    }
}
