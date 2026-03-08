package com.loopers.application.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.LocalDate

data class CreateOrderCriteria(
    val loginId: String,
    val items: List<CreateOrderItemCriteria>,
    val couponId: Long? = null,
) {
    init {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 1개 이상이어야 합니다.")
        }
        couponId?.let {
            if (it <= 0) throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 ID는 0보다 커야 합니다.")
        }
    }
}

data class CreateOrderItemCriteria(
    val productId: Long,
    val quantity: Int,
) {
    init {
        if (productId <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 ID는 0보다 커야 합니다.")
        }
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다.")
        }
    }
}

data class GetOrdersCriteria(
    val loginId: String,
    val startAt: LocalDate,
    val endAt: LocalDate,
    val page: Int,
    val size: Int,
) {
    init {
        if (startAt.isAfter(endAt)) {
            throw CoreException(ErrorType.BAD_REQUEST, "시작일은 종료일 이전이어야 합니다.")
        }
    }
}

data class GetOrderCriteria(
    val loginId: String,
    val orderId: Long,
)

data class CancelOrderCriteria(
    val loginId: String,
    val orderId: Long,
)
