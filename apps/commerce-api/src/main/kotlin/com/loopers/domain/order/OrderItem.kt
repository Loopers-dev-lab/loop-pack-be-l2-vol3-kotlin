package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "order_items")
class OrderItem(
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "product_name", nullable = false)
    val productName: String,

    @Column(name = "product_price", nullable = false)
    val productPrice: Long,

    @Column(nullable = false)
    val quantity: Int,
) : BaseEntity()
