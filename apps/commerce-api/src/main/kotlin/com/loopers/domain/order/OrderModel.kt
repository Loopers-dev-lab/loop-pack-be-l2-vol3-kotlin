package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class OrderModel(
    userId: Long,
    orderStatus: OrderStatus = OrderStatus.ORDERED,
) : BaseEntity() {

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    var orderStatus: OrderStatus = orderStatus
        protected set

    @Column(name = "coupon_issue_id")
    var couponIssueId: Long? = null
        protected set

    @Column(name = "original_total_amount", nullable = false)
    var originalTotalAmount: Long = 0
        protected set

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = 0
        protected set

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItemModel> = mutableListOf()

    /** 주문 항목을 추가하고 총 금액을 재계산한다. */
    fun addItem(item: OrderItemModel) {
        orderItems.add(item)
        recalculateTotalAmount()
    }

    /** 쿠폰 할인을 적용한다. */
    fun applyCouponDiscount(couponIssueId: Long, discountAmount: Long) {
        this.couponIssueId = couponIssueId
        this.discountAmount = discountAmount
        this.totalAmount = (this.originalTotalAmount - discountAmount).coerceAtLeast(0)
    }

    private fun recalculateTotalAmount() {
        val itemsTotal = orderItems.sumOf { it.subTotal }
        this.originalTotalAmount = itemsTotal
        this.totalAmount = itemsTotal - this.discountAmount
    }
}
