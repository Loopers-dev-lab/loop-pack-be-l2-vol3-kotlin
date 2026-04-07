package com.loopers.application.event.handler

import com.loopers.application.event.LikeToggledEvent
import com.loopers.application.event.OrderCreatedEvent
import com.loopers.application.event.PaymentCompletedEvent
import com.loopers.application.event.PaymentFailedEvent
import com.loopers.application.event.ProductViewedEvent
import com.loopers.domain.activity.ActivityType
import com.loopers.domain.activity.TargetType
import com.loopers.domain.activity.UserActivityLog
import com.loopers.domain.activity.UserActivityLogRepository
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActivityEventHandler(
    private val userActivityLogRepository: UserActivityLogRepository,
) {

    @Async("asyncEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        userActivityLogRepository.save(
            UserActivityLog(
                userId = event.userId,
                activityType = ActivityType.ORDER,
                targetType = TargetType.ORDER,
                targetId = event.orderId,
            ),
        )
    }

    @Async("asyncEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentCompleted(event: PaymentCompletedEvent) {
        userActivityLogRepository.save(
            UserActivityLog(
                userId = event.userId,
                activityType = ActivityType.PAYMENT,
                targetType = TargetType.ORDER,
                targetId = event.orderId,
            ),
        )
    }

    @Async("asyncEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        userActivityLogRepository.save(
            UserActivityLog(
                userId = event.userId,
                activityType = ActivityType.PAYMENT_FAILED,
                targetType = TargetType.ORDER,
                targetId = event.orderId,
            ),
        )
    }

    @Async("asyncEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleLikeToggled(event: LikeToggledEvent) {
        val activityType = if (event.liked) ActivityType.LIKE else ActivityType.UNLIKE
        userActivityLogRepository.save(
            UserActivityLog(
                userId = event.userId,
                activityType = activityType,
                targetType = TargetType.PRODUCT,
                targetId = event.productId,
            ),
        )
    }

    @Async("asyncEventExecutor")
    @EventListener
    fun handleProductViewed(event: ProductViewedEvent) {
        userActivityLogRepository.save(
            UserActivityLog(
                userId = event.userId ?: 0L,
                activityType = ActivityType.VIEW,
                targetType = TargetType.PRODUCT,
                targetId = event.productId,
            ),
        )
    }
}
