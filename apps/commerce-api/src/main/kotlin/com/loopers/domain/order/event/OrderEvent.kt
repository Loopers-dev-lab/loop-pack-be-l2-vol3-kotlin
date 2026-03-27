package com.loopers.domain.order.event

import com.loopers.domain.Money
import com.loopers.domain.event.DomainEvent
import com.loopers.domain.event.EventType

/**
 * 주문 도메인 이벤트.
 *
 * 네이밍 원칙: 비즈니스 상태 변화 중심
 * - OrderEvent.Created (O) — "주문이 생성되었다"
 * - DecreaseStockEvent (X) — 특정 구독자를 위한 네이밍은 결합도를 높임
 */
object OrderEvent {

    /**
     * 주문 생성 이벤트.
     *
     * AFTER_COMMIT 시점에 발행되어 후속 처리를 트리거한다:
     * - 쿠폰 USED 마킹 (비동기)
     * - 유저 행동 로깅
     * - 알림 시뮬레이션
     * - (Step 2) Kafka 발행을 위한 Outbox 저장
     */
    data class Created(
        override val aggregateId: Long,
        val userId: Long,
        val orderNumber: String,
        val totalAmount: Money,
        val couponIssueId: Long?,
        val couponDiscountAmount: Money,
        val orderItems: List<OrderItemSnapshot>,
    ) : DomainEvent() {
        override val eventType: EventType = EventType.ORDER_CREATED
    }

    /**
     * 주문 아이템 스냅샷 — 이벤트 페이로드용.
     * 엔티티가 아닌 값 객체로, 이벤트에 필요한 정보만 담는다.
     */
    data class OrderItemSnapshot(
        val productId: Long,
        val productName: String,
        val quantity: Int,
        val unitPrice: Money,
    )
}
