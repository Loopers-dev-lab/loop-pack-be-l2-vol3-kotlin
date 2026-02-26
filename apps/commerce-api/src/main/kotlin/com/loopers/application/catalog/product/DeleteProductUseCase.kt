package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(private val productRepository: ProductRepository) {
    @Transactional
    fun execute(productId: Long) {
        val product = productRepository.findById(ProductId(productId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }
        product.delete()
        productRepository.save(product)
    }
}
