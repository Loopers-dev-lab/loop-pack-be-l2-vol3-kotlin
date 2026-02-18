package com.loopers.infrastructure.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_item")
class OrderItemEntity(
    id: Long?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: OrderEntity,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "brand_name", nullable = false, length = 100)
    val brandName: String,

    @Column(name = "price", nullable = false)
    val price: Long,

    @Column(name = "quantity", nullable = false)
    val quantity: Int,
) : BaseEntity() {
    init {
        this.id = id
    }
}
