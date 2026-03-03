package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.model.Order
import com.loopers.domain.withBaseFields
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
    @Column(name = "original_price", nullable = false, precision = 10, scale = 2)
    var originalPrice: BigDecimal,
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    var discountAmount: BigDecimal,
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    var totalPrice: BigDecimal,
    @Column(name = "ref_coupon_id")
    var refCouponId: Long?,
) : BaseEntity() {

    companion object {
        fun fromDomain(order: Order): OrderEntity {
            return OrderEntity(
                refUserId = order.refUserId.value,
                status = order.status,
                originalPrice = order.originalPrice.value,
                discountAmount = order.discountAmount.value,
                totalPrice = order.totalPrice.value,
                refCouponId = order.refCouponId?.value,
            ).withBaseFields(
                id = order.id.value,
                deletedAt = order.deletedAt,
            )
        }
    }

    fun toDomain(): Order = Order.fromPersistence(
        id = OrderId(id),
        refUserId = UserId(refUserId),
        status = status,
        originalPrice = Money(originalPrice),
        discountAmount = Money(discountAmount),
        totalPrice = Money(totalPrice),
        refCouponId = refCouponId?.let { CouponId(it) },
        deletedAt = deletedAt,
    )
}
