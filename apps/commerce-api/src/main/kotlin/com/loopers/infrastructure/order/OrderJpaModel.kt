package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "orders")
class OrderJpaModel(
    memberId: Long,
) : BaseEntity() {
    @Column(name = "member_id", nullable = false)
    var memberId: Long = memberId
        protected set

    @Column(name = "order_number", nullable = false, unique = true, length = 36)
    var orderNumber: String = UUID.randomUUID().toString()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: OrderStatus = OrderStatus.ORDERED
        protected set

    @Column(name = "ordered_at", nullable = false)
    var orderedAt: ZonedDateTime = ZonedDateTime.now()
        protected set

    @Column(name = "coupon_id")
    var couponId: Long? = null
        protected set

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = 0
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableList<OrderItemJpaModel> = mutableListOf()
        protected set

    fun addItem(orderItem: OrderItemJpaModel) {
        orderItems.add(orderItem)
        orderItem.order = this
    }

    fun toModel(): OrderModel = OrderModel(
        id = id,
        memberId = memberId,
        orderNumber = orderNumber,
        status = status,
        orderedAt = orderedAt,
        items = orderItems.map { it.toModel() },
        couponId = couponId,
        discountAmount = discountAmount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    companion object {
        fun from(model: OrderModel): OrderJpaModel {
            val jpaModel = OrderJpaModel(memberId = model.memberId)
            jpaModel.orderNumber = model.orderNumber
            jpaModel.status = model.status
            jpaModel.orderedAt = model.orderedAt
            jpaModel.couponId = model.couponId
            jpaModel.discountAmount = model.discountAmount
            model.items.forEach { item ->
                jpaModel.addItem(OrderItemJpaModel.from(item))
            }
            return jpaModel
        }
    }
}
