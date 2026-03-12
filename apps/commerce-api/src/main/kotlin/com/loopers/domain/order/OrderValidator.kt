package com.loopers.domain.order

import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStock
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import com.loopers.support.error.OrderValidationError
import com.loopers.support.error.OrderValidationException
import org.springframework.stereotype.Component

@Component
class OrderValidator {

    companion object {
        private const val MAX_ORDER_TYPES = 20
        private const val MAX_ORDER_QUANTITY = 99
    }

    fun validate(
        orderLines: List<OrderLine>,
        productMap: Map<Long, Product>,
        productStockMap: Map<Long, ProductStock>,
    ) {
        val errors = mutableListOf<OrderValidationError>()

        if (orderLines.isEmpty()) {
            throw CoreException(OrderErrorCode.EMPTY_ORDER_ITEMS)
        }

        val productIds = orderLines.map { it.productId }
        if (productIds.distinct().size != productIds.size) {
            throw CoreException(OrderErrorCode.DUPLICATE_ORDER_ITEM)
        }

        if (orderLines.size > MAX_ORDER_TYPES) {
            throw CoreException(OrderErrorCode.EXCEED_MAX_ORDER_TYPES)
        }

        orderLines.forEach { line ->
            if (line.quantity.value > MAX_ORDER_QUANTITY) {
                throw CoreException(OrderErrorCode.EXCEED_MAX_ORDER_QUANTITY)
            }
        }

        orderLines.forEach { line ->
            val product = productMap[line.productId]
            if (product == null) {
                errors.add(
                    OrderValidationError(
                        productId = line.productId,
                        reason = "PRODUCT_NOT_FOUND",
                        detail = "존재하지 않는 상품입니다.",
                    ),
                )
                return@forEach
            }
            if (product.isDeleted()) {
                errors.add(
                    OrderValidationError(
                        productId = line.productId,
                        reason = "PRODUCT_DELETED",
                        detail = "삭제된 상품입니다.",
                    ),
                )
                return@forEach
            }

            val productStock = productStockMap[line.productId]
            val stockQuantity = productStock?.stock?.quantity ?: 0
            if (stockQuantity < line.quantity.value) {
                errors.add(
                    OrderValidationError(
                        productId = line.productId,
                        reason = "INSUFFICIENT_STOCK",
                        detail = "재고가 부족합니다. (요청: ${line.quantity.value}개, 잔여: ${stockQuantity}개)",
                    ),
                )
            }
        }

        if (errors.isNotEmpty()) {
            throw OrderValidationException(errors)
        }
    }
}
