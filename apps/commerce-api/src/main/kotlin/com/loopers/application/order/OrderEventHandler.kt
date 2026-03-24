package com.loopers.application.order

import com.loopers.domain.order.event.OrderCompletedEvent
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 주문 완료 이벤트 핸들러.
 *
 * 주문 트랜잭션 커밋 후 부가 로직(판매량 집계, 알림, 이력 로깅)을 비동기로 처리한다.
 * - AFTER_COMMIT: 주문+결제 트랜잭션 커밋 확인 후 실행
 * - @Async: 별도 스레드에서 실행하여 주문 응답 지연 방지
 * - @Transactional(REQUIRES_NEW): 부가 로직 실패가 주문에 영향 주지 않음
 */
@Component
class OrderEventHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onOrderCompleted(event: OrderCompletedEvent) {
        log.info(
            "[주문 완료] orderId={}, userId={}, items={}",
            event.orderId,
            event.userId,
            event.items.joinToString { "${it.productName}(x${it.quantity})" },
        )
    }
}
