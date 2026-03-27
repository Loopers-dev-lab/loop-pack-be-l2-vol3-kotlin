package com.loopers.domain.productlike

/**
 * 상품별 좋아요 개수 집계 결과를 담는 DTO
 * @property productId 상품 ID
 * @property count 좋아요 개수
 */
data class ProductLikeCountDto(
    val productId: Long,
    val count: Long,
)
