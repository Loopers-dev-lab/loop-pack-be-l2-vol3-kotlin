package com.loopers.domain.common

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class LikeCount private constructor(
    val value: Int,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is LikeCount && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    fun increment(): LikeCount = LikeCount(value + 1)

    fun decrement(): LikeCount = if (value > 0) LikeCount(value - 1) else this

    companion object {
        fun of(value: Int): LikeCount {
            if (value < 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수는 0 이상이어야 합니다.")
            }
            return LikeCount(value)
        }
    }
}
