package com.loopers.application.handler.command.cache

import com.loopers.application.brand.BrandCacheStore
import com.loopers.domain.common.command.EvictBrandCacheCommand
import org.springframework.stereotype.Component

@Component
class BrandCacheCommandHandler(
    private val brandCacheStore: BrandCacheStore,
) {
    fun handle(command: EvictBrandCacheCommand) {
        brandCacheStore.evictBrand(command.brandId)
    }
}
