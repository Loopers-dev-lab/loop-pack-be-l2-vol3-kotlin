package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class StockQuantity private constructor(
    val value: Int,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is StockQuantity && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    operator fun minus(other: Quantity): StockQuantity {
        val result = value - other.value
        if (result < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
        }
        return StockQuantity(result)
    }

    companion object {
        fun of(value: Int): StockQuantity {
            if (value < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.")
            }
            return StockQuantity(value)
        }
    }
}
