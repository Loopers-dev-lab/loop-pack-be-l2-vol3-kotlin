package com.loopers.application.handler.like

import com.loopers.domain.common.command.DeleteProductLikesCommand
import com.loopers.domain.common.event.ProductDeletedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProductDeletedLikeEventHandler(
    private val deleteProductLikesCommandHandler: DeleteProductLikesCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProductDeletedEvent) {
        deleteProductLikesCommandHandler.handle(
            DeleteProductLikesCommand(productId = event.productId),
        )
    }
}
