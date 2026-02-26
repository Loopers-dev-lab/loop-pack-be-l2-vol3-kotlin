package com.loopers.domain.common.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Money(val value: BigDecimal) {
    init {
        if (value < BigDecimal.ZERO) {
            throw CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.")
        }
    }

    operator fun plus(other: Money): Money = Money(value + other.value)

    operator fun minus(other: Money): Money = Money(value - other.value)

    operator fun times(quantity: Int): Money = Money(value * BigDecimal(quantity))

    fun toLong(): Long = value.setScale(0, RoundingMode.HALF_UP).toLong()
}
