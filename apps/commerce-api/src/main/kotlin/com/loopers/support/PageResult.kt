package com.loopers.support

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
        ): PageResult<T> {
            val totalPages = if (size == 0) 0 else ((totalElements + size - 1) / size).toInt()
            return PageResult(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
            )
        }
    }
}
