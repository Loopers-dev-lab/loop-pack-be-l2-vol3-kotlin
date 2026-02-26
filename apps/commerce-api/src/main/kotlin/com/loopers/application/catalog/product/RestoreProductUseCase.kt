package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RestoreProductUseCase(private val productRepository: ProductRepository) {
    @Transactional
    fun execute(productId: Long): ProductInfo {
        val product = productRepository.findById(ProductId(productId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.restore()
        val saved = productRepository.save(product)
        return ProductInfo.from(saved)
    }
}
