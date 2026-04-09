package com.loopers.interfaces.api.v1.product

import com.loopers.application.product.ProductImageInfo
import com.loopers.application.product.ProductInfo
import com.loopers.application.ranking.ProductRankInfo

data class GetProductDetailResponse(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val brandLogoUrl: String?,
    val name: String,
    val description: String?,
    val price: Long,
    val stock: Int,
    val thumbnailUrl: String?,
    val status: String,
    val likeCount: Int,
    val images: List<ProductImageResponse>,
    val ranking: ProductRankingResponse?,
) {
    companion object {
        fun from(productInfo: ProductInfo, rankInfo: ProductRankInfo? = null) = GetProductDetailResponse(
            id = productInfo.id,
            brandId = productInfo.brandId,
            brandName = productInfo.brandName,
            brandLogoUrl = productInfo.brandLogoUrl,
            name = productInfo.name,
            description = productInfo.description,
            price = productInfo.price,
            stock = productInfo.stock,
            thumbnailUrl = productInfo.thumbnailUrl,
            status = productInfo.status,
            likeCount = productInfo.likeCount,
            images = productInfo.images.map { ProductImageResponse.from(it) },
            ranking = rankInfo?.let { ProductRankingResponse(rank = it.rank, score = it.score) },
        )
    }
}

data class ProductRankingResponse(
    val rank: Long,
    val score: Double,
)

data class ProductImageResponse(
    val id: Long,
    val imageUrl: String,
    val displayOrder: Int,
) {
    companion object {
        fun from(imageInfo: ProductImageInfo) = ProductImageResponse(
            id = imageInfo.id,
            imageUrl = imageInfo.imageUrl,
            displayOrder = imageInfo.displayOrder,
        )
    }
}
