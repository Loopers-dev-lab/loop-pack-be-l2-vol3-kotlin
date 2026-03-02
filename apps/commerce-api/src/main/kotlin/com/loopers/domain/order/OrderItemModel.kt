package com.loopers.domain.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "tb_order_item")
class OrderItemModel(
    orderId: Long,
    brandId: Long,
    productId: Long,
    quantity: Quantity,
    unitPrice: Price,
    productName: ProductName,
    imageUrl: ImageUrl
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(nullable = false)
    var orderId: Long = orderId
        protected set

    @Column(nullable = false)
    var brandId: Long = brandId
        protected set

    @Column(nullable = false)
    var productId: Long = productId
        protected set

    @Column(nullable = false)
    var quantity: Quantity = quantity
        protected set

    @Column(nullable = false)
    var unitPrice: Price = unitPrice
        protected set

    @Column(nullable = false)
    var productName: ProductName = productName
        protected set

    @Column(nullable = false)
    var imageUrl: ImageUrl = imageUrl
        protected set

    val subTotal: Long
        get() = quantity.value * unitPrice.value
}
