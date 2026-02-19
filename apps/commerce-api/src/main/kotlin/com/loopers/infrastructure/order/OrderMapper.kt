package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.product.Money

object OrderMapper {

    fun toDomain(entity: OrderEntity): Order {
        val id = requireNotNull(entity.id) {
            "OrderEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return Order.reconstitute(
            persistenceId = id,
            refUserId = entity.userId,
            status = entity.status,
            totalAmount = Money(entity.totalAmount),
            orderedAt = entity.orderedAt,
            items = entity.items.map { toItemDomain(it) },
        )
    }

    fun toEntity(domain: Order): OrderEntity {
        val entity = OrderEntity(
            id = domain.persistenceId,
            userId = domain.refUserId,
            status = domain.status,
            totalAmount = domain.totalAmount.amount,
            orderedAt = domain.orderedAt,
        )
        domain.items.forEach { item ->
            entity.items.add(toItemEntity(item, entity))
        }
        return entity
    }

    private fun toItemDomain(entity: OrderItemEntity): OrderItem {
        val id = requireNotNull(entity.id) {
            "OrderItemEntity.id가 null입니다. 저장된 Entity만 Domain으로 변환 가능합니다."
        }
        return OrderItem.reconstitute(
            persistenceId = id,
            refProductId = entity.productId,
            productName = entity.productName,
            brandName = entity.brandName,
            price = Money(entity.price),
            quantity = entity.quantity,
        )
    }

    private fun toItemEntity(domain: OrderItem, orderEntity: OrderEntity): OrderItemEntity {
        return OrderItemEntity(
            id = domain.persistenceId,
            order = orderEntity,
            productId = domain.refProductId,
            productName = domain.productName,
            brandName = domain.brandName,
            price = domain.price.amount,
            quantity = domain.quantity,
        )
    }
}
