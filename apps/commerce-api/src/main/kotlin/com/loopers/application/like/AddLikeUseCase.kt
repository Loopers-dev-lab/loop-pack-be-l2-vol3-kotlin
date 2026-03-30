package com.loopers.application.like

import com.loopers.application.catalog.product.ProductCacheEvent
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val catalogOutboxRepository: CatalogOutboxRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findByIdForUpdate(ProductId(productId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (product.isDeleted() || !product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }

        if (likeRepository.findByUserIdAndProductIdForUpdate(UserId(userId), ProductId(productId)) != null) return

        likeRepository.save(Like(refUserId = UserId(userId), refProductId = ProductId(productId)))
        product.increaseLikeCount()
        val saved = productRepository.save(product)
        // 좋아요는 빈번한 이벤트이므로 매번 목록 캐시를 무효화하면 캐시 효과가 소멸된다.
        // TTL 5분 내 자동 갱신으로 충분하므로 evictList = false (기본값)를 유지한다.
        eventPublisher.publishEvent(ProductCacheEvent.DetailUpdated(saved))
        catalogOutboxRepository.save(
            CatalogOutbox(
                eventType = CatalogOutbox.CatalogOutboxEventType.LIKE_ADDED,
                productId = ProductId(productId),
                userId = UserId(userId),
            ),
        )
    }
}
