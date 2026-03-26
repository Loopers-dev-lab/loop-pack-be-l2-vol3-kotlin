package com.loopers.application.common

import com.loopers.domain.common.UserActivityLogModel
import com.loopers.domain.common.UserActivityLogRepository
import com.loopers.domain.common.event.ActivityType
import com.loopers.domain.common.event.OrderCreatedEvent
import com.loopers.domain.common.event.ProductLikedEvent
import com.loopers.domain.common.event.ProductUnlikedEvent
import com.loopers.domain.common.event.ProductViewedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActivityEventListener(
    private val userActivityLogRepository: UserActivityLogRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        try {
            userActivityLogRepository.save(
                UserActivityLogModel(
                    userId = event.userId,
                    loginId = event.loginId,
                    activityType = ActivityType.ORDER_CREATE,
                    targetId = event.orderId,
                ),
            )
        } catch (e: Exception) {
            log.error("주문 생성 활동 로그 저장 실패 - orderId: {}", event.orderId, e)
        }
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductLiked(event: ProductLikedEvent) {
        try {
            userActivityLogRepository.save(
                UserActivityLogModel(
                    userId = event.userId,
                    loginId = event.loginId,
                    activityType = ActivityType.PRODUCT_LIKE,
                    targetId = event.productId,
                ),
            )
        } catch (e: Exception) {
            log.error("좋아요 활동 로그 저장 실패 - productId: {}", event.productId, e)
        }
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductUnliked(event: ProductUnlikedEvent) {
        try {
            userActivityLogRepository.save(
                UserActivityLogModel(
                    userId = event.userId,
                    loginId = event.loginId,
                    activityType = ActivityType.PRODUCT_UNLIKE,
                    targetId = event.productId,
                ),
            )
        } catch (e: Exception) {
            log.error("좋아요 취소 활동 로그 저장 실패 - productId: {}", event.productId, e)
        }
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun handleProductViewed(event: ProductViewedEvent) {
        try {
            userActivityLogRepository.save(
                UserActivityLogModel(
                    userId = event.userId,
                    loginId = event.loginId,
                    activityType = ActivityType.PRODUCT_VIEW,
                    targetId = event.productId,
                ),
            )
        } catch (e: Exception) {
            log.error("상품 조회 활동 로그 저장 실패 - productId: {}", event.productId, e)
        }
    }
}
