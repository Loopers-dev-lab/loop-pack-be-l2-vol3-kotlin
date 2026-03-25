package com.loopers.application.payment.event

import com.loopers.application.order.event.OrderPlacedEvent
import com.loopers.application.payment.PaymentFacade
import com.loopers.application.payment.RequestPaymentCommand
import com.loopers.infrastructure.catalog.product.ProductCacheService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderPlacedEventHandler(
    private val paymentFacade: PaymentFacade,
    private val productCacheService: ProductCacheService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    fun handleOrderPlaced(event: OrderPlacedEvent) {
        log.info("[Event] OrderPlaced: orderId=${event.orderId}, userId=${event.userId}")

        // 1. 결제 요청 (PG 호출)
        try {
            paymentFacade.requestPayment(
                RequestPaymentCommand(
                    orderId = event.orderId,
                    amount = event.totalPrice,
                    cardType = event.cardType,
                    cardNo = event.cardNo,
                ),
            )
        } catch (ex: Exception) {
            log.error("[Event] 결제 요청 실패: orderId=${event.orderId}, error=${ex.message}", ex)
        }

        // 2. 캐시 무효화
        try {
            event.items.forEach { productCacheService.evictProductDetail(it.productId) }
            productCacheService.evictAllProductLists()
        } catch (ex: Exception) {
            log.error("[Event] 캐시 무효화 실패: orderId=${event.orderId}, error=${ex.message}", ex)
        }

        // 3. 유저 행동 로깅 (TODO: Kafka 발행으로 전환 예정)
        log.info("[UserAction] ORDER_PLACED: userId=${event.userId}, orderId=${event.orderId}, totalPrice=${event.totalPrice}")
    }
}
