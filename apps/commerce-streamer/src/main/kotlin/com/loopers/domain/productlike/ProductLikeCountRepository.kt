package com.loopers.domain.productlike

interface ProductLikeCountRepository {

    fun increment(productId: Long)

    fun decrement(productId: Long)

    /**
     * 좋아요 개수를 정확한 값으로 업데이트 (배치 용도)
     * @param productId 상품 ID
     * @param count 정확한 좋아요 개수
     */
    fun updateCount(productId: Long, count: Long)
}
