package com.loopers.support.page

class PageRequest {
    var page: Int = 0
        set(value) {
            field = maxOf(value, 0)
        }

    var size: Int = DEFAULT_SIZE
        set(value) {
            field = value.coerceIn(MIN_SIZE, MAX_SIZE)
        }

    companion object {
        const val MIN_SIZE = 10
        const val DEFAULT_SIZE = 20
        const val MAX_SIZE = 100
    }
}
