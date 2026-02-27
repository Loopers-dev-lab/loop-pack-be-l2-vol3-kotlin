package com.loopers.application.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.like.Like
import com.loopers.domain.product.Product

class LikeInfo {

    data class Registered(
        val id: Long,
        val memberId: Long,
        val productId: Long,
    ) {
        companion object {
            fun from(like: Like) = Registered(
                id = requireNotNull(like.id) { "좋아요 저장 후 ID가 할당되지 않았습니다." },
                memberId = like.memberId,
                productId = like.productId,
            )
        }
    }

    data class Detail(
        val id: Long,
        val productId: Long,
        val productName: String,
        val price: Long,
        val brandName: String,
    ) {
        companion object {
            fun from(like: Like, product: Product, brand: Brand?) = Detail(
                id = requireNotNull(like.id),
                productId = product.id!!,
                productName = product.name.value,
                price = product.price.value,
                brandName = brand?.name?.value ?: "",
            )
        }
    }
}
