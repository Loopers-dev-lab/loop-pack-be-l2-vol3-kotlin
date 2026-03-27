package com.loopers.application.handler.like

import com.loopers.domain.common.command.DeleteProductLikesCommand
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component

@Component
class DeleteProductLikesCommandHandler(
    private val productLikeRepository: ProductLikeRepository,
) {
    fun handle(command: DeleteProductLikesCommand) {
        productLikeRepository.deleteAllByProductId(command.productId)
    }
}
