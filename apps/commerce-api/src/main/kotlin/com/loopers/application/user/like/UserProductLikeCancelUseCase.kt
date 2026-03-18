package com.loopers.application.user.like

import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.ProductQueryInvalidator
import com.loopers.domain.product.ProductRepository
import com.loopers.support.transaction.AfterCommitExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeCancelUseCase(
    private val afterCommitExecutor: AfterCommitExecutor,
    private val productQueryInvalidator: ProductQueryInvalidator,
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun cancel(command: UserProductLikeCommand.Cancel) {
        val deleted = productLikeRepository.deleteByUserIdAndProductId(command.userId, command.productId)
        if (deleted) {
            productRepository.decrementLikeCount(command.productId)
            afterCommitExecutor.execute {
                productQueryInvalidator.invalidateDetails(listOf(command.productId))
            }
        }
    }
}
