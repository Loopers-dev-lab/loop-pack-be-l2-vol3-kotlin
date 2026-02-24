package com.loopers.application.order

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.point.PointDeductor
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PlaceOrderUseCase(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val pointDeductor: PointDeductor,
) {

    @Transactional
    fun execute(userId: Long, command: PlaceOrderCommand): OrderInfo {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }

        val products = productRepository.findAllByIdsForUpdate(command.items.map { it.productId })
        if (products.size != command.items.size) {
            throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
        }
        val productMap = products.associateBy { it.id }

        val orderItemInputs = command.items.map { item ->
            val product = productMap[item.productId]
                ?: throw CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 상품이 포함되어 있습니다.")
            if (!product.isAvailableForOrder()) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 가능한 상태가 아닌 상품이 포함되어 있습니다.")
            }
            product.decreaseStock(item.quantity)
            OrderProductInfo(product.id, product.name, product.price) to item.quantity
        }
        productRepository.saveAll(products)

        val order = Order.create(userId, orderItemInputs)
        val savedOrder = orderRepository.save(order)

        order.assignOrderIdToItems(savedOrder.id)
        val savedItems = orderItemRepository.saveAll(order.items)

        pointDeductor.usePoints(userId, savedOrder.totalPrice, savedOrder.id)

        return OrderInfo.from(OrderDetail(savedOrder, savedItems))
    }
}
