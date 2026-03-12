package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.order.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class OrderEntity(
    userId: Long,
    originalTotalPrice: Int,
    discountAmount: Int = 0,
    totalPrice: Int,
    userCouponId: Long? = null,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "original_total_price", nullable = false)
    val originalTotalPrice: Int = originalTotalPrice

    @Column(name = "discount_amount", nullable = false)
    val discountAmount: Int = discountAmount

    @Column(name = "total_price", nullable = false)
    val totalPrice: Int = totalPrice

    @Column(name = "user_coupon_id")
    val userCouponId: Long? = userCouponId

    fun toDomain(items: List<OrderItemEntity>): Order = Order(
        id = this.id,
        userId = this.userId,
        items = items.map { it.toDomain() },
        originalTotalPrice = this.originalTotalPrice,
        discountAmount = this.discountAmount,
        userCouponId = this.userCouponId,
    )

    companion object {
        fun from(order: Order): OrderEntity = OrderEntity(
            userId = order.userId,
            originalTotalPrice = order.originalTotalPrice,
            discountAmount = order.discountAmount,
            totalPrice = order.totalPrice,
            userCouponId = order.userCouponId,
        )
    }
}
