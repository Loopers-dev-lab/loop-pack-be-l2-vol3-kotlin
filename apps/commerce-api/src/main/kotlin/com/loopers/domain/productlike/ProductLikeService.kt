package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.productlike.event.LikeCountEvent
import com.loopers.domain.productlike.event.LikeCountEventType
import com.loopers.domain.user.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun addProductLike(user: User, product: Product) {
        val productLike = ProductLike.create(user, product)
        productLikeRepository.save(productLike)
        val likeCountEvent = LikeCountEvent(this, product.id, LikeCountEventType.INCREMENT, userId = user.id)
        eventPublisher.publishEvent(likeCountEvent)
        kafkaTemplate.send("like.count", objectMapper.writeValueAsString(likeCountEvent))
    }

    @Transactional
    fun removeProductLike(user: User, product: Product): Int {
        // 삭제된 행 수로 동시성 제어
        val deletedCount = productLikeRepository.deleteByUserIdAndProductId(user.id, product.id)

        // 실제로 삭제된 경우(deletedCount > 0)에만 like_count 감소
        if (deletedCount > 0) {
            val likeCountEvent = LikeCountEvent(this, product.id, LikeCountEventType.DECREMENT, userId = user.id)
            eventPublisher.publishEvent(likeCountEvent)
            kafkaTemplate.send("like.count", objectMapper.writeValueAsString(likeCountEvent))
        }

        return deletedCount
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeRepository.findLikedProducts(userId, pageable)
            .map { LikedProductInfo.from(it) }
}
