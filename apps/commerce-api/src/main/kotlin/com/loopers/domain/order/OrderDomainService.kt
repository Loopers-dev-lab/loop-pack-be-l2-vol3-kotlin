package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.ProductStock
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

object OrderDomainService {

    data class OrderItemRequest(
        val productStock: ProductStock,
        val snapshot: OrderSnapshot,
        val quantity: Quantity,
    )

    data class CreateOrderResult(
        val order: Order,
        val decreasedStocks: List<ProductStock>,
    )

    data class InsufficientStockInfo(
        val productId: Long,
        val productName: String,
        val requestedQuantity: Int,
        val availableQuantity: Int,
    )

    fun createOrder(
        userId: Long,
        idempotencyKey: IdempotencyKey,
        orderItemRequests: List<OrderItemRequest>,
        issuedCouponId: Long? = null,
        discountAmount: Money = Money.ZERO,
    ): CreateOrderResult {
        // 0. Precondition 검증
        validatePreconditions(orderItemRequests)

        // 1. 전체 재고 검증 (All-or-Nothing)
        val insufficientList = orderItemRequests
            .filter { !it.productStock.quantity.isEnoughFor(it.quantity) }
            .map {
                InsufficientStockInfo(
                    productId = it.snapshot.productId,
                    productName = it.snapshot.productName,
                    requestedQuantity = it.quantity.value,
                    availableQuantity = it.productStock.quantity.value,
                )
            }

        if (insufficientList.isNotEmpty()) {
            throw CoreException(
                errorType = ErrorType.PRODUCT_STOCK_INSUFFICIENT,
                data = insufficientList,
            )
        }

        // 2. 재고 차감
        val decreasedStocks = orderItemRequests.map { it.productStock.decrease(it.quantity) }

        // 3. OrderItem 생성
        val orderItems = orderItemRequests.map {
            OrderItem.create(snapshot = it.snapshot, quantity = it.quantity)
        }

        // 4. Order 생성
        val order = Order.create(
            userId = userId,
            idempotencyKey = idempotencyKey,
            items = orderItems,
            issuedCouponId = issuedCouponId,
            discountAmount = discountAmount,
        )

        return CreateOrderResult(order = order, decreasedStocks = decreasedStocks)
    }

    private fun validatePreconditions(orderItemRequests: List<OrderItemRequest>) {
        orderItemRequests.forEach {
            require(it.productStock.productId == it.snapshot.productId) {
                "ProductStock.productId(${it.productStock.productId})와 " +
                    "OrderSnapshot.productId(${it.snapshot.productId})가 일치하지 않습니다."
            }
        }

        val productIds = orderItemRequests.map { it.snapshot.productId }
        require(productIds.size == productIds.toSet().size) {
            "동일 상품이 중복으로 포함되어 있습니다. " +
                "Application Layer에서 합산 후 전달해야 합니다."
        }
    }
}
