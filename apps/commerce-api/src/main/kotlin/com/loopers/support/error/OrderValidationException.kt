package com.loopers.support.error

data class OrderValidationError(
    val productId: Long,
    val reason: String,
    val detail: String? = null,
)

class OrderValidationException(
    val errors: List<OrderValidationError>,
) : CoreException(
    errorCode = OrderErrorCode.ORDER_VALIDATION_FAILED,
    message = "주문 검증에 실패했습니다. (${errors.size}건)",
)
