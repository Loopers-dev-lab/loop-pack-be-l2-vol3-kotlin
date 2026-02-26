package com.loopers.domain.order

import com.loopers.domain.product.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "order_items")
class OrderItem private constructor(
    orderId: Long,
    productId: Long,
    productName: String,
    productPrice: Money,
    brandName: String,
    imageUrl: String,
    quantity: Quantity,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "order_id", nullable = false)
    val orderId: Long = orderId

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String = productName

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "product_price", nullable = false))
    val productPrice: Money = productPrice

    @Column(name = "brand_name", nullable = false, length = 50)
    val brandName: String = brandName

    @Column(name = "image_url", nullable = false)
    val imageUrl: String = imageUrl

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "quantity", nullable = false))
    val quantity: Quantity = quantity

    companion object {
        fun create(orderId: Long, snapshot: OrderItemSnapshot): OrderItem {
            return OrderItem(
                orderId = orderId,
                productId = snapshot.productId,
                productName = snapshot.productName,
                productPrice = snapshot.productPrice,
                brandName = snapshot.brandName,
                imageUrl = snapshot.imageUrl,
                quantity = snapshot.quantity,
            )
        }
    }
}
