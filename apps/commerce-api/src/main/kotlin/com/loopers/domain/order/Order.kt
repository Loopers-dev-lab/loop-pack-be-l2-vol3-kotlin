package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class Order(
    @Column(name = "user_id", nullable = false)
    val userId: Long,

    items: List<OrderItem> = emptyList(),

    @Column(name = "coupon_id")
    val couponId: Long? = null,

    discountAmount: Long = 0,
) : BaseEntity() {

    @OneToMany(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    val items: MutableList<OrderItem> = items.toMutableList()

    @Column(name = "original_total_price", nullable = false)
    val originalTotalPrice: Long = items.sumOf { it.productPrice * it.quantity }

    @Column(name = "discount_amount", nullable = false)
    val discountAmount: Long = discountAmount

    @Column(name = "total_price", nullable = false)
    var totalPrice: Long = (originalTotalPrice - discountAmount).coerceAtLeast(0)
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    fun pay() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 가능한 상태가 아닙니다.")
        }
        status = OrderStatus.PAID
    }

    fun cancel() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "취소 가능한 상태가 아닙니다.")
        }
        status = OrderStatus.CANCELLED
    }
}
