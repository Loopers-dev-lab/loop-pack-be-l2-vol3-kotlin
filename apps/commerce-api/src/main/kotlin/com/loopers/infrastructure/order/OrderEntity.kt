package com.loopers.infrastructure.order

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_member_id", columnList = "member_id"),
    ],
)
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "total_price", nullable = false)
    val totalPrice: Long,

    @Column(name = "ordered_at", nullable = false)
    val orderedAt: ZonedDateTime,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    val orderItems: MutableList<OrderItemEntity> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    fun addOrderItem(item: OrderItemEntity) {
        orderItems.add(item)
        item.order = this
    }

    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
