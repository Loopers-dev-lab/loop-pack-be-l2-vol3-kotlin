package com.loopers.infrastructure.common

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.common.SortOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

fun PageQuery.toPageRequest(): PageRequest {
    val direction = when (sort.direction) {
        SortOrder.Direction.ASC -> Sort.Direction.ASC
        SortOrder.Direction.DESC -> Sort.Direction.DESC
    }
    return PageRequest.of(page, size, Sort.by(direction, sort.property))
}

fun <T> Page<T>.toPageResult(): PageResult<T> {
    return PageResult(
        content = content,
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )
}
