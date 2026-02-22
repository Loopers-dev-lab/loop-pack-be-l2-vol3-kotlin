package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
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
class OrderModel(
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

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableList<OrderItemModel> = mutableListOf()
        protected set

    fun addItem(orderItem: OrderItemModel) {
        orderItems.add(orderItem)
        orderItem.order = this
    }

    fun getTotalAmount(): Long = orderItems.sumOf { it.amount }

    fun validateOwner(memberId: Long) {
        if (this.memberId != memberId) {
            throw CoreException(ErrorType.FORBIDDEN, "본인의 주문만 조회할 수 있습니다.")
        }
    }
}
