package com.loopers.support.page

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
) {
    val totalPages: Int
        get() = if (size == 0) 0 else ((totalElements + size - 1) / size).toInt()

    fun <R> map(transform: (T) -> R): PageResponse<R> =
        PageResponse(content.map(transform), totalElements, page, size)
}
