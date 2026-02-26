package com.loopers.application.catalog

import com.loopers.application.UseCase
import com.loopers.domain.catalog.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminDeleteProductUseCase(
    private val productRepository: ProductRepository,
) : UseCase<Long, Unit> {

    @Transactional
    override fun execute(productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        product.delete()
        productRepository.update(product)
    }
}
