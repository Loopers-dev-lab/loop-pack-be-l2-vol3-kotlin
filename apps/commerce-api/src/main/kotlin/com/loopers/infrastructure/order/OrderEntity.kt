package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.Money
import com.loopers.domain.order.entity.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "orders")
class OrderEntity(
    @Column(name = "ref_user_id", nullable = false)
    var refUserId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: Order.OrderStatus,
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal,
) : BaseEntity() {

    companion object {
        fun fromDomain(order: Order): OrderEntity {
            return OrderEntity(
                refUserId = order.refUserId,
                status = order.status,
                totalPrice = order.totalPrice.value,
            ).also { entity ->
                if (order.id != 0L) {
                    setBaseEntityField(entity, "id", order.id)
                    setBaseEntityField(entity, "createdAt", order.createdAt)
                    setBaseEntityField(entity, "updatedAt", order.updatedAt)
                }
                order.deletedAt?.let { setBaseEntityField(entity, "deletedAt", it) }
            }
        }

        private fun setBaseEntityField(entity: BaseEntity, fieldName: String, value: Any) {
            BaseEntity::class.java.getDeclaredField(fieldName).apply {
                isAccessible = true
                set(entity, value)
            }
        }
    }

    fun toDomain(): Order = Order.fromPersistence(
        id = id,
        refUserId = refUserId,
        status = status,
        totalPrice = Money(totalPrice),
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
