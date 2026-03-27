package com.loopers.domain.common.command

data class EvictAuthCacheCommand(
    val loginId: String,
)

data class EvictBrandCacheCommand(
    val brandId: Long,
)

data class EvictProductCacheCommand(
    val productId: Long,
)
