package com.loopers.interfaces.api.v1.product

import com.loopers.application.product.ProductInfo

data class GetProductListResponse(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val brandLogoUrl: String?,
    val name: String,
    val price: Long,
    val stock: Int,
    val thumbnailUrl: String?,
    val status: String,
    val likeCount: Int,
) {
    companion object {
        fun from(productInfo: ProductInfo) = GetProductListResponse(
            id = productInfo.id,
            brandId = productInfo.brandId,
            brandName = productInfo.brandName,
            brandLogoUrl = productInfo.brandLogoUrl,
            name = productInfo.name,
            price = productInfo.price,
            stock = productInfo.stock,
            thumbnailUrl = productInfo.thumbnailUrl,
            status = productInfo.status,
            likeCount = productInfo.likeCount,
        )
    }
}
