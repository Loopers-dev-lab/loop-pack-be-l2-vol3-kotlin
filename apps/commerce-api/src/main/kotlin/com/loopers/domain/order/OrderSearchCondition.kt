package com.loopers.domain.order

import java.time.LocalDate

data class OrderSearchCondition(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val page: Int,
    val size: Int,
)
