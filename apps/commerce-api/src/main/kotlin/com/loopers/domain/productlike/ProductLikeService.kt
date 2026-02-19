package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
) {

    @Transactional
    fun addProductLike(user: User, product: Product) {
        productLikeRepository.findByUserIdAndProductId(user.id, product.id)?.let {
            return
        }
        val productLike = ProductLike.create(user, product)
        productLikeRepository.save(productLike)
        product.incrementLikeCount()
    }

    @Transactional
    fun removeProductLike(user: User, product: Product) {
        productLikeRepository.findByUserIdAndProductId(user.id, product.id)?.let {
            productLikeRepository.delete(it)
            product.decrementLikeCount()
        }
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeRepository.findLikedProducts(userId, pageable)
            .map { LikedProductInfo.from(it) }
}
