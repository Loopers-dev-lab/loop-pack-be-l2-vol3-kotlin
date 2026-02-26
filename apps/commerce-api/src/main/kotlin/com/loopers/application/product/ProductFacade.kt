package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.application.like.LikeInfo
import com.loopers.application.like.LikeService
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val likeService: LikeService,
) {

    fun createProduct(criteria: CreateProductCriteria): ProductInfo {
        brandService.getBrand(criteria.brandId)
        val product = productService.createProduct(criteria)
        return ProductInfo.from(product)
    }

    /**
     * 상품 좋아요 등록
     * - 상품 존재 검증 (Soft Delete 포함)
     * - LikeService에 위임 (멱등성 처리 포함)
     */
    fun addLike(userId: Long, productId: Long): LikeInfo {
        productService.getProductIncludingDeleted(productId)
        val like = likeService.addLike(userId, productId)
        return LikeInfo.from(like)
    }
}
