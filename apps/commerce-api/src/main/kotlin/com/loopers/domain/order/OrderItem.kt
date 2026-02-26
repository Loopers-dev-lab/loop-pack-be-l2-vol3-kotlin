package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "order_items",
    indexes = [Index(name = "idx_order_items_order_id", columnList = "order_id")],
)
class OrderItem(
    order: Order,
    productId: Long,
    quantity: Quantity,
    productName: String,
    productPrice: Money,
    brandName: String,
) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order = order
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Quantity = quantity
        protected set

    @Column(name = "product_name", nullable = false)
    var productName: String = productName
        protected set

    @Column(name = "product_price", nullable = false)
    var productPrice: Money = productPrice
        protected set

    @Column(name = "brand_name", nullable = false)
    var brandName: String = brandName
        protected set
}
