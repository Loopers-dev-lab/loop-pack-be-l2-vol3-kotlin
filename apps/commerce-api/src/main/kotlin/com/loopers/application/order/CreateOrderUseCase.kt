package com.loopers.application.order

import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderItemSnapshot
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderValidator
import com.loopers.domain.product.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val orderValidator: OrderValidator,
) {

    @Transactional
    fun execute(command: OrderCommand.Create): OrderInfo {
        val orderLines = command.toOrderLines()

        // 데드락 방지를 위해 productId 오름차순으로 비관적 락 획득
        val sortedProductIds = orderLines.map { it.productId }.distinct().sorted()
        val products = sortedProductIds.mapNotNull { productId ->
            productRepository.findByIdForUpdate(productId)
        }
        val productMap = products.associateBy { it.id }

        orderValidator.validate(orderLines, productMap)

        val brandIds = products.map { it.brandId }.distinct()
        val brandMap = brandRepository.findAllByIds(brandIds).associateBy { it.id }

        val snapshots = orderLines.map { line ->
            val product = productMap.getValue(line.productId)
            val brand = brandMap[product.brandId]
            OrderItemSnapshot(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                brandName = brand?.name ?: "",
                imageUrl = product.imageUrl,
                quantity = line.quantity,
            )
        }

        val order = com.loopers.domain.order.Order.create(userId = command.userId, items = snapshots)
        val savedOrder = orderRepository.save(order)

        val orderItems = snapshots.map { snapshot ->
            OrderItem.create(orderId = savedOrder.id, snapshot = snapshot)
        }
        orderItemRepository.saveAll(orderItems)

        orderLines.forEach { line ->
            val product = productMap.getValue(line.productId)
            product.decreaseStock(line.quantity.value)
        }

        return OrderInfo.from(savedOrder)
    }
}
