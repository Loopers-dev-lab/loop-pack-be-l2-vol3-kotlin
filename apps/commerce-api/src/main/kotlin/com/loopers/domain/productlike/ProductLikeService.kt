package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.user.User
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val productRepository: ProductRepository,
    private val entityManager: EntityManager,
) {

    @Transactional
    fun addProductLike(user: User, product: Product) {
        val productLike = ProductLike.create(user, product)
        productLikeRepository.save(productLike)
        productRepository.incrementLikeCountAtomic(product.id)
    }

    @Transactional
    fun removeProductLike(user: User, product: Product) {
        // 삭제된 행 수로 동시성 제어
        val deletedCount = productLikeRepository.deleteByUserIdAndProductId(user.id, product.id)

        // 실제로 삭제된 경우(deletedCount > 0)에만 like_count 감소
        if (deletedCount > 0) {
            productRepository.decrementLikeCountAtomic(product.id)
        }
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeRepository.findLikedProducts(userId, pageable)
            .map { LikedProductInfo.from(it) }
}
