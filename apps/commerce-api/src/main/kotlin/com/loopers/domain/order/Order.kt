package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

/**
 * 주문 도메인 모델 - Aggregate Root (JPA 비의존)
 *
 * @property id 식별자 (영속화 전에는 0L)
 * @property userId 주문한 사용자 DB ID
 * @property items 주문 항목 목록 (스냅샷)
 * @property originalTotalPrice 할인 전 총 금액
 * @property discountAmount 할인 금액
 * @property totalPrice 최종 결제 금액 (originalTotalPrice - discountAmount)
 * @property userCouponId 사용된 쿠폰 ID (nullable)
 */
class Order(
    val userId: Long,
    val items: List<OrderItem>,
    val originalTotalPrice: Int,
    val discountAmount: Int = 0,
    val userCouponId: Long? = null,
    val id: Long = 0L,
) {
    val totalPrice: Int get() = originalTotalPrice - discountAmount

    init {
        if (items.isEmpty()) throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있을 수 없습니다.")
        if (originalTotalPrice < 0) throw CoreException(ErrorType.BAD_REQUEST, "총 금액은 0 이상이어야 합니다.")
        if (discountAmount < 0) throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.")
        if (totalPrice < 0) throw CoreException(ErrorType.BAD_REQUEST, "최종 결제 금액은 0 이상이어야 합니다.")
    }

    companion object {
        fun create(
            userId: Long,
            items: List<OrderItem>,
            discountAmount: Int = 0,
            userCouponId: Long? = null,
        ): Order {
            if (items.isEmpty()) throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있을 수 없습니다.")
            val originalTotalPrice = items.sumOf { it.subtotal() }
            return Order(
                userId = userId,
                items = items,
                originalTotalPrice = originalTotalPrice,
                discountAmount = discountAmount,
                userCouponId = userCouponId,
            )
        }
    }
}
