package com.loopers.application.handler.command.like

import com.loopers.domain.common.command.DeleteProductLikesCommand
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteProductLikesCommandHandler(
    private val productLikeRepository: ProductLikeRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handle(command: DeleteProductLikesCommand) {
        productLikeRepository.deleteAllByProductId(command.productId)
    }
}
