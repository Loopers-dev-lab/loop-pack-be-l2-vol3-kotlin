package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class ProductLikeCountUpdater(
    private val productRepository: ProductRepository,
) {

    fun increase(productId: Long) {
        if (productRepository.incrementLikeCount(productId) == 0) {
            throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    fun decrease(productId: Long) {
        if (productRepository.decrementLikeCount(productId) == 0) {
            throw CoreException(ErrorType.PRODUCT_LIKE_COUNT_SYNC_FAILED)
        }
    }
}
