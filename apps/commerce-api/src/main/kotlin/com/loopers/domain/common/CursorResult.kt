package com.loopers.domain.common

data class CursorResult<T>(
    val content: List<T>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
