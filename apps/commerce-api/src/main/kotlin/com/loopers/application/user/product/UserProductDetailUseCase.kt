package com.loopers.application.user.product

import com.loopers.domain.product.ProductQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductDetailUseCase(
    private val productQueryRepository: ProductQueryRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(productId: Long): UserProductResult.Detail =
        UserProductResult.Detail.from(productQueryRepository.getDetail(productId))
}
