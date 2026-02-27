package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderAdminV1Dto {

    @Schema(description = "주문 응답 (어드민)")
    data class OrderAdminResponse(
        @Schema(description = "주문 ID", example = "1")
        val id: Long,
        @Schema(description = "유저 ID", example = "1")
        val userId: Long,
        @Schema(description = "총 주문 금액", example = "258000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목")
        val items: List<OrderItemAdminResponse>,
        @Schema(description = "주문 생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderAdminResponse {
                return OrderAdminResponse(
                    id = info.id,
                    userId = info.userId,
                    totalAmount = info.totalAmount,
                    items = info.items.map { OrderItemAdminResponse.from(it) },
                    createdAt = info.createdAt,
                )
            }
        }
    }

    @Schema(description = "주문 항목 응답 (어드민)")
    data class OrderItemAdminResponse(
        @Schema(description = "주문 항목 ID", example = "1")
        val id: Long,
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,
        @Schema(description = "상품명", example = "에어맥스 90")
        val productName: String,
        @Schema(description = "브랜드명", example = "나이키")
        val brandName: String,
        @Schema(description = "수량", example = "2")
        val quantity: Int,
        @Schema(description = "단가", example = "129000")
        val unitPrice: BigDecimal,
    ) {
        companion object {
            fun from(info: OrderItemInfo): OrderItemAdminResponse {
                return OrderItemAdminResponse(
                    id = info.id,
                    productId = info.productId,
                    productName = info.productName,
                    brandName = info.brandName,
                    quantity = info.quantity,
                    unitPrice = info.unitPrice,
                )
            }
        }
    }
}
