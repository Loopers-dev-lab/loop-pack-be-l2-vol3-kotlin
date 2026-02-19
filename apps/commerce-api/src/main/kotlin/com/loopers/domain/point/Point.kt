package com.loopers.domain.point

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Point(val value: Long) {

    init {
        if (value < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.")
        }
    }

    fun plus(other: Point): Point = Point(value + other.value)

    fun minus(other: Point): Point {
        if (value < other.value) {
            throw CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.")
        }
        return Point(value - other.value)
    }
}
