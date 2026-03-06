package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.product.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "orders")
class Order private constructor(
    userId: Long,
    originalAmount: Money,
    discountAmount: Money,
    totalAmount: Money,
    userCouponId: Long?,
    status: OrderStatus,
    orderedAt: ZonedDateTime,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "original_amount", nullable = false))
    val originalAmount: Money = originalAmount

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "discount_amount", nullable = false))
    val discountAmount: Money = discountAmount

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "total_amount", nullable = false))
    var totalAmount: Money = totalAmount
        protected set

    @Column(name = "user_coupon_id")
    val userCouponId: Long? = userCouponId

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus = status
        protected set

    @Column(name = "ordered_at", nullable = false)
    val orderedAt: ZonedDateTime = orderedAt

    companion object {
        fun create(
            userId: Long,
            items: List<OrderItemSnapshot>,
            discountAmount: Money = Money(0),
            userCouponId: Long? = null,
        ): Order {
            if (items.isEmpty()) throw CoreException(OrderErrorCode.EMPTY_ORDER_ITEMS)
            val originalAmount = Money(items.sumOf { it.productPrice.amount * it.quantity.value })
            val totalAmount = Money(originalAmount.amount - discountAmount.amount)
            return Order(
                userId = userId,
                originalAmount = originalAmount,
                discountAmount = discountAmount,
                totalAmount = totalAmount,
                userCouponId = userCouponId,
                status = OrderStatus.ORDERED,
                orderedAt = ZonedDateTime.now(),
            )
        }
    }
}
