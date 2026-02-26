package com.loopers.domain.common

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
)
