package com.loopers.infrastructure.order

import com.loopers.support.jpa.BaseEntity
import com.loopers.domain.order.OrderStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "orders")
class OrderEntity(
    id: Long?,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: OrderStatus,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long,

    @Column(name = "ordered_at", nullable = false)
    val orderedAt: ZonedDateTime,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val items: MutableList<OrderItemEntity> = mutableListOf(),
) : BaseEntity() {
    init {
        this.id = id
    }
}
