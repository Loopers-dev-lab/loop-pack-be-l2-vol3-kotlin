package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderStatus
import org.springframework.stereotype.Component

@Component
class OrderMapper {

    fun toDomain(entity: OrderEntity): Order {
        val orderItems = entity.orderItems.map { itemEntity ->
            OrderItem(
                id = itemEntity.id,
                productId = itemEntity.productId,
                productName = itemEntity.productName,
                productPrice = itemEntity.productPrice,
                quantity = itemEntity.quantity,
            )
        }

        return Order(
            id = entity.id,
            memberId = entity.memberId,
            orderItems = orderItems,
            totalPrice = entity.totalPrice,
            discountAmount = entity.discountAmount,
            finalPrice = entity.finalPrice,
            couponId = entity.couponId,
            orderedAt = entity.orderedAt,
            status = OrderStatus.valueOf(entity.status),
        )
    }

    fun toEntity(domain: Order): OrderEntity {
        val orderEntity = OrderEntity(
            memberId = domain.memberId,
            status = domain.status.name,
            totalPrice = domain.totalPrice,
            discountAmount = domain.discountAmount,
            finalPrice = domain.finalPrice,
            couponId = domain.couponId,
            orderedAt = domain.orderedAt,
        )

        domain.orderItems.forEach { item ->
            val itemEntity = OrderItemEntity(
                productId = item.productId,
                productName = item.productName,
                productPrice = item.productPrice,
                quantity = item.quantity,
            )
            orderEntity.addOrderItem(itemEntity)
        }

        return orderEntity
    }

    fun update(entity: OrderEntity, domain: Order) {
        entity.status = domain.status.name
    }
}
