package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductLikeRepository {

    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?

    fun findLikedProducts(userId: Long, pageable: Pageable): Page<Product>

    fun save(productLike: ProductLike): ProductLike

    fun delete(productLike: ProductLike)
}
