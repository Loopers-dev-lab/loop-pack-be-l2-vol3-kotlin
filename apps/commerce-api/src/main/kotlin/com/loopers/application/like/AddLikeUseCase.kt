package com.loopers.application.like

import com.loopers.application.outbox.OutboxEventWriter
import com.loopers.domain.event.LikeCreatedEvent
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.outbox.OutboxEventType
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.LikeErrorCode
import com.loopers.support.error.ProductErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxEventWriter: OutboxEventWriter,
) {

    @Transactional
    fun execute(command: LikeCommand.Create) {
        val product = productRepository.findActiveByIdOrNull(command.productId)
            ?: throw CoreException(ProductErrorCode.PRODUCT_NOT_FOUND)

        if (likeRepository.existsByUserIdAndProductId(command.userId, command.productId)) {
            throw CoreException(LikeErrorCode.ALREADY_LIKED)
        }

        val like = Like.create(userId = command.userId, productId = command.productId)
        try {
            likeRepository.save(like)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(LikeErrorCode.ALREADY_LIKED)
        }

        val event = LikeCreatedEvent(userId = command.userId, productId = product.id)
        eventPublisher.publishEvent(event)
        outboxEventWriter.write(
            eventType = OutboxEventType.LIKE_CREATED,
            partitionKey = product.id.toString(),
            payload = event,
        )
    }
}
