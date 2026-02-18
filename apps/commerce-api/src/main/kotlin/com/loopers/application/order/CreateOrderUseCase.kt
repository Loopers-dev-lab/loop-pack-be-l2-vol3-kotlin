package com.loopers.application.order

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Order + Product(재고 차감) + Brand(스냅샷 조회)
 * MSA 분리 시 Product 재고 차감 → Product Service API 호출 + Saga 보상 트랜잭션 필요
 */
@Component
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun create(userId: Long, command: CreateOrderCommand): Long {
        require(command.items.isNotEmpty()) { "주문 항목은 최소 1개 이상이어야 합니다." }

        val sortedItems = command.items.sortedBy { it.productId }

        val orderItems = sortedItems.map { item ->
            val product = productRepository.findByIdForUpdate(item.productId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: ${item.productId}")

            product.assertOrderable(item.quantity)

            val affected = productRepository.decreaseStock(item.productId, item.quantity)
            if (affected == 0) {
                throw CoreException(
                    ErrorType.CONFLICT,
                    "재고 차감에 실패했습니다. 상품: ${product.name.value}",
                )
            }

            val brand = brandRepository.findById(product.brandId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다: ${product.brandId}")

            OrderItem.create(
                product = product,
                brand = brand,
                quantity = item.quantity,
            )
        }

        val order = Order.create(userId = userId, items = orderItems)
        val completedOrder = order.complete()
        return orderRepository.save(completedOrder)
    }
}
