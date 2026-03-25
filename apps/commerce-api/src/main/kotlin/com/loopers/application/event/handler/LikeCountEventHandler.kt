package com.loopers.application.event.handler

import com.loopers.application.event.LikeToggledEvent
import com.loopers.application.product.ProductService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LikeCountEventHandler(
    private val productService: ProductService,
) {

    @Async("asyncEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: LikeToggledEvent) {
        if (event.liked) {
            productService.incrementLikeCount(event.productId)
        } else {
            productService.decrementLikeCount(event.productId)
        }
    }
}
