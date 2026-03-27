package com.loopers.application.handler.like

import com.loopers.domain.common.command.DeleteProductLikesCommand
import com.loopers.domain.common.event.ProductDeletedEvent
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductDeletedLikeEventHandler(
    private val deleteProductLikesCommandHandler: DeleteProductLikesCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProductDeletedEvent) {
        deleteProductLikesCommandHandler.handle(DeleteProductLikesCommand(productId = event.productId))
    }
}

@Component
class DeleteProductLikesCommandHandler(
    private val productLikeRepository: ProductLikeRepository,
) {
    fun handle(command: DeleteProductLikesCommand) {
        productLikeRepository.deleteAllByProductId(command.productId)
    }
}
