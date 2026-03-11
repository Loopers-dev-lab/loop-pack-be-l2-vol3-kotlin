package com.loopers.application.like

import com.loopers.domain.catalog.brand.BrandRepository
import com.loopers.domain.catalog.product.ProductRepository
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long) {
        productService.getById(productId) // 상품 존재 확인
        likeService.addLike(userId, productId)
        productService.incrementLikeCount(productId)
    }

    @Transactional
    fun removeLike(userId: Long, productId: Long) {
        likeService.removeLike(userId, productId)
        productService.decrementLikeCount(productId)
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
