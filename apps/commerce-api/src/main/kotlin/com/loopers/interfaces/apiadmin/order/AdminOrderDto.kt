package com.loopers.interfaces.apiadmin.order

import com.loopers.application.order.AdminOrderInfo
import com.loopers.domain.common.PageResult
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

class AdminOrderDto {
    data class PageResponse(
        val content: List<ListItem>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    ) {
        companion object {
            fun from(page: PageResult<AdminOrderInfo>): PageResponse {
                return PageResponse(
                    content = page.content.map { ListItem.from(it) },
                    page = page.page,
                    size = page.size,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                )
            }
        }
    }

    data class ListItem(
        val orderId: Long,
        val userId: Long,
        val totalAmount: Long,
        val status: OrderStatus,
        val orderedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: AdminOrderInfo): ListItem {
                return ListItem(
                    orderId = info.orderId,
                    userId = info.userId,
                    totalAmount = info.totalAmount,
                    status = info.status,
                    orderedAt = info.orderedAt,
                )
            }
        }
    }
}
