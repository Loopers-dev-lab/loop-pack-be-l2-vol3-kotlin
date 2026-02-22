package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_item")
class OrderItemModel(
    productId: Long,
    productName: String,
    productPrice: Long,
    brandName: String,
    quantity: Int,
) : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: OrderModel? = null
        internal set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "product_name", nullable = false)
    var productName: String = productName
        protected set

    @Column(name = "product_price", nullable = false)
    var productPrice: Long = productPrice
        protected set

    @Column(name = "brand_name", nullable = false)
    var brandName: String = brandName
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(name = "amount", nullable = false)
    var amount: Long = productPrice * quantity
        protected set
}
