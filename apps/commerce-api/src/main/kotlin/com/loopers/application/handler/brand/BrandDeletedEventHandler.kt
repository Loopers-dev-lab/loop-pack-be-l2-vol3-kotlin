package com.loopers.application.handler.brand

import com.loopers.domain.common.command.CascadeDeleteProductsCommand
import com.loopers.domain.common.event.BrandDeletedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class BrandDeletedEventHandler(
    private val cascadeDeleteProductsCommandHandler: CascadeDeleteProductsCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: BrandDeletedEvent) {
        cascadeDeleteProductsCommandHandler.handle(
            CascadeDeleteProductsCommand(brandId = event.brandId),
        )
    }
}
