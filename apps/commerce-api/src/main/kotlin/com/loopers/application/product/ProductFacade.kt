package com.loopers.application.product

import com.loopers.application.brand.BrandService
import com.loopers.application.like.LikeService
import com.loopers.domain.like.Like
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val likeService: LikeService,
) {

    fun createProduct(command: CreateProductCommand): Product {
        brandService.getBrand(command.brandId)
        return productService.createProduct(command)
    }

    /**
     * 상품 좋아요 등록
     * - 상품 존재 검증 (Soft Delete 포함)
     * - LikeService에 위임 (멱등성 처리 포함)
     */
    fun addLike(userId: Long, productId: Long): Like {
        productService.getProductIncludingDeleted(productId)
        return likeService.addLike(userId, productId)
    }
}
