package com.loopers.domain.order

import com.loopers.domain.payment.PaymentStatus
import com.loopers.domain.product.ProductStockDeductor
import org.springframework.stereotype.Component

@Component
class OrderPaymentProcessor(
    private val orderReader: OrderReader,
    private val orderRepository: OrderRepository,
    private val productStockDeductor: ProductStockDeductor,
) {

    fun beginPayment(orderId: Long, memberId: Long): Order {
        val order = orderReader.getByIdForUpdate(orderId)
        order.validateOwner(memberId)
        order.beginPayment()
        return orderRepository.save(order)
    }

    fun applyPaymentResult(orderId: Long, memberId: Long, paymentStatus: PaymentStatus): Order {
        val order = orderReader.getByIdForUpdate(orderId)
        order.validateOwner(memberId)
        return applyPaymentResult(order, paymentStatus)
    }

    fun applyPaymentResult(orderId: Long, paymentStatus: PaymentStatus): Order {
        val order = orderReader.getByIdForUpdate(orderId)
        return applyPaymentResult(order, paymentStatus)
    }

    private fun applyPaymentResult(order: Order, paymentStatus: PaymentStatus): Order {
        val shouldRestoreStock =
            order.status == OrderStatus.PAYMENT_PENDING &&
                (paymentStatus == PaymentStatus.REQUEST_FAILED || paymentStatus == PaymentStatus.FAILED)

        when (paymentStatus) {
            PaymentStatus.SUCCESS -> order.markPaid()
            PaymentStatus.REQUEST_FAILED,
            PaymentStatus.FAILED,
            -> order.markPaymentFailed()

            PaymentStatus.REQUESTED,
            PaymentStatus.PENDING,
            PaymentStatus.UNKNOWN,
            -> Unit
        }

        val savedOrder = orderRepository.save(order)

        if (shouldRestoreStock) {
            savedOrder.orderItems.forEach { item ->
                productStockDeductor.restoreStock(item.productId, item.quantity)
            }
        }

        return savedOrder
    }
}
