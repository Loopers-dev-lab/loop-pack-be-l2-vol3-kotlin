package com.loopers.domain.common

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    fun <R> map(transform: (T) -> R): PageResult<R> = PageResult(
        content.map(transform),
        page,
        size,
        totalElements,
        totalPages,
    )
}
