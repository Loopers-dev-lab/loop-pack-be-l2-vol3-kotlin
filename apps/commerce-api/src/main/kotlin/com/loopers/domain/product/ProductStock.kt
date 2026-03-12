package com.loopers.domain.product

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "product_stocks")
class ProductStock private constructor(
    productId: Long,
    stock: Stock,
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long = productId

    @Embedded
    @AttributeOverride(name = "quantity", column = Column(name = "stock", nullable = false))
    var stock: Stock = stock
        protected set

    fun updateStock(stock: Stock) {
        this.stock = stock
    }

    fun decreaseStock(quantity: Int) {
        this.stock = stock.deduct(quantity)
    }

    companion object {
        fun create(productId: Long, stock: Stock): ProductStock {
            return ProductStock(productId = productId, stock = stock)
        }
    }
}
