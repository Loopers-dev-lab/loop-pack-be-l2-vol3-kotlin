package com.loopers.application

import org.springframework.data.domain.Slice

data class SliceResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <E, T> from(slice: Slice<E>, transform: (E) -> T): SliceResult<T> {
            return SliceResult(
                content = slice.content.map(transform),
                page = slice.number,
                size = slice.size,
                hasNext = slice.hasNext(),
            )
        }
    }
}
