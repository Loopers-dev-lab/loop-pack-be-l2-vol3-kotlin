package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Money private constructor(
    val value: Long,
) {
    operator fun plus(other: Money): Money = Money(value + other.value)

    operator fun times(quantity: Int): Money = Money(value * quantity)

    operator fun times(quantity: Quantity): Money = Money(value * quantity.value)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Money && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        val ZERO = Money(0)

        fun of(value: Long): Money {
            if (value < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.")
            }
            return Money(value)
        }
    }
}
