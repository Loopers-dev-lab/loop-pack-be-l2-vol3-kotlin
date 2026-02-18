package com.loopers.application.product

import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductUseCase(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun delete(id: Long) {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")

        if (product.isDeleted()) {
            return
        }

        val deletedProduct = product.delete()
        productRepository.save(deletedProduct)
    }
}
