package com.loopers.domain.common.event

data class BrandCreatedEvent(
    val brandId: Long,
)

data class BrandUpdatedEvent(
    val brandId: Long,
)

data class BrandDeletedEvent(
    val brandId: Long,
)
