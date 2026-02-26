package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Quantity private constructor(
    val value: Int,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is Quantity && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    companion object {
        fun of(value: Int): Quantity {
            if (value <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.")
            }
            return Quantity(value)
        }
    }
}
