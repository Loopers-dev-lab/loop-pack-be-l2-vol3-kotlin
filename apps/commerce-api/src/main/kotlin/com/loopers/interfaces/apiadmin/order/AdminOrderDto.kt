package com.loopers.interfaces.apiadmin.order

import com.loopers.application.order.AdminOrderInfo
import com.loopers.application.order.OrderDetailInfo
import com.loopers.application.order.OrderItemInfo
import com.loopers.domain.common.PageResult
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
        val status: String,
        val orderedAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: AdminOrderInfo): ListItem {
                return ListItem(
                    orderId = info.orderId,
                    userId = info.userId,
                    totalAmount = info.totalAmount,
                    status = info.status.name,
                    orderedAt = info.orderedAt,
                )
            }
        }
    }

    data class DetailResponse(
        val orderId: Long,
        val userId: Long,
        val totalAmount: Long,
        val status: String,
        val orderedAt: ZonedDateTime,
        val items: List<ItemResponse>,
    ) {
        companion object {
            fun from(info: OrderDetailInfo): DetailResponse {
                return DetailResponse(
                    orderId = info.orderId,
                    userId = info.userId,
                    totalAmount = info.totalAmount,
                    status = info.status.name,
                    orderedAt = info.orderedAt,
                    items = info.items.map { ItemResponse.from(it) },
                )
            }
        }
    }

    data class ItemResponse(
        val productId: Long,
        val quantity: Int,
        val productName: String,
        val productPrice: Long,
        val brandName: String,
    ) {
        companion object {
            fun from(info: OrderItemInfo): ItemResponse {
                return ItemResponse(
                    productId = info.productId,
                    quantity = info.quantity,
                    productName = info.productName,
                    productPrice = info.productPrice,
                    brandName = info.brandName,
                )
            }
        }
    }
}
