package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(name = "orders")
@Entity
class OrderEntity(
    id: Long? = null,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: Order.Status,
    @Column(name = "issued_coupon_id")
    val issuedCouponId: Long? = null,
    @Column(name = "discount_amount", nullable = false)
    val discountAmount: BigDecimal = BigDecimal.ZERO,
) : BaseEntity() {

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(
        name = "order_id",
        nullable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    @OrderBy("id ASC")
    val items: MutableList<OrderItemEntity> = mutableListOf()

    init {
        this.id = id
    }
}
