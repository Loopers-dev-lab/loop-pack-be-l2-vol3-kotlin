package com.loopers.application.handler.event.order

import com.loopers.application.handler.command.coupon.UseCouponCommandHandler
import com.loopers.application.handler.command.order.CreateOrderCommandHandler
import com.loopers.application.handler.command.product.DeductStockCommandHandler
import com.loopers.domain.common.command.CreateOrderCommand
import com.loopers.domain.common.command.DeductStockCommand
import com.loopers.domain.common.command.UseCouponCommand
import com.loopers.domain.common.event.OrderRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderRequestedEventHandler(
    private val deductStockCommandHandler: DeductStockCommandHandler,
    private val useCouponCommandHandler: UseCouponCommandHandler,
    private val createOrderCommandHandler: CreateOrderCommandHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderRequestedEvent) {
        try {
            // 1. 재고 차감 (productId 오름차순 — 데드락 방지)
            event.items.sortedBy { it.productId }.forEach { item ->
                deductStockCommandHandler.handle(
                    DeductStockCommand(productId = item.productId, quantity = item.quantity),
                )
            }

            // 2. 쿠폰 사용
            if (event.couponId != null) {
                useCouponCommandHandler.handle(
                    UseCouponCommand(issuedCouponId = event.couponId, memberId = event.memberId),
                )
            }

            // 3. 주문 생성 (스냅샷은 이미 이벤트에 확정되어 있음)
            createOrderCommandHandler.handle(
                CreateOrderCommand(
                    memberId = event.memberId,
                    items = event.items.map { item ->
                        CreateOrderCommand.CreateOrderItem(
                            productId = item.productId,
                            quantity = item.quantity,
                            productName = item.productName,
                            productPrice = item.productPrice,
                            brandName = item.brandName,
                        )
                    },
                    couponId = event.couponId,
                    discountAmount = event.discountAmount,
                    orderAmount = event.orderAmount,
                    finalAmount = event.finalAmount,
                ),
            )
        } catch (e: Exception) {
            log.error("주문 요청 처리 실패: memberId={}, error={}", event.memberId, e.message, e)
            // 보상은 Polling 배치에 전임
        }
    }
}
