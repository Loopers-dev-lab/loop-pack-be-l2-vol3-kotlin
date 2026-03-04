package com.loopers.interfaces.api.order

import com.loopers.application.order.ExcludedItemInfo
import com.loopers.application.order.OrderInfo
import com.loopers.application.order.OrderItemCriteria
import com.loopers.application.order.OrderItemInfo
import com.loopers.application.order.OrderResultInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.ZonedDateTime

class OrderV1Dto {

    @Schema(description = "주문 생성 요청")
    data class CreateRequest(
        @Schema(description = "주문 항목 목록")
        val items: List<OrderItemRequest>,
        @Schema(description = "발급 쿠폰 ID (optional)", example = "1", required = false)
        val couponId: Long? = null,
    ) {
        fun toCriteria(): List<OrderItemCriteria> {
            return items.map { OrderItemCriteria(productId = it.productId, quantity = it.quantity) }
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
        @Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long?,
        @Schema(description = "원래 주문 금액", example = "258000")
        val originalAmount: BigDecimal,
        @Schema(description = "할인 금액", example = "5000")
        val discountAmount: BigDecimal,
        @Schema(description = "최종 주문 금액", example = "253000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목")
        val items: List<OrderItemResponse>,
        @Schema(description = "제외된 항목")
        val excludedItems: List<ExcludedItemResponse>,
    ) {
        companion object {
            fun from(result: OrderResultInfo): CreateOrderResponse {
                return CreateOrderResponse(
                    orderId = result.order.id,
                    couponId = result.order.couponId,
                    originalAmount = result.order.originalAmount,
                    discountAmount = result.order.discountAmount,
                    totalAmount = result.order.totalAmount,
                    items = result.order.items.map { OrderItemResponse.from(it) },
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
        @Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long?,
        @Schema(description = "원래 주문 금액", example = "258000")
        val originalAmount: BigDecimal,
        @Schema(description = "할인 금액", example = "5000")
        val discountAmount: BigDecimal,
        @Schema(description = "최종 주문 금액", example = "253000")
        val totalAmount: BigDecimal,
        @Schema(description = "주문 항목")
        val items: List<OrderItemResponse>,
        @Schema(description = "주문 생성일시")
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo): OrderResponse {
                return OrderResponse(
                    id = info.id,
                    userId = info.userId,
                    couponId = info.couponId,
                    originalAmount = info.originalAmount,
                    discountAmount = info.discountAmount,
                    totalAmount = info.totalAmount,
                    items = info.items.map { OrderItemResponse.from(it) },
                    createdAt = info.createdAt,
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
            fun from(info: OrderInfo): OrderSummaryResponse {
                return OrderSummaryResponse(
                    id = info.id,
                    totalAmount = info.totalAmount,
                    itemCount = info.items.size,
                    createdAt = info.createdAt,
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
            fun from(info: OrderItemInfo): OrderItemResponse {
                return OrderItemResponse(
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

    @Schema(description = "제외된 항목 응답")
    data class ExcludedItemResponse(
        @Schema(description = "상품 ID", example = "1")
        val productId: Long,
        @Schema(description = "제외 사유", example = "재고가 부족합니다.")
        val reason: String,
    ) {
        companion object {
            fun from(info: ExcludedItemInfo): ExcludedItemResponse {
                return ExcludedItemResponse(
                    productId = info.productId,
                    reason = info.reason,
                )
            }
        }
    }
}
