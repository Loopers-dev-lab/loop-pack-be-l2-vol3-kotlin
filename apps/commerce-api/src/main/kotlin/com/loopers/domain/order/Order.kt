package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

/**
 * 주문 도메인 모델 - Aggregate Root (JPA 비의존)
 *
 * @property id 식별자 (영속화 전에는 0L)
 * @property userId 주문한 사용자 DB ID
 * @property items 주문 항목 목록 (스냅샷)
 * @property totalPrice 총 금액
 */
class Order(
    val userId: Long,
    val items: List<OrderItem>,
    val totalPrice: Int,
    val id: Long = 0L,
) {
    init {
        if (items.isEmpty()) throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있을 수 없습니다.")
        if (totalPrice < 0) throw CoreException(ErrorType.BAD_REQUEST, "총 금액은 0 이상이어야 합니다.")
    }

    companion object {
        fun create(userId: Long, items: List<OrderItem>): Order {
            if (items.isEmpty()) throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있을 수 없습니다.")
            val totalPrice = items.sumOf { it.subtotal() }
            return Order(userId = userId, items = items, totalPrice = totalPrice)
        }
    }
}
