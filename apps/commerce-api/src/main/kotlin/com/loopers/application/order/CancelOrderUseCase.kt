package com.loopers.application.order

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Order + Product(재고 복원)
 * MSA 분리 시 Product 재고 복원 → Product Service API 호출 + Saga 보상 트랜잭션 필요
 */
@Component
class CancelOrderUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun cancel(userId: Long, orderId: Long) {
        val order = orderRepository.findByIdForUpdate(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")

        order.assertOwnedBy(userId)

        val cancelledOrder = order.cancel()
        orderRepository.save(cancelledOrder)

        cancelledOrder.items.forEach { item ->
            productRepository.increaseStock(item.productId, item.quantity)
        }
    }
}
