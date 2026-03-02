package com.loopers.domain.point.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

@JvmInline
value class Point(val value: Long) {

    init {
        if (value < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.")
        }
    }

    fun plus(other: Point): Point = Point(value + other.value)

    fun minus(other: Point): Point = Point(value - other.value)

    fun isGreaterThanOrEqual(other: Point): Boolean = value >= other.value
}
