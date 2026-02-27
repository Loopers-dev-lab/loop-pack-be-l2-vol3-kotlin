package com.loopers.support.common

data class SortOrder(
    val property: String,
    val direction: Direction,
) {
    enum class Direction { ASC, DESC }

    companion object {
        val UNSORTED = SortOrder("createdAt", Direction.DESC)

        fun by(property: String, direction: Direction) = SortOrder(property, direction)
    }
}
