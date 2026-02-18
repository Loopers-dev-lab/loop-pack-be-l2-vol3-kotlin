package com.loopers.application.product

import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetProductUseCase(
    private val productRepository: ProductRepository,
) {
    fun getById(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        return ProductInfo.from(product)
    }

    fun getActiveById(id: Long): ProductInfo {
        val product = productRepository.findById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $id")
        }
        return ProductInfo.from(product)
    }
}
