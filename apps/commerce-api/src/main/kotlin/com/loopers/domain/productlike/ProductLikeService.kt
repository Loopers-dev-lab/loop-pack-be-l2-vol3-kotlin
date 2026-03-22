package com.loopers.domain.productlike

import com.loopers.domain.product.Product
import com.loopers.domain.productlike.dto.LikedProductInfo
import com.loopers.domain.productlike.event.LikeCountEvent
import com.loopers.domain.productlike.event.LikeCountEventType
import com.loopers.domain.user.User
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductLikeService(
    private val productLikeRepository: ProductLikeRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun addProductLike(user: User, product: Product) {
        val productLike = ProductLike.create(user, product)
        productLikeRepository.save(productLike)
        // 기본 ApplicationEventPublisher + @EventListener 조합은 동기 처리된다.
        // 따라서 현재 구현에서는 같은 호출 흐름/트랜잭션 안에서 projection 갱신이 실행된다.
        eventPublisher.publishEvent(LikeCountEvent(this, product.id, LikeCountEventType.INCREMENT))
    }

    @Transactional
    fun removeProductLike(user: User, product: Product) {
        // 삭제된 행 수로 동시성 제어
        val deletedCount = productLikeRepository.deleteByUserIdAndProductId(user.id, product.id)

        // 실제로 삭제된 경우(deletedCount > 0)에만 like_count 감소
        if (deletedCount > 0) {
            // 이벤트 리스너는 기본적으로 같은 스레드에서 즉시 실행된다.
            eventPublisher.publishEvent(LikeCountEvent(this, product.id, LikeCountEventType.DECREMENT))
        }
    }

    fun getMyLikedProducts(userId: Long, pageable: Pageable): Page<LikedProductInfo> =
        productLikeRepository.findLikedProducts(userId, pageable)
            .map { LikedProductInfo.from(it) }
}
