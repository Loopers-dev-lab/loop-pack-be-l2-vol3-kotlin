package com.loopers.application.event

sealed interface CatalogEvent {
    data class LikeAdded(
        val productId: Long,
        val userId: Long,
    ) : CatalogEvent

    data class LikeRemoved(
        val productId: Long,
        val userId: Long,
    ) : CatalogEvent

    data class ProductViewed(
        val productId: Long,
        val userId: Long?,
    ) : CatalogEvent
}
