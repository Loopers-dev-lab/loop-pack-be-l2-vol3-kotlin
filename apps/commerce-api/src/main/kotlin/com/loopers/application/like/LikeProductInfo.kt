package com.loopers.application.like

import com.loopers.domain.catalog.product.entity.Product
import com.loopers.domain.like.entity.Like

data class LikeProductInfo(
    val like: Like,
    val product: Product,
) {
    companion object {
        fun from(like: Like, product: Product): LikeProductInfo = LikeProductInfo(like = like, product = product)
    }
}
