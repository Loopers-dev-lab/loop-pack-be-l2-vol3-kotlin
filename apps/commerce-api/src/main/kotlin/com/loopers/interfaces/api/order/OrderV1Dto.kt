package com.loopers.interfaces.api.order

import com.loopers.domain.order.ExcludedItem
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderResult
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {

    @Schema(description = "주문 생성 요청")
    data class CreateRequest(
        @Schema(description = "주문 항목 목록")
        val items: List<OrderItemRequest>,
    ) {
        fun toCommands(): List<OrderItemCommand> {
            return items.map { OrderItemCommand(productId = it.productId, quantity = it.quantity) }
        }
    }

    @Schema(description = "주문 항목 요청")
    data class OrderItemRequest(
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,
        @Schema(description = "수량", example = "2")
        val quantity: Int,
    )

    @Schema(description = "주문 생성 응답")
    data class CreateOrderResponse(
        @Schema(description = "주문 ID", example = "1")
        val orderId: Long,
        @Schema(description = "총 주문 금액", example = "258000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목")
        val items: List<OrderItemResponse>,
        @Schema(description = "제외된 항목")
        val excludedItems: List<ExcludedItemResponse>,
    ) {
        companion object {
            fun from(result: OrderResult): CreateOrderResponse {
                return CreateOrderResponse(
                    orderId = result.order.id,
                    totalAmount = result.order.totalAmount,
                    items = result.order.orderItems.map { OrderItemResponse.from(it) },
                    excludedItems = result.excludedItems.map { ExcludedItemResponse.from(it) },
                )
            }
        }
    }

    @Schema(description = "주문 응답")
    data class OrderResponse(
        @Schema(description = "주문 ID", example = "1")
        val id: Long,
        @Schema(description = "유저 ID", example = "1")
        val userId: Long,
        @Schema(description = "총 주문 금액", example = "258000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목")
        val items: List<OrderItemResponse>,
        @Schema(description = "주문 생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderResponse {
                return OrderResponse(
                    id = order.id,
                    userId = order.userId,
                    totalAmount = order.totalAmount,
                    items = order.orderItems.map { OrderItemResponse.from(it) },
                    createdAt = order.createdAt,
                )
            }
        }
    }

    @Schema(description = "주문 목록 응답")
    data class OrderSummaryResponse(
        @Schema(description = "주문 ID", example = "1")
        val id: Long,
        @Schema(description = "총 주문 금액", example = "258000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목 수", example = "3")
        val itemCount: Int,
        @Schema(description = "주문 생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(order: Order): OrderSummaryResponse {
                return OrderSummaryResponse(
                    id = order.id,
                    totalAmount = order.totalAmount,
                    itemCount = order.orderItems.size,
                    createdAt = order.createdAt,
                )
            }
        }
    }

    @Schema(description = "주문 항목 응답")
    data class OrderItemResponse(
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
            fun from(item: OrderItem): OrderItemResponse {
                return OrderItemResponse(
                    id = item.id,
                    productId = item.productId,
                    productName = item.productName,
                    brandName = item.brandName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                )
            }
        }
    }

    @Schema(description = "제외된 항목 응답")
    data class ExcludedItemResponse(
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,
        @Schema(description = "제외 사유", example = "재고가 부족합니다.")
        val reason: String,
    ) {
        companion object {
            fun from(item: ExcludedItem): ExcludedItemResponse {
                return ExcludedItemResponse(
                    productId = item.productId,
                    reason = item.reason,
                )
            }
        }
    }
}
