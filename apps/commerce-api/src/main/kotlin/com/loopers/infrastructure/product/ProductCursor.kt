package com.loopers.infrastructure.product

sealed interface ProductCursor {
    data class Latest(val id: Long) : ProductCursor
    data class PriceAsc(val price: Long, val id: Long) : ProductCursor
    data class LikesDesc(val likeCount: Int, val id: Long) : ProductCursor
}
