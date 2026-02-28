package com.loopers.interfaces.api.common

import com.loopers.application.common.PageResult

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T, R> from(pageResult: PageResult<T>, transform: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = pageResult.content.map(transform),
                page = pageResult.page,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
            )
        }
    }
}
