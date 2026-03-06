package com.loopers.interfaces.api

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val size: Int,
    val number: Int,
    val numberOfElements: Int,
    val empty: Boolean,
    val first: Boolean,
    val last: Boolean,
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                size = page.size,
                number = page.number,
                numberOfElements = page.numberOfElements,
                empty = page.isEmpty,
                first = page.isFirst,
                last = page.isLast,
            )
        }
    }
}
