package com.loopers.domain.like

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse

interface ProductLikeRepository {
    fun save(productLike: ProductLike)
    fun deleteByUserIdAndProductId(userId: Long, productId: Long)
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findAllByUserId(userId: Long, pageRequest: PageRequest): PageResponse<ProductLike>

    /** 스케줄러에서 PRODUCT.like_count 동기화 시 사용 예정 */
    fun countByProductId(productId: Long): Int
}
