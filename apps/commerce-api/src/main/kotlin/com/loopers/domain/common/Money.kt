package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal

class Money private constructor(
    val value: BigDecimal,
) {
    companion object {
        fun of(value: BigDecimal): Money {
            if (value <= BigDecimal.ZERO) {
                throw CoreException(ErrorType.BAD_REQUEST, "금액은 0보다 커야 합니다.")
            }
            return Money(value)
        }
    }
}
