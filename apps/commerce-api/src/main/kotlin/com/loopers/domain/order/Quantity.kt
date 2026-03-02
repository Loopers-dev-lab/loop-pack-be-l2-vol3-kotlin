package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import jakarta.persistence.Embeddable

@Embeddable
data class Quantity(
    val value: Int = 1,
) {
    init {
        if (value < 1) {
            throw CoreException(OrderErrorCode.INVALID_QUANTITY)
        }
    }
}
