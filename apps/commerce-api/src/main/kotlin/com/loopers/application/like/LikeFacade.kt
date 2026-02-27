package com.loopers.application.like

import com.loopers.application.product.ProductResult
import com.loopers.domain.brand.BrandService
import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductInfo
import com.loopers.domain.product.ProductService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
) {

    @Transactional
    fun like(userId: Long, productId: Long) {
        productService.findById(productId)
        likeService.like(userId, productId)
        productService.increaseLikeCount(productId)
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        likeService.unlike(userId, productId)
        productService.decreaseLikeCountIfExists(productId)
    }

    fun getLikedProducts(userId: Long): List<LikedProductResult> {
        val likes = likeService.findAllByUserId(userId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.productId }
        val products = productService.findByIds(productIds)
        val productMap = products.associateBy { it.id }

        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandService.findByIds(brandIds).associateBy { it.id }

        return likes.mapNotNull { likeInfo ->
            val product = productMap[likeInfo.productId] ?: return@mapNotNull null
            val brand = brands[product.brandId] ?: return@mapNotNull null
            val productResult = ProductResult.from(ProductInfo.from(product), brand.name)
            LikedProductResult.from(likeInfo, productResult)
        }
    }
}
