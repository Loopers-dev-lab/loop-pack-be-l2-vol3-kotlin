package com.loopers.domain.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: OrderModel): OrderModel

    fun findById(id: Long): OrderModel?

    fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderModel>

    fun findAll(pageQuery: PageQuery): PageResult<OrderModel>
}
