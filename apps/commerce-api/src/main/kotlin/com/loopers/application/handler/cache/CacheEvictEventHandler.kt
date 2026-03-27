package com.loopers.application.handler.cache

import com.loopers.application.auth.AuthCacheStore
import com.loopers.application.brand.BrandCacheStore
import com.loopers.application.product.ProductCacheStore
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

@Component
class AuthCacheCommandHandler(
    private val authCacheStore: AuthCacheStore,
) {
    fun handle(command: EvictAuthCacheCommand) {
        authCacheStore.evictAuth(command.loginId)
    }
}

@Component
class BrandCacheCommandHandler(
    private val brandCacheStore: BrandCacheStore,
) {
    fun handle(command: EvictBrandCacheCommand) {
        brandCacheStore.evictBrand(command.brandId)
    }
}

@Component
class ProductCacheCommandHandler(
    private val productCacheStore: ProductCacheStore,
) {
    fun handle(command: EvictProductCacheCommand) {
        productCacheStore.evictProduct(command.productId)
    }
}
