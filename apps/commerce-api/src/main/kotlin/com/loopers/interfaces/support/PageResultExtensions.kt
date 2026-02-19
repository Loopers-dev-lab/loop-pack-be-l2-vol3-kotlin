package com.loopers.interfaces.support

import com.loopers.domain.PageResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

fun <T> PageResult<T>.toSpringPage(): Page<T> {
    return PageImpl(content, PageRequest.of(page, size), totalElements)
}
