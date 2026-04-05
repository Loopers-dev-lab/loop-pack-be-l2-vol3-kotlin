package com.loopers.domain.productlike

import com.loopers.domain.outbox.OutboxPublisher
import com.loopers.domain.product.Product
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.event.LikeCountEvent
import com.loopers.domain.event.LikeCountEventType
import com.loopers.domain.productlike.event.ProductUnlikedEvent
import com.loopers.domain.user.User
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val outboxPublisher: OutboxPublisher,
) {

    @Transactional
    @CacheEvict(value = ["product-info"], key = "#product.id")
    fun addProductLike(user: User, product: Product) {
        val productLike = ProductLike.create(user, product)
        productLikeRepository.save(productLike)

        // Outbox → AfterCommit → Kafka → commerce-streamer처리
        val likeCountEvent = LikeCountEvent(
            productId = product.id,
            type = LikeCountEventType.INCREMENT,
            userId = user.id,
        )
        outboxPublisher.publish(
            likeCountEvent,
            product.id,
            topic = "like-events",
            partitionKey = product.id.toString(),
        )
    }

    @Transactional
    @CacheEvict(value = ["product-info"], key = "#product.id")
    fun removeProductLike(user: User, product: Product): Int {
        // 삭제된 행 수로 동시성 제어
        val deletedCount = productLikeRepository.deleteByUserIdAndProductId(user.id, product.id)

        // 실제로 삭제된 경우(deletedCount > 0)에만 이벤트 발행
        if (deletedCount > 0) {
            val likeCountEvent = LikeCountEvent(
                productId = product.id,
                type = LikeCountEventType.DECREMENT,
                userId = user.id,
            )
            outboxPublisher.publish(
                likeCountEvent,
                product.id,
                topic = "like-events",
                partitionKey = product.id.toString(),
            )

            // ProductUnlikedEvent를 별도로 outbox에 발행
            val productUnlikedEvent = ProductUnlikedEvent(
                productId = product.id,
                userId = user.id,
            )
            outboxPublisher.publish(
                productUnlikedEvent,
                product.id,
                topic = "like-events",
            )
        }

        return deletedCount
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeRepository.findLikedProducts(userId, pageable)
            .map { LikedProductInfo.from(it) }
}
