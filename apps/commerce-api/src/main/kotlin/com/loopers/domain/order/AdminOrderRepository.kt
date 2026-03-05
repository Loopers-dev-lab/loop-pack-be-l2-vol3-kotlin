package com.loopers.domain.order

import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import java.time.ZonedDateTime

interface AdminOrderRepository {
    fun findById(id: Long): Order?
    fun findAll(
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        pageRequest: PageRequest,
    ): PageResponse<Order>
}
