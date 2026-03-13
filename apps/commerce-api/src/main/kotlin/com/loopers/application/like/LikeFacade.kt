package com.loopers.application.like

import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun like(memberId: Long, productId: Long) {
        productService.getProduct(productId) // ACTIVE 검증 (BR-L4)
        if (likeService.exists(memberId, productId)) return // 이미 좋아요 → 멱등 처리 (BR-L2)
        likeService.like(memberId, productId)
    }

    @Transactional
    fun unlike(memberId: Long, productId: Long) {
        likeService.unlike(memberId, productId) // 상품 존재 검증 없이 멱등 삭제
    }

    @Transactional(readOnly = true)
    fun getMyLikes(memberId: Long): List<LikedProductInfo> {
        val productIds = likeService.getLikedProductIds(memberId)
        if (productIds.isEmpty()) return emptyList()

        val products = productService.getProductsByIds(productIds)
        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandIds.associateWith { brandId ->
            runCatching { brandService.getBrand(brandId) }.getOrNull()?.name ?: ""
        }

        return products.map { product ->
            LikedProductInfo(
                productId = product.id,
                productName = product.name,
                price = product.price,
                imageUrl = product.imageUrl,
                brandName = brandMap[product.brandId] ?: "",
            )
        }
    }
}

data class LikedProductInfo(
    val productId: Long,
    val productName: String,
    val price: Long,
    val imageUrl: String,
    val brandName: String,
)
