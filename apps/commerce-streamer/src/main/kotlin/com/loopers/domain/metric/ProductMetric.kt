package com.loopers.domain.metric

class ProductMetric private constructor(
    val productId: Long,
    val viewCount: Int,
    val likeCount: Int,
    val unitsSold: Int,
    val catalogEventVersion: Long,
    val orderEventVersion: Long,
) {
    fun recordDetailViewed(eventVersion: Long): ProductMetric? =
        applyCatalogEvent(eventVersion) { copy(viewCount = viewCount + 1) }

    fun synchronizeLikeCount(
        eventVersion: Long,
        likeCount: Int,
    ): ProductMetric? =
        applyCatalogEvent(eventVersion) { copy(likeCount = likeCount) }

    fun recordUnitsSold(quantity: Int): ProductMetric = copy(unitsSold = unitsSold + quantity)

    private fun applyCatalogEvent(
        eventVersion: Long,
        update: ProductMetric.() -> ProductMetric,
    ): ProductMetric? {
        if (eventVersion <= catalogEventVersion) return null
        return update().copy(catalogEventVersion = eventVersion)
    }

    private fun copy(
        productId: Long = this.productId,
        viewCount: Int = this.viewCount,
        likeCount: Int = this.likeCount,
        unitsSold: Int = this.unitsSold,
        catalogEventVersion: Long = this.catalogEventVersion,
        orderEventVersion: Long = this.orderEventVersion,
    ): ProductMetric = ProductMetric(
        productId = productId,
        viewCount = viewCount,
        likeCount = likeCount,
        unitsSold = unitsSold,
        catalogEventVersion = catalogEventVersion,
        orderEventVersion = orderEventVersion,
    )

    companion object {
        fun register(productId: Long): ProductMetric = ProductMetric(
            productId = productId,
            viewCount = 0,
            likeCount = 0,
            unitsSold = 0,
            catalogEventVersion = 0L,
            orderEventVersion = 0L,
        )

        fun retrieve(
            productId: Long,
            viewCount: Int,
            likeCount: Int,
            unitsSold: Int,
            catalogEventVersion: Long,
            orderEventVersion: Long,
        ): ProductMetric = ProductMetric(
            productId = productId,
            viewCount = viewCount,
            likeCount = likeCount,
            unitsSold = unitsSold,
            catalogEventVersion = catalogEventVersion,
            orderEventVersion = orderEventVersion,
        )
    }
}
