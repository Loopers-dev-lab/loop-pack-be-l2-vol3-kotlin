package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.math.BigDecimal
import java.math.RoundingMode

class Money private constructor(val amount: BigDecimal) {
    init {
        if (amount < BigDecimal.ZERO) {
            throw CoreException(ErrorType.INVALID_MONEY)
        }
    }

    // TODO: Money가 Quantity를 직접 받는 게 적절한지 Order 구현 시 재검토. Money * Int 또는 operator times 전환 고려.
    fun multiply(quantity: Quantity): Money = Money(amount.multiply(BigDecimal(quantity.value)))

    operator fun plus(other: Money): Money = Money(amount.add(other.amount))

    operator fun minus(other: Money): Money {
        val result = amount.subtract(other.amount)
        if (result < BigDecimal.ZERO) {
            throw CoreException(ErrorType.INVALID_MONEY)
        }
        return Money(result)
    }

    fun isGreaterThan(other: Money): Boolean = amount > other.amount

    fun isGreaterThanOrEqual(other: Money): Boolean = amount >= other.amount

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return amount.compareTo(other.amount) == 0
    }

    override fun hashCode(): Int = amount.stripTrailingZeros().hashCode()

    override fun toString(): String = "Money(amount=$amount)"

    companion object {
        private const val SCALE = 2

        operator fun invoke(amount: BigDecimal): Money {
            if (amount < BigDecimal.ZERO) {
                throw CoreException(ErrorType.INVALID_MONEY)
            }
            return Money(amount.setScale(SCALE, RoundingMode.HALF_UP))
        }
    }
}
