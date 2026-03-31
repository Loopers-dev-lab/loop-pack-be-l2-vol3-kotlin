package com.loopers.domain.metric

interface ProductLikeCountRepository {
    fun countByProductId(productId: Long): Int
}
