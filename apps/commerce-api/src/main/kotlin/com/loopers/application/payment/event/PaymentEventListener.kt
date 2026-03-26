package com.loopers.application.payment.event

import com.loopers.application.product.ProductCacheStore
import com.loopers.domain.event.PaymentApprovedEvent
import com.loopers.domain.event.PaymentFailedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener(
    private val productCacheStore: ProductCacheStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentApproved(event: PaymentApprovedEvent) {
        try {
            event.productIds.forEach { productCacheStore.evictDetail(it) }
            log.info(
                "결제 승인 이벤트 처리 완료 — 캐시 무효화 [paymentId={}, productIds={}]",
                event.paymentId,
                event.productIds,
            )
        } catch (e: Exception) {
            log.error(
                "결제 승인 이벤트 처리 실패 [paymentId={}, orderId={}]",
                event.paymentId,
                event.orderId,
                e,
            )
        }
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePaymentFailed(event: PaymentFailedEvent) {
        log.info(
            "결제 실패 이벤트 수신 [paymentId={}, orderId={}, reason={}]",
            event.paymentId,
            event.orderId,
            event.reason,
        )
    }
}
