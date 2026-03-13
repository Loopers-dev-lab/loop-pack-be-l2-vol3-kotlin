package com.loopers.domain.product.dto

import com.loopers.domain.product.Product

/**
 * 상품과 좋아요 수를 함께 담는 DTO
 *
 * @property product 상품 엔티티
 * @property likeCount 좋아요 수
 */
data class ProductWithLikeCount(
    val product: Product,
    val likeCount: Long,
)
