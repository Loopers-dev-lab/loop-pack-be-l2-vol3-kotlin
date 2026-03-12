package com.loopers.application.like

import com.loopers.domain.catalog.brand.BrandRepository
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.like.LikeService
import com.loopers.infrastructure.catalog.product.ProductCacheService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val productCacheService: ProductCacheService,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long) {
        productService.getById(productId) // 상품 존재 확인
        likeService.addLike(userId, productId)
        productService.incrementLikeCount(productId)
        productCacheService.evictProductDetail(productId)
        productCacheService.evictAllProductLists()
    }

    @Transactional
    fun removeLike(userId: Long, productId: Long) {
        likeService.removeLike(userId, productId)
        productService.decrementLikeCount(productId)
        productCacheService.evictProductDetail(productId)
        productCacheService.evictAllProductLists()
    }

    @Transactional(readOnly = true)
    fun getLikedProducts(userId: Long): List<LikedProductResult> {
        val likes = likeService.getLikedByUser(userId)
        val productIds = likes.map { it.productId }
        val productMap = productRepository.findAllByIds(productIds).associateBy { it.id }
        val brandIds = productMap.values.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        return likes.mapNotNull { like ->
            val product = productMap[like.productId] ?: return@mapNotNull null
            val brand = brandMap[product.brandId] ?: return@mapNotNull null
            LikedProductResult.from(product, brand)
        }
    }
}
