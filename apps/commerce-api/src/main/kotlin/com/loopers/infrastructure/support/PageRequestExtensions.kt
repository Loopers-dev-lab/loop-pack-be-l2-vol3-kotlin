package com.loopers.infrastructure.support

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

private val DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "id")

fun defaultPageRequest(page: Int, size: Int): PageRequest =
    PageRequest.of(page, size, DEFAULT_SORT)
