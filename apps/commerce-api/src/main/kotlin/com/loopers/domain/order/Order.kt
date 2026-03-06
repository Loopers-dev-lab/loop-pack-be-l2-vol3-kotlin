package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(
    name = "orders",
    indexes = [Index(name = "idx_orders_user_created", columnList = "user_id, created_at")],
)
class Order(
    userId: Long,
    idempotencyKey: String? = null,
    status: OrderStatus = OrderStatus.ORDERED,
) : BaseEntity() {

    @Version
    var version: Long = 0
        protected set

    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "idempotency_key", unique = true)
    var idempotencyKey: String? = idempotencyKey
        protected set

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Money = Money.ZERO
        protected set

    @Column(name = "coupon_id")
    var couponId: Long? = null
        protected set

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Money = Money.ZERO
        protected set

    @Column(name = "payment_amount", nullable = false)
    var paymentAmount: Money = Money.ZERO
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = status
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    @SQLRestriction("deleted_at IS NULL")
    private val orderItems: MutableList<OrderItem> = mutableListOf()

    val items: List<OrderItem>
        get() = orderItems.toList()

    fun addItems(items: List<OrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }
        val productIds = items.map { it.productId }
        if (productIds.size != productIds.toSet().size) {
            throw CoreException(ErrorType.BAD_REQUEST, "중복된 상품이 포함되어 있습니다.")
        }
        items.forEach { addItem(it) }
    }

    private fun addItem(item: OrderItemCommand) {
        orderItems.add(
            OrderItem(
                order = this,
                productId = item.productId,
                quantity = item.quantity,
                productName = item.productName,
                productPrice = item.productPrice,
                brandName = item.brandName,
            ),
        )
        calculateTotalAmount()
    }

    fun applyCouponDiscount(couponId: Long, discountAmount: Money) {
        this.couponId = couponId
        this.discountAmount = discountAmount
        calculatePaymentAmount()
    }

    fun changeStatus(next: OrderStatus) {
        if (!status.canTransitionTo(next)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "${status.name}에서 ${next.name}(으)로 상태를 변경할 수 없습니다.",
            )
        }
        this.status = next
    }

    private fun calculateTotalAmount() {
        this.totalAmount = orderItems.fold(Money.ZERO) { acc, item ->
            acc + item.productPrice * item.quantity
        }
        calculatePaymentAmount()
    }

    private fun calculatePaymentAmount() {
        this.paymentAmount = Money.of(totalAmount.value - discountAmount.value)
    }
}
