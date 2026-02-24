package com.loopers.application.order

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.common.Money
import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.point.PointPaymentProcessor
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Component
class PlaceOrderUseCase(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val pointPaymentProcessor: PointPaymentProcessor,
) {

    @Transactional
    fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }

        val productIds = command.items.map { it.productId }
        val products = productRepository.findAllByIdsForUpdate(productIds)

        val foundIds = products.map { it.id }.toSet()
        val missingIds = productIds.filter { it !in foundIds }
        if (missingIds.isNotEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
        }

        products.forEach { product ->
            if (!product.isAvailableForOrder()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
            }
        }

        val quantityMap = command.items.associate { it.productId to it.quantity }

        products.forEach { product ->
            product.decreaseStock(quantityMap[product.id]!!)
        }
        productRepository.saveAll(products)

        val productMap = products.associateBy { it.id }
        val totalPrice = command.items.fold(Money(BigDecimal.ZERO)) { acc, item ->
            val product = productMap[item.productId]!!
            acc + (product.price * item.quantity)
        }

        val order = Order.create(userId, totalPrice)
        val savedOrder = orderRepository.save(order)

        val orderItems = command.items.map { item ->
            val product = productMap[item.productId]!!
            OrderItem.create(
                OrderProductInfo(product.id, product.name, product.price),
                item.quantity,
                savedOrder.id,
            )
        }
        val savedItems = orderItemRepository.saveAll(orderItems)

        pointPaymentProcessor.usePoints(userId, totalPrice, savedOrder.id)

        return OrderInfo.from(OrderDetail(savedOrder, savedItems))
    }
}
