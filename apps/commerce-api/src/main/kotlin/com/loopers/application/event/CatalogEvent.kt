package com.loopers.application.event

sealed interface CatalogEvent {
    data class ProductViewed(
        val productId: Long,
        val userId: Long?,
    ) : CatalogEvent
}
