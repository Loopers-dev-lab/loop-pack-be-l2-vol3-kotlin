package com.loopers.infrastructure.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderSnapshot
import org.springframework.stereotype.Component

@Component
class OrderMapper {

    fun toDomain(entity: OrderEntity): Order {
        val items = entity.items.map { itemEntity ->
            OrderItem.retrieve(
                id = itemEntity.id!!,
                snapshot = OrderSnapshot(
                    productId = itemEntity.productId,
                    productName = itemEntity.productName,
                    brandId = itemEntity.brandId,
                    brandName = itemEntity.brandName,
                    regularPrice = Money(itemEntity.regularPrice),
                    sellingPrice = Money(itemEntity.sellingPrice),
                    thumbnailUrl = itemEntity.thumbnailUrl,
                ),
                quantity = Quantity(itemEntity.quantity),
            )
        }

        return Order.retrieve(
            id = entity.id!!,
            userId = entity.userId,
            idempotencyKey = IdempotencyKey(entity.idempotencyKey),
            status = entity.status,
            items = items,
            issuedCouponId = entity.issuedCouponId,
            discountAmount = Money(entity.discountAmount),
            createdAt = entity.createdAt,
        )
    }

    fun toEntity(order: Order): OrderEntity {
        val entity = OrderEntity(
            id = order.id,
            userId = order.userId,
            idempotencyKey = order.idempotencyKey.value,
            status = order.status,
            issuedCouponId = order.issuedCouponId,
            discountAmount = order.discountAmount.amount,
        )
        entity.items.addAll(
            order.items.map { item ->
                OrderItemEntity(
                    id = item.id,
                    productId = item.snapshot.productId,
                    productName = item.snapshot.productName,
                    brandId = item.snapshot.brandId,
                    brandName = item.snapshot.brandName,
                    regularPrice = item.snapshot.regularPrice.amount,
                    sellingPrice = item.snapshot.sellingPrice.amount,
                    thumbnailUrl = item.snapshot.thumbnailUrl,
                    quantity = item.quantity.value,
                )
            },
        )
        return entity
    }
}
