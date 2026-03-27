package com.loopers.support.cache

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

data class CachedPage<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    fun toPage(): Page<T> = PageImpl(content, PageRequest.of(page, size), totalElements)

    companion object {
        fun <T> from(page: Page<T>): CachedPage<T> = CachedPage(
            content = page.content,
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
        )
    }
}
