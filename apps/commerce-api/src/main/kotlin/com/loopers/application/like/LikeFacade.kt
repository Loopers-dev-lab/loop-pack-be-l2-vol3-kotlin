package com.loopers.application.like

import com.loopers.domain.catalog.brand.BrandService
import com.loopers.domain.catalog.product.ProductService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val brandService: BrandService,
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
    fun getLikedProducts(userId: Long): List<LikedProductResult> =
        likeService.getLikedByUser(userId).map { like ->
            val product = productService.getById(like.productId)
            val brand = brandService.getById(product.brandId)
            LikedProductResult.from(product, brand)
        }
}
