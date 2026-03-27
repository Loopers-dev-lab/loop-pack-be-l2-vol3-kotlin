package com.loopers.application.user.like

import com.loopers.domain.like.ProductLikeRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProductLikeCancelUseCase(
    private val eventPublisher: ApplicationEventPublisher,
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional
    fun cancel(command: UserProductLikeCommand.Cancel) {
        val deleted = productLikeRepository.deleteByUserIdAndProductId(command.userId, command.productId)
        if (deleted) {
            eventPublisher.publishEvent(
                ProductLikeCanceledEvent(
                    userId = command.userId,
                    productId = command.productId,
                ),
            )
        }
    }
}
