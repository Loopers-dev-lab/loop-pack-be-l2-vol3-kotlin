package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductLikeRepository {

    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?

    fun findLikedProducts(userId: Long, pageable: Pageable): Page<Product>

    fun save(productLike: ProductLike): ProductLike

    fun delete(productLike: ProductLike)

    /**
     * 사용자와 상품의 좋아요를 삭제하고 삭제된 행 수를 반환
     * @return 삭제된 좋아요 개수 (0 또는 1)
     */
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int
}
