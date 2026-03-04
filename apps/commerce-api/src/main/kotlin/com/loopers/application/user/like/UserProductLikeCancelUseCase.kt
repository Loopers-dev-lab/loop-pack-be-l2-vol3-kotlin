package com.loopers.application.user.like

import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeCancelUseCase(
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun cancel(command: UserProductLikeCommand.Cancel) {
        if (!productLikeRepository.existsByUserIdAndProductId(command.userId, command.productId)) return
        productLikeRepository.deleteByUserIdAndProductId(command.userId, command.productId)
    }
}
