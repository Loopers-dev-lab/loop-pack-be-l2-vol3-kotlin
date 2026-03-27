package com.loopers.domain.order

import com.loopers.domain.OrderBaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(
    name = "orders",
    indexes = [Index(name = "idx_orders_user_id", columnList = "user_id")],
)
class Order(
    id: Long,
    userId: Long,
    couponId: Long? = null,
) : OrderBaseEntity() {

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING
        private set

    @Column(name = "user_id", nullable = false)
    val userId: Long = userId

    @Column(name = "coupon_id")
    val couponId: Long? = couponId

    @Column(name = "original_amount", nullable = false, precision = 15, scale = 2)
    var originalAmount: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    var discountAmount: BigDecimal = BigDecimal.ZERO
        private set

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO
        private set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    private val _orderItems: MutableList<OrderItem> = mutableListOf()

    val orderItems: List<OrderItem>
        get() = _orderItems.toList()

    init {
        this.id = id
        require(userId > 0) { throw CoreException(ErrorType.BAD_REQUEST, "유저 ID는 1 이상이어야 합니다.") }
    }

    fun addItem(
        productId: Long,
        productName: String,
        brandName: String,
        quantity: Int,
        unitPrice: BigDecimal,
    ) {
        val item = OrderItem(
            order = this,
            productId = productId,
            productName = productName,
            brandName = brandName,
            quantity = quantity,
            unitPrice = unitPrice,
        )
        _orderItems.add(item)
        recalculateTotalAmount()
    }

    fun applyDiscount(discountAmount: BigDecimal) {
        this.discountAmount = discountAmount
        this.totalAmount = this.originalAmount - discountAmount
    }

    fun validateNotEmpty() {
        if (_orderItems.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }
    }

    fun markPaid() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 결제 완료 처리가 가능합니다.")
        }
        status = OrderStatus.PAID
    }

    fun markFailed() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태에서만 결제 실패 처리가 가능합니다.")
        }
        status = OrderStatus.FAILED
    }

    fun validatePayable() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.BAD_REQUEST, "결제 가능한 상태가 아닙니다. 현재 상태: $status")
        }
    }

    fun validateOwner(userId: Long) {
        if (this.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "해당 주문에 대한 권한이 없습니다.")
        }
    }

    private fun recalculateTotalAmount() {
        originalAmount = _orderItems.sumOf { it.getSubtotal() }
        totalAmount = originalAmount - discountAmount
    }
}
