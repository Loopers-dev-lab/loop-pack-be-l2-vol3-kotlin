package com.loopers.domain.order

import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    // ✅ Product 조회용 추가
    private val productService: ProductService,
) {

    @Transactional
    fun createOrder(userId: Long, items: List<CreateOrderItemCommand>, couponId: Long? = null): Order {
        validateItems(items)

        // Order 먼저 생성 및 저장
        val order = Order.create(userId, couponId)
        val savedOrder = orderRepository.save(order)

        // OrderItemSpec 준비
        val itemSpecs = items.map { cmd ->
            OrderItemSpec(
                product = productService.getProduct(cmd.productId),
                quantity = cmd.quantity,
                price = cmd.price,
            )
        }

        // 저장된 Order에 OrderItem 추가
        // Order.id는 이미 할당되어 있으므로, OrderItem.create()가 올바른 orderId를 복사함
        itemSpecs.forEach { spec ->
            savedOrder.addItem(spec.product, spec.quantity, spec.price)
        }

        return savedOrder
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> {
        return orderRepository.findByUserId(userId, pageable).map { OrderedInfo.from(it) }
    }

    fun getOrderById(userId: Long, orderId: Long): Order =
        orderRepository.findById(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForAdmin(orderId: Long): Order =
        orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    private fun validateItems(items: List<CreateOrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다")
        }

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다")
            }
        }
    }
}
