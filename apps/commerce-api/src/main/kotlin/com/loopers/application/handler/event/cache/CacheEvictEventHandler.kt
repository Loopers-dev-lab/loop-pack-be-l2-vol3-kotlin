package com.loopers.application.handler.event.cache

import com.loopers.application.handler.command.cache.AuthCacheCommandHandler
import com.loopers.application.handler.command.cache.BrandCacheCommandHandler
import com.loopers.application.handler.command.cache.ProductCacheCommandHandler
import com.loopers.domain.common.command.EvictAuthCacheCommand
import com.loopers.domain.common.command.EvictBrandCacheCommand
import com.loopers.domain.common.command.EvictProductCacheCommand
import com.loopers.domain.common.event.BrandDeletedEvent
import com.loopers.domain.common.event.BrandUpdatedEvent
import com.loopers.domain.common.event.MemberPasswordChangedEvent
import com.loopers.domain.common.event.ProductDeletedEvent
import com.loopers.domain.common.event.ProductUpdatedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CacheEvictEventHandler(
    private val authCacheCommandHandler: AuthCacheCommandHandler,
    private val brandCacheCommandHandler: BrandCacheCommandHandler,
    private val productCacheCommandHandler: ProductCacheCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: MemberPasswordChangedEvent) {
        authCacheCommandHandler.handle(EvictAuthCacheCommand(loginId = event.loginId))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: BrandUpdatedEvent) {
        brandCacheCommandHandler.handle(EvictBrandCacheCommand(brandId = event.brandId))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onBrandDeleted(event: BrandDeletedEvent) {
        brandCacheCommandHandler.handle(EvictBrandCacheCommand(brandId = event.brandId))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: ProductUpdatedEvent) {
        productCacheCommandHandler.handle(EvictProductCacheCommand(productId = event.productId))
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onProductDeleted(event: ProductDeletedEvent) {
        productCacheCommandHandler.handle(EvictProductCacheCommand(productId = event.productId))
    }
}
