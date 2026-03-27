package com.loopers.application.like

import com.loopers.domain.common.event.LikeCancelledEvent
import com.loopers.domain.common.event.LikeCreatedEvent
import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class LikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {
    fun like(memberId: Long, productId: Long) {
        productLikeRepository.save(ProductLikeModel(memberId = memberId, productId = productId))
        eventPublisher.publishEvent(LikeCreatedEvent(memberId = memberId, productId = productId))
    }

    fun exists(memberId: Long, productId: Long): Boolean {
        return productLikeRepository.findByMemberIdAndProductId(memberId, productId) != null
    }

    fun unlike(memberId: Long, productId: Long) {
        productLikeRepository.deleteByMemberIdAndProductId(memberId, productId)
        eventPublisher.publishEvent(LikeCancelledEvent(memberId = memberId, productId = productId))
    }

    fun getLikedProductIds(memberId: Long): List<Long> {
        return productLikeRepository.findAllByMemberId(memberId)
            .map { it.productId }
    }
}
